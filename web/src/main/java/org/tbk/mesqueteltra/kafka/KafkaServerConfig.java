package org.tbk.mesqueteltra.kafka;

import kafka.server.KafkaConfig;
import kafka.server.KafkaServerStartable;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

@Slf4j
@Configuration
public class KafkaServerConfig {

    @Bean
    public KafkaServerStartable kafkaServerStartable(KafkaConfig kafkaConfig) {
        return new KafkaServerStartable(kafkaConfig);
    }

    @Bean
    public KafkaConfig kafkaSedrverStartable(Properties kafkaProperties) {
        boolean enableLogging = true;
        return new KafkaConfig(kafkaProperties, enableLogging);
    }

    @Bean
    public Properties kafkaProperties(ServerConfig config) {
        Properties props = new Properties();
        props.put("zookeeper.connect", String.format("0.0.0.0:%d", config.getClientPortAddress().getPort()));
        props.put("broker.id", "1");
        props.put("log.dirs", "./.~kafka/logs");
        props.put("offsets.topic.replication.factor", "1");
        props.put("max.poll.records", "100");
        props.put("max.poll.interval.ms", TimeUnit.SECONDS.toMillis(10));
        props.put("session.timeout.ms", TimeUnit.SECONDS.toMillis(30));
        return props;
    }

    @Bean
    public ZooKeeperServerMain zooKeeperServer(ServerConfig config) {
        ZooKeeperServerMain zooKeeperServerMain = new ZooKeeperServerMain();

        Executors.newSingleThreadExecutor()
                .execute(() -> {
                    try {
                        zooKeeperServerMain.runFromConfig(config);
                    } catch (IOException e) {
                        log.error("", e);
                        throw new RuntimeException(e);
                    }
                });

        return zooKeeperServerMain;
    }

    @Bean
    public ServerConfig zooKeeperServerConfig() {
        ServerConfig config = new ServerConfig();
        int port = 2999;
        String dataDirectory = "./.~zookeeper";
        config.parse(new String[]{
                String.valueOf(port),
                dataDirectory
        });
        return config;
    }

    @Bean
    public EmbeddedKafkaInitializer KafkaServerInitializer(KafkaServerStartable kafkaServer) {
        return new EmbeddedKafkaInitializer(kafkaServer);
    }

    public static class EmbeddedKafkaInitializer implements InitializingBean, DisposableBean {
        private final KafkaServerStartable kafkaServer;

        public EmbeddedKafkaInitializer(KafkaServerStartable kafkaServer) {
            this.kafkaServer = requireNonNull(kafkaServer);
        }

        @Override
        public void afterPropertiesSet() throws Exception {
            this.kafkaServer.startup();
        }

        @Override
        public void destroy() throws Exception {
            this.kafkaServer.awaitShutdown();
            this.kafkaServer.shutdown();
        }

    }
}
