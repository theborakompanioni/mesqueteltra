package org.tbk.mesqueteltra.moquette.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tbk.mesqueteltra.moquette.handler.LoggingHandler;

@Configuration
public class MoquetteHandlerConfig {

    @Bean
    public LoggingHandler LoggingHandler() {
        return new LoggingHandler();
    }

}
