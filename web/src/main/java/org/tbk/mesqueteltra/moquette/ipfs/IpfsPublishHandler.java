package org.tbk.mesqueteltra.moquette.ipfs;

import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.*;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.tbk.mesqueteltra.mqtt.Mqttable;

@Slf4j
public class IpfsPublishHandler implements InterceptHandler {

    private final Mqttable mqttable;

    public IpfsPublishHandler(Mqttable mqttable) {
        this.mqttable = mqttable;
    }

    @Override
    public String getID() {
        return "IpfsPublishHandler";
    }

    @Override
    public Class<?>[] getInterceptedMessageTypes() {
        //return InterceptHandler.ALL_MESSAGE_TYPES;
        return new Class[]{InterceptPublishMessage.class};
    }

    @Override
    public void onConnect(InterceptConnectMessage msg) {
    }

    @Override
    public void onDisconnect(InterceptDisconnectMessage msg) {
    }

    @Override
    public void onConnectionLost(InterceptConnectionLostMessage msg) {

    }

    @Override
    public void onPublish(InterceptPublishMessage msg) {
        MqttPublishMessage mqttMessage = MqttMessageBuilders.publish()
                .topicName(msg.getTopicName())
                .retained(msg.isRetainFlag())
                .qos(msg.getQos())
                .payload(Unpooled.copiedBuffer(msg.getPayload()))
                .build();

        log.debug("publishing PUBLISH msg via IPFS");

        mqttable.publish(mqttMessage, msg.getClientID())
                .subscribe(next -> {
                    log.debug("successfully published PUBLISH msg via IPFS");
                }, e -> {
                    log.debug("error while publishing PUBLISH msg via IPFS: {}", e.getMessage());
                });
    }

    @Override
    public void onSubscribe(InterceptSubscribeMessage msg) {
    }

    @Override
    public void onUnsubscribe(InterceptUnsubscribeMessage msg) {
    }

    @Override
    public void onMessageAcknowledged(InterceptAcknowledgedMessage msg) {
    }

    @Data
    public static class IpfsMqttDto {
        private String serverId;
        private String content;
        private String topic;
        private String clientId;
        private int qos;
        private boolean retained;
        private boolean duplicate;
    }
}