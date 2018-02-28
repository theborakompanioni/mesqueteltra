package org.tbk.mesqueteltra;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("mesqueteltra.ipfs")
public class IpfsProperties {
    private boolean enabled;
    private String multiaddr;
}
