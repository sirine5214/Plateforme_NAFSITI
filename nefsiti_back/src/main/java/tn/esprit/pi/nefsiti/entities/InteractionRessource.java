package tn.esprit.pi.nefsiti.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Ressource consultée (et éventuellement aimée) par un utilisateur : historique pour la recommandation. */
@Entity
@Table(name = "interactions_ressource",
        uniqueConstraints = @UniqueConstraint(columnNames = {"utilisateur_id", "ressource_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InteractionRessource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ressource_id", nullable = false)
    private Ressource ressource;

    @Column(nullable = false)
    @Builder.Default
    private boolean aime = false;

    @Column(nullable = false)
    private LocalDateTime derniereConsultation;
}
