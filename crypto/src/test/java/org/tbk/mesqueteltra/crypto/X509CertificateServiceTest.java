package org.tbk.mesqueteltra.crypto;

import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.junit.Before;
import org.junit.Test;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.tbk.mesqueteltra.crypto.TestValues.*;

@Slf4j
public class X509CertificateServiceTest {
    static {
        MoreProviders.addBounceCastleProvider();
    }

    private X509CertificateServiceImpl sut;

    @Before
    public void setUp() {
        this.sut = new X509CertificateServiceImpl();
    }

    @Test
    public void createSelfSignedCertificate() throws Exception {
        final X509Certificate selfSignedCertificate = sut.createSelfSignedCertificate(ANY_CA_NAME, ANY_CA_KEYPAIR);

        assertThat(selfSignedCertificate, is(notNullValue()));
        assertThat(selfSignedCertificate.getVersion(), is(3));

        log.info("X509Certificate:\n{}", selfSignedCertificate);
    }

    @Test
    public void createCertificateSigningRequest() throws Exception {
        final KeyPair caKeyPair = new RsaKeyPairFactoryImpl().createKeyPair();

        final PKCS10CertificationRequest certificationRequest = sut.createSigningRequest(ANY_PRINCIPAL, caKeyPair);

        assertThat(certificationRequest, is(notNullValue()));

        log.info("PKCS10CertificationRequest:\n{}", certificationRequest);
    }

    @Test
    public void createCertificateFromSigningRequest() throws Exception {
        final X509Certificate caCertificate = sut.createSelfSignedCertificate(ANY_CA_NAME, ANY_CA_KEYPAIR);

        final KeyPair clientKeyPair = new RsaKeyPairFactoryImpl().createKeyPair();

        final PKCS10CertificationRequest csr = sut.createSigningRequest(ANY_PRINCIPAL, clientKeyPair);

        final X509Certificate clientCertificate = sut.createCertificateFromRequest(csr, caCertificate, ANY_CA_KEYPAIR.getPrivate());

        assertThat(clientCertificate, is(notNullValue()));

        clientCertificate.checkValidity();
        clientCertificate.verify(ANY_CA_KEYPAIR.getPublic());

        log.info("X509Certificate:\n{}", clientCertificate);
    }

}