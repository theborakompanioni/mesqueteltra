package org.tbk.mesqueteltra.moquette.ipfs;

import com.google.common.base.Charsets;
import com.google.common.util.concurrent.RateLimiter;
import io.moquette.interception.InterceptHandler;
import io.moquette.interception.messages.*;
import io.vertx.core.json.Json;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.tbk.mesqueteltra.IpfsService;
import org.tbk.mesqueteltra.moquette.config.IpfsableMqttServer;
import org.tbk.mesqueteltra.moquette.config.ServerWithInternalPublish;
import reactor.core.scheduler.Schedulers;

import java.util.UUID;

import static java.util.Objects.requireNonNull;

@Slf4j
public class IpfsPublishHandler implements InterceptHandler {


    private final IpfsableMqttServer ipfsableMqttServer;

    public IpfsPublishHandler(IpfsableMqttServer ipfsableMqttServer) {
        this.ipfsableMqttServer = ipfsableMqttServer;
    }

    @Override
    public String getID() {
        return "IpfsPublishHandler";
    }

    @Override
    public Class<?>[] getInterceptedMessageTypes() {
        //return InterceptHandler.ALL_MESSAGE_TYPES;
        return new Class[]{InterceptPublishMessage.class};
    }

    @Override
    public void onConnect(InterceptConnectMessage msg) {
    }

    @Override
    public void onDisconnect(InterceptDisconnectMessage msg) {
    }

    @Override
    public void onConnectionLost(InterceptConnectionLostMessage msg) {

    }

    @Override
    public void onPublish(InterceptPublishMessage msg) {
        final String content = msg.getPayload().toString(Charsets.UTF_8);

        IpfsMqttDto dto = new IpfsMqttDto();
        dto.setClientId(msg.getClientID());
        dto.setTopic(msg.getTopicName());
        dto.setContent(content);
        dto.setQos(msg.getQos().value());
        dto.setRetained(msg.isRetainFlag());
        dto.setDuplicate(msg.isDupFlag());

        ipfsableMqttServer.publishToIpfsOnly(dto);
    }

    @Override
    public void onSubscribe(InterceptSubscribeMessage msg) {
    }

    @Override
    public void onUnsubscribe(InterceptUnsubscribeMessage msg) {
    }

    @Override
    public void onMessageAcknowledged(InterceptAcknowledgedMessage msg) {
    }

    @Data
    public static class IpfsMqttDto {
        private String serverId;
        private String content;
        private String topic;
        private String clientId;
        private int qos;
        private boolean retained;
        private boolean duplicate;
    }
}