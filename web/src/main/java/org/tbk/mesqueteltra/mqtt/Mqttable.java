package org.tbk.mesqueteltra.mqtt;

import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.Builder;
import lombok.Value;
import reactor.core.publisher.Flux;

import java.util.UUID;

public interface Mqttable {
    UUID getUuid();

    Flux<Boolean> publish(MqttPublishMessage data, String clientId);

    Flux<MqttMessageWithClientId> subscribe(String topic);

    Flux<MqttMessageWithClientId> subscribeToAll();

    @Value
    @Builder
    class MqttMessageWithClientId {
        private String clientId;
        private MqttPublishMessage message;
    }

}
