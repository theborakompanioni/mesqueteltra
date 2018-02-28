package org.tbk.mesqueteltra.moquette.config;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import io.moquette.interception.InterceptHandler;
import io.moquette.server.Server;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.springframework.beans.factory.InitializingBean;

import java.util.List;

import static java.util.Objects.requireNonNull;

public class ServerWithInternalPublish extends Server {

    private final EventBus eventBus;

    public ServerWithInternalPublish(EventBus eventBus) {
        super();

        this.eventBus = requireNonNull(eventBus);
    }

    public void internalPublish(MqttPublishMessage msg, String clientId) {
        super.internalPublish(msg, clientId);

        this.eventBus.post(InterceptInternalPublishedMessage.builder()
                .clientId(clientId)
                .msg(msg)
                .build());
    }

    @Value
    @Builder
    public static class InterceptInternalPublishedMessage {
        private String clientId;

        @NonNull
        private MqttPublishMessage msg;

        public String getTopicName() {
            return this.msg.variableHeader().topicName();
        }

        public boolean isRetainFlag() {
            return this.msg.fixedHeader().isRetain();
        }

        public boolean isDupFlag() {
            return this.msg.fixedHeader().isDup();
        }

        public MqttQoS getQos() {
            return this.msg.fixedHeader().qosLevel();
        }
    }

    public interface InterceptHandlerWithInternalMessageSupport extends InterceptHandler {
        void onInternalPublish(InterceptInternalPublishedMessage msg);
    }


    public static class MoquettePublishInternalBridge implements InitializingBean {
        private final EventBus eventBus;
        private final List<InterceptHandlerWithInternalMessageSupport> handler;

        public MoquettePublishInternalBridge(EventBus eventBus, List<InterceptHandlerWithInternalMessageSupport> handler) {
            this.eventBus = eventBus;
            this.handler = handler;
        }

        @Override
        public void afterPropertiesSet() throws Exception {
            eventBus.register(this);
        }

        @Subscribe
        public void onInternalPublishEvent(InterceptInternalPublishedMessage msg) {
            handler.forEach(h -> h.onInternalPublish(msg));
        }
    }
}
