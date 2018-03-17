package org.tbk.mesqueteltra;

import io.ipfs.api.IPFS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.tbk.mesqueteltra.crypto.Issuer;
import org.tbk.mesqueteltra.crypto.KeyPairCipher;
import org.tbk.mesqueteltra.crypto.RsaKeyPairCipherImpl;

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
    public KeyPairCipher keyPairCipher(Issuer issuer) {
        return new RsaKeyPairCipherImpl(issuer.getKeyPair());
    }

    @Bean
    public IpfsService ipfsService(IPFS ipfs) {
        return new IpfsServiceImpl(ipfs);
    }

    @Bean
    @Primary
    public IpfsService encryptedIpfsService(IPFS ipfs, KeyPairCipher keyPairCipher) {
        IpfsService ipfsService = ipfsService(ipfs);
        return new EncryptedIpfsService(ipfsService, keyPairCipher);
    }
}
