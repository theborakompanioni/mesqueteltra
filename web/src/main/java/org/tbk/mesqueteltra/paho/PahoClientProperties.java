package org.tbk.mesqueteltra.paho;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("mesqueteltra.mqtt.client")
public class PahoClientProperties {
    private boolean enabled;
    private String broker = "tcp://localhost:1883";
    private String clientName = "mesqueteltra-client";
    private String user = "root";
    private String password = "public";
}
