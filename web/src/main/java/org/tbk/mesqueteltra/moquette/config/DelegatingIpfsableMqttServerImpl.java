package org.tbk.mesqueteltra.moquette.config;

import com.google.common.collect.ImmutableList;
import com.hazelcast.core.HazelcastInstance;
import io.moquette.connections.IConnectionsManager;
import io.moquette.interception.InterceptHandler;
import io.moquette.server.Server;
import io.moquette.server.config.IConfig;
import io.moquette.spi.impl.ProtocolProcessor;
import io.moquette.spi.impl.subscriptions.Subscription;
import io.moquette.spi.security.IAuthenticator;
import io.moquette.spi.security.IAuthorizator;
import io.moquette.spi.security.ISslContextCreator;
import io.netty.handler.codec.mqtt.MqttPublishMessage;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ScheduledExecutorService;

import static java.util.Objects.requireNonNull;

public abstract class DelegatingIpfsableMqttServerImpl extends AbstractIpfsableMqttServer {
    private final ServerWithInternalPublish server;

    public DelegatingIpfsableMqttServerImpl(ServerWithInternalPublish server) {
        this.server = requireNonNull(server);
    }

    public void startServer() throws IOException {
        this.server.startServer();
    }

    public void startServer(File configFile) throws IOException {
        this.server.startServer(configFile);
    }

    public void startServer(Properties configProps) throws IOException {
        this.server.startServer(configProps);
    }

    public void startServer(IConfig config) throws IOException {
        this.server.startServer(config);
    }

    public void startServer(IConfig config, List<? extends InterceptHandler> handlers) throws IOException {
        this.server.startServer(config, handlers);
    }

    public void startServer(IConfig config, List<? extends InterceptHandler> handlers, ISslContextCreator sslCtxCreator, IAuthenticator authenticator, IAuthorizator authorizator) throws IOException {
        server.startServer(config, handlers, sslCtxCreator, authenticator, authorizator);
    }

    public void stopServer() {
        server.stopServer();
    }

    public void internalPublish(MqttPublishMessage msg, String clientId) {
        server.internalPublish(msg, clientId);
    }

    public void addInterceptHandler(InterceptHandler interceptHandler) {
        server.addInterceptHandler(interceptHandler);
    }

    public void removeInterceptHandler(InterceptHandler interceptHandler) {
        server.removeInterceptHandler(interceptHandler);
    }

    public HazelcastInstance getHazelcastInstance() {
        return server.getHazelcastInstance();
    }

    public List<Subscription> getSubscriptions() {
        return server.getSubscriptions();
    }


    public IConnectionsManager getConnectionsManager() {
        return server.getConnectionsManager();
    }

    public ProtocolProcessor getProcessor() {
        return server.getProcessor();
    }

    public ScheduledExecutorService getScheduler() {
        return server.getScheduler();
    }


}
