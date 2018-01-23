package org.tbk.mesqueteltra.redis.mqtt;

import io.moquette.persistence.MemorySessionStore;
import io.moquette.server.config.IConfig;
import io.moquette.spi.IMessagesStore;
import io.moquette.spi.ISessionsStore;
import io.moquette.spi.IStore;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.concurrent.ScheduledExecutorService;

public class RedisStorageService implements IStore {

    private final RedisMessageStore messagesStore;
    private final ISessionsStore sessionsStore;

    public RedisStorageService(IConfig props, ScheduledExecutorService scheduler) {
        String host = props.getProperty("mesqueteltra_jedis_host", "localhost");
        int port = Integer.valueOf(props.getProperty("mesqueteltra_jedis_port", "9092"));

        //JedisPool jedisPool = createJedisPool(host, port);
        Jedis jedis = new Jedis(host, port);
        this.messagesStore = new RedisMessageStore(jedis);
        this.sessionsStore = new MemorySessionStore();
    }

    private JedisPool createJedisPool(String host, int port) {
        GenericObjectPoolConfig jedisPoolConfig = new GenericObjectPoolConfig();
        jedisPoolConfig.setMinIdle(10);
        jedisPoolConfig.setMaxIdle(15);
        jedisPoolConfig.setMaxTotal(20);

        return new JedisPool(host, port);
    }

    @Override
    public void initStore() {
        this.messagesStore.initStore();
        this.sessionsStore.initStore();
    }

    @Override
    public void close() {
    }

    @Override
    public IMessagesStore messagesStore() {
        return messagesStore;
    }

    @Override
    public ISessionsStore sessionsStore() {
        return sessionsStore;
    }
}
