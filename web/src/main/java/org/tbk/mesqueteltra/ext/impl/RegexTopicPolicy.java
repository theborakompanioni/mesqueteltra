package org.tbk.mesqueteltra.ext.impl;

import io.moquette.spi.impl.subscriptions.Topic;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import org.tbk.mesqueteltra.ext.spi.ITopicPolicy;

import static java.util.Objects.requireNonNull;

@Value
@Builder
public class RegexTopicPolicy implements ITopicPolicy {
    @NonNull
    private String regex;
    private boolean readable;
    private boolean writeable;

    @Override
    public boolean supports(Topic topic) {
        requireNonNull(topic);

        String topicName = requireNonNull(topic.toString());

        return topicName.matches(regex);
    }
}
