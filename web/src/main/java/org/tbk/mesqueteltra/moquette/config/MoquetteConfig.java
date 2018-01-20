package org.tbk.mesqueteltra.moquette.config;

import io.moquette.interception.InterceptHandler;
import io.moquette.server.Server;
import io.moquette.server.config.ClasspathResourceLoader;
import io.moquette.server.config.IConfig;
import io.moquette.server.config.IResourceLoader;
import io.moquette.server.config.ResourceLoaderConfig;
import io.moquette.spi.security.IAuthenticator;
import io.moquette.spi.security.IAuthorizator;
import io.moquette.spi.security.ISslContextCreator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.tbk.mesqueteltra.moquette.SimpleAuthenticator;
import org.tbk.mesqueteltra.moquette.SimpleAuthorizator;

import javax.net.ssl.SSLContext;
import java.util.List;

import static java.util.Objects.requireNonNull;

@Configuration
public class MoquetteConfig {

    @Bean
    public IResourceLoader resourceLoader() {
        return new ClasspathResourceLoader();
    }

    @Bean
    public IConfig config() {
        return new ResourceLoaderConfig(resourceLoader());
    }

    @Bean
    public IAuthorizator authorizator() {
        return new SimpleAuthorizator();
    }

    @Bean
    public IAuthenticator authenticator() {
        return new SimpleAuthenticator();
    }

    @Bean
    public ISslContextCreator sslContextCreator() {
        return new ISslContextCreator() {
            @Override
            public SSLContext initSSLContext() {
                return null; // disable ssl atm
            }
        };
    }

    @Bean
    public Server moquetteServer() {
        return new Server();
    }

    @Bean
    public MoquetteServerInitializingBean moquetteServerInitializer(Server server,
                                                                    IConfig config,
                                                                    List<? extends InterceptHandler> handlers,
                                                                    ISslContextCreator sslContextCreator,
                                                                    IAuthenticator authenticator,
                                                                    IAuthorizator authorizator) {
        return new MoquetteServerInitializingBean(server, config, handlers, sslContextCreator, authenticator, authorizator);
    }

    @Slf4j
    public static class MoquetteServerInitializingBean implements InitializingBean, DisposableBean {
        private final Server moquetteServer;
        private final IConfig config;
        private final List<? extends InterceptHandler> handlers;
        private ISslContextCreator sslContextCreator;
        private final IAuthenticator authenticator;
        private final IAuthorizator authorizator;

        public MoquetteServerInitializingBean(Server moquetteServer,
                                              IConfig config,
                                              List<? extends InterceptHandler> handlers,
                                              ISslContextCreator sslContextCreator,
                                              IAuthenticator authenticator,
                                              IAuthorizator authorizator) {
            this.moquetteServer = requireNonNull(moquetteServer);
            this.config = requireNonNull(config);
            this.handlers = requireNonNull(handlers);
            this.sslContextCreator = requireNonNull(sslContextCreator);
            this.authenticator = requireNonNull(authenticator);
            this.authorizator = requireNonNull(authorizator);
        }

        @Override
        public void afterPropertiesSet() throws Exception {
            log.info("Starting MQTT Server");

            moquetteServer.startServer(config, handlers, sslContextCreator, authenticator, authorizator);

            log.info("MQTT Server started");
        }

        @Override
        public void destroy() {
            log.info("Stopping MQTT Server");

            moquetteServer.stopServer();

            log.info("MQTT Server stopped");
        }
    }
}
