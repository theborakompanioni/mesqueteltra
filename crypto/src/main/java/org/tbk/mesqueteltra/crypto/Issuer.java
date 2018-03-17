package org.tbk.mesqueteltra.crypto;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.bouncycastle.asn1.x500.X500Name;

import java.security.KeyPair;
import java.security.cert.X509Certificate;

@Value
@Builder
public class Issuer {
    @NonNull
    private X500Name name;
    @NonNull
    private KeyPair keyPair;
    @NonNull
    private X509Certificate certificate;
}
