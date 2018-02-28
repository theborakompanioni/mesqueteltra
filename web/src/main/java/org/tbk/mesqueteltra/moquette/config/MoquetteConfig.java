package org.tbk.mesqueteltra.moquette.config;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
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
import org.tbk.mesqueteltra.moquette.config.ServerWithInternalPublish.InterceptHandlerWithInternalMessageSupport;
import org.tbk.mesqueteltra.moquette.config.ServerWithInternalPublish.MoquettePublishInternalBridge;
import org.tbk.mesqueteltra.moquette.ext.spi.ITopicPolicy;

import javax.net.ssl.SSLContext;
import java.util.List;
import java.util.concurrent.Executors;

import static java.util.Objects.requireNonNull;

@Configuration
public class MoquetteConfig {

    @Bean
    public MoquettePublishInternalBridge moquettePublishInternalBridge(EventBus eventBus, List<InterceptHandlerWithInternalMessageSupport> handler) {
        return new MoquettePublishInternalBridge(eventBus, handler);
    }

    @Bean
    public IResourceLoader resourceLoader() {
        return new ClasspathResourceLoader();
    }

    @Bean
    public IConfig config() {
        return new ResourceLoaderConfig(resourceLoader());
    }

    @Bean
    public IAuthorizator authorizator(List<ITopicPolicy> topicPolicies) {
        return new SimpleAuthorizator(topicPolicies);
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

    @Bean(destroyMethod = "stopServer")
    public Server moquetteServer() {
        return new ServerWithInternalPublish(googleEventBus());
    }

    @Bean
    public EventBus googleEventBus() {
        return new AsyncEventBus(Executors.newSingleThreadExecutor());
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
