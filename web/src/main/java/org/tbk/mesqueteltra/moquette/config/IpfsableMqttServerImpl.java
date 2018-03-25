package org.tbk.mesqueteltra.moquette.config;

import com.google.common.base.Charsets;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.tbk.mesqueteltra.moquette.ipfs.IpfsPublishHandler;
import org.tbk.mesqueteltra.mqtt.Mqttable;

import static java.util.Objects.requireNonNull;

@Slf4j
public class IpfsableMqttServerImpl extends DelegatingIpfsableMqttServerImpl {

    private final Mqttable mqttable;

    public IpfsableMqttServerImpl(ServerWithInternalPublish server, Mqttable mqttable) {
        super(server);
        this.mqttable = requireNonNull(mqttable);
    }

    @Override
    public void internalPublish(MqttPublishMessage msg, String clientId) {
        internalPublishWithIpfs(msg, clientId);
    }

    @Override
    public void internalPublishFromIpfs(MqttPublishMessage msg, String clientId) {
        internalPublishWithoutIpfs(msg, clientId);
    }

    @Override
    public void publishToIpfsOnly(MqttPublishMessage msg, String clientId) {
        publishToIpfs(msg, clientId);
    }

    private void internalPublishWithIpfs(MqttPublishMessage msg, String clientId) {
        internalPublishWithoutIpfs(msg, clientId);
        publishToIpfs(msg.copy(), clientId);
    }

    private void internalPublishWithoutIpfs(MqttPublishMessage msg, String clientId) {
        super.internalPublish(msg, clientId);
    }

    private void publishToIpfs(MqttPublishMessage msg, String clientId) {
        mqttable.publish(msg, clientId)
                .subscribe(next -> {
                    log.debug("successfully published INTERNAL PUBLISH via IPFS: {}", msg.variableHeader().packetId());
                }, e -> {
                    log.debug("error while publishing INTERNAL PUBLISH via IPFS: {} - {}",
                            msg.variableHeader().packetId(), e.getMessage());
                });
    }
}
