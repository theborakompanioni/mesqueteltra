package org.tbk.mesqueteltra.moquette;

import com.google.common.collect.ImmutableList;
import io.moquette.spi.impl.subscriptions.Topic;
import io.moquette.spi.security.IAuthorizator;
import lombok.extern.slf4j.Slf4j;
import org.tbk.mesqueteltra.moquette.ext.spi.ITopicPolicy;

import java.util.List;

@Slf4j
public class SimpleAuthorizator implements IAuthorizator {

    private final List<ITopicPolicy> topicPolicies;

    public SimpleAuthorizator(List<ITopicPolicy> topicPolicies) {
        this.topicPolicies = ImmutableList.copyOf(topicPolicies);
    }

    @Override
    public boolean canWrite(Topic topic, String s, String s1) {
        boolean writeForbidden = topicPolicies.stream()
                .filter(p -> p.supports(topic))
                .anyMatch(p -> !p.isWriteable());

        if (writeForbidden) {
            log.warn("Block WRITE ACCESS for topic {} from {} ({})",
                    topic, s, s1);
        }

        return !writeForbidden;
    }

    @Override
    public boolean canRead(Topic topic, String s, String s1) {
        boolean readForbidden = topicPolicies.stream()
                .filter(p -> p.supports(topic))
                .anyMatch(p -> !p.isReadable());

        if (readForbidden) {
            log.warn("Block READ ACCESS for topic {} from {} ({})",
                    topic, s, s1);
        }

        return !readForbidden;

    }
}
