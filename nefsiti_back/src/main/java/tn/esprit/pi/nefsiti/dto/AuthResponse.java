package tn.esprit.pi.nefsiti.dto;

public record AuthResponse(String token, String type, long expiresIn, UtilisateurResponse utilisateur) {
}
