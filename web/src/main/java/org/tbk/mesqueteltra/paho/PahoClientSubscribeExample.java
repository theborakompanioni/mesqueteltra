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
import java.util.function.Supplier;

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

    private void publishHelloWorldMessage() {
        log.info("Publishing 'hello world' message");
        helloWorldMessagePublisher(mqttClient).blockFirst();
        log.info("Message published");
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
                    log.info("[A] Answering with message in topic {}: {}", topic, "pong");
                    Flux.just(1)
                            .delayElements(Duration.of(3, ChronoUnit.SECONDS))
                            .flatMap(foo -> pongMessagePublisher(topic, mqttClientA))
                            .subscribe(msg -> {
                                log.info("[A] Answered with message in topic {}: {}", topic, msg);
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
                    log.info("[B] Answering with message in topic {}: {}", topic, "ping");
                    Flux.just(1)
                            .delayElements(Duration.of(7, ChronoUnit.SECONDS))
                            .flatMap(foo -> pingMessagePublisher(topic, mqttClientB))
                            .subscribe(msg -> {
                                log.info("[B] Answered with message in topic {}: {}", topic, msg);
                            });
                }
            }
        });

        pingMessagePublisher("/ping-pong", mqttClientB)
                .delayElements(Duration.of(10, ChronoUnit.SECONDS))
                .subscribe();

        heartbeatMessagePublisher(mqttClientB)
                .delayElements(Duration.of(15, ChronoUnit.SECONDS))
                .repeat()
                .subscribe();
    }


    private Flux<MqttMessage> helloWorldMessagePublisher(MqttClient client) {
        return messagePublisher("/hello", client, this::helloWorldMessage);
    }

    private Flux<MqttMessage> pongMessagePublisher(String topic, MqttClient client) {
        return messagePublisher(topic, client, this::pongMessage);
    }

    private Flux<MqttMessage> pingMessagePublisher(String topic, MqttClient client) {
        return messagePublisher(topic, client, this::pingMessage);
    }

    private Flux<MqttMessage> heartbeatMessagePublisher(MqttClient client) {
        return messagePublisher("/heartbeat", client, this::heartbeatMessage);
    }

    private Flux<MqttMessage> messagePublisher(String topic, MqttClient client, Supplier<MqttMessage> messageSupplier) {
        return Flux.just(client)
                .filter(MqttClient::isConnected)
                .map(c -> {
                    try {
                        MqttMessage message = messageSupplier.get();
                        c.publish(topic, message);
                        return message;
                    } catch (MqttException e) {
                        throw new RuntimeException(e);
                    }
                })
                .onErrorResume(e -> messagePublisher(topic, client, messageSupplier));
    }

    private MqttMessage heartbeatMessage() {
        MqttMessage message = new MqttMessage("heartbeat".getBytes(Charsets.UTF_8));
        message.setQos(MqttQoS.AT_MOST_ONCE.value());
        message.setRetained(false);
        return message;
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

    private MqttMessage helloWorldMessage() {
        MqttMessage message = new MqttMessage("hello".getBytes(Charsets.UTF_8));
        message.setQos(MqttQoS.EXACTLY_ONCE.value());
        message.setRetained(true);
        return message;
    }
}
