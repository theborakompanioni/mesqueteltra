package org.tbk.mesqueteltra.crypto;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.ContentVerifierProvider;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaContentVerifierProviderBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.PKCSException;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

import javax.security.auth.x500.X500Principal;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public class X509CertificateServiceImpl implements X509CertificateService {

    private static String SIGNATURE_ALGORITHM = "SHA512withRSA";

    @Override
    public X509Certificate createSelfSignedCertificate(X500Name name, KeyPair caKeyPair) {
        requireNonNull(name);
        requireNonNull(caKeyPair);

        BigInteger serial = createSerial();

        Date notBefore = Date.from(LocalDateTime.now()
                .minusDays(1)
                .toInstant(ZoneOffset.UTC));

        Date notAfter = Date.from(LocalDateTime.now()
                .plusDays(365)
                .toInstant(ZoneOffset.UTC));

        JcaX509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(
                name,
                serial,
                notBefore,
                notAfter,
                name,
                caKeyPair.getPublic());

        try {

            JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
            certificateBuilder.addExtension(
                    Extension.subjectKeyIdentifier,
                    false,
                    extUtils.createSubjectKeyIdentifier(caKeyPair.getPublic()));
            certificateBuilder.addExtension(
                    Extension.basicConstraints,
                    true,
                    new BasicConstraints(true));

            ContentSigner contentSigner = createContentSigner(caKeyPair.getPrivate());
            X509CertificateHolder certificateHolder = certificateBuilder.build(contentSigner);

            X509Certificate certificate = createJcaX509CertificateConverter()
                    .getCertificate(certificateHolder);

            verifyCertificateOrThrow(caKeyPair.getPublic(), certificate);

            return certificate;
        } catch (CertificateException |
                CertIOException |
                NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public X509Certificate createCertificateFromRequest(PKCS10CertificationRequest csr, X509Certificate caCertificate, PrivateKey caPrivateKey) {
        requireNonNull(csr);
        requireNonNull(caCertificate);
        requireNonNull(caPrivateKey);
        checkArgument(verifyPkcs10Request(csr), "`PKCS10CertificationRequest` not valid");

        Date notBefore = Date.from(LocalDateTime.now()
                .minusDays(1)
                .toInstant(ZoneOffset.UTC));

        Date notAfter = Date.from(LocalDateTime.now()
                .plusDays(365)
                .toInstant(ZoneOffset.UTC));

        // This should be a serial number that the CA keeps track of
        BigInteger serial = createSerial();

        // These are the details of the CA
        X500Name issuer = new X500Name(caCertificate.getSubjectX500Principal().getName());

        try {
            PKCS10CertificationRequest csrHolder = new PKCS10CertificationRequest(csr.getEncoded());

            // Blanket grant the subject as requested in the CSR
            // A real CA would want to vet this.
            X500Name subject = csrHolder.getSubject();

            X509v3CertificateBuilder certificateGenerator = new X509v3CertificateBuilder(
                    issuer,
                    serial,
                    notBefore,
                    notAfter,
                    subject,
                    csr.getSubjectPublicKeyInfo()
            );

            JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();
            certificateGenerator.addExtension(Extension.subjectKeyIdentifier, false,
                    csr.getSubjectPublicKeyInfo());
            certificateGenerator.addExtension(Extension.authorityKeyIdentifier, false,
                    extUtils.createAuthorityKeyIdentifier(caCertificate));
            certificateGenerator.addExtension(Extension.basicConstraints, true,
                    new BasicConstraints(false));

            ContentSigner contentSigner = createContentSigner(caPrivateKey);

            X509CertificateHolder holder = certificateGenerator.build(contentSigner);
            CertificateFactory certificateFactory = getCertificateFactory();
            try (ByteArrayInputStream is = new ByteArrayInputStream(holder.toASN1Structure().getEncoded())) {
                return (X509Certificate) certificateFactory.generateCertificate(is);
            }
        } catch (CertificateException |
                NoSuchAlgorithmException |
                NoSuchProviderException |
                IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public PKCS10CertificationRequest createSigningRequest(X500Principal principal, KeyPair keyPair) {
        PKCS10CertificationRequestBuilder p10Builder = new JcaPKCS10CertificationRequestBuilder(
                principal,
                keyPair.getPublic()
        );

        ContentSigner signer = createContentSigner(keyPair.getPrivate());
        return p10Builder.build(signer);
    }

    private CertificateFactory getCertificateFactory() throws CertificateException, NoSuchProviderException {
        return CertificateFactory.getInstance("X.509", BouncyCastleProvider.PROVIDER_NAME);
    }

    private ContentSigner createContentSigner(PrivateKey privateKey, String algorithm) {
        try {
            JcaContentSignerBuilder csBuilder = new JcaContentSignerBuilder(algorithm)
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME);
            return csBuilder.build(privateKey);
        } catch (OperatorCreationException e) {
            throw new RuntimeException(e);
        }
    }

    private ContentSigner createContentSigner(PrivateKey privateKey) {
        return createContentSigner(privateKey, SIGNATURE_ALGORITHM);
    }


    private JcaX509CertificateConverter createJcaX509CertificateConverter() {
        return new JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME);
    }

    private void verifyCertificateOrThrow(PublicKey caPublicKey, X509Certificate certificate) {
        try {
            certificate.checkValidity();
            certificate.verify(caPublicKey);
        } catch (CertificateException |
                NoSuchAlgorithmException |
                InvalidKeyException |
                SignatureException |
                NoSuchProviderException e) {
            throw new RuntimeException(e);
        }
    }

    private BigInteger createSerial() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] serialValue = new byte[8];
        secureRandom.nextBytes(serialValue);
        return new BigInteger(serialValue);
    }

    private boolean verifyPkcs10Request(PKCS10CertificationRequest pkcs10Request) {
        try {
            ContentVerifierProvider verifierProvider = new JcaContentVerifierProviderBuilder()
                    .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                    .build(pkcs10Request.getSubjectPublicKeyInfo());
            return pkcs10Request.isSignatureValid(verifierProvider);
        } catch (OperatorCreationException | PKCSException e) {
            throw new RuntimeException(e);
        }
    }
}
