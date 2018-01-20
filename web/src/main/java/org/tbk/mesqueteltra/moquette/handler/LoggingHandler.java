package org.tbk.mesqueteltra.moquette.handler;

import com.google.common.base.Charsets;
import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.*;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LoggingHandler implements InterceptHandler {

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
        log.info("CONNECT from client: {} ({})", msg.getUsername(), msg.getClientID());
    }

    @Override
    public void onDisconnect(InterceptDisconnectMessage msg) {
        log.info("DISCONNECT from client: {} ({})", msg.getUsername(), msg.getClientID());
    }

    @Override
    public void onConnectionLost(InterceptConnectionLostMessage msg) {
        log.info("CONNECTION LOST from client: {} ({})", msg.getUsername(), msg.getClientID());
    }

    @Override
    public void onPublish(InterceptPublishMessage msg) {
        log.info("onPublish from client: {} ({})", msg.getUsername(), msg.getClientID());
        log.info("topic: {}", msg.getTopicName());

        ByteBuf payload = msg.getPayload();
        log.info("content: {}", payload.toString(Charsets.UTF_8));
    }

    @Override
    public void onSubscribe(InterceptSubscribeMessage msg) {
        log.info("SUBSCRIBE from client: {} ({})", msg.getUsername(), msg.getClientID());
        log.info("topic filter: {}", msg.getTopicFilter());
    }

    @Override
    public void onUnsubscribe(InterceptUnsubscribeMessage msg) {
        log.info("UNSUBSCRIBE from client: {} ({})", msg.getUsername(), msg.getClientID());
        log.info("topic filter: {}", msg.getTopicFilter());
    }

    @Override
    public void onMessageAcknowledged(InterceptAcknowledgedMessage msg) {
        log.info("ACK from client: {}", msg.getUsername());
        log.info("topic: {}", msg.getTopic());
    }
}