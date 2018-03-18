package org.tbk.mesqueteltra.moquette.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("mesqueteltra.mqtt.server")
public class MoquetteProperties {
    private int port = 1883;
    private int websocketPort = 18883;
    private String host = "0.0.0.0";
    private boolean allowAnonymous;
}
