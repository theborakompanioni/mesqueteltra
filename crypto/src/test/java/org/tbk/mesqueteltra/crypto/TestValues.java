package org.tbk.mesqueteltra.crypto;

import org.bouncycastle.asn1.x500.X500Name;

import javax.security.auth.x500.X500Principal;
import java.security.KeyPair;

public class TestValues {
    public static final X500Name ANY_CA_NAME = new X500Name("CN=Issuer CA");
    public static final KeyPair ANY_CA_KEYPAIR = new RsaKeyPairFactoryImpl().createKeyPair();
    public static final X500Principal ANY_PRINCIPAL = new X500Principal("C=US, L=Vienna, O=Your Company Inc, CN=yourdomain.com/emailAddress=your@email.com");
}
