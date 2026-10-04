package tn.esprit.pi.nefsiti.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.pi.nefsiti.entities.EntreeJournal;

import java.time.LocalDateTime;
import java.util.List;

public interface EntreeJournalRepository extends JpaRepository<EntreeJournal, Long> {

    List<EntreeJournal> findByPatientIdOrderByDateCreationDesc(Long patientId);

    List<EntreeJournal> findByPatientIdAndDateCreationAfterOrderByDateCreationAsc(Long patientId, LocalDateTime apres);

    List<EntreeJournal> findTop10ByPatientIdOrderByDateCreationDesc(Long patientId);

    @Modifying
    @Query("delete from EntreeJournal e where e.patient.id = :id")
    void supprimerParPatient(@Param("id") Long id);
}
