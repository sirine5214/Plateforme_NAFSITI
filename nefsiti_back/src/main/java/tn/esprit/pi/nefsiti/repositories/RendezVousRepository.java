package tn.esprit.pi.nefsiti.repositories;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.pi.nefsiti.entities.RendezVous;
import tn.esprit.pi.nefsiti.entities.StatutRendezVous;

import java.time.LocalDateTime;
import java.util.List;

public interface RendezVousRepository extends JpaRepository<RendezVous, Long> {

    @EntityGraph(attributePaths = {"patient", "therapeute"})
    List<RendezVous> findByPatientIdOrderByDateHeureDesc(Long patientId);

    @EntityGraph(attributePaths = {"patient", "therapeute"})
    List<RendezVous> findByTherapeuteIdOrderByDateHeureDesc(Long therapeuteId);

    @EntityGraph(attributePaths = {"patient", "therapeute"})
    List<RendezVous> findAllByOrderByDateHeureDesc();

    @Query("""
            select count(r) > 0 from RendezVous r
            where r.patient.id = :patientId and r.statut <> :annule
              and r.dateHeure < :fin and r.dateFin > :debut
            """)
    boolean patientOccupe(@Param("patientId") Long patientId,
                          @Param("debut") LocalDateTime debut,
                          @Param("fin") LocalDateTime fin,
                          @Param("annule") StatutRendezVous annule);

    @Modifying
    @Query("update RendezVous r set r.disponibilite = null where r.disponibilite.id = :disponibiliteId")
    void detacherDisponibilite(@Param("disponibiliteId") Long disponibiliteId);

    /** Libère les créneaux encore réservés par ce patient (avant suppression de son compte). */
    @Modifying
    @Query("""
            update Disponibilite d set d.reserve = false
            where d.id in (select r.disponibilite.id from RendezVous r
                           where r.patient.id = :patientId and r.statut <> :annule)
            """)
    void libererCreneauxDuPatient(@Param("patientId") Long patientId, @Param("annule") StatutRendezVous annule);

    @Modifying
    @Query("delete from RendezVous r where r.patient.id = :id or r.therapeute.id = :id")
    void supprimerParUtilisateur(@Param("id") Long id);
}
