package tn.esprit.pi.nefsiti.services;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi.nefsiti.dto.*;
import tn.esprit.pi.nefsiti.entities.MethodeConnexion;
import tn.esprit.pi.nefsiti.entities.Role;
import tn.esprit.pi.nefsiti.entities.Utilisateur;
import tn.esprit.pi.nefsiti.exceptions.ApiException;
import tn.esprit.pi.nefsiti.repositories.UtilisateurRepository;
import tn.esprit.pi.nefsiti.security.ContexteConnexion;
import tn.esprit.pi.nefsiti.security.JwtService;
import tn.esprit.pi.nefsiti.security.TokenBlacklistService;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UtilisateurRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final TokenBlacklistService blacklist;
    private final SecuriteConnexionService securite;
    private final VisageService visageService;

    @Value("${app.securite.mfa-expiration-ms}")
    private long mfaExpirationMs;

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
                .dateConsentement(LocalDateTime.now())
                .build());
        return reponse(u);
    }

    /** Connexion par mot de passe. BadCredentialsException / DisabledException gérées par GlobalExceptionHandler. */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req, ContexteConnexion ctx) {
        String email = normaliserEmail(req.email());
        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, req.motDePasse()));
        } catch (BadCredentialsException e) {
            Long id = repository.findByEmailIgnoreCase(email).map(Utilisateur::getId).orElse(null);
            securite.enregistrerEchec(id, MethodeConnexion.MOT_DE_PASSE, ctx, SecuriteConnexionService.ECHEC_MOT_DE_PASSE);
            throw e;
        }
        Utilisateur u = repository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
        return finaliser(u, MethodeConnexion.MOT_DE_PASSE, ctx);
    }

    /** Connexion par reconnaissance faciale : vérification 1:1 entre l'image et l'empreinte du compte. */
    @Transactional(readOnly = true)
    public AuthResponse loginVisage(LoginVisageRequest req, ContexteConnexion ctx) {
        Utilisateur u = repository.findByEmailIgnoreCase(normaliserEmail(req.email()))
                .orElseThrow(VisageService::nonDisponible);
        if (!u.isActif()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Votre compte est désactivé. Contactez un administrateur.");
        }
        if (!visageService.correspond(u.getId(), req.image())) {
            securite.enregistrerEchec(u.getId(), MethodeConnexion.VISAGE, ctx, SecuriteConnexionService.ECHEC_VISAGE);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Visage non reconnu. Réessayez ou utilisez votre mot de passe.");
        }
        return finaliser(u, MethodeConnexion.VISAGE, ctx);
    }

    /** Second facteur demandé par l'IA après un mot de passe correct : confirmation par le visage. */
    @Transactional(readOnly = true)
    public AuthResponse validerMfaVisage(MfaVisageRequest req, ContexteConnexion ctx) {
        Claims claims;
        try {
            claims = jwtService.lire(req.mfaToken());
        } catch (JwtException | IllegalArgumentException e) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Vérification expirée, reconnectez-vous");
        }
        if (!JwtService.estMfa(claims) || blacklist.estRevoque(claims.getId())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Vérification expirée, reconnectez-vous");
        }
        Utilisateur u = repository.findById(Long.valueOf(claims.getSubject()))
                .filter(Utilisateur::isActif)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Compte indisponible"));
        if (!visageService.correspond(u.getId(), req.image())) {
            securite.enregistrerEchec(u.getId(), MethodeConnexion.VISAGE, ctx, SecuriteConnexionService.ECHEC_VISAGE);
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Visage non reconnu. Réessayez.");
        }
        blacklist.revoquer(claims.getId(), claims.getExpiration().getTime()); // jeton à usage unique
        securite.enregistrerSucces(u.getId(), MethodeConnexion.VISAGE, ctx, "MFA_VALIDEE");
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

    /** Identité vérifiée : blocage temporaire, puis décision du détecteur d'anomalies (module 1). */
    private AuthResponse finaliser(Utilisateur u, MethodeConnexion methode, ContexteConnexion ctx) {
        securite.verifierNonBloque(u.getId());
        boolean mfaPossible = methode == MethodeConnexion.MOT_DE_PASSE && visageService.estEnregistre(u.getId());
        SecuriteConnexionService.Decision decision = securite.evaluerEtEnregistrer(u.getId(), methode, ctx, mfaPossible);
        if (decision.bloquer()) {
            throw securite.erreurBlocage();
        }
        if (decision.mfaExigee()) {
            return AuthResponse.mfa(jwtService.genererMfa(u, mfaExpirationMs));
        }
        return reponse(u);
    }

    private AuthResponse reponse(Utilisateur u) {
        return AuthResponse.connecte(jwtService.generer(u), jwtService.getExpirationMs(), UtilisateurResponse.from(u));
    }

    static String normaliserEmail(String email) {
        return email.trim().toLowerCase();
    }
}
