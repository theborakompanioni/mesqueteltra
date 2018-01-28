package org.tbk.mesqueteltra.moquette.custom.redis.mqtt;

import io.moquette.spi.IMatchingCondition;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.impl.subscriptions.Topic;
import lombok.extern.slf4j.Slf4j;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

@Slf4j
public class PooledRedisMessageStore implements IMessagesStore {

    private static final RedisKeys.MqttKey STORED_MESSAGE_KEY = RedisKeys.storedMessage();

    private final JedisPool jedisPool;

    public PooledRedisMessageStore(JedisPool jedisPool) {
        this.jedisPool = checkNotNull(jedisPool);
    }

    @Override
    public void initStore() {

    }

    @Override
    public Collection<StoredMessage> searchMatching(IMatchingCondition iMatchingCondition) {
        String redisHashRegex = STORED_MESSAGE_KEY.wildcard().name();

        Jedis resource = jedisPool.getResource();
        Set<String> keys = resource.keys(redisHashRegex);

        log.debug("REDIS searchMatching '{}' found {} key(s)", redisHashRegex, keys.size());

        Set<Topic> matchingTopics = keys.stream()
                .map(STORED_MESSAGE_KEY::toTopic)
                .filter(iMatchingCondition::match)
                .collect(Collectors.toSet());

        List<StoredMessage> matchingMessages = matchingTopics.stream()
                .map(STORED_MESSAGE_KEY::append)
                .map(RedisKeys.Key::name)
                .map(resource::hgetAll)
                .map(MoreRedisStoredMessages::fromMap)
                .collect(toImmutableList());

        return matchingMessages;
    }

    @Override
    public void cleanRetained(Topic topic) {
        String redisHash = STORED_MESSAGE_KEY.append(topic).name();

        Jedis resource = jedisPool.getResource();
        Set<String> keys = resource.keys(redisHash);

        log.debug("REDIS clearRetained '{}' found {} key(s)", redisHash, keys.size());

        if (!keys.isEmpty()) {
            resource.del(keys.toArray(new String[keys.size()]));
        }
    }

    @Override
    public void storeRetained(Topic topic, StoredMessage storedMessage) {
        log.trace("Store retained message for topic={}, clientId={}", topic, storedMessage.getClientID());

        if (storedMessage.getClientID() == null) {
            throw new IllegalArgumentException("Message to be persisted must have a not null client ID");
        }

        String redisHash = STORED_MESSAGE_KEY.append(topic).name();

        log.debug("REDIS storeRetained '{}': topic={}, clientId={}", redisHash,
                storedMessage.getTopic(), storedMessage.getClientID());

        Map<String, String> storedMessageAsMap = MoreRedisStoredMessages.toMap(storedMessage);

        Jedis resource = jedisPool.getResource();
        try (Pipeline pipelined = resource.pipelined()) {
            storedMessageAsMap.forEach((key, val) -> pipelined.hset(redisHash, key, val));
            pipelined.sync();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
