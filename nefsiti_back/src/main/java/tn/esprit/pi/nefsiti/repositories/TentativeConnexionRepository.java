package tn.esprit.pi.nefsiti.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.pi.nefsiti.entities.TentativeConnexion;

import java.time.LocalDateTime;
import java.util.List;

public interface TentativeConnexionRepository extends JpaRepository<TentativeConnexion, Long> {

    @Query("""
            select count(t) from TentativeConnexion t
            where t.utilisateur.id = :id and t.succes = false and t.action in :motifs and t.dateTentative > :apres
            """)
    long compterEchecs(@Param("id") Long utilisateurId, @Param("motifs") List<String> motifs,
                       @Param("apres") LocalDateTime apres);

    boolean existsByUtilisateurIdAndSuccesTrue(Long utilisateurId);

    boolean existsByUtilisateurIdAndSuccesTrueAndAppareil(Long utilisateurId, String appareil);

    boolean existsByUtilisateurIdAndActionAndDateTentativeAfter(Long utilisateurId, String action, LocalDateTime apres);

    /** Connexions réussies récentes : historique « normal » pour le ré-entraînement du modèle. */
    List<TentativeConnexion> findBySuccesTrueAndDateTentativeAfter(LocalDateTime apres);

    @Query("""
            select t.dateTentative from TentativeConnexion t
            where t.utilisateur.id = :id and t.succes = true and t.dateTentative > :apres
            """)
    List<LocalDateTime> datesConnexion(@Param("id") Long utilisateurId, @Param("apres") LocalDateTime apres);

    @Modifying
    @Query("delete from TentativeConnexion t where t.utilisateur.id = :id")
    void supprimerParUtilisateur(@Param("id") Long id);
}
