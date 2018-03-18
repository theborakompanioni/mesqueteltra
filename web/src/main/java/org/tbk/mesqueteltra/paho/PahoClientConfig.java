package org.tbk.mesqueteltra.paho;

import com.google.common.base.Charsets;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.tbk.mesqueteltra.paho.PahoClientProperties.PahoSslProperties;

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
@EnableConfigurationProperties(PahoClientProperties.class)
@ConditionalOnProperty(value = "mesqueteltra.mqtt.client.enabled", havingValue = "true")
public class PahoClientConfig {

    private final PahoClientProperties pahoClientProperties;

    @Autowired
    public PahoClientConfig(PahoClientProperties pahoClientProperties) {
        this.pahoClientProperties = requireNonNull(pahoClientProperties);
    }

    @Bean(autowire = Autowire.BY_TYPE)
    public PahoClientSubscribeExample pahoClientSubscribeExample() {
        return new PahoClientSubscribeExample();
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public MqttClientPersistence mqttClientPersistence() {
        return new MemoryPersistence();
    }

    @Bean(destroyMethod = "disconnectForcibly")
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public MqttClient mqttClient(MqttClientPersistence persistence) throws MqttException {
        String clientId = String.format("%s-%s",
                pahoClientProperties.getClientName(), UUID.randomUUID());
        return new MqttClient(pahoClientProperties.getBroker(), clientId, persistence);
    }

    @Bean
    public MqttConnectOptions mqttConnectOptions() {
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
            connOpts.setSocketFactory(sslContext(pahoClientProperties.getSsl().get()).getSocketFactory());
        }
        return connOpts;
    }

    private SSLContext sslContext(PahoSslProperties sslProperties) {
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
