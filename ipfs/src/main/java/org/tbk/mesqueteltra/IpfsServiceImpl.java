package org.tbk.mesqueteltra;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import io.ipfs.api.IPFS;
import io.ipfs.api.MerkleNode;
import io.ipfs.api.NamedStreamable;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

import static com.google.common.io.BaseEncoding.base64Url;
import static java.util.Objects.requireNonNull;

@Slf4j
public class IpfsServiceImpl implements IpfsService {

    private final IPFS ipfs;

    public IpfsServiceImpl(IPFS ipfs) {
        this.ipfs = requireNonNull(ipfs);
    }

    @Override
    public Flux<MerkleNode> persist(String fileName, byte[] data) {
        return Flux.just(data)
                .doOnNext(val -> log.info("persist file via IPFS: {}", fileName))
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
                .doOnNext(val -> log.info("sending via IPFS: {} {}", topic, data))
                .map(msg -> {
                    try {
                        String encodedMsg = base64Url().encode(data.getBytes(Charsets.UTF_8));
                        return Optional.ofNullable(ipfs.pubsub.pub(topic, encodedMsg));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Override
    public Flux<IpfsMsg> subscribe(String topic) {
        return Flux.create(fluxSink -> {
            try {
                final Supplier<Object> sub = ipfs.pubsub.sub(topic);

                while (true) {
                    final Object o = sub.get();
                    if (fluxSink.isCancelled()) {
                        return;
                    }

                    if (o != null) {
                        if (Map.class.isAssignableFrom(o.getClass())) {
                            Map<String, ?> map = (Map<String, ?>) o;
                            Optional<IpfsMsg> msg = tryParseAsIpfsMsg(map);

                            if (msg.isPresent()) {
                                fluxSink.next(msg.get());
                            } else {
                                log.warn("Refuse to emit message - map not well formatted: {}", map);
                            }
                        } else {
                            log.warn("Refuse to emit message - object not of instance Map: {}", o);
                        }
                    }

                    if (fluxSink.isCancelled()) {
                        return;
                    }
                }
            } catch (Exception e) {
                fluxSink.error(e);
            }
        });
    }

    private Optional<IpfsMsg> tryParseAsIpfsMsg(Map<String, ?> map) {
        Object data = map.get("data");
        if (data == null) {
            return Optional.empty();
        } else if (!String.class.isAssignableFrom(data.getClass())) {
            return Optional.empty();
        } else {
            String dataAsString = (String) data;

            List<String> topics = Lists.newArrayList();
            Object topicIDs = map.get("topicIDs");
            if (topicIDs != null && Collection.class.isAssignableFrom(topicIDs.getClass())) {
                topics.addAll((Collection) topicIDs);
            }

            String dataBase64 = new String(base64Url().decode(dataAsString), Charsets.UTF_8);
            return Optional.of(IpfsMsg.builder()
                    .dataBase64(dataBase64)
                    .topicIds(topics)
                    .build());
        }
    }

}
