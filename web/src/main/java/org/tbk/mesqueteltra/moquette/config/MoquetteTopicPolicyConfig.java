package org.tbk.mesqueteltra.moquette.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.tbk.mesqueteltra.moquette.ext.impl.RegexTopicPolicy;
import org.tbk.mesqueteltra.moquette.ext.spi.ITopicPolicy;

@Configuration
public class MoquetteTopicPolicyConfig {

    @Bean
    @Order(1_000_000)
    public ITopicPolicy readonlyTopicPolicy() {
        return RegexTopicPolicy.builder()
                .regex("/?readonly/?.*")
                .readable(true)
                .writeable(false)
                .build();
    }

    @Bean
    @Order(2_000_000)
    public ITopicPolicy writeonlyTopicPolicy() {
        return RegexTopicPolicy.builder()
                .regex("/?writeonly/?.*")
                .readable(false)
                .writeable(true)
                .build();
    }

    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE)
    public ITopicPolicy fallbackTopicPolicy() {
        return RegexTopicPolicy.builder()
                .regex(".+")
                .readable(true)
                .writeable(true)
                .build();
    }

}
