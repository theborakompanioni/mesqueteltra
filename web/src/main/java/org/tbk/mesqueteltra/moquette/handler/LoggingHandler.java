package org.tbk.mesqueteltra.moquette.handler;

import com.google.common.base.Charsets;
import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.*;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import org.tbk.mesqueteltra.moquette.config.ServerWithInternalPublish;
import org.tbk.mesqueteltra.moquette.config.ServerWithInternalPublish.InterceptHandlerWithInternalMessageSupport;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

@Slf4j
public class LoggingHandler implements InterceptHandlerWithInternalMessageSupport {

    private final UUID serverId;

    public LoggingHandler(UUID serverId) {
        this.serverId = requireNonNull(serverId);
    }

    @Override
    public String getID() {
        return "LoggingHandler";
    }

    @Override
    public Class<?>[] getInterceptedMessageTypes() {
        return InterceptHandler.ALL_MESSAGE_TYPES;
    }

    @Override
    public void onConnect(InterceptConnectMessage msg) {
        log.info("CONNECT from stream: username:={} (clientId:={})", msg.getUsername(), msg.getClientID());
    }

    @Override
    public void onDisconnect(InterceptDisconnectMessage msg) {
        log.info("DISCONNECT from stream: username:={} (clientId:={})", msg.getUsername(), msg.getClientID());
    }

    @Override
    public void onConnectionLost(InterceptConnectionLostMessage msg) {
        log.info("CONNECTION LOST from stream: username:={} (clientId:={})", msg.getUsername(), msg.getClientID());
    }

    @Override
    public void onPublish(InterceptPublishMessage msg) {
        ByteBuf payload = msg.getPayload();
        log.info("{} PUBLISH from stream: username:={} (clientId:={}), topic:={}, content:={}",
                serverId.toString().substring(0, 5),
                msg.getUsername(), msg.getClientID(), msg.getTopicName(), payload.toString(Charsets.UTF_8));
    }

    @Override
    public void onInternalPublish(ServerWithInternalPublish.InterceptInternalPublishedMessage msg) {
        ByteBuf payload = msg.getMsg().payload();
        log.info("{} INTERNAL PUBLISH from stream: clientId:={}, topic:={}, content:={}",
                serverId.toString().substring(0, 5),
                msg.getClientId(), msg.getTopicName(), payload.toString(Charsets.UTF_8));
    }

    @Override
    public void onSubscribe(InterceptSubscribeMessage msg) {
        log.info("SUBSCRIBE from stream: username:={} (clientId:={}), topic filter:={}",
                msg.getUsername(), msg.getClientID(), msg.getTopicFilter());
    }

    @Override
    public void onUnsubscribe(InterceptUnsubscribeMessage msg) {
        log.info("UNSUBSCRIBE from stream: username:={} (clientId:={}), topic filter:={}",
                msg.getUsername(), msg.getClientID(), msg.getTopicFilter());
    }

    @Override
    public void onMessageAcknowledged(InterceptAcknowledgedMessage msg) {
        log.info("ACK from stream: username:={}, topic:={}", msg.getUsername(), msg.getTopic());
    }
}