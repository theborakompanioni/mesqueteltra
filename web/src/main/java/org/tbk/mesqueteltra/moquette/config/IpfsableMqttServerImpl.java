package org.tbk.mesqueteltra.moquette.config;

import com.google.common.base.Charsets;
import com.google.common.util.concurrent.RateLimiter;
import io.moquette.server.Server;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.mqtt.MqttMessageBuilders;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.json.Json;
import lombok.extern.slf4j.Slf4j;
import org.tbk.mesqueteltra.IpfsService;
import org.tbk.mesqueteltra.moquette.ipfs.IpfsPublishHandler;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

@Slf4j
public class IpfsableMqttServerImpl extends DelegatingIpfsableMqttServerImpl {

    // we do not want to spam the network - its a demo!
    private final RateLimiter rateLimiter = RateLimiter.create(100);

    private final UUID uuid;
    private final IpfsService ipfsService;

    public IpfsableMqttServerImpl(UUID uuid, Server server, IpfsService ipfsService) {
        super(server);
        this.uuid = requireNonNull(uuid);
        this.ipfsService = requireNonNull(ipfsService);
    }

    @Override
    public void internalPublish(MqttPublishMessage msg, String clientId) {
        internalPublishWithIpfs(msg, clientId);
    }

    @Override
    public void internalPublishFromIpfs(IpfsPublishHandler.IpfsMqttDto ipfsMqttDto) {
        if (ipfsMqttDto.getServerId().equals(uuid.toString())) {
            log.debug("Refrain from publishing IPFS message internally because it has been sent by this instance: {}", ipfsMqttDto);
            return;
        }

        MqttPublishMessage mqttMessage = MqttMessageBuilders.publish()
                .topicName(ipfsMqttDto.getTopic())
                .retained(ipfsMqttDto.isRetained())
                .qos(MqttQoS.valueOf(ipfsMqttDto.getQos()))
                .payload(Unpooled.copiedBuffer(ipfsMqttDto.getContent().getBytes(Charsets.UTF_8)))
                .build();

        internalPublishWithoutIpfs(mqttMessage, ipfsMqttDto.getClientId());
    }

    @Override
    public void publishToIpfsOnly(IpfsPublishHandler.IpfsMqttDto msg) {
        publishToIpfs(msg);
    }

    private void internalPublishWithIpfs(MqttPublishMessage msg, String clientId) {
        super.internalPublish(msg, clientId);
        publishToIpfs(msg.copy(), clientId);
    }

    private void internalPublishWithoutIpfs(MqttPublishMessage msg, String clientId) {
        super.internalPublish(msg, clientId);
    }

    private void publishToIpfs(MqttPublishMessage msg, String clientId) {
        final String content = msg.content().toString(Charsets.UTF_8);

        IpfsPublishHandler.IpfsMqttDto dto = new IpfsPublishHandler.IpfsMqttDto();
        dto.setClientId(clientId);
        dto.setTopic(msg.variableHeader().topicName());
        dto.setContent(content);
        dto.setQos(msg.fixedHeader().qosLevel().value());
        dto.setRetained(msg.fixedHeader().isRetain());
        dto.setDuplicate(msg.fixedHeader().isDup());

        publishToIpfs(dto);
    }


    private void publishToIpfs(IpfsPublishHandler.IpfsMqttDto dto) {
        if (!rateLimiter.tryAcquire()) {
            log.warn("Could not acquire RateLimiter to Protect Message Flood in DEMO mode (rate:={})", rateLimiter.getRate());
        } else {
            dto.setServerId(uuid.toString());

            String json = Json.encode(dto);

            log.info("Publishing via IPFS on topic {}: {}", dto.getTopic(), dto.getContent());

            ipfsService.publish(dto.getTopic(), json)
                    .subscribeOn(Schedulers.elastic())
                    .subscribe();
        }
    }
}
