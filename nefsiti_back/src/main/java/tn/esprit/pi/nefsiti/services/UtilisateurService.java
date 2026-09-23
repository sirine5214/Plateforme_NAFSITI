package tn.esprit.pi.nefsiti.services;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tn.esprit.pi.nefsiti.dto.*;
import tn.esprit.pi.nefsiti.entities.Role;
import tn.esprit.pi.nefsiti.entities.StatutRendezVous;
import tn.esprit.pi.nefsiti.entities.Utilisateur;
import tn.esprit.pi.nefsiti.exceptions.ApiException;
import tn.esprit.pi.nefsiti.repositories.DisponibiliteRepository;
import tn.esprit.pi.nefsiti.repositories.RendezVousRepository;
import tn.esprit.pi.nefsiti.repositories.UtilisateurRepository;

import java.util.ArrayList;
import java.util.List;

import static tn.esprit.pi.nefsiti.services.AuthService.normaliserEmail;

@Service
@RequiredArgsConstructor
public class UtilisateurService {

    private final UtilisateurRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final RendezVousRepository rendezVousRepository;
    private final DisponibiliteRepository disponibiliteRepository;

    // ===== Administration =====

    @Transactional(readOnly = true)
    public List<UtilisateurResponse> lister(String recherche, Role role, Boolean actif) {
        Specification<Utilisateur> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            if (recherche != null && !recherche.isBlank()) {
                String like = "%" + recherche.trim().toLowerCase() + "%";
                preds.add(cb.or(
                        cb.like(cb.lower(root.get("nom")), like),
                        cb.like(cb.lower(root.get("prenom")), like),
                        cb.like(cb.lower(root.get("email")), like)));
            }
            if (role != null) {
                preds.add(cb.equal(root.get("role"), role));
            }
            if (actif != null) {
                preds.add(cb.equal(root.get("actif"), actif));
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };
        return repository.findAll(spec, Sort.by(Sort.Direction.DESC, "dateCreation")).stream()
                .map(UtilisateurResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public UtilisateurResponse trouver(Long id) {
        return UtilisateurResponse.from(charger(id));
    }

    @Transactional(readOnly = true)
    public UtilisateurStats statistiques() {
        return new UtilisateurStats(
                repository.count(),
                repository.countByRole(Role.PATIENT),
                repository.countByRole(Role.THERAPEUTE),
                repository.countByRole(Role.ADMINISTRATEUR),
                repository.countByActif(true),
                repository.countByActif(false));
    }

    @Transactional
    public UtilisateurResponse creer(UtilisateurRequest req) {
        String email = normaliserEmail(req.email());
        if (repository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("Un compte existe déjà avec cet email");
        }
        if (!PasswordRules.estValide(req.motDePasse())) {
            throw ApiException.badRequest(PasswordRules.MESSAGE);
        }
        Utilisateur u = Utilisateur.builder()
                .nom(req.nom().trim())
                .prenom(req.prenom().trim())
                .email(email)
                .motDePasse(passwordEncoder.encode(req.motDePasse()))
                .role(req.role())
                .actif(req.actif() == null || req.actif())
                .build();
        return UtilisateurResponse.from(repository.save(u));
    }

    @Transactional
    public UtilisateurResponse modifier(Long id, UtilisateurRequest req, Long adminId) {
        Utilisateur u = charger(id);
        String email = normaliserEmail(req.email());
        if (repository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw ApiException.conflict("Un compte existe déjà avec cet email");
        }
        boolean actif = req.actif() == null ? u.isActif() : req.actif();
        if (id.equals(adminId) && (req.role() != Role.ADMINISTRATEUR || !actif)) {
            throw ApiException.badRequest("Vous ne pouvez pas retirer votre propre rôle administrateur ni vous désactiver");
        }
        u.setNom(req.nom().trim());
        u.setPrenom(req.prenom().trim());
        u.setEmail(email);
        u.setRole(req.role());
        u.setActif(actif);
        if (req.motDePasse() != null && !req.motDePasse().isBlank()) {
            if (!PasswordRules.estValide(req.motDePasse())) {
                throw ApiException.badRequest(PasswordRules.MESSAGE);
            }
            u.setMotDePasse(passwordEncoder.encode(req.motDePasse()));
        }
        return UtilisateurResponse.from(u);
    }

    @Transactional
    public UtilisateurResponse changerStatut(Long id, boolean actif, Long adminId) {
        if (id.equals(adminId) && !actif) {
            throw ApiException.badRequest("Vous ne pouvez pas désactiver votre propre compte");
        }
        Utilisateur u = charger(id);
        u.setActif(actif);
        return UtilisateurResponse.from(u);
    }

    @Transactional
    public void supprimer(Long id, Long adminId) {
        if (id.equals(adminId)) {
            throw ApiException.badRequest("Vous ne pouvez pas supprimer votre propre compte");
        }
        Utilisateur u = charger(id);
        // Rendez-vous et créneaux liés, sinon violation de clé étrangère
        rendezVousRepository.libererCreneauxDuPatient(id, StatutRendezVous.ANNULE);
        rendezVousRepository.supprimerParUtilisateur(id);
        disponibiliteRepository.supprimerParTherapeute(id);
        repository.delete(u);
    }

    // ===== Profil connecté =====

    @Transactional
    public UtilisateurResponse modifierProfil(Long id, ProfilRequest req) {
        Utilisateur u = charger(id);
        String email = normaliserEmail(req.email());
        if (repository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw ApiException.conflict("Un compte existe déjà avec cet email");
        }
        u.setNom(req.nom().trim());
        u.setPrenom(req.prenom().trim());
        u.setEmail(email);
        return UtilisateurResponse.from(u);
    }

    @Transactional
    public void changerMotDePasse(Long id, ChangementMotDePasseRequest req) {
        Utilisateur u = charger(id);
        if (!passwordEncoder.matches(req.ancienMotDePasse(), u.getMotDePasse())) {
            throw ApiException.badRequest("L'ancien mot de passe est incorrect");
        }
        if (passwordEncoder.matches(req.nouveauMotDePasse(), u.getMotDePasse())) {
            throw ApiException.badRequest("Le nouveau mot de passe doit être différent de l'ancien");
        }
        u.setMotDePasse(passwordEncoder.encode(req.nouveauMotDePasse()));
    }

    private Utilisateur charger(Long id) {
        return repository.findById(id).orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
    }
}
