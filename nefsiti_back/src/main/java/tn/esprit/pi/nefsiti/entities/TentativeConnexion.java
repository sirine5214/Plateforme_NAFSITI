package tn.esprit.pi.nefsiti.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Journal des accès : alimente le détecteur d'anomalies (module 1) et l'audit. */
@Entity
@Table(name = "tentatives_connexion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TentativeConnexion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Null si l'email saisi ne correspond à aucun compte. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @Column(nullable = false)
    private LocalDateTime dateTentative;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MethodeConnexion methode;

    @Column(nullable = false)
    private boolean succes;

    @Column(length = 64)
    private String ip;

    /** Empreinte SHA-256 du User-Agent (pas le User-Agent brut). */
    @Column(length = 64)
    private String appareil;

    // Caractéristiques envoyées au modèle, conservées pour le ré-entraînement hebdomadaire
    private Boolean nouvelAppareil;

    private Integer echecs24h;

    private Double scoreRisque;

    /** AUTORISER, MFA_RENFORCEE, BLOQUER_ET_ALERTER, ou motif d'échec. */
    @Column(length = 40)
    private String action;
}
