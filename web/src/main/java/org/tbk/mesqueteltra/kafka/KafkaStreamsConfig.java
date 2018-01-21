package org.tbk.mesqueteltra.kafka;

import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KeyValueMapper;
import org.apache.kafka.streams.processor.WallclockTimestampExtractor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.KafkaStreamsDefaultConfiguration;
import org.springframework.kafka.core.StreamsBuilderFactoryBean;

import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@ConditionalOnProperty(value = "mesqueteltra.kafka.enabled", havingValue = "true")
public class KafkaStreamsConfig {
    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_BUILDER_BEAN_NAME)
    public StreamsBuilderFactoryBean streamsBuilderFactoryBean(StreamsConfig streamsConfig) {
        return new StreamsBuilderFactoryBean(streamsConfig);
    }

    @Bean(name = KafkaStreamsDefaultConfiguration.DEFAULT_STREAMS_CONFIG_BEAN_NAME)
    public StreamsConfig streamsConfig(KafkaSpringConfig.KafkaConfigExt kafkaConfigExt) {
        Map<String, Object> props = Maps.newHashMap();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "mesqueteltra-streams");
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, TimeUnit.SECONDS.toMillis(5));

        //final Serializer<JsonNode> jsonSerializer = new JsonSerializer();
        //final Deserializer<JsonNode> jsonDeserializer = new JsonDeserializer();
        //final Serde<JsonNode> jsonSerde = Serdes.serdeFrom(jsonSerializer, jsonDeserializer);

        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfigExt.getBootstrapServers());
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        props.put(StreamsConfig.DEFAULT_TIMESTAMP_EXTRACTOR_CLASS_CONFIG, WallclockTimestampExtractor.class.getName());
        return new StreamsConfig(props);
    }

    /*@Bean
    public KafkaVerticle kafkaVerticle(KafkaTemplate<String, String> kafkaTemplate) {
        return new KafkaVerticle(kafkaTemplate);
    }*/

    @Bean
    public KStream<String, String> incomingMqttMessageStream(StreamsBuilder streamBuilder) {

        KStream<String, String> stream = streamBuilder.stream("incoming_mqtt_message");

        //stream.print(Printed.toSysOut());
        stream.map(new KeyValueMapper<String, String, KeyValue<String, String>>() {
            @Override
            public KeyValue<String, String> apply(String key, String value) {
                log.info("INCOMING message from kafka: {}", value);
                return new KeyValue<>(key, value);
            }
        }).to("incoming_mqtt_message_raw");

        return stream;
    }
}