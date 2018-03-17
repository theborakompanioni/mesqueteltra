package org.tbk.mesqueteltra.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.*;

public class RsaKeyPairFactoryImpl implements KeyPairFactory {

    @Override
    public KeyPair createKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", BouncyCastleProvider.PROVIDER_NAME);

            keyGen.initialize(2048, new SecureRandom());
            return keyGen.generateKeyPair();
        } catch (NoSuchAlgorithmException |
                NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }
}