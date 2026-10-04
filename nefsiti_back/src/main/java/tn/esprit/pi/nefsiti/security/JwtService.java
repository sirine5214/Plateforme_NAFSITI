package tn.esprit.pi.nefsiti.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tn.esprit.pi.nefsiti.entities.Utilisateur;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private static final String CLAIM_TYPE = "typ";
    private static final String TYPE_MFA = "mfa";

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.expirationMs = expirationMs;
    }

    /** Le sujet est l'id (stable même si l'utilisateur change d'email). */
    public String generer(Utilisateur u) {
        Date maintenant = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(u.getId()))
                .claim("email", u.getEmail())
                .claim("role", u.getRole().name())
                .issuedAt(maintenant)
                .expiration(new Date(maintenant.getTime() + expirationMs))
                .signWith(key)
                .compact();
    }

    /**
     * Jeton intermédiaire délivré quand l'IA exige une vérification faciale :
     * il ne donne accès à aucune route (refusé par JwtAuthenticationFilter).
     */
    public String genererMfa(Utilisateur u, long dureeMs) {
        Date maintenant = new Date();
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(u.getId()))
                .claim(CLAIM_TYPE, TYPE_MFA)
                .issuedAt(maintenant)
                .expiration(new Date(maintenant.getTime() + dureeMs))
                .signWith(key)
                .compact();
    }

    public static boolean estMfa(Claims claims) {
        return TYPE_MFA.equals(claims.get(CLAIM_TYPE, String.class));
    }

    /** @throws JwtException si le token est invalide, falsifié ou expiré. */
    public Claims lire(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public long getExpirationMs() {
        return expirationMs;
    }
}
