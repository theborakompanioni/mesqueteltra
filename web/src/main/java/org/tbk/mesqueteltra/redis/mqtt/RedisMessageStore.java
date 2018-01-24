package org.tbk.mesqueteltra.redis.mqtt;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableMap;
import io.moquette.spi.IMatchingCondition;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.impl.subscriptions.Topic;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

@Slf4j
public class RedisMessageStore implements IMessagesStore {

    private final Jedis jedis;

    public RedisMessageStore(Jedis jedis) {
        this.jedis = checkNotNull(jedis);
    }

    @Override
    public void initStore() {
        // empty on purpose
    }

    @Override
    public Collection<StoredMessage> searchMatching(IMatchingCondition iMatchingCondition) {
        String redisHashRegex = topicNameToRedisKey("*");

        Set<String> keys = jedis.keys(redisHashRegex);

        log.debug("REDIS searchMatching '{}' found {} key(s)", redisHashRegex, keys.size());

        Set<Topic> matchingTopics = keys.stream()
                .map(this::redisKeyToTopic)
                .filter(iMatchingCondition::match)
                .collect(Collectors.toSet());

        List<StoredMessage> matchingMessages = matchingTopics.stream()
                .map(this::topicToRedisKey)
                .map(jedis::hgetAll)
                .map(this::fromMap)
                .collect(toImmutableList());

        return matchingMessages;
    }

    @Override
    public void cleanRetained(Topic topic) {
        String redisHash = topicToRedisKey(topic);

        Set<String> keys = jedis.keys(redisHash);

        log.debug("REDIS clearRetained '{}' found {} key(s)", redisHash, keys.size());

        if (!keys.isEmpty()) {
            jedis.del(keys.toArray(new String[keys.size()]));
        }
    }

    @Override
    public void storeRetained(Topic topic, StoredMessage storedMessage) {
        log.trace("About to store retained message for topic={}, CId={}", topic, storedMessage.getClientID());

        if (storedMessage.getClientID() == null) {
            throw new IllegalArgumentException("Message to be persisted must have a not null client ID");
        }

        String redisHash = topicToRedisKey(topic);

        log.debug("REDIS storeRetained '{}': topic={}, clientId={}", redisHash,
                storedMessage.getTopic(), storedMessage.getClientID());

        Map<String, String> storedMessageAsMap = toMap(storedMessage);

        storedMessageAsMap.forEach((key, val) -> jedis.hset(redisHash, key, val));
    }

    private Map<String, String> toMap(StoredMessage storedMessage) {
        return ImmutableMap.<String, String>builder()
                .put("m_qos", String.valueOf(storedMessage.getQos().value()))
                .put("m_payload", storedMessage.getPayload().toString(Charsets.UTF_8))
                .put("m_topic", storedMessage.getTopic())
                .put("m_retained", String.valueOf(storedMessage.isRetained()))
                .put("m_clientID", storedMessage.getClientID())
                .build();
    }

    private StoredMessage fromMap(Map<String, String> map) {
        MqttQoS qos = MqttQoS.valueOf(Integer.valueOf(map.get("m_qos")));
        byte[] payload = map.get("m_payload").getBytes(Charsets.UTF_8);
        String topic = map.get("m_topic");
        String clientId = map.get("m_clientID");
        boolean retained = Boolean.valueOf(map.get("m_retained"));

        StoredMessage storedMessage = new StoredMessage(payload, qos, topic);
        storedMessage.setClientID(clientId);
        storedMessage.setRetained(retained);
        return storedMessage;
    }

    private String topicToRedisKey(Topic topic) {
        String topicName = topic.toString();
        return topicNameToRedisKey(topicName);
    }

    private Topic redisKeyToTopic(String redisKey) {
        String topicName = redisKey.replace("mqtt:message_store:", "");
        return new Topic(topicName);
    }

    private String topicNameToRedisKey(String topicName) {
        return String.format("mqtt:message_store:%s", topicName);
    }
}
