package org.tbk.mesqueteltra.moquette.config;

import io.moquette.server.Server;
import io.netty.handler.codec.mqtt.MqttPublishMessage;
import lombok.extern.slf4j.Slf4j;
import org.tbk.mesqueteltra.moquette.ipfs.IpfsPublishHandler;

import static java.util.Objects.requireNonNull;

@Slf4j
public class IpfsableMqttServerNoop extends AbstractIpfsableMqttServer {

    private final Server server;

    public IpfsableMqttServerNoop(Server server) {
        this.server = requireNonNull(server);
    }

    @Override
    public void internalPublish(MqttPublishMessage msg, String clientId) {
        server.internalPublish(msg, clientId);
    }

    @Override
    public void internalPublishFromIpfs(IpfsPublishHandler.IpfsMqttDto ipfsMqttDto) {
        // noop
    }

    @Override
    public void publishToIpfsOnly(IpfsPublishHandler.IpfsMqttDto msg) {
        // noop
    }
}
