package org.tbk.mesqueteltra.paho;

import com.google.common.base.Charsets;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.tbk.mesqueteltra.IpfsService;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
public class PahoClientSubscribeExample implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    @Qualifier("mqttClient")
    MqttClient mqttClient;

    @Autowired
    @Qualifier("mqttConnectOptions")
    MqttConnectOptions mqttConnectOptions;

    @Autowired
    @Qualifier("mqttClientA")
    MqttClient mqttClientA;

    @Autowired
    @Qualifier("mqttConnectOptionsA")
    MqttConnectOptions mqttConnectOptionsA;

    @Autowired
    @Qualifier("mqttClientB")
    MqttClient mqttClientB;

    @Autowired
    @Qualifier("mqttConnectOptionsA")
    MqttConnectOptions mqttConnectOptionsB;

    @Autowired
    Optional<IpfsService> ipfsService;

    @PreDestroy
    public void shutdown() throws MqttException {
        mqttClient.disconnect();
        mqttClientA.disconnect();
        mqttClientB.disconnect();
    }

    @PostConstruct
    public void init() throws MqttException {
        mqttClient.connect(mqttConnectOptions);
        mqttClientA.connect(mqttConnectOptionsA);
        mqttClientB.connect(mqttConnectOptionsB);
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

        MqttMessage message = new MqttMessage(content.getBytes(Charsets.UTF_8));
        message.setQos(qos);
        message.setRetained(true);

        log.info("Publishing message: {}", content);
        mqttClient.publish(topic, message);
        log.info("Message published");
    }

    private void publishHeartbeatMessage(MqttClient mqttClient) throws MqttException {
        String topic = "/heartbeat";
        String content = "empty";
        int qos = 2;

        MqttMessage message = new MqttMessage(content.getBytes(Charsets.UTF_8));
        message.setQos(qos);
        message.setRetained(true);

        mqttClient.publish(topic, message);
    }

    private void subscribe() throws MqttException {
        ipfsService.ifPresent(ipfsService -> Flux.from(ipfsService.subscribe("/time"))
                .subscribeOn(Schedulers.newSingle("ipfs-subscribe"))
                .subscribe(msg -> {
                    log.info("Message arrived via IPFS on topic {}: {}", msg.getTopicIds(), msg.getDataAsString());
                }));

        mqttClientA.subscribe("/#", new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws MqttException {
                log.info("[A] Message arrived via MQTT in topic {}: {}", topic, message);

                boolean isPingMessage = Arrays.equals(message.getPayload(),
                        "ping".getBytes(Charsets.UTF_8));

                if (isPingMessage) {
                    Flux.just(mqttClientA)
                            .delayElements(Duration.of(5, ChronoUnit.SECONDS))
                            .filter(MqttClient::isConnected)
                            .subscribe(client -> {
                                MqttMessage answer = pongMessage();
                                log.info("[A] Answering with message in topic {}: {}", topic, answer);
                                try {
                                    client.publish(topic, answer);
                                } catch (MqttException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                }
            }
        });

        mqttClientB.subscribe("/#", new IMqttMessageListener() {
            @Override
            public void messageArrived(String topic, MqttMessage message) throws MqttException {
                log.info("[B] Message arrived via MQTT in topic {}: {}", topic, message);

                boolean isPongMessage = Arrays.equals(message.getPayload(),
                        "pong".getBytes(Charsets.UTF_8));

                if (isPongMessage) {
                    Flux.just(mqttClientB)
                            .delayElements(Duration.of(5, ChronoUnit.SECONDS))
                            .filter(MqttClient::isConnected)
                            .subscribe(client -> {
                                MqttMessage answer = pingMessage();
                                log.info("[B] Answering with message in topic {}: {}", topic, answer);
                                try {
                                    client.publish(topic, answer);
                                } catch (MqttException e) {
                                    throw new RuntimeException(e);
                                }
                            });
                }
            }
        });

        Flux.just(mqttClientB)
                .delayElements(Duration.of(10, ChronoUnit.SECONDS))
                .filter(MqttClient::isConnected)
                .subscribe(client -> {
                    try {
                        client.publish("/ping-pong", pingMessage());
                    } catch (MqttException e) {
                        throw new RuntimeException(e);
                    }
                });

        Flux.just(mqttClientB)
                .delayElements(Duration.of(15, ChronoUnit.SECONDS))
                .filter(MqttClient::isConnected)
                .subscribe(client -> {
                    try {
                        publishHeartbeatMessage(client);
                    } catch (MqttException e) {
                        throw new RuntimeException(e);
                    }
                });

    }

    private MqttMessage pongMessage() {
        MqttMessage message = new MqttMessage("pong".getBytes(Charsets.UTF_8));
        message.setQos(MqttQoS.AT_LEAST_ONCE.value());
        message.setRetained(false);
        return message;
    }

    private MqttMessage pingMessage() {
        MqttMessage message = new MqttMessage("ping".getBytes(Charsets.UTF_8));
        message.setQos(MqttQoS.AT_LEAST_ONCE.value());
        message.setRetained(false);
        return message;
    }
}
