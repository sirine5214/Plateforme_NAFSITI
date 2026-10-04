package tn.esprit.pi.nefsiti.entities;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.pi.nefsiti.security.ChiffrementConverter;

import java.time.LocalDateTime;

/** Entrée quotidienne du journal d'humeur (module 3). */
@Entity
@Table(name = "entrees_journal")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntreeJournal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Utilisateur patient;

    /** Note d'humeur de 1 à 10. */
    @Column(nullable = false)
    private int humeur;

    @Convert(converter = ChiffrementConverter.class)
    @Column(columnDefinition = "text")
    private String texte;

    /** Tags d'émotion séparés par des virgules. */
    @Column(length = 300)
    private String tags;

    /** Valence du texte de 1 (négatif) à 5 (positif), calculée par l'IA ; null si indisponible. */
    private Double valence;

    /** Niveau de risque 0-3 (module 6) ; null si non analysé. */
    private Integer niveauRisque;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    void prePersist() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }
}
