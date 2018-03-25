package org.tbk.mesqueteltra.moquette.ipfs;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.tbk.mesqueteltra.moquette.config.IpfsableMqttServer;
import org.tbk.mesqueteltra.mqtt.Mqttable;
import reactor.core.Disposable;

import static java.util.Objects.requireNonNull;

@Slf4j
public class PublishInternalMqttIpfsSubscriber implements InitializingBean, DisposableBean {

    private final Mqttable ipfsService;
    private final IpfsableMqttServer server;

    private volatile Disposable subscribe;

    public PublishInternalMqttIpfsSubscriber(Mqttable ipfsService, IpfsableMqttServer server) {
        this.ipfsService = requireNonNull(ipfsService);
        this.server = requireNonNull(server);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.subscribe = ipfsService.subscribeToAll()
                .subscribe(msg -> server.internalPublishFromIpfs(msg.getMessage(), msg.getClientId()));
    }

    @Override
    public void destroy() throws Exception {
        this.subscribe.dispose();
    }
}