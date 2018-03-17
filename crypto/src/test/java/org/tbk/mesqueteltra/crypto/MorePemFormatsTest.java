package org.tbk.mesqueteltra.crypto;

import org.junit.Test;

import java.security.cert.X509Certificate;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.*;
import static org.tbk.mesqueteltra.crypto.TestValues.ANY_CA_KEYPAIR;
import static org.tbk.mesqueteltra.crypto.TestValues.ANY_CA_NAME;

public class MorePemFormatsTest {
    static {
        MoreProviders.addBounceCastleProvider();
    }

    @Test
    public void exportAndImportFromPem() throws Exception {
        final X509Certificate selfSignedCertificate = new X509CertificateServiceImpl().createSelfSignedCertificate(ANY_CA_NAME, ANY_CA_KEYPAIR);

        assertThat(selfSignedCertificate, is(notNullValue()));

        final String pem = MorePemFormats.writeCertificateAsPem(selfSignedCertificate);

        final X509Certificate importedCert = MorePemFormats.readCertificateFromPem(pem);

        assertThat(importedCert, is(selfSignedCertificate));
    }
}