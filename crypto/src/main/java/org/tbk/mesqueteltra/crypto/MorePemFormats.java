package org.tbk.mesqueteltra.crypto;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class MorePemFormats {

    public static String writeCertificateAsPem(X509Certificate certificate) throws IOException {
        try (StringWriter stringWriter = new StringWriter();
             JcaPEMWriter pemWriter = new JcaPEMWriter(stringWriter)) {
            pemWriter.writeObject(certificate);
            pemWriter.flush();
            stringWriter.flush();
            return stringWriter.toString();
        }
    }

    public static X509Certificate readCertificateFromPem(String pemEncoding) {
        try {
            PEMParser parser = new PEMParser(new StringReader(pemEncoding));
            X509CertificateHolder certHolder = (X509CertificateHolder) parser.readObject();
            return new JcaX509CertificateConverter()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .getCertificate(certHolder);
        } catch (CertificateException |
                IOException e) {
            throw new RuntimeException(e);
        }
    }

    /*
    public static String writePrivateKeyAsEncryptedPem(char[] passwd, PrivateKey privateKey) {
        try (StringWriter sw = new StringWriter();
             JcaPEMWriter pemWriter = new JcaPEMWriter(sw)) {
            PKCS8EncryptedPrivateKeyInfoBuilder pkcs8Builder = new JcaPKCS8EncryptedPrivateKeyInfoBuilder(privateKey);
            pemWriter.writeObject(pkcs8Builder.build(new JcePKCSPBEOutputEncryptorBuilder(
                    NISTObjectIdentifiers.id_aes256_CBC)
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build(passwd)));
            return sw.toString();
        } catch (IOException | OperatorCreationException e) {
            throw new RuntimeException(e);
        }
    }

    public static PrivateKey readPrivateKeyFromEncryptedPem(char[] password, String pemEncoding) {
        try (PEMParser parser = new PEMParser(new StringReader(pemEncoding))) {
            PKCS8EncryptedPrivateKeyInfo encPrivKeyInfo = (PKCS8EncryptedPrivateKeyInfo) parser.readObject();
            InputDecryptorProvider pkcs8Prov = new JcePKCSPBEInputDecryptorProviderBuilder()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build(password);
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME);
            return converter.getPrivateKey(encPrivKeyInfo.decryptPrivateKeyInfo(pkcs8Prov));
        } catch (IOException | PKCSException e) {
            throw new RuntimeException(e);
        }
    }*/

}
