package org.tbk.mesqueteltra.moquette.config;

import io.netty.handler.codec.mqtt.MqttPublishMessage;
import org.tbk.mesqueteltra.moquette.ipfs.IpfsPublishHandler;
import org.tbk.mesqueteltra.mqtt.Mqttable;

public interface IpfsableMqttServer {

    void internalPublish(MqttPublishMessage msg, String clientId);

    void internalPublishFromIpfs(MqttPublishMessage msg, String clientId);

    void publishToIpfsOnly(MqttPublishMessage msg, String clientId);

}
