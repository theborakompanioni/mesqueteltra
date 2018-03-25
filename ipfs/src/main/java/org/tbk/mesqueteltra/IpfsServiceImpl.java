package org.tbk.mesqueteltra;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import io.ipfs.api.IPFS;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable;
import io.vertx.core.json.Json;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

@Slf4j
public class IpfsServiceImpl implements IpfsService {
    private final static BaseEncoding BASE64_URLSAFE = BaseEncoding.base64Url();

    private final IPFS ipfs;

    public IpfsServiceImpl(IPFS ipfs) {
        this.ipfs = requireNonNull(ipfs);
    }

    @Override
    public Flux<MerkleNode> persist(String fileName, byte[] data) {
        return Flux.just(data)
                .doOnNext(val -> log.debug("persist file via IPFS: {}", fileName))
                .map(msg -> new NamedStreamable.ByteArrayWrapper(fileName, msg))
                .flatMapIterable(msg -> {
                    try {
                        return ipfs.add(msg);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Override
    public Flux<MerkleNode> persist(String fileName, String data) {
        return persist(fileName, data.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Flux<Optional<Object>> publish(String topic, String data) {
        return Flux.just(data)
                .doOnNext(val -> log.debug("sending via IPFS: {} {}", topic, data))
                .map(msg -> {
                    try {
                        InternalIpfsPubSubPayload payload = new InternalIpfsPubSubPayload();
                        payload.setData(data);
                        payload.setTopic(topic);

                        String payloadJson = Json.encode(payload);

                        String encodedMsg = BASE64_URLSAFE.encode(payloadJson.getBytes(Charsets.UTF_8));

                        return Optional.ofNullable(ipfs.pubsub.pub(topic, encodedMsg));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Override
    public Flux<IpfsMsg> subscribe(String topic) {
        return subscribeToIpfs(ipfs, topic);
    }

    private static Optional<IpfsMsg> tryParseAsIpfsMsg(Map<String, ?> map) {
        Object data = map.get("data");
        if (data == null) {
            return Optional.empty();
        } else if (!String.class.isAssignableFrom(data.getClass())) {
            return Optional.empty();
        } else {
            String jsonDataAsString = (String) data;
            String jsonDataBase64 = new String(BASE64_URLSAFE.decode(jsonDataAsString), Charsets.UTF_8);
            String json = new String(Base64.getDecoder().decode(jsonDataBase64), Charsets.UTF_8);

            final InternalIpfsPubSubPayload internalIpfsPubSubPayload = Json.decodeValue(json, InternalIpfsPubSubPayload.class);

            List<String> topics = Lists.newArrayList(internalIpfsPubSubPayload.getTopic());

            Object topicIDs = map.get("topicIDs");
            if (topicIDs != null && Collection.class.isAssignableFrom(topicIDs.getClass())) {
                topics.addAll((Collection) topicIDs);
            }

            final String dataBase64 = Base64.getEncoder()
                    .encodeToString(internalIpfsPubSubPayload.getData()
                            .getBytes(Charsets.UTF_8));

            return Optional.of(IpfsMsg.builder()
                    .dataBase64(dataBase64)
                    .topicIds(topics)
                    .build());
        }
    }

    private static Flux<IpfsMsg> subscribeToIpfs(IPFS ipfs, String topic) {
        final Flux<Map<String, ?>> ipfsMessageFlux = Flux.create(fluxSink -> {
            try {
                final Supplier<CompletableFuture<Map<String, Object>>> sub = ipfs.pubsub.sub(topic);

                while (true) {
                    final CompletableFuture<Map<String, Object>> f = sub.get();
                    if (fluxSink.isCancelled()) {
                        return;
                    }

                    if (f != null) {
                        Map<String, Object> map = f.get();
                        fluxSink.next(map);
                    }

                    if (fluxSink.isCancelled()) {
                        return;
                    }
                }
            } catch (Exception e) {
                fluxSink.error(e);
            }
        });

        return ipfsMessageFlux.map(map -> {
            Optional<IpfsMsg> msg = tryParseAsIpfsMsg(map);

            if (!msg.isPresent()) {
                log.warn("Refuse to emit message - map not well formatted: {}", map);
            }
            return msg;
        })
                .filter(Optional::isPresent)
                .map(Optional::get)
                .retry(e -> {
                    log.warn("Error occurred while subscribing to all topics: {}", e.getMessage());
                    return true;
                })
                .repeat(() -> {
                    log.info("Will be repeating subscribing to all topics");
                    return true;
                });
    }

    @Data
    @NoArgsConstructor
    public static class InternalIpfsPubSubPayload {
        private String data;
        private String topic;
    }

}
