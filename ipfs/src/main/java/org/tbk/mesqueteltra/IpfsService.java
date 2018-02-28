package org.tbk.mesqueteltra;

import io.ipfs.api.MerkleNode;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;
import reactor.core.publisher.Flux;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

public interface IpfsService {
    Flux<MerkleNode> persist(String fileName, byte[] data);

    Flux<MerkleNode> persist(String fileName, String data);

    Flux<Optional<Object>> publish(String topic, String data);

    Flux<IpfsMsg> subscribe(String topic);


    @Value
    @Builder
    class IpfsMsg {
        private String fromBase64;
        private String dataBase64;
        private String seqnoBase64;

        @Singular("addTopicId")
        private List<String> topicIds;

        public byte[] getData() {
            return Base64.getDecoder().decode(dataBase64);
        }

        public String getDataAsString() {
            return new String(getData(), StandardCharsets.UTF_8);
        }
    }
}
