package org.tbk.mesqueteltra.paho;

import com.google.common.base.Charsets;
import com.google.common.util.concurrent.RateLimiter;
import io.moquette.server.Server;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Autowired;

import javax.annotation.PostConstruct;
import java.util.Arrays;

@Slf4j
public class PahoClientSubscribeExample {

    @Autowired
    MqttClient mqttClient;
    @Autowired
    MqttConnectOptions mqttConnectOptions;
    @Autowired
    Server server;

    RateLimiter rateLimiter = RateLimiter.create(1);

    @PostConstruct
    public void init() throws MqttException {
        String topic = "/hello";
        String content = "hello world";
        int qos = 2;

        mqttClient.connect(mqttConnectOptions);

        log.info("Connected");
        log.info("Publishing message: {}", content);

        MqttMessage message = new MqttMessage(content.getBytes());
        message.setQos(qos);

        mqttClient.publish(topic, message);

        log.info("Message published");

        mqttClient.subscribe("/#", new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws MqttException {
                log.info("Message arrived in topic {}: {}", topic, message);

                boolean isPingMessage = Arrays.equals(message.getPayload(),
                        "ping".getBytes(Charsets.UTF_8));

                if (isPingMessage) {
                    if (rateLimiter.tryAcquire()) {
                        MqttMessage answer = pongMessage();
                        log.info("Answering with message in topic {}: {}", topic, answer);
                        mqttClient.publish(topic, answer);
                    }
                }

                server.internalPublish(MqttMessageBuilders.publish()
                        .messageId(message.getId())
                        .topicName(topic)
                        .retained(message.isRetained())
                        .qos(MqttQoS.valueOf(message.getQos()))
                        .payload(Unpooled.copiedBuffer(message.getPayload()))
                        .build(), "INTRLPUB");

            }
        });
    }

    public MqttMessage pongMessage() {
        MqttMessage message = new MqttMessage("pong".getBytes(Charsets.UTF_8));
        message.setQos(MqttQoS.AT_LEAST_ONCE.value());
        message.setRetained(false);
        return message;
    }
}
