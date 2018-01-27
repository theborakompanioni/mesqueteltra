package org.tbk.mesqueteltra.redis.mqtt;

import io.moquette.spi.IMatchingCondition;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.impl.subscriptions.Topic;
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
    private static final RedisKeys.MqttKey STORED_MESSAGE_KEY = RedisKeys.storedMessage();

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
        String redisHashRegex = STORED_MESSAGE_KEY.wildcard().name();

        Set<String> keys = jedis.keys(redisHashRegex);

        log.debug("REDIS searchMatching '{}' found {} key(s)", redisHashRegex, keys.size());

        Set<Topic> matchingTopics = keys.stream()
                .map(STORED_MESSAGE_KEY::toTopic)
                .filter(iMatchingCondition::match)
                .collect(Collectors.toSet());

        List<StoredMessage> matchingMessages = matchingTopics.stream()
                .map(STORED_MESSAGE_KEY::append)
                .map(RedisKeys.Key::name)
                .map(jedis::hgetAll)
                .map(MoreRedisStoredMessages::fromMap)
                .collect(toImmutableList());

        return matchingMessages;
    }

    @Override
    public void cleanRetained(Topic topic) {
        String redisHash = STORED_MESSAGE_KEY.append(topic).name();

        Set<String> keys = jedis.keys(redisHash);

        log.debug("REDIS clearRetained '{}' found {} key(s)", redisHash, keys.size());

        if (!keys.isEmpty()) {
            jedis.del(keys.toArray(new String[keys.size()]));
        }
    }

    @Override
    public void storeRetained(Topic topic, StoredMessage storedMessage) {
        log.trace("About to store retained message for topic={}, clientId={}", topic, storedMessage.getClientID());

        if (storedMessage.getClientID() == null) {
            throw new IllegalArgumentException("Message to be persisted must have a not null client ID");
        }

        String redisHash = STORED_MESSAGE_KEY.append(topic).name();

        log.debug("REDIS storeRetained '{}': topic={}, clientId={}", redisHash,
                storedMessage.getTopic(), storedMessage.getClientID());

        Map<String, String> storedMessageAsMap = MoreRedisStoredMessages.toMap(storedMessage);

        storedMessageAsMap.forEach((key, val) -> jedis.hset(redisHash, key, val));
    }
}
