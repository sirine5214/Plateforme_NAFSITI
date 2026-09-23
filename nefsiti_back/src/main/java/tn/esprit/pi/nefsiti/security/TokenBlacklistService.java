package tn.esprit.pi.nefsiti.security;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Tokens révoqués par un logout, conservés jusqu'à leur expiration naturelle. */
@Service
public class TokenBlacklistService {

    private final Map<String, Long> revoques = new ConcurrentHashMap<>();

    public void revoquer(String jti, long expirationEpochMs) {
        purger();
        revoques.put(jti, expirationEpochMs);
    }

    public boolean estRevoque(String jti) {
        return jti != null && revoques.containsKey(jti);
    }

    private void purger() {
        long maintenant = System.currentTimeMillis();
        revoques.values().removeIf(exp -> exp < maintenant);
    }
}
