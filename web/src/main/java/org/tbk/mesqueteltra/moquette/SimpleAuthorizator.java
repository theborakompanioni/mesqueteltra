package org.tbk.mesqueteltra.moquette;

import io.moquette.spi.impl.subscriptions.Topic;
import io.moquette.spi.security.IAuthorizator;

public class SimpleAuthorizator implements IAuthorizator {
    @Override
    public boolean canWrite(Topic topic, String s, String s1) {
        return true;
    }

    @Override
    public boolean canRead(Topic topic, String s, String s1) {
        return true;
    }
}
