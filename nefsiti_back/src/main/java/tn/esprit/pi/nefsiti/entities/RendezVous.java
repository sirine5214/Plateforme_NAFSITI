package tn.esprit.pi.nefsiti.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "rendez_vous")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RendezVous {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime dateHeure;

    @Column(nullable = false)
    private LocalDateTime dateFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatutRendezVous statut = StatutRendezVous.EN_ATTENTE;

    @Column(length = 500)
    private String motif;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Utilisateur patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "therapeute_id", nullable = false)
    private Utilisateur therapeute;

    /** Créneau réservé (null si le créneau a été supprimé après annulation). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disponibilite_id")
    private Disponibilite disponibilite;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    // ===== Avis du patient après la séance (alimente la note moyenne du matching) =====

    /** Note de 1 à 5 ; null tant que le patient n'a pas donné son avis. */
    private Integer noteAvis;

    @Column(length = 1000)
    private String commentaireAvis;

    private LocalDateTime dateAvis;

    @PrePersist
    void prePersist() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }
}
