package org.tbk.mesqueteltra.paho;

import com.google.common.base.Charsets;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Configuration
@EnableConfigurationProperties(PahoClientProperties.class)
@ConditionalOnProperty(value = "mesqueteltra.mqtt.client.enabled", havingValue = "true")
public class PahoConfig {

    private final PahoClientProperties pahoClientProperties;

    @Autowired
    public PahoConfig(PahoClientProperties pahoClientProperties) {
        this.pahoClientProperties = requireNonNull(pahoClientProperties);
    }

    @Bean(autowire = Autowire.BY_TYPE)
    public PahoClientSubscribeExample pahoClientSubscribeExample() {
        return new PahoClientSubscribeExample();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public MqttClientPersistence mqttClientPersistence() {
        return new MemoryPersistence();
    }

    @Bean(destroyMethod = "disconnectForcibly")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public MqttClient mqttClient(MqttClientPersistence persistence) throws MqttException {
        String clientId = String.format("%s-%s",
                pahoClientProperties.getClientName(), UUID.randomUUID());
        return new MqttClient(pahoClientProperties.getBroker(), clientId, persistence);
    }

    @Bean
    public MqttConnectOptions mqttConnectOptions() {
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        connOpts.setWill("/v",
                "goodbye".getBytes(Charsets.UTF_8),
                1,
                false);

        boolean useCredentials = isNotBlank(pahoClientProperties.getUser())
                && isNotBlank(pahoClientProperties.getPassword());

        if (useCredentials) {
            connOpts.setUserName(pahoClientProperties.getUser());
            connOpts.setPassword(pahoClientProperties.getPassword().toCharArray());
        }

        return connOpts;
    }


}
