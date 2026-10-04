package tn.esprit.pi.nefsiti.dto;

/**
 * Réponse de connexion. Si {@code mfaRequis} est vrai (connexion jugée inhabituelle par l'IA),
 * aucun token n'est délivré : le client doit confirmer avec son visage via /api/auth/mfa/visage.
 */
public record AuthResponse(String token, String type, long expiresIn, UtilisateurResponse utilisateur,
                           boolean mfaRequis, String mfaToken) {

    public static AuthResponse connecte(String token, long expiresIn, UtilisateurResponse utilisateur) {
        return new AuthResponse(token, "Bearer", expiresIn, utilisateur, false, null);
    }

    public static AuthResponse mfa(String mfaToken) {
        return new AuthResponse(null, null, 0, null, true, mfaToken);
    }
}
