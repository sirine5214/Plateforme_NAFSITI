package tn.esprit.pi.nefsiti.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi.nefsiti.dto.AuthResponse;
import tn.esprit.pi.nefsiti.dto.LoginRequest;
import tn.esprit.pi.nefsiti.dto.RegisterRequest;
import tn.esprit.pi.nefsiti.dto.UtilisateurResponse;
import tn.esprit.pi.nefsiti.entities.Role;
import tn.esprit.pi.nefsiti.entities.Utilisateur;
import tn.esprit.pi.nefsiti.exceptions.ApiException;
import tn.esprit.pi.nefsiti.repositories.UtilisateurRepository;
import tn.esprit.pi.nefsiti.security.JwtService;
import tn.esprit.pi.nefsiti.security.TokenBlacklistService;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UtilisateurRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final TokenBlacklistService blacklist;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (req.role() == Role.ADMINISTRATEUR) {
            throw ApiException.badRequest("Inscription impossible avec le rôle administrateur");
        }
        String email = normaliserEmail(req.email());
        if (repository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Un compte existe déjà avec cet email");
        }
        Utilisateur u = repository.save(Utilisateur.builder()
                .nom(req.nom().trim())
                .prenom(req.prenom().trim())
                .email(email)
                .motDePasse(passwordEncoder.encode(req.motDePasse()))
                .role(req.role())
                .actif(true)
                .build());
        return reponse(u);
    }

    /** BadCredentialsException / DisabledException gérées par GlobalExceptionHandler. */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        String email = normaliserEmail(req.email());
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, req.motDePasse()));
        Utilisateur u = repository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
        return reponse(u);
    }

    /** Révoque le token courant jusqu'à son expiration. */
    public void logout(String token) {
        try {
            Claims claims = jwtService.lire(token);
            blacklist.revoquer(claims.getId(), claims.getExpiration().getTime());
        } catch (JwtException | IllegalArgumentException ignored) {
            // token déjà invalide : rien à révoquer
        }
    }

    @Transactional(readOnly = true)
    public UtilisateurResponse me(Long id) {
        return repository.findById(id)
                .map(UtilisateurResponse::from)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
    }

    private AuthResponse reponse(Utilisateur u) {
        return new AuthResponse(jwtService.generer(u), "Bearer", jwtService.getExpirationMs(),
                UtilisateurResponse.from(u));
    }

    static String normaliserEmail(String email) {
        return email.trim().toLowerCase();
    }
}
