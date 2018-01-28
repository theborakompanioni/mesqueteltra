package org.tbk.mesqueteltra.moquette.custom.kafka;

import com.google.common.collect.Maps;
import kafka.server.KafkaConfig;
import lombok.Builder;
import lombok.Value;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.kafka.core.*;
import org.springframework.util.StringUtils;

import java.util.Map;

@EnableKafka
@EnableKafkaStreams
@Configuration
@ConditionalOnProperty(value = "mesqueteltra.kafka.enabled", havingValue = "true")
public class KafkaSpringConfig {

    @Bean
    public KafkaConfigExt kafkaConfigExt(KafkaConfig kafkaConfig) {
        return KafkaConfigExt.builder()
                .kafkaConfig(kafkaConfig)
                .build();
    }

    @Bean
    public KafkaAdmin admin(KafkaConfigExt kafkaConfigExt) {
        Map<String, Object> configs = Maps.newHashMap();
        configs.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfigExt.getBootstrapServers());

        return new KafkaAdmin(configs);
    }

    @Bean
    public NewTopic topic() {
        return new NewTopic("incoming_mqtt_message", -1, (short) -1);
    }

    @Bean
    public ProducerFactory<String, String> producerFactory(KafkaConfigExt kafkaConfig) {
        return new DefaultKafkaProducerFactory<>(producerConfigs(kafkaConfig));
    }

    @Bean
    public Map<String, Object> producerConfigs(KafkaConfigExt kafkaConfigExt) {
        Map<String, Object> configs = Maps.newHashMap();
        configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfigExt.getBootstrapServers());
        configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        // See https://kafka.apache.org/documentation/#producerconfigs for more properties
        return configs;
    }

    @Bean
    public KafkaTemplate<String, String> kafkaTemplate(KafkaConfigExt kafkaConfig) {
        return new KafkaTemplate<>(producerFactory(kafkaConfig));
    }

    /*@Bean
    public ConsumerFactory<String, String> consumerFactory(KafkaConfigExt kafkaConfigExt) {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs(kafkaConfigExt));
    }

    @Bean
    public Map<String, Object> consumerConfigs(KafkaConfigExt kafkaConfigExt) {
        Map<String, Object> configs = Maps.newHashMap();
        configs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaConfigExt.getBootstrapServers());
        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        // See https://kafka.apache.org/documentation/#producerconfigs for more properties
        return configs;
    }*/

    @Value
    @Builder
    public static class KafkaConfigExt {
        private KafkaConfig kafkaConfig;

        public String getBootstrapServers() {
            String server = kafkaConfig.advertisedHostName() + ":" + kafkaConfig.advertisedPort();

            return StringUtils.arrayToCommaDelimitedString(new Object[]{
                    server
            });
        }

    }
}
