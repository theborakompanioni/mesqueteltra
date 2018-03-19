package org.tbk.mesqueteltra.moquette.config;

import com.google.common.collect.ImmutableList;
import io.moquette.interception.InterceptHandler;
import io.moquette.server.Server;
import io.moquette.server.config.IConfig;
import io.moquette.server.config.MemoryConfig;
import io.moquette.spi.security.IAuthenticator;
import io.moquette.spi.security.IAuthorizator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.tbk.mesqueteltra.IpfsService;
import org.tbk.mesqueteltra.moquette.SimpleAuthenticator;
import org.tbk.mesqueteltra.moquette.SimpleAuthorizator;
import org.tbk.mesqueteltra.moquette.config.MoquetteProperties.MoquetteSslProperties;
import org.tbk.mesqueteltra.moquette.ext.spi.ITopicPolicy;
import org.tbk.mesqueteltra.moquette.handler.LoggingHandler;
import org.tbk.mesqueteltra.moquette.ipfs.IpfsPublishHandler;
import org.tbk.mesqueteltra.moquette.ipfs.PublishInternalMqttIpfsSubscriber;

import java.util.*;

import static java.util.Objects.requireNonNull;

@Configuration
@EnableConfigurationProperties({
        MoquetteProperties.class
})
public class MoquetteConfig {
    public final static UUID SERVER_ONE_UUID = UUID.randomUUID();
    public final static UUID SERVER_TWO_UUID = UUID.randomUUID();

    private final MoquetteProperties moquetteProperties;
    private final Environment environment;

    @Autowired
    public MoquetteConfig(Environment environment, MoquetteProperties moquetteProperties) {
        this.environment = requireNonNull(environment);
        this.moquetteProperties = requireNonNull(moquetteProperties);
    }

    @Bean
    public IAuthorizator authorizator(List<ITopicPolicy> topicPolicies) {
        return new SimpleAuthorizator(topicPolicies);
    }

    @Bean
    public IAuthenticator authenticator() {
        return new SimpleAuthenticator();
    }


    @Bean("mqttServerOne")
    public Server mqttServerOne() {
        return createMoquetteServer();
    }


    @Bean("mqttServerTwo")
    public Server mqttServerTwo() {
        return createMoquetteServer();
    }

    @Bean("ipfsMqttServerOne")
    @Primary
    public AbstractIpfsableMqttServer ipfsMqttServerOne(Optional<IpfsService> ipfsOptional) {
        Server moquetteServer = mqttServerOne();
        if (!ipfsOptional.isPresent()) {
            return new IpfsableMqttServerNoop(moquetteServer);
        }

        return new IpfsableMqttServerImpl(SERVER_ONE_UUID, moquetteServer, ipfsOptional.get());
    }

    @Bean("ipfsMqttServerTwo")
    public AbstractIpfsableMqttServer ipfsMqttServerTwo(Optional<IpfsService> ipfsOptional) {
        Server moquetteServer = mqttServerTwo();
        if (!ipfsOptional.isPresent()) {
            return new IpfsableMqttServerNoop(moquetteServer);
        }

        return new IpfsableMqttServerImpl(SERVER_TWO_UUID, moquetteServer, ipfsOptional.get());

    }

    @Bean
    public MoquetteServerInitializingBean moquetteServerOneInitializer(
            IAuthenticator authenticator,
            IAuthorizator authorizator,
            Optional<IpfsService> ipfsOptional) {
        UUID serverOneUuid = SERVER_ONE_UUID;
        AbstractIpfsableMqttServer ipfsMqttServerOne = ipfsMqttServerOne(ipfsOptional);
        IConfig config = configServerOne(moquetteProperties);
        List<InterceptHandler> handlers = ImmutableList.<InterceptHandler>builder()
                .add(new LoggingHandler(serverOneUuid))
                .addAll(ipfsOptional
                        .map(ipfs -> new IpfsPublishHandler(ipfsMqttServerOne))
                        .map(Collections::singletonList)
                        .orElse(Collections.emptyList()))
                .build();
        return new MoquetteServerInitializingBean(ipfsMqttServerOne, config, handlers, authenticator, authorizator);
    }

    @Bean
    public MoquetteServerInitializingBean moquetteServerTwoInitializer(
            IAuthenticator authenticator,
            IAuthorizator authorizator,
            Optional<IpfsService> ipfsOptional) {
        UUID serverTwoUuid = SERVER_TWO_UUID;
        AbstractIpfsableMqttServer ipfsMqttServerTwo = ipfsMqttServerTwo(ipfsOptional);
        IConfig config = configServerTwo(moquetteProperties);
        List<InterceptHandler> handlers = ImmutableList.<InterceptHandler>builder()
                .add(new LoggingHandler(serverTwoUuid))
                .addAll(ipfsOptional
                        .map(ipfs -> new IpfsPublishHandler(ipfsMqttServerTwo))
                        .map(Collections::singletonList)
                        .orElse(Collections.emptyList()))
                .build();
        return new MoquetteServerInitializingBean(ipfsMqttServerTwo, config, handlers, authenticator, authorizator);
    }

    @Bean
    public PublishInternalMqttIpfsSubscriber ipfsSubcriberServerOne(IpfsService ipfsService,
                                                                    @Qualifier("ipfsMqttServerOne") IpfsableMqttServer serverOne) {
        return new PublishInternalMqttIpfsSubscriber(ipfsService, serverOne);
    }

    @Bean
    public PublishInternalMqttIpfsSubscriber ipfsSubcriberServerTwo(IpfsService ipfsService,
                                                                    @Qualifier("ipfsMqttServerTwo") IpfsableMqttServer serverTwo) {
        return new PublishInternalMqttIpfsSubscriber(ipfsService, serverTwo);
    }

    private Server createMoquetteServer() {
        return new ServerWithInternalPublish();
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
