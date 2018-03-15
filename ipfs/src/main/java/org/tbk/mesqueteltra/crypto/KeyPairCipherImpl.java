package org.tbk.mesqueteltra.crypto;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

public class KeyPairCipherImpl implements KeyPairCipher {
    private static String DEFAULT_XFORM = "RSA/ECB/PKCS1Padding";
    private final KeyPair keyPair;

    public KeyPairCipherImpl(KeyPair keyPair) {
        this.keyPair = Objects.requireNonNull(keyPair);
    }

    public byte[] encrypt(byte[] inpBytes) {
        try {
            Cipher cipher = Cipher.getInstance(DEFAULT_XFORM);
            cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPublic());
            return cipher.doFinal(inpBytes);
        } catch (NoSuchAlgorithmException |
                InvalidKeyException |
                BadPaddingException |
                NoSuchPaddingException |
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
