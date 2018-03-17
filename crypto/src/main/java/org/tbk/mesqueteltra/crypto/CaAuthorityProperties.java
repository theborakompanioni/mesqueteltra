package org.tbk.mesqueteltra.crypto;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties("mesqueteltra.crypto.ca")
public class CaAuthorityProperties {
    //private boolean enabled;
    private boolean createIfMissing = true;
    private String pathToCertificatePem;
}
