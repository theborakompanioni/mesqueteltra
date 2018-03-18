package org.tbk.mesqueteltra.moquette.config;

import com.google.common.eventbus.AsyncEventBus;
import com.google.common.eventbus.EventBus;
import io.moquette.interception.InterceptHandler;
import io.moquette.server.Server;
import io.moquette.server.config.IConfig;
import io.moquette.server.config.MemoryConfig;
import io.moquette.spi.security.IAuthenticator;
import io.moquette.spi.security.IAuthorizator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.tbk.mesqueteltra.moquette.SimpleAuthenticator;
import org.tbk.mesqueteltra.moquette.SimpleAuthorizator;
import org.tbk.mesqueteltra.moquette.config.MoquetteProperties.MoquetteSslProperties;
import org.tbk.mesqueteltra.moquette.config.ServerWithInternalPublish.InterceptHandlerWithInternalMessageSupport;
import org.tbk.mesqueteltra.moquette.config.ServerWithInternalPublish.MoquettePublishInternalBridge;
import org.tbk.mesqueteltra.moquette.ext.spi.ITopicPolicy;

import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.Executors;

import static java.util.Objects.requireNonNull;

@Configuration
@EnableConfigurationProperties({
        MoquetteProperties.class
})

public class MoquetteConfig {

    @Bean
    public MoquettePublishInternalBridge moquettePublishInternalBridge(EventBus eventBus, List<InterceptHandlerWithInternalMessageSupport> handler) {
        return new MoquettePublishInternalBridge(eventBus, handler);
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
    public EventBus googleEventBus() {
        return new AsyncEventBus(Executors.newSingleThreadExecutor());
    }


    @Bean("serverOne")
    @Primary
    public Server serverOne() {
        return moquetteServer();
    }

    @Bean("serverTwo")
    public Server serverTwo() {
        return moquetteServer();
    }

    @Bean
    public MoquetteServerInitializingBean moquetteServerOneInitializer(MoquetteProperties moquetteProperties,
                                                                       List<? extends InterceptHandler> handlers,
                                                                       IAuthenticator authenticator,
                                                                       IAuthorizator authorizator) {
        Server server = serverOne();
        IConfig config = configServerOne(moquetteProperties);
        return new MoquetteServerInitializingBean(server, config, handlers, authenticator, authorizator);
    }

    @Bean
    public MoquetteServerInitializingBean moquetteServerTwoInitializer(MoquetteProperties moquetteProperties,
                                                                       List<? extends InterceptHandler> handlers,
                                                                       IAuthenticator authenticator,
                                                                       IAuthorizator authorizator) {
        Server server = serverTwo();
        IConfig config = configServerTwo(moquetteProperties);
        return new MoquetteServerInitializingBean(server, config, handlers, authenticator, authorizator);
    }

    private Server moquetteServer() {
        return new ServerWithInternalPublish(googleEventBus());
    }

    private IConfig configServerTwo(MoquetteProperties moquetteProperties) {
        Properties properties = new Properties();
        properties.setProperty("port", String.valueOf(moquetteProperties.getPort() + 10));
        properties.setProperty("websocket_port", String.valueOf(moquetteProperties.getWebsocketPort() + 10));
        properties.setProperty("allow_anonymous", String.valueOf(moquetteProperties.isAllowAnonymous()));
        properties.setProperty("host", moquetteProperties.getHost());

        final Optional<MoquetteSslProperties> sslOptional = moquetteProperties.getSsl();
        if (sslOptional.isPresent()) {
            MoquetteSslProperties ssl = sslOptional.get();
            properties.setProperty("ssl_port", String.valueOf(ssl.getPort() + 10));
            properties.setProperty("jks_path", ssl.getJksPath());
            properties.setProperty("key_store_password", ssl.getKeyStorePassword());
            properties.setProperty("key_manager_password", ssl.getKeyManagerPassword());
        }

        return new MemoryConfig(properties);
    }

    private IConfig configServerOne(MoquetteProperties moquetteProperties) {
        Properties properties = new Properties();
        properties.setProperty("port", String.valueOf(moquetteProperties.getPort()));
        properties.setProperty("websocket_port", String.valueOf(moquetteProperties.getWebsocketPort()));
        properties.setProperty("allow_anonymous", String.valueOf(moquetteProperties.isAllowAnonymous()));
        properties.setProperty("host", moquetteProperties.getHost());

        final Optional<MoquetteSslProperties> sslOptional = moquetteProperties.getSsl();
        if (sslOptional.isPresent()) {
            MoquetteSslProperties ssl = sslOptional.get();
            properties.setProperty("ssl_port", String.valueOf(ssl.getPort()));
            properties.setProperty("jks_path", ssl.getJksPath());
            properties.setProperty("key_store_password", ssl.getKeyStorePassword());
            properties.setProperty("key_manager_password", ssl.getKeyManagerPassword());
        }

        return new MemoryConfig(properties);
    }

    @Slf4j
    public static class MoquetteServerInitializingBean implements InitializingBean, DisposableBean {
        private final Server moquetteServer;
        private final IConfig config;
        private final List<? extends InterceptHandler> handlers;
        private final IAuthenticator authenticator;
        private final IAuthorizator authorizator;

        public MoquetteServerInitializingBean(Server moquetteServer,
                                              IConfig config,
                                              List<? extends InterceptHandler> handlers,
                                              IAuthenticator authenticator,
                                              IAuthorizator authorizator) {
            this.moquetteServer = requireNonNull(moquetteServer);
            this.config = requireNonNull(config);
            this.handlers = requireNonNull(handlers);
            this.authenticator = requireNonNull(authenticator);
            this.authorizator = requireNonNull(authorizator);
        }

        @Override
        public void afterPropertiesSet() throws Exception {
            log.info("Starting MQTT Server");

            moquetteServer.startServer(config, handlers, null, authenticator, authorizator);

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
