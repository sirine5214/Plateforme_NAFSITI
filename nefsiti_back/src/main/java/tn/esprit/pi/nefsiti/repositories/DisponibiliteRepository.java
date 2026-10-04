package tn.esprit.pi.nefsiti.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.pi.nefsiti.entities.Disponibilite;

import java.time.LocalDateTime;
import java.util.List;

public interface DisponibiliteRepository extends JpaRepository<Disponibilite, Long> {

    List<Disponibilite> findByTherapeuteIdAndDebutAfterOrderByDebutAsc(Long therapeuteId, LocalDateTime apres);

    List<Disponibilite> findByTherapeuteIdAndReserveFalseAndDebutAfterOrderByDebutAsc(Long therapeuteId, LocalDateTime apres);

    long countByTherapeuteIdAndReserveFalseAndDebutAfter(Long therapeuteId, LocalDateTime apres);

    long countByTherapeuteIdAndReserveFalseAndDebutBetween(Long therapeuteId, LocalDateTime debut, LocalDateTime fin);

    @Query("""
            select count(d) > 0 from Disponibilite d
            where d.therapeute.id = :therapeuteId and d.debut < :fin and d.fin > :debut
            """)
    boolean chevauche(@Param("therapeuteId") Long therapeuteId,
                      @Param("debut") LocalDateTime debut,
                      @Param("fin") LocalDateTime fin);

    @Modifying
    @Query("delete from Disponibilite d where d.therapeute.id = :therapeuteId")
    void supprimerParTherapeute(@Param("therapeuteId") Long therapeuteId);
}
