package org.tbk.mesqueteltra.moquette.custom.redis.mqtt;

import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Ints;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.impl.subscriptions.Topic;
import io.netty.handler.codec.mqtt.MqttQoS;

import java.util.Map;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

public final class MoreRedisSubscriptions {
    private MoreRedisSubscriptions() {
        throw new UnsupportedOperationException();
    }

    public static Map<String, String> toMap(Subscription subscription) {
        checkNotNull(subscription);

        return ImmutableMap.<String, String>builder()
                .put("s_requested_qos", String.valueOf(subscription.getRequestedQos().value()))
                .put("m_topic_filter", subscription.getTopicFilter().toString())
                .put("m_active", String.valueOf(subscription.isActive()))
                .put("m_clientID", subscription.getClientId())
                .build();
    }

    public static Subscription fromMap(Map<String, String> map) {
        checkNotNull(map);

        Topic topicFilter = Optional.ofNullable(map.get("m_topic_filter"))
                .map(Topic::new)
                .orElseThrow(() -> new IllegalArgumentException("`m_topic_filter` key not present in map"));

        String clientId = Optional.ofNullable(map.get("m_clientID"))
                .orElseThrow(() -> new IllegalArgumentException("`m_clientID` key not present in map"));

        boolean active = Optional.ofNullable(map.get("m_active"))
                .map(Boolean::valueOf)
                .orElseThrow(() -> new IllegalArgumentException("`m_active` key not present in map"));

        MqttQoS requestedQos = Optional.ofNullable(map.get("s_requested_qos"))
                .map(Ints::tryParse)
                .map(MqttQoS::valueOf)
                .orElseThrow(() -> new IllegalArgumentException("`s_requested_qos` key not present in map"));


        return new Subscription(clientId, topicFilter, requestedQos);
    }
}
