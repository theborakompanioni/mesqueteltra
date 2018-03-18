package org.tbk.mesqueteltra.paho;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Optional;

@Data
@ConfigurationProperties("mesqueteltra.mqtt.demo.clientA")
public class PahoClientAProperties {
    private boolean enabled;
    private String broker = "tcp://localhost:18881";
    private String clientName = "example-client";
    private String user = "example-user";
    private String password = "example";
    private PahoSslProperties ssl;


    public Optional<PahoSslProperties> getSsl() {
        return Optional.ofNullable(ssl);
    }

    @Data
    public static class PahoSslProperties {
        private String jksPath = "keystore/example/client_example_keystore.jks";
        private String keyStorePassword = "example";
    }
}
