package org.tbk.mesqueteltra.crypto;

import org.junit.Before;
import org.junit.Test;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Base64;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

public class RsaKeyPairFactoryImplTest {
    static {
        MoreProviders.addBounceCastleProvider();
    }

    private RsaKeyPairFactoryImpl sut;

    @Before
    public void setUp() {
        this.sut = new RsaKeyPairFactoryImpl();
    }

    @Test
    public void itShouldCreateKeyPair() {
        KeyPair keyPair = this.sut.createKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        assertThat(privateKey, is(notNullValue()));
        assertThat(publicKey, is(notNullValue()));
    }


    public static void main(String[] args) {
        KeyPair keyPair = new RsaKeyPairFactoryImpl().createKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        Base64.Encoder encoder = Base64.getEncoder();
        System.out.println("privateKey: " + encoder.encodeToString(privateKey.getEncoded()));
        System.out.println("publicKey: " + encoder.encodeToString(publicKey.getEncoded()));
    }


}