package org.tbk.mesqueteltra.moquette.config;

import io.netty.handler.codec.mqtt.MqttPublishMessage;
import org.tbk.mesqueteltra.moquette.ipfs.IpfsPublishHandler;

public interface IpfsableMqttServer {

    void internalPublish(MqttPublishMessage msg, String clientId);

    void internalPublishFromIpfs(IpfsPublishHandler.IpfsMqttDto msg);

    void publishToIpfsOnly(IpfsPublishHandler.IpfsMqttDto msg);

}
