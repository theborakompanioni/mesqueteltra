package org.tbk.mesqueteltra.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;

import static java.util.Objects.requireNonNull;

@Slf4j
@Configuration
@ConditionalOnProperty(value = "mesqueteltra.redis.enabled", havingValue = "true")
@EnableRedisRepositories
public class RedisSpringConfig {

    private final RedisProperties redisProperties;

    @Autowired
    public RedisSpringConfig(RedisProperties redisProperties) {
        this.redisProperties = requireNonNull(redisProperties);
    }
}
