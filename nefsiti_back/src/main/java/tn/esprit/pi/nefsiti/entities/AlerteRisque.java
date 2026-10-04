package tn.esprit.pi.nefsiti.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Signal de détresse (niveau ≥ 2) détecté dans le journal ou la messagerie (module 6).
 * Ne contient pas le texte : le professionnel est invité à contacter le patient, pas à lire son journal.
 */
@Entity
@Table(name = "alertes_risque")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlerteRisque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Utilisateur patient;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SourceAlerte source;

    @Column(nullable = false)
    private int niveau;

    private Double probabilite;

    /** « regle » ou « modele ». */
    @Column(length = 20)
    private String origineDecision;

    @Column(nullable = false)
    private LocalDateTime dateCreation;

    @Column(nullable = false)
    @Builder.Default
    private boolean traitee = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "traitee_par_id")
    private Utilisateur traiteePar;

    private LocalDateTime dateTraitement;
}
