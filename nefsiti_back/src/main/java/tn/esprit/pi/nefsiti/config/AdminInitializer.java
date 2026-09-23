package tn.esprit.pi.nefsiti.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import tn.esprit.pi.nefsiti.entities.Role;
import tn.esprit.pi.nefsiti.entities.Utilisateur;
import tn.esprit.pi.nefsiti.repositories.UtilisateurRepository;

/** Crée un administrateur au premier démarrage (sinon personne ne peut gérer les comptes). */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInitializer implements CommandLineRunner {

    private final UtilisateurRepository repository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String email;

    @Value("${app.admin.password}")
    private String password;

    @Override
    public void run(String... args) {
        if (repository.existsByRole(Role.ADMINISTRATEUR) || repository.existsByEmailIgnoreCase(email)) {
            return;
        }
        repository.save(Utilisateur.builder()
                .nom("Admin")
                .prenom("Nafsiti")
                .email(email.trim().toLowerCase())
                .motDePasse(passwordEncoder.encode(password))
                .role(Role.ADMINISTRATEUR)
                .actif(true)
                .build());
        log.info("Administrateur initial créé : {}", email);
    }
}
