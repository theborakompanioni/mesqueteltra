package org.tbk.mesqueteltra.moquette.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.tbk.mesqueteltra.moquette.handler.KafkaHandler;
import org.tbk.mesqueteltra.moquette.handler.LoggingHandler;

@Configuration
public class MoquetteHandlerConfig {

    @Bean
    public LoggingHandler loggingHandler() {
        return new LoggingHandler();
    }

    @Bean
    public KafkaHandler kafkaHandler(KafkaTemplate<String, String> kafkaTemplate) {
        return new KafkaHandler(kafkaTemplate);
    }

}
