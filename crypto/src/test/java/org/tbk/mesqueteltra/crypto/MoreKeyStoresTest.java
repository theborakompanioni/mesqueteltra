package org.tbk.mesqueteltra.crypto;

import com.google.common.io.BaseEncoding;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.RandomStringGenerator;
import org.junit.Test;

import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.text.CharacterPredicates.ASCII_ALPHA_NUMERALS;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.tbk.mesqueteltra.crypto.TestValues.ANY_CA_KEYPAIR;
import static org.tbk.mesqueteltra.crypto.TestValues.ANY_CA_NAME;

@Slf4j
public class MoreKeyStoresTest {
    static {
        MoreProviders.addBounceCastleProvider();
    }

    private static final RandomStringGenerator RANDOM_STRING_GENERATOR = new RandomStringGenerator.Builder()
            .filteredBy(ASCII_ALPHA_NUMERALS)
            .build();

    @Test
    public void itShouldExportAndImportKeyStore() throws Exception {
        final X509Certificate cert = new X509CertificateServiceImpl().createSelfSignedCertificate(ANY_CA_NAME, ANY_CA_KEYPAIR);

        char[] password = RANDOM_STRING_GENERATOR.generate(8).toCharArray();
        final PrivateKey privateKey = ANY_CA_KEYPAIR.getPrivate();
        final byte[] pfxPdu = MoreKeyStores.createPfx(password, privateKey, cert, cert);

        String encodedKeyStore = BaseEncoding.base64Url().encode(pfxPdu);
        log.info("keystore: \n", encodedKeyStore);

        byte[] decodedKeyStore = BaseEncoding.base64Url().decode(encodedKeyStore);

        final KeyStore keyStore = MoreKeyStores.readPfx(password, decodedKeyStore);

        assertThat(keyStore, is(notNullValue()));

        final List<Certificate> certificates = Collections.list(keyStore.aliases()).stream()
                .distinct()
                .filter(alias -> {
                    try {
                        return keyStore.isCertificateEntry(alias);
                    } catch (KeyStoreException e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(alias -> {
                    try {
                        return keyStore.getCertificate(alias);
                    } catch (KeyStoreException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());

        assertThat(certificates, hasSize(2));
        assertThat(certificates.get(0), is(cert));
        assertThat(certificates.get(1), is(cert));

        final List<Key> keys = Collections.list(keyStore.aliases()).stream()
                .distinct()
                .filter(alias -> {
                    try {
                        return keyStore.isKeyEntry(alias);
                    } catch (KeyStoreException e) {
                        throw new RuntimeException(e);
                    }
                })
                .map(alias -> {
                    try {
                        return keyStore.getKey(alias, password);
                    } catch (UnrecoverableKeyException | KeyStoreException | NoSuchAlgorithmException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());

        assertThat(keys, hasSize(1));
        assertThat(keys.get(0), is(privateKey));
    }

}