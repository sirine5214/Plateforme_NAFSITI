package tn.esprit.pi.nefsiti.entities;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.pi.nefsiti.security.ChiffrementConverter;

import java.time.LocalDateTime;

/** Message sécurisé entre un patient et un thérapeute (module 5). */
@Entity
@Table(name = "messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "expediteur_id", nullable = false)
    private Utilisateur expediteur;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "destinataire_id", nullable = false)
    private Utilisateur destinataire;

    @Convert(converter = ChiffrementConverter.class)
    @Column(nullable = false, columnDefinition = "text")
    private String contenu;

    @Column(nullable = false)
    private LocalDateTime dateEnvoi;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutMessage statut;

    /** Décision de la modération automatique (PUBLIER, MASQUER_EN_ATTENTE_REVUE, ESCALADE_HUMAINE, NON_ANALYSE). */
    @Column(length = 40)
    private String decisionModeration;

    private Double toxicite;

    private Integer niveauRisque;

    @Column(nullable = false)
    @Builder.Default
    private boolean lu = false;
}
