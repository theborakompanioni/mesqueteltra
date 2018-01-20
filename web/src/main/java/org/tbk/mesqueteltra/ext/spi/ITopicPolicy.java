package org.tbk.mesqueteltra.ext.spi;

import io.moquette.spi.impl.subscriptions.Topic;

public interface ITopicPolicy {

    boolean supports(Topic topicName);

    boolean isReadable();

    boolean isWriteable();
}
