package org.tbk.mesqueteltra.crypto;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkArgument;

public class RsaKeyPairCipherImpl implements KeyPairCipher {
    private static String DEFAULT_XFORM = "RSA/ECB/PKCS1Padding";
    private final KeyPair keyPair;

    public RsaKeyPairCipherImpl(KeyPair keyPair) {
        this.keyPair = Objects.requireNonNull(keyPair);

        checkArgument(keyPair.getPublic().getAlgorithm().startsWith("RSA"));
        checkArgument(keyPair.getPrivate().getAlgorithm().startsWith("RSA"));
    }

    public byte[] encrypt(byte[] inpBytes) {
        try {
            Cipher cipher = Cipher.getInstance(DEFAULT_XFORM, BouncyCastleProvider.PROVIDER_NAME);
            cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
            return cipher.doFinal(inpBytes);
        } catch (NoSuchAlgorithmException |
                InvalidKeyException |
                BadPaddingException |
                NoSuchPaddingException |
                NoSuchProviderException |
                IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        }
    }

    public byte[] decrypt(byte[] inpBytes) {
        try {
            Cipher cipher = Cipher.getInstance(DEFAULT_XFORM);
            cipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
            return cipher.doFinal(inpBytes);
        } catch (NoSuchAlgorithmException |
                InvalidKeyException |
                NoSuchPaddingException |
                BadPaddingException |
                IllegalBlockSizeException e) {
            throw new RuntimeException(e);
        }
    }

}
