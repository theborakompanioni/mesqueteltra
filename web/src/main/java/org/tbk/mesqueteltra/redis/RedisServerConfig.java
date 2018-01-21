package org.tbk.mesqueteltra.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.embedded.RedisServer;

import static java.util.Objects.requireNonNull;

@Slf4j
@Configuration
@EnableConfigurationProperties({
        RedisProperties.class
})
@ConditionalOnProperty(value = "mesqueteltra.redis.enabled", havingValue = "true")
public class RedisServerConfig {

    private final RedisProperties redisProperties;

    @Autowired
    public RedisServerConfig(RedisProperties redisProperties) {
        this.redisProperties = requireNonNull(redisProperties);
    }

    @Bean
    public RedisServer redisServer() {
        return RedisServer.builder()
                .port(redisProperties.getPort())
                .build();
    }

    @Bean
    public EmbeddedRedisInitializer embeddedRedisInitializer(RedisServer redisServer) {
        return new EmbeddedRedisInitializer(redisServer);
    }

    public static class EmbeddedRedisInitializer implements InitializingBean, DisposableBean {
        private final RedisServer redisServer;

        public EmbeddedRedisInitializer(RedisServer redisServer) {
            this.redisServer = requireNonNull(redisServer);
        }

        @Override
        public void afterPropertiesSet() throws Exception {
            this.redisServer.start();
        }

        @Override
        public void destroy() throws Exception {
            this.redisServer.stop();
        }

    }
}
