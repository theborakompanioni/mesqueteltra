package org.tbk.mesqueteltra.moquette.custom.kafka.mqtt;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.tbk.mesqueteltra.moquette.custom.kafka.mqtt.handler.KafkaHandler;

@Configuration
@ConditionalOnProperty(value = "mesqueteltra.kafka.enabled", havingValue = "true")
public class KafkaMoquetteHandlerConfig {

    @Bean
    public KafkaHandler kafkaHandler(KafkaTemplate<String, String> kafkaTemplate) {
        return new KafkaHandler(kafkaTemplate);
    }

}
