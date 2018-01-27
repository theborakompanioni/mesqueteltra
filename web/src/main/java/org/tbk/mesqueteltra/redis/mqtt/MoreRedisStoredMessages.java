package org.tbk.mesqueteltra.redis.mqtt;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Ints;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.impl.subscriptions.Topic;
import io.netty.handler.codec.mqtt.MqttQoS;

import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public final class MoreRedisStoredMessages {
    private MoreRedisStoredMessages() {
        throw new UnsupportedOperationException();
    }

    public static Map<String, String> toMap(IMessagesStore.StoredMessage storedMessage) {
        checkNotNull(storedMessage);

        return ImmutableMap.<String, String>builder()
                .put("m_qos", String.valueOf(storedMessage.getQos().value()))
                .put("m_payload", storedMessage.getPayload().toString(Charsets.UTF_8))
                .put("m_topic", storedMessage.getTopic())
                .put("m_retained", String.valueOf(storedMessage.isRetained()))
                .put("m_clientID", storedMessage.getClientID())
                .build();
    }

    public static IMessagesStore.StoredMessage fromMap(Map<String, String> map) {
        checkNotNull(map);

        byte[] payload = Optional.ofNullable(map.get("m_payload"))
                .map(val -> val.getBytes(Charsets.UTF_8))
                .orElseThrow(() -> new IllegalArgumentException("`m_payload` key not present in map"));

        String topic = Optional.ofNullable(map.get("m_topic"))
                .orElseThrow(() -> new IllegalArgumentException("`m_topic` key not present in map"));

        String clientId = Optional.ofNullable(map.get("m_clientID"))
                .orElseThrow(() -> new IllegalArgumentException("`m_clientID` key not present in map"));

        boolean retained = Optional.ofNullable(map.get("m_retained"))
                .map(Boolean::valueOf)
                .orElseThrow(() -> new IllegalArgumentException("`m_retained` key not present in map"));

        MqttQoS qos = Optional.ofNullable(map.get("m_qos"))
                .map(Ints::tryParse)
                .map(MqttQoS::valueOf)
                .orElseThrow(() -> new IllegalArgumentException("`m_qos` key not present in map"));

        IMessagesStore.StoredMessage storedMessage = new IMessagesStore.StoredMessage(payload, qos, topic);
        storedMessage.setClientID(clientId);
        storedMessage.setRetained(retained);

        return storedMessage;
    }
}
