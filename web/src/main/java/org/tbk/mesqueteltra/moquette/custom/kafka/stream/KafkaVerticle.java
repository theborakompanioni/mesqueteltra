package org.tbk.mesqueteltra.moquette.custom.kafka.stream;

import io.vertx.core.Future;
import io.vertx.rxjava.core.AbstractVerticle;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

@Slf4j
public class KafkaVerticle extends AbstractVerticle {
    private static final String TOPIC = "incoming_mqtt_message";

    private final KafkaTemplate<String, String> kafkaTemplate;

    public KafkaVerticle(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = requireNonNull(kafkaTemplate);
    }

    @Override
    public void start(Future<Void> startFuture) {
        long delayInMs = 10_000;

        log.info("start stream in {}s", TimeUnit.MILLISECONDS.toSeconds(delayInMs));

        vertx.setTimer(delayInMs, event -> {
            log.info("starting stream of topic {}", TOPIC);
            startStream();
        });

        startFuture.complete();
    }

    private void startStream() {
        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "mqequeteltra-KafkaVerticle");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, TimeUnit.SECONDS.toMillis(5));
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, String> messageStream = builder.stream(TOPIC);

        messageStream.map(new KeyValueMapper<String, String, KeyValue<String, String>>() {
            @Override
            public KeyValue<String, String> apply(String key, String value) {
                log.info("INCOMING message from kafka: {}", value);
                return new KeyValue<>(key, value);
            }
        }).to("incoming_mqtt_message_raw");

        KafkaStreams streams = new KafkaStreams(builder.build(), config);
        streams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}
