package tn.esprit.pi.nefsiti.entities;

import jakarta.persistence.*;
import lombok.*;
import tn.esprit.pi.nefsiti.security.ChiffrementConverter;

import java.time.LocalDateTime;

/** Empreinte faciale (vecteur FaceNet) d'un utilisateur : donnée biométrique, chiffrée, jamais d'image. */
@Entity
@Table(name = "empreintes_faciales")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmpreinteFaciale {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false, unique = true)
    private Utilisateur utilisateur;

    /** Composantes séparées par des virgules. */
    @Convert(converter = ChiffrementConverter.class)
    @Column(nullable = false, columnDefinition = "text")
    private String vecteur;

    @Column(nullable = false, length = 50)
    private String modele;

    @Column(nullable = false)
    private LocalDateTime dateEnregistrement;
}
