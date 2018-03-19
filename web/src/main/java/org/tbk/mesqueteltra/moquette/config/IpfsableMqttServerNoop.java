package org.tbk.mesqueteltra.moquette.config;

import io.moquette.server.Server;
import lombok.extern.slf4j.Slf4j;
import org.tbk.mesqueteltra.moquette.ipfs.IpfsPublishHandler;

@Slf4j
public class IpfsableMqttServerNoop extends DelegatingIpfsableMqttServerImpl {

    public IpfsableMqttServerNoop(Server server) {
        super(server);
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
