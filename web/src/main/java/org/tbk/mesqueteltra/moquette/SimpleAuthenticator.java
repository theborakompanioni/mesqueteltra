package org.tbk.mesqueteltra.moquette;

import com.google.common.base.Charsets;
import io.moquette.spi.security.IAuthenticator;

public class SimpleAuthenticator implements IAuthenticator {
    @Override
    public boolean checkValid(String clientId, String username, byte[] password) {
        return checkValid(clientId, username, new String(password, Charsets.UTF_8));
    }

    private boolean checkValid(String clientId, String username, String password) {
        return true;
    }
}
