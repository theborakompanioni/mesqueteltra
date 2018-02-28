package org.tbk.mesqueteltra;

import io.moquette.server.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.tbk.mesqueteltra.moquette.custom.client.MqttTimeVerticle;

import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

@Configuration
@Import(IpfsConfig.class)
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
    //@Profile("dev")
    public MqttTimeVerticle mqttTimeVerticle(Server server) {
        return new MqttTimeVerticle(server, TimeUnit.SECONDS, 10);
    }
}
