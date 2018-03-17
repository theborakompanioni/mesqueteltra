package org.tbk.mesqueteltra.crypto;

import org.bouncycastle.asn1.x500.X500Name;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

import static java.util.Objects.requireNonNull;


@Configuration
@EnableConfigurationProperties(CaAuthorityProperties.class)
//@ConditionalOnProperty(value = "mesqueteltra.crypto.ca.enabled", havingValue = "true")
public class CertAuthorityConfig {
    static {
        MoreProviders.addBounceCastleProvider();
    }

    private final Environment environment;
    private final CaAuthorityProperties caAuthorityProperties;

    @Autowired
    public CertAuthorityConfig(Environment environment, CaAuthorityProperties caAuthorityProperties) {
        this.environment = requireNonNull(environment);
        this.caAuthorityProperties = requireNonNull(caAuthorityProperties);
    }

    @Bean
    public Issuer issuer() {
        X500Name name = new X500Name("CN=Issuer CA");
        KeyPair keyPair = keyPairFactory().createKeyPair();
        X509Certificate certificate = x509Certificates().createSelfSignedCertificate(name, keyPair);

        return Issuer.builder()
                .name(name)
                .keyPair(keyPair)
                .certificate(certificate)
                .build();
    }

    @Bean
    public KeyPairFactory keyPairFactory() {
        return new RsaKeyPairFactoryImpl();
    }

    @Bean
    public X509CertificateService x509Certificates() {
        return new X509CertificateServiceImpl();
    }
}
