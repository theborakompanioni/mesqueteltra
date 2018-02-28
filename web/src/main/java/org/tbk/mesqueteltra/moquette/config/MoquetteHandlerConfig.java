package org.tbk.mesqueteltra.moquette.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tbk.mesqueteltra.IpfsService;
import org.tbk.mesqueteltra.moquette.handler.IpfsHandler;
import org.tbk.mesqueteltra.moquette.handler.LoggingHandler;

@Configuration
public class MoquetteHandlerConfig {

    @Bean
    public LoggingHandler loggingHandler() {
        return new LoggingHandler();
    }

    @Bean
    public IpfsHandler ipfsHandler(IpfsService ipfs) {
        return new IpfsHandler(ipfs);
    }

}
