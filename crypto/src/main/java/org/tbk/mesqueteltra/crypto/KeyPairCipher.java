package org.tbk.mesqueteltra.crypto;

public interface KeyPairCipher {
    byte[] encrypt(byte[] bytes);

    byte[] decrypt(byte[] inpBytes);
}
