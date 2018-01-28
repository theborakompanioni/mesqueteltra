package org.tbk.mesqueteltra.moquette.custom.client;

import com.google.common.base.Charsets;
import io.moquette.server.Server;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.Future;
import io.vertx.rxjava.core.AbstractVerticle;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static java.util.Objects.requireNonNull;

public class MqttTimeVerticle extends AbstractVerticle {

    private final Server server;
    private final TimeUnit timeUnit;
    private final long delay;

    public MqttTimeVerticle(Server server, TimeUnit timeUnit, long delay) {
        this.server = requireNonNull(server);
        this.timeUnit = timeUnit;
        this.delay = delay;
    }

    @Override
    public void start(Future<Void> startFuture) {
        vertx.setTimer(1, event -> publishTimeBlocking());

        vertx.setPeriodic(timeUnit.toMillis(delay), event -> {
            publishTimeBlocking();
        });

        startFuture.complete();
    }

    private void publishTimeBlocking() {
        vertx.<Void>executeBlocking(future -> {
            publishTime();
            future.complete();
        }, result -> {
        });
    }

    private void publishTime() {
        LocalDateTime now = LocalDateTime.now();
        byte[] bytes = now.toString().getBytes(Charsets.UTF_8);

        server.internalPublish(MqttMessageBuilders.publish()
                .topicName("/time")
                .retained(true)
                .qos(MqttQoS.AT_LEAST_ONCE)
                .payload(Unpooled.copiedBuffer(bytes))
                .build(), "INTRLPUB");
    }
}
