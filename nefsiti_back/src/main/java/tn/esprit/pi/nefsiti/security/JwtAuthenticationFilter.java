package tn.esprit.pi.nefsiti.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import tn.esprit.pi.nefsiti.entities.Utilisateur;
import tn.esprit.pi.nefsiti.repositories.UtilisateurRepository;

import java.io.IOException;
import java.util.List;

/** Instancié dans SecurityConfig (pas @Component, pour ne pas être enregistré une 2e fois hors de la chaîne). */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER = "Bearer ";

    private final JwtService jwtService;
    private final TokenBlacklistService blacklist;
    private final UtilisateurRepository utilisateurRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            authentifier(header.substring(BEARER.length()));
        }
        chain.doFilter(request, response);
    }

    private void authentifier(String token) {
        Claims claims;
        try {
            claims = jwtService.lire(token);
        } catch (JwtException | IllegalArgumentException e) {
            return; // token invalide : la requête reste anonyme -> 401 si la route est protégée
        }
        if (blacklist.estRevoque(claims.getId())) {
            return;
        }
        Long id;
        try {
            id = Long.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            return;
        }
        // Rôle et statut relus en base : désactivation / changement de rôle appliqués immédiatement.
        utilisateurRepository.findById(id)
                .filter(Utilisateur::isActif)
                .ifPresent(u -> {
                    UtilisateurPrincipal principal = new UtilisateurPrincipal(u.getId(), u.getEmail(), u.getRole());
                    var auth = new UsernamePasswordAuthenticationToken(principal, token,
                            List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });
    }
}
