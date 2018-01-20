package org.tbk.mesqueteltra.client;

import com.google.common.base.Charsets;
import io.moquette.server.Server;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Future;
import io.vertx.rxjava.core.AbstractVerticle;

import java.time.LocalDateTime;

import static java.util.Objects.requireNonNull;

public class MqttTimeVerticle extends AbstractVerticle {

    private final Server server;

    public MqttTimeVerticle(Server server) {
        this.server = requireNonNull(server);
    }

    @Override
    public void start(Future<Void> startFuture) {
        vertx.setPeriodic(1_000, event -> {
            LocalDateTime now = LocalDateTime.now();
            byte[] bytes = now.toString().getBytes(Charsets.UTF_8);

            server.internalPublish(MqttMessageBuilders.publish()
                    .topicName("/time")
                    .retained(true)
                    .qos(MqttQoS.AT_LEAST_ONCE)
                    .payload(Unpooled.copiedBuffer(bytes))
                    .build(), "INTRLPUB");
        });

        startFuture.complete();
    }
}
