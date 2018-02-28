package org.tbk.mesqueteltra;

import io.ipfs.api.IPFS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.io.IOException;

import static java.util.Objects.requireNonNull;

@Configuration
@EnableConfigurationProperties(IpfsProperties.class)
@ConditionalOnProperty(value = "mesqueteltra.ipfs.enabled", havingValue = "true")
public class IpfsConfig {

    private final Environment environment;
    private final IpfsProperties ipfsProperties;

    @Autowired
    public IpfsConfig(Environment environment, IpfsProperties ipfsProperties) {
        this.environment = requireNonNull(environment);
        this.ipfsProperties = requireNonNull(ipfsProperties);
    }

    @Bean
    public IPFS ipfs() throws IOException {
        final IPFS ipfs = new IPFS(ipfsProperties.getMultiaddr());
        ipfs.refs.local();
        return ipfs;
    }

    @Bean
    public IpfsService ipfsService(IPFS ipfs) {
        return new IpfsServiceImpl(ipfs);
    }

}
