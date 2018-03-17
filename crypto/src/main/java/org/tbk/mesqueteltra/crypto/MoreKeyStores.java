package org.tbk.mesqueteltra.crypto;

import org.bouncycastle.asn1.DERBMPString;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS12PfxPduBuilder;
import org.bouncycastle.pkcs.PKCS12SafeBag;
import org.bouncycastle.pkcs.PKCS12SafeBagBuilder;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.pkcs.jcajce.JcaPKCS12SafeBagBuilder;
import org.bouncycastle.pkcs.jcajce.JcePKCS12MacCalculatorBuilder;
import org.bouncycastle.pkcs.jcajce.JcePKCSPBEOutputEncryptorBuilder;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static com.google.common.base.Preconditions.checkArgument;

public class MoreKeyStores {
    private MoreKeyStores() {
        throw new UnsupportedOperationException();
    }

    public static KeyStore readPfx(char[] passphrase, byte[] bytes) {
        try (InputStream is = new BufferedInputStream(new ByteArrayInputStream(bytes))) {
            KeyStore ks = KeyStore.getInstance("pkcs12", BouncyCastleProvider.PROVIDER_NAME);
            ks.load(is, passphrase);
            return ks;
        } catch (IOException |
                CertificateException |
                NoSuchAlgorithmException |
                KeyStoreException |
                NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] createPfx(char[] passphrase, PrivateKey privateKey, X509Certificate keyCert, X509Certificate caCert) {
        return createPfx(passphrase, privateKey, new X509Certificate[]{keyCert, caCert});
    }

    private static byte[] createPfx(char[] passphrase, PrivateKey privateKey, X509Certificate[] certs) {
        checkArgument(certs.length == 2);

        try {
            JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();

            // store the ca certificate
            PKCS12SafeBagBuilder caCertBagBuilder = new JcaPKCS12SafeBagBuilder(certs[1]);
            caCertBagBuilder.addBagAttribute(
                    PKCSObjectIdentifiers.pkcs_9_at_friendlyName, new DERBMPString("CA Certificate"));

            // store the key certificate
            PKCS12SafeBagBuilder eeCertBagBuilder = new JcaPKCS12SafeBagBuilder(certs[0]);
            eeCertBagBuilder.addBagAttribute(
                    PKCSObjectIdentifiers.pkcs_9_at_friendlyName, new DERBMPString("End Entity Key Cert"));
            eeCertBagBuilder.addBagAttribute(
                    PKCSObjectIdentifiers.pkcs_9_at_localKeyId,
                    extUtils.createSubjectKeyIdentifier(certs[0].getPublicKey()));

            // store the private key
            PKCS12SafeBagBuilder keyBagBuilder = new JcaPKCS12SafeBagBuilder(privateKey,
                    new JcePKCSPBEOutputEncryptorBuilder(
                            PKCSObjectIdentifiers.pbeWithSHAAnd3_KeyTripleDES_CBC)
                            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                            .build(passphrase));
            keyBagBuilder.addBagAttribute(
                    PKCSObjectIdentifiers.pkcs_9_at_friendlyName, new DERBMPString("End Entity Key"));
            keyBagBuilder.addBagAttribute(
                    PKCSObjectIdentifiers.pkcs_9_at_localKeyId,
                    extUtils.createSubjectKeyIdentifier(certs[0].getPublicKey()));

            // create the actual PKCS#12 blob.
            PKCS12SafeBag[] safeBags = new PKCS12SafeBag[2];
            safeBags[0] = eeCertBagBuilder.build();
            safeBags[1] = caCertBagBuilder.build();

            PKCS12PfxPduBuilder pfxPduBuilder = new PKCS12PfxPduBuilder()
                    .addEncryptedData(new JcePKCSPBEOutputEncryptorBuilder(
                            PKCSObjectIdentifiers.pbeWithSHAAnd3_KeyTripleDES_CBC)
                            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                            .build(passphrase), safeBags)
                    .addData(keyBagBuilder.build());

            return pfxPduBuilder.build(new JcePKCS12MacCalculatorBuilder()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME), passphrase).getEncoded();
        } catch (NoSuchAlgorithmException | OperatorCreationException | IOException | PKCSException e) {
            throw new RuntimeException(e);
        }
    }
}
