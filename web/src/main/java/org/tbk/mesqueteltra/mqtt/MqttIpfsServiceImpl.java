package org.tbk.mesqueteltra.mqtt;

import com.google.common.base.Charsets;
import com.google.common.base.Predicate;
import com.google.common.util.concurrent.RateLimiter;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.json.Json;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.tbk.mesqueteltra.IpfsService;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

@Slf4j
public class MqttIpfsServiceImpl implements Mqttable {

    // we do not want to spam the network - its a demo!
    private final RateLimiter rateLimiter = RateLimiter.create(100);

    public static final String TOPIC_SUBSTITUTE = "mqtt-to-ipfs";

    private final IpfsService ipfs;
    private final Flux<IpfsService.IpfsMsg> publish;
    private final UUID uuid;

    public MqttIpfsServiceImpl(UUID uuid, IpfsService ipfsService) {
        this.uuid = requireNonNull(uuid);
        this.ipfs = requireNonNull(ipfsService);

        this.publish = ipfs.subscribe(TOPIC_SUBSTITUTE)
                .subscribeOn(Schedulers.elastic())
                .publish()
                .autoConnect();
    }

    @Override
    public UUID getUuid() {
        return uuid;
    }

    @Override
    public Flux<Boolean> publish(MqttPublishMessage message, String clientId) {
        requireNonNull(message);
        requireNonNull(clientId);

        return publishToIpfs(message, clientId)
                .subscribeOn(Schedulers.elastic())
                .map(foo -> true);
    }

    @Override
    public Flux<MqttMessageWithClientId> subscribe(String topic) {
        requireNonNull(topic);

        return subscribeToAll(msg -> {
            if (msg.getTopicIds().contains(topic)) {
                return true;
            } else {
                log.debug("Drop message because of mismatching topic : {} not in {}", msg.getTopicIds());
                return false;
            }
        });
    }

    @Override
    public Flux<MqttMessageWithClientId> subscribeToAll() {
        return subscribeToAll(msg -> true);
    }

    private Flux<MqttMessageWithClientId> subscribeToAll(Predicate<IpfsService.IpfsMsg> predicate) {
        return publish
                .filter(predicate)
                .map(msg -> {
                    String json = msg.getDataAsString();
                    IpfsMqttDto ipfsMqttDto = Json.decodeValue(json, IpfsMqttDto.class);
                    return ipfsMqttDto;
                })
                .filter(dto -> {
                    if (uuid.toString().equals(dto.getServerId())) {
                        log.debug("Refrain from emitting msg published via IPFS: coming from this instance");
                        return false;
                    }
                    return true;
                })
                .doOnNext(dto -> {
                    log.debug("Message arrived via IPFS on topic {}: {}", dto.getTopic(), dto.getContent());
                })
                .map(ipfsMqttDto -> {
                    MqttPublishMessage mqttMessage = MqttMessageBuilders.publish()
                            .topicName(ipfsMqttDto.getTopic())
                            .retained(ipfsMqttDto.isRetained())
                            .qos(MqttQoS.valueOf(ipfsMqttDto.getQos()))
                            .payload(Unpooled.copiedBuffer(ipfsMqttDto.getContent().getBytes(Charsets.UTF_8)))
                            .build();

                    return MqttMessageWithClientId.builder()
                            .clientId(ipfsMqttDto.getClientId())
                            .message(mqttMessage)
                            .build();
                });
    }

    private Flux<Optional<Object>> publishToIpfs(MqttPublishMessage msg, String clientId) {
        final String content = msg.content().toString(Charsets.UTF_8);

        IpfsMqttDto dto = new IpfsMqttDto();
        dto.setClientId(clientId);
        dto.setTopic(msg.variableHeader().topicName());
        dto.setContent(content);
        dto.setQos(msg.fixedHeader().qosLevel().value());
        dto.setRetained(msg.fixedHeader().isRetain());
        dto.setDuplicate(msg.fixedHeader().isDup());

        return publishToIpfs(dto);
    }


    private Flux<Optional<Object>> publishToIpfs(IpfsMqttDto dto) {
        if (!rateLimiter.tryAcquire()) {
            log.warn("Could not acquire RateLimiter to Protect Message Flood in DEMO mode (rate:={})", rateLimiter.getRate());
            return Flux.empty();
        } else {
            dto.setServerId(uuid.toString());

            String json = Json.encode(dto);

            log.debug("Publishing via IPFS on topic {}: {}", dto.getTopic(), dto.getContent());

            return ipfs.publish(TOPIC_SUBSTITUTE, json);
        }
    }

    @Data
    public static class IpfsMqttDto {
        private String serverId;
        private String content;
        private String topic;
        private String clientId;
        private int qos;
        private boolean retained;
        private boolean duplicate;
    }
}
