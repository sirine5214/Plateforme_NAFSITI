package tn.esprit.pi.nefsiti.repositories;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.pi.nefsiti.entities.AlerteRisque;
import tn.esprit.pi.nefsiti.entities.StatutRendezVous;

import java.util.List;

public interface AlerteRisqueRepository extends JpaRepository<AlerteRisque, Long> {

    @EntityGraph(attributePaths = {"patient", "traiteePar"})
    List<AlerteRisque> findAllByOrderByTraiteeAscDateCreationDesc();

    /** Alertes des patients suivis par ce thérapeute et ayant consenti au partage. */
    @EntityGraph(attributePaths = {"patient", "traiteePar"})
    @Query("""
            select a from AlerteRisque a
            where a.patient.partageAlertes = true
              and a.patient.id in (select r.patient.id from RendezVous r
                                   where r.therapeute.id = :therapeuteId and r.statut <> :annule)
            order by a.traitee asc, a.dateCreation desc
            """)
    List<AlerteRisque> pourTherapeute(@Param("therapeuteId") Long therapeuteId,
                                      @Param("annule") StatutRendezVous annule);

    @Modifying
    @Query("delete from AlerteRisque a where a.patient.id = :id")
    void supprimerParPatient(@Param("id") Long id);

    @Modifying
    @Query("update AlerteRisque a set a.traiteePar = null where a.traiteePar.id = :id")
    void detacherTraitant(@Param("id") Long id);
}
