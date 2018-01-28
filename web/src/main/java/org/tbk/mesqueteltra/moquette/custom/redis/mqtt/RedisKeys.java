package org.tbk.mesqueteltra.moquette.custom.redis.mqtt;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import io.moquette.spi.impl.subscriptions.Topic;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;

import java.util.List;
import java.util.stream.Collectors;

public final class RedisKeys {
    private RedisKeys() {
        throw new UnsupportedOperationException();
    }

    public static MqttKey storedMessage() {
        return MqttKeyImpl.newMqttKeyBuilder()
                .delimiter(":")
                .addToken("mqtt")
                .addToken("stored_message")
                .build();
    }

    public interface Key {
        Key append(String topicName);

        String name();

        String delimiter();

        List<String> token();

        MqttKey wildcard();
    }

    public interface MqttKey extends Key {
        MqttKey append(Topic topic);

        Topic toTopic(String redisKey);
    }

    @Getter
    @Builder(builderMethodName = "newMqttKeyBuilder", toBuilder = true)
    public static class MqttKeyImpl implements MqttKey {
        @Singular("addToken")
        private List<String> token;

        private String delimiter;

        @Override
        public String name() {
            return token.stream()
                    .collect(Collectors.joining(delimiter()));
        }

        @Override
        public String delimiter() {
            return Strings.nullToEmpty(delimiter);
        }

        @Override
        public List<String> token() {
            return ImmutableList.copyOf(token);
        }

        @Override
        public MqttKey wildcard() {
            return append("*");
        }

        @Override
        public MqttKey append(String topicName) {
            return this.toBuilder()
                    .addToken(topicName)
                    .build();
        }

        @Override
        public MqttKey append(Topic topic) {
            String topicName = topic.toString();
            return append(topicName);
        }

        @Override
        public Topic toTopic(String redisKey) {
            String topicName = redisKey.replace(name(), "");
            return new Topic(topicName);
        }
    }
}
