package org.tbk.mesqueteltra.crypto;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;

import javax.security.auth.x500.X500Principal;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;

public interface X509CertificateService {

    X509Certificate createSelfSignedCertificate(X500Name name, KeyPair caKeyPair);

    X509Certificate createCertificateFromRequest(PKCS10CertificationRequest csr, X509Certificate caCertificate, PrivateKey caPrivateKey);

    PKCS10CertificationRequest createSigningRequest(X500Principal principal, KeyPair keyPair);
}
