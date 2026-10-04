package tn.esprit.pi.nefsiti.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "utilisateurs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nom;

    @Column(nullable = false, length = 100)
    private String prenom;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String motDePasse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    @Builder.Default
    private boolean actif = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    /** Consentement RGPD explicite donné à l'inscription (null pour les comptes créés par un admin). */
    private LocalDateTime dateConsentement;

    /** Le patient accepte que ses alertes de détresse soient transmises à ses thérapeutes. */
    @Column(nullable = false, columnDefinition = "boolean default false") // colonne ajoutée sur une table existante
    @Builder.Default
    private boolean partageAlertes = false;

    // ===== Profil thérapeute (matching patient ↔ thérapeute) =====

    @Column(length = 500)
    private String specialites;

    @Column(length = 500)
    private String approche;

    /** Codes de langue séparés par des virgules, ex. « fr,ar,en ». */
    @Column(length = 50)
    private String langues;

    @PrePersist
    void prePersist() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }
}
