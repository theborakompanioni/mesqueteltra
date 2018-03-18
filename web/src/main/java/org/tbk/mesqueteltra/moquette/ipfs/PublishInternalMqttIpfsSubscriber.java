package org.tbk.mesqueteltra.moquette.ipfs;

import io.vertx.core.json.Json;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.tbk.mesqueteltra.IpfsService;
import org.tbk.mesqueteltra.moquette.config.IpfsableMqttServer;
import org.tbk.mesqueteltra.moquette.ipfs.IpfsPublishHandler.IpfsMqttDto;
import reactor.core.Disposable;

import static java.util.Objects.requireNonNull;

@Slf4j
public class PublishInternalMqttIpfsSubscriber implements InitializingBean, DisposableBean {

    private final IpfsService ipfsService;
    private final IpfsableMqttServer server;

    private volatile Disposable subscribe;

    public PublishInternalMqttIpfsSubscriber(IpfsService ipfsService, IpfsableMqttServer server) {
        this.ipfsService = requireNonNull(ipfsService);
        this.server = requireNonNull(server);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        this.subscribe = this.ipfsService.subscribeToAll()
                .map(msg -> {
                    String json = msg.getDataAsString();
                    IpfsMqttDto ipfsMqttDto = Json.decodeValue(json, IpfsMqttDto.class);
                    return ipfsMqttDto;
                })
                .doOnNext(dto -> {
                    log.debug("Message arrived via IPFS on topic {}: {}", dto.getTopic(), dto.getContent());
                })
                .subscribe(server::internalPublishFromIpfs);
    }

    @Override
    public void destroy() throws Exception {
        this.subscribe.dispose();
    }
}