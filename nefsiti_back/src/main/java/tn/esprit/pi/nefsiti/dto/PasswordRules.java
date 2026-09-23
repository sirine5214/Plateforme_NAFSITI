package tn.esprit.pi.nefsiti.dto;

/** Règle de mot de passe partagée avec le front (PASSWORD_PATTERN). */
public final class PasswordRules {

    public static final String PATTERN = "^(?=.*[A-Za-z])(?=.*\\d).{8,}$";
    public static final String MESSAGE = "Le mot de passe doit contenir au moins 8 caractères, dont une lettre et un chiffre";

    private PasswordRules() {
    }

    public static boolean estValide(String motDePasse) {
        return motDePasse != null && motDePasse.matches(PATTERN);
    }
}
