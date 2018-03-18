package org.tbk.mesqueteltra;

import com.google.common.base.Charsets;
import com.google.common.io.BaseEncoding;
import io.ipfs.api.MerkleNode;
import lombok.extern.slf4j.Slf4j;
import org.tbk.mesqueteltra.crypto.KeyPairCipher;
import reactor.core.publisher.Flux;

import java.util.Base64;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

@Slf4j
public class EncryptedIpfsService implements IpfsService {
    private final static BaseEncoding BASE64_URLSAFE = BaseEncoding.base64Url();

    private final IpfsService delegate;
    private final KeyPairCipher keyPairCipher;

    public EncryptedIpfsService(IpfsService delegate, KeyPairCipher keyPairCipher) {
        this.delegate = requireNonNull(delegate);
        this.keyPairCipher = requireNonNull(keyPairCipher);
    }

    @Override
    public Flux<MerkleNode> persist(String fileName, byte[] data) {
        return delegate.persist(fileName, keyPairCipher.encrypt(data));
    }

    @Override
    public Flux<MerkleNode> persist(String fileName, String data) {
        return delegate.persist(fileName, keyPairCipher.encrypt(data.getBytes(Charsets.UTF_8)));
    }

    @Override
    public Flux<Optional<Object>> publish(String topic, String data) {
        final byte[] encryptedData = keyPairCipher.encrypt(data.getBytes(Charsets.UTF_8));
        final String encryptedDataBase64 = BASE64_URLSAFE.encode(encryptedData);
        return delegate.publish(topic, encryptedDataBase64);
    }

    @Override
    public Flux<IpfsMsg> subscribe(String topic) {
        return delegate.subscribe(topic)
                .map(this::decryptData);
    }

    @Override
    public Flux<IpfsMsg> subscribeToAll() {
        return delegate.subscribeToAll()
                .map(this::decryptData);
    }

    private IpfsMsg decryptData(IpfsMsg msg) {
        final String encryptedStringBase64 = new String(msg.getData(), Charsets.UTF_8);
        byte[] encryptedData = BASE64_URLSAFE.decode(encryptedStringBase64);

        final byte[] decryptedData = keyPairCipher.decrypt(encryptedData);
        String decryptedString = new String(decryptedData, Charsets.UTF_8);
        String decryptedStringBase64 = Base64.getEncoder()
                .encodeToString(decryptedString.getBytes(Charsets.UTF_8));

        return msg.toBuilder()
                .dataBase64(decryptedStringBase64)
                .build();
    }
}
