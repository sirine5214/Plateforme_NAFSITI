package tn.esprit.pi.nefsiti.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import tn.esprit.pi.nefsiti.dto.*;
import tn.esprit.pi.nefsiti.security.ContexteConnexion;
import tn.esprit.pi.nefsiti.security.UtilisateurPrincipal;
import tn.esprit.pi.nefsiti.services.AuthService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    /** Connexion manuelle (email + mot de passe). Peut renvoyer mfaRequis=true si l'IA juge la connexion inhabituelle. */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest request) {
        return authService.login(req, ContexteConnexion.from(request));
    }

    /** Connexion par reconnaissance faciale (email + capture webcam). */
    @PostMapping("/login/visage")
    public AuthResponse loginVisage(@Valid @RequestBody LoginVisageRequest req, HttpServletRequest request) {
        return authService.loginVisage(req, ContexteConnexion.from(request));
    }

    /** Second facteur : confirmation par le visage après un mot de passe correct. */
    @PostMapping("/mfa/visage")
    public AuthResponse mfaVisage(@Valid @RequestBody MfaVisageRequest req, HttpServletRequest request) {
        return authService.validerMfaVisage(req, ContexteConnexion.from(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        authService.logout(authorization.replaceFirst("^Bearer ", ""));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me")
    public UtilisateurResponse me(@AuthenticationPrincipal UtilisateurPrincipal principal) {
        return authService.me(principal.id());
    }
}
