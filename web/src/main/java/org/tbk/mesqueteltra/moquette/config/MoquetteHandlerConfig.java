package org.tbk.mesqueteltra.moquette.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.tbk.mesqueteltra.IpfsConfig;
import org.tbk.mesqueteltra.IpfsService;
import org.tbk.mesqueteltra.moquette.config.ServerWithInternalPublish.InterceptHandlerWithInternalMessageSupport;
import org.tbk.mesqueteltra.moquette.handler.IpfsHandler;
import org.tbk.mesqueteltra.moquette.handler.LoggingHandler;
import org.tbk.mesqueteltra.moquette.handler.NoopHandler;

import java.util.Optional;

@Configuration
@Import(IpfsConfig.class)
public class MoquetteHandlerConfig {
    @Bean
    public LoggingHandler loggingHandler() {
        return new LoggingHandler();
    }

    @Bean
    public InterceptHandlerWithInternalMessageSupport ipfsHandler(Optional<IpfsService> ipfs) {
        return ipfs.<InterceptHandlerWithInternalMessageSupport>map(IpfsHandler::new)
                .orElseGet(this::noopHandler);
    }

    /**
     * Quick hack to be able to autowire List of InterceptHandler
     * avoiding a NoSuchBeanDefinitionException.
     *
     * @return a handler doing nothing
     */
    @Bean
    public NoopHandler noopHandler() {
        return new NoopHandler();
    }

}
