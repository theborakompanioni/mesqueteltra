package org.tbk.mesqueteltra.paho;

import com.google.common.base.Charsets;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.*;
import java.net.URL;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.UUID;

import static java.util.Objects.requireNonNull;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Slf4j
@Configuration
@Import(PahoClientConfig.class)
@EnableConfigurationProperties({PahoClientAProperties.class, PahoClientBProperties.class})
public class PahoClientExampleConfig {

    private final PahoClientAProperties pahoClientAProperties;
    private final PahoClientBProperties pahoClientBProperties;

    @Autowired
    public PahoClientExampleConfig(PahoClientAProperties pahoClientAProperties, PahoClientBProperties pahoClientBProperties) {
        this.pahoClientAProperties = requireNonNull(pahoClientAProperties);
        this.pahoClientBProperties = requireNonNull(pahoClientBProperties);
    }

    @Bean(value = "mqttClientA", destroyMethod = "disconnectForcibly")
    public MqttClient mqttClientA(MqttClientPersistence persistence) throws MqttException {
        String clientId = String.format("%s-%s",
                pahoClientAProperties.getClientName(), UUID.randomUUID());
        return new MqttClient(pahoClientAProperties.getBroker(), clientId, persistence);
    }

    @Bean(value = "mqttClientB", destroyMethod = "disconnectForcibly")
    public MqttClient mqttClientB(MqttClientPersistence persistence) throws MqttException {
        String clientId = String.format("%s-%s",
                pahoClientBProperties.getClientName(), UUID.randomUUID());
        return new MqttClient(pahoClientBProperties.getBroker(), clientId, persistence);
    }

    @Bean(value = "mqttConnectOptionsA")
    public MqttConnectOptions mqttConnectOptionsA(PahoClientAProperties pahoClientProperties) {
        return createMqttConnectOptionsA(pahoClientProperties);
    }

    @Bean(value = "mqttConnectOptionsB")
    public MqttConnectOptions mqttConnectOptionsB(PahoClientBProperties pahoClientProperties) {
        return createMqttConnectOptionsB(pahoClientProperties);
    }

    private MqttConnectOptions createMqttConnectOptionsA(PahoClientAProperties pahoClientProperties) {
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        connOpts.setWill("/goodbye",
                "goodbye".getBytes(Charsets.UTF_8),
                1,
                false);

        boolean useCredentials = isNotBlank(pahoClientProperties.getUser())
                && isNotBlank(pahoClientProperties.getPassword());

        if (useCredentials) {
            connOpts.setUserName(pahoClientProperties.getUser());
            connOpts.setPassword(pahoClientProperties.getPassword().toCharArray());
        }

        if (pahoClientProperties.getSsl().isPresent()) {
            connOpts.setSocketFactory(sslContextA(pahoClientProperties.getSsl().get()).getSocketFactory());
        }
        return connOpts;
    }

    private MqttConnectOptions createMqttConnectOptionsB(PahoClientBProperties pahoClientProperties) {
        MqttConnectOptions connOpts = new MqttConnectOptions();
        connOpts.setCleanSession(true);
        connOpts.setWill("/goodbye",
                "goodbye".getBytes(Charsets.UTF_8),
                1,
                false);

        boolean useCredentials = isNotBlank(pahoClientProperties.getUser())
                && isNotBlank(pahoClientProperties.getPassword());

        if (useCredentials) {
            connOpts.setUserName(pahoClientProperties.getUser());
            connOpts.setPassword(pahoClientProperties.getPassword().toCharArray());
        }

        if (pahoClientProperties.getSsl().isPresent()) {
            connOpts.setSocketFactory(sslContextB(pahoClientProperties.getSsl().get()).getSocketFactory());
        }
        return connOpts;
    }

    private SSLContext sslContextB(PahoClientBProperties.PahoSslProperties sslProperties) {
        try (InputStream is = jksDatastore(sslProperties.getJksPath())) {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(is, sslProperties.getKeyStorePassword().toCharArray());
            trustManagerFactory.init(ks);
            sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
            return sslContext;
        } catch (IOException |
                CertificateException |
                NoSuchAlgorithmException |
                KeyStoreException |
                KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }


    private SSLContext sslContextA(PahoClientAProperties.PahoSslProperties sslProperties) {
        try (InputStream is = jksDatastore(sslProperties.getJksPath())) {
            SSLContext sslContext = SSLContext.getInstance("SSL");
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            KeyStore ks = KeyStore.getInstance("JKS");
            ks.load(is, sslProperties.getKeyStorePassword().toCharArray());
            trustManagerFactory.init(ks);
            sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
            return sslContext;
        } catch (IOException |
                CertificateException |
                NoSuchAlgorithmException |
                KeyStoreException |
                KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    private InputStream jksDatastore(String jksPath) {
        try {
            URL jksUrl = this.getClass().getClassLoader().getResource(jksPath);
            if (jksUrl != null) {
                log.info("Starting with jks at {}, jks normal {}", jksUrl.toExternalForm(), jksUrl);
                return this.getClass().getClassLoader().getResourceAsStream(jksPath);
            } else {
                log.warn("No keystore has been found in the bundled resources. Scanning filesystem...");
                File jksFile = new File(jksPath);
                if (jksFile.exists()) {
                    log.info("Loading external keystore. Url = {}.", jksFile.getAbsolutePath());
                    return new FileInputStream(jksFile);
                } else {
                    log.warn("The keystore file does not exist. Url = {}.", jksFile.getAbsolutePath());
                    return null;
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
