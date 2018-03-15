package org.tbk.mesqueteltra.paho;

import com.google.common.base.Charsets;
import com.google.common.util.concurrent.RateLimiter;
import io.moquette.server.Server;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.tbk.mesqueteltra.IpfsService;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Arrays;

@Slf4j
public class PahoClientSubscribeExample implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    MqttClient mqttClient;
    @Autowired
    MqttConnectOptions mqttConnectOptions;
    @Autowired
    Server server;
    @Autowired
    IpfsService ipfsService;

    RateLimiter rateLimiter = RateLimiter.create(1);

    @PreDestroy
    public void shutdown() throws MqttException {
        mqttClient.disconnect();
    }

    @PostConstruct
    public void init() throws MqttException {
        mqttClient.connect(mqttConnectOptions);
        log.info("Connected");
    }

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        try {
            subscribe();

            publishHelloWorldMessage();

        } catch (MqttException e) {
            throw new RuntimeException(e);
        }
    }

    private void publishHelloWorldMessage() throws MqttException {
        String topic = "hello";
        String content = "hello";
        int qos = 2;

        MqttMessage message = new MqttMessage(content.getBytes());
        message.setQos(qos);
        message.setRetained(true);

        log.info("Publishing message: {}", content);
        mqttClient.publish(topic, message);
        log.info("Message published");
    }

    private void subscribe() throws MqttException {
        Flux.from(ipfsService.subscribe("/time"))
                .subscribeOn(Schedulers.newSingle("ipfs-subscribe"))
                .subscribe(msg -> {
                    log.info("Message arrived via IPFS on topic {}: {}", msg.getTopicIds(), msg.getDataAsString());
                });

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
                /*
                // do NOT publish to the broker it is connected to!
                server.internalPublish(MqttMessageBuilders.publish()
                        .messageId(message.getId())
                        .topicName(topic)
                        .retained(message.isRetained())
                        .qos(MqttQoS.valueOf(message.getQos()))
                        .payload(Unpooled.copiedBuffer(message.getPayload()))
                        .build(), "INTRLPUB");*/

            }
        });
    }

    private MqttMessage pongMessage() {
        MqttMessage message = new MqttMessage("pong".getBytes(Charsets.UTF_8));
        message.setQos(MqttQoS.AT_LEAST_ONCE.value());
        message.setRetained(false);
        return message;
    }
}
