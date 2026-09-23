package tn.esprit.pi.nefsiti.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Créneau proposé par un thérapeute, réservable par un patient. */
@Entity
@Table(name = "disponibilites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Disponibilite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "therapeute_id", nullable = false)
    private Utilisateur therapeute;

    @Column(nullable = false)
    private LocalDateTime debut;

    @Column(nullable = false)
    private LocalDateTime fin;

    @Column(nullable = false)
    @Builder.Default
    private boolean reserve = false;

    /** Verrou optimiste : deux patients ne peuvent pas réserver le même créneau. */
    @Version
    private Long version;
}
