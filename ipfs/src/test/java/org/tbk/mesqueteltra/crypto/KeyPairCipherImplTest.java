package org.tbk.mesqueteltra.crypto;

import org.junit.Before;
import org.junit.Test;

import java.security.KeyPair;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class KeyPairCipherImplTest {

    private KeyPairCipherImpl sut;

    @Before
    public void setUp() {
        final KeyPair keyPair = new KeyPairFactoryImpl().createKeyPair();
        this.sut = new KeyPairCipherImpl(keyPair);
    }

    @Test
    public void itShouldEncryptContent() {
        byte[] dataBytes = "Hello World!".getBytes();

        byte[] encBytes = this.sut.encrypt(dataBytes);
        byte[] decBytes = this.sut.decrypt(encBytes);

        assertThat(decBytes, is(dataBytes));
    }

}