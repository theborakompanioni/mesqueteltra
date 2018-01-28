package org.tbk.mesqueteltra.moquette.custom.redis;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "mesqueteltra.redis")
public class RedisProperties {
    private boolean enabled;

    private int port;
}
