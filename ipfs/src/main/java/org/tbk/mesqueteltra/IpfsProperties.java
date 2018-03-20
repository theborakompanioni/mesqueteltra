package org.tbk.mesqueteltra;

import com.google.common.base.Strings;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("mesqueteltra.ipfs")
public class IpfsProperties {
    private boolean enabled;
    private String multiaddr;
    private String host;
    private int port;
    private String path = "/api/v0/";

    boolean hasHostAndPort() {
        return !Strings.isNullOrEmpty(host) && (port > 0 || port == -1);
    }
}
