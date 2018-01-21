package org.tbk.mesqueteltra.kafka.mqtt.handler;

import com.google.common.base.Charsets;
import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.*;
import io.vertx.core.json.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import static java.util.Objects.requireNonNull;

@Slf4j
public class KafkaHandler implements InterceptHandler {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaHandler(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = requireNonNull(kafkaTemplate);
    }

    @Override
    public String getID() {
        return "KafkaHandler";
    }

    @Override
    public Class<?>[] getInterceptedMessageTypes() {
        return InterceptHandler.ALL_MESSAGE_TYPES;
    }

    @Override
    public void onConnect(InterceptConnectMessage msg) {
        JsonObject json = new JsonObject();
        json.put("op", "CONNECT");
        json.put("client_id", msg.getClientID());
        json.put("username", msg.getUsername());
        json.put("keep_alive", msg.getKeepAlive());
        json.put("protocol_name", msg.getProtocolName());
        json.put("protocol_version", msg.getProtocolVersion());
        json.put("will_topic", msg.getWillTopic());

        ListenableFuture<SendResult<String, String>> future = kafkaTemplate.send("incoming_mqtt_message", json.toString());
        future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
            @Override
            public void onSuccess(SendResult<String, String> result) {
                log.info("sent CONNECT from stream: {} ({})", msg.getUsername(), msg.getClientID());
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("error while sending CONNECT from stream: {} ({})", msg.getUsername(), msg.getClientID());
                log.error("", t);
            }
        });
    }

    @Override
    public void onDisconnect(InterceptDisconnectMessage msg) {
        JsonObject json = new JsonObject();
        json.put("op", "DISCONNECT");
        json.put("client_id", msg.getClientID());
        json.put("username", msg.getUsername());
        sendToKafka("DISCONNECT", json.toString());
    }

    @Override
    public void onConnectionLost(InterceptConnectionLostMessage msg) {
        JsonObject json = new JsonObject();
        json.put("op", "CONNECTION_LOST");
        json.put("client_id", msg.getClientID());
        json.put("username", msg.getUsername());
        sendToKafka("CONNECTION_LOST", json.toString());
    }

    @Override
    public void onPublish(InterceptPublishMessage msg) {
        JsonObject json = new JsonObject();
        json.put("op", "PUBLISH");
        json.put("client_id", msg.getClientID());
        json.put("username", msg.getUsername());
        json.put("topic_name", msg.getTopicName());
        json.put("payload", msg.getPayload().toString(Charsets.UTF_8));
        sendToKafka("PUBLISH", json.toString());
    }

    @Override
    public void onSubscribe(InterceptSubscribeMessage msg) {
        JsonObject json = new JsonObject();
        json.put("op", "UNSUBSCRIBE");
        json.put("client_id", msg.getClientID());
        json.put("username", msg.getUsername());
        sendToKafka("SUBSCRIBE", json.toString());
    }

    @Override
    public void onUnsubscribe(InterceptUnsubscribeMessage msg) {
        JsonObject json = new JsonObject();
        json.put("op", "UNSUBSCRIBE");
        json.put("client_id", msg.getClientID());
        json.put("username", msg.getUsername());
        sendToKafka("UNSUBSCRIBE", json.toString());
    }

    @Override
    public void onMessageAcknowledged(InterceptAcknowledgedMessage msg) {
        JsonObject json = new JsonObject();
        json.put("op", "ACK");
        json.put("username", msg.getUsername());
        json.put("topic", msg.getTopic());
        json.put("packet_id", msg.getPacketID());
        sendToKafka("ACK", json.toString());
    }

    private void sendToKafka(String operation, String json) {
        ListenableFuture<SendResult<String, String>> future = kafkaTemplate.send("incoming_mqtt_message", json);
        future.addCallback(new ListenableFutureCallback<SendResult<String, String>>() {
            @Override
            public void onSuccess(SendResult<String, String> result) {
                log.info("sent {}", operation);
            }

            @Override
            public void onFailure(Throwable t) {
                log.warn("error while sending {}", operation);
                log.error("", t);
            }
        });
    }
}