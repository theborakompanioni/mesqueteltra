package org.tbk.mesqueteltra.moquette.config;

import com.google.common.collect.ImmutableList;
import io.moquette.interception.InterceptHandler;
import io.moquette.server.Server;
import io.moquette.server.config.IConfig;
import io.moquette.spi.security.IAuthenticator;
import io.moquette.spi.security.IAuthorizator;
import io.moquette.spi.security.ISslContextCreator;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class ServerWithInternalPublish extends Server {

    private volatile List<? extends InterceptHandler> handlers = Collections.emptyList();

    @Override
    public void addInterceptHandler(InterceptHandler interceptHandler) {
        throw new UnsupportedOperationException("This method cannot be called on this class." +
                "All handlers must be provided in startServer().");
    }

    @Override
    public void removeInterceptHandler(InterceptHandler interceptHandler) {
        throw new UnsupportedOperationException("This method cannot be called on this class." +
                "All handlers must be provided in startServer().");
    }

    @Override
    public void internalPublish(MqttPublishMessage msg, String clientId) {
        super.internalPublish(msg, clientId);

        InterceptInternalPublishedMessage internalPublishedMessage = InterceptInternalPublishedMessage.builder()
                .clientId(clientId)
                .msg(msg)
                .build();

        Flux.fromIterable(handlers)
                .filter(h -> InterceptHandlerWithInternalMessageSupport.class.isAssignableFrom(h.getClass()))
                .cast(InterceptHandlerWithInternalMessageSupport.class)
                .subscribe(h -> h.onInternalPublish(internalPublishedMessage));
    }

    @Override
    public void startServer(IConfig config, List<? extends InterceptHandler> handlers, ISslContextCreator sslCtxCreator, IAuthenticator authenticator, IAuthorizator authorizator) throws IOException {
        this.handlers = ImmutableList.copyOf(Optional.ofNullable(handlers).orElseGet(Collections::emptyList));
        super.startServer(config, handlers, sslCtxCreator, authenticator, authorizator);
    }

    @Value
    @Builder
    public static class InterceptInternalPublishedMessage {
        private String clientId;

        @NonNull
        private MqttPublishMessage msg;

        public String getTopicName() {
            return this.msg.variableHeader().topicName();
        }

        public boolean isRetainFlag() {
            return this.msg.fixedHeader().isRetain();
        }

        public boolean isDupFlag() {
            return this.msg.fixedHeader().isDup();
        }

        public MqttQoS getQos() {
            return this.msg.fixedHeader().qosLevel();
        }
    }

    public interface InterceptHandlerWithInternalMessageSupport extends InterceptHandler {
        default void onInternalPublish(InterceptInternalPublishedMessage msg) {
            // empty on purpose
        }
    }
}
