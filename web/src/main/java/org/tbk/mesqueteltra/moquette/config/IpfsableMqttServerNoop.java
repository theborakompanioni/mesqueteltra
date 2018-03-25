package org.tbk.mesqueteltra.moquette.config;

import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IpfsableMqttServerNoop extends DelegatingIpfsableMqttServerImpl {

    public IpfsableMqttServerNoop(ServerWithInternalPublish server) {
        super(server);
    }

    @Override
    public void internalPublishFromIpfs(MqttPublishMessage msg, String clientId) {
        // noop
    }

    @Override
    public void publishToIpfsOnly(MqttPublishMessage msg, String clientId) {
        // noop
    }
}
