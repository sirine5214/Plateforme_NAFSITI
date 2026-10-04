package tn.esprit.pi.nefsiti.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Chiffrement AES-256-GCM des données de santé au repos (journal, messages, empreintes faciales).
 * La clé est statique car Hibernate instancie lui-même les AttributeConverter.
 */
@Component
public class Chiffrement {

    private static final String PREFIXE = "v1:";
    private static final int IV_OCTETS = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private static SecretKey cle;

    public Chiffrement(@Value("${app.chiffrement.cle}") String cleBase64) {
        byte[] octets = Base64.getDecoder().decode(cleBase64);
        if (octets.length != 32) {
            throw new IllegalStateException("app.chiffrement.cle doit contenir 32 octets encodés en Base64");
        }
        cle = new SecretKeySpec(octets, "AES");
    }

    public static String chiffrer(String clair) {
        if (clair == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_OCTETS];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, cleOuErreur(), new GCMParameterSpec(TAG_BITS, iv));
            byte[] chiffre = cipher.doFinal(clair.getBytes(StandardCharsets.UTF_8));
            return PREFIXE + Base64.getEncoder().encodeToString(
                    ByteBuffer.allocate(iv.length + chiffre.length).put(iv).put(chiffre).array());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Chiffrement impossible", e);
        }
    }

    public static String dechiffrer(String valeur) {
        if (valeur == null || !valeur.startsWith(PREFIXE)) {
            return valeur; // donnée antérieure au chiffrement
        }
        try {
            byte[] octets = Base64.getDecoder().decode(valeur.substring(PREFIXE.length()));
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, cleOuErreur(), new GCMParameterSpec(TAG_BITS, octets, 0, IV_OCTETS));
            return new String(cipher.doFinal(octets, IV_OCTETS, octets.length - IV_OCTETS), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Déchiffrement impossible (clé app.chiffrement.cle modifiée ?)", e);
        }
    }

    private static SecretKey cleOuErreur() {
        if (cle == null) {
            throw new IllegalStateException("Clé de chiffrement non initialisée");
        }
        return cle;
    }
}
