package tn.esprit.pi.nefsiti.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Contenu d'auto-aide de la bibliothèque (module 4). */
@Entity
@Table(name = "ressources")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Ressource {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titre;

    @Column(nullable = false, length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeRessource type;

    /** Texte de l'article ou consignes de l'exercice. */
    @Column(columnDefinition = "text")
    private String contenu;

    private Integer dureeMinutes;

    /** Lien vers un audio / une vidéo externe (facultatif). */
    @Column(length = 500)
    private String url;

    @Column(nullable = false)
    @Builder.Default
    private boolean valideParPro = false;

    /** Nombre de consultations. */
    @Column(nullable = false)
    @Builder.Default
    private int popularite = 0;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    void prePersist() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
    }
}
