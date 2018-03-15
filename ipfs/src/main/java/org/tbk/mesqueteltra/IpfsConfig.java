package org.tbk.mesqueteltra;

import io.ipfs.api.IPFS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.tbk.mesqueteltra.crypto.KeyPairCipher;
import org.tbk.mesqueteltra.crypto.KeyPairCipherImpl;
import org.tbk.mesqueteltra.crypto.KeyPairFactory;
import org.tbk.mesqueteltra.crypto.KeyPairFactoryImpl;

import java.io.IOException;
import java.security.KeyPair;

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
    public KeyPairCipher keyPairCipher(KeyPair keyPair) {
        return new KeyPairCipherImpl(keyPair);
    }

    @Bean
    public KeyPairFactory keyPairFactory() {
        return new KeyPairFactoryImpl();
    }

    @Bean
    public KeyPair keyPair(KeyPairFactory keyPairFactory) {
        return keyPairFactory.createKeyPair();
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
