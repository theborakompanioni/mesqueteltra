package org.tbk.mesqueteltra;

import io.moquette.server.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.kafka.core.KafkaTemplate;
import org.tbk.mesqueteltra.client.KafkaVerticle;
import org.tbk.mesqueteltra.client.MqttTimeVerticle;

import static java.util.Objects.requireNonNull;

@Configuration
@EnableConfigurationProperties(ApplicationProperties.class)
class ApplicationConfig {

    private final Environment environment;
    private final ApplicationProperties applicationProperties;

    @Autowired
    public ApplicationConfig(Environment environment, ApplicationProperties applicationProperties) {
        this.environment = requireNonNull(environment);
        this.applicationProperties = requireNonNull(applicationProperties);
    }

    @Bean
    @Profile("dev")
    public MqttTimeVerticle mqttTimeVerticle(Server server) {
        return new MqttTimeVerticle(server);
    }

    @Bean
    public KafkaVerticle kafkaVerticle(KafkaTemplate<String, String> kafkaTemplate) {
        return new KafkaVerticle(kafkaTemplate);
    }
}
