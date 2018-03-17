package org.tbk.mesqueteltra.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;

public final class MoreProviders {
    private MoreProviders() {
        throw new UnsupportedOperationException();
    }

    public static void addBounceCastleProvider() {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.insertProviderAt(new BouncyCastleProvider(), 1);
        }
    }
}
