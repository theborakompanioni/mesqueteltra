package org.tbk.mesqueteltra.kafka;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "mesqueteltra.kafka")
public class KafkaProperties {
    private boolean enabled;
}
