package tn.esprit.pi.nefsiti.repositories;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.pi.nefsiti.entities.Message;
import tn.esprit.pi.nefsiti.entities.StatutMessage;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    /** Fil entre deux personnes, vu par « moi » : ses propres messages + les messages publiés de l'autre. */
    @EntityGraph(attributePaths = {"expediteur", "destinataire"})
    @Query("""
            select m from Message m
            where (m.expediteur.id = :moi and m.destinataire.id = :autre)
               or (m.expediteur.id = :autre and m.destinataire.id = :moi and m.statut = :publie)
            order by m.dateEnvoi asc
            """)
    List<Message> conversation(@Param("moi") Long moi, @Param("autre") Long autre,
                               @Param("publie") StatutMessage publie);

    long countByDestinataireIdAndExpediteurIdAndLuFalseAndStatut(Long destinataireId, Long expediteurId,
                                                                 StatutMessage statut);

    @Modifying
    @Query("""
            update Message m set m.lu = true
            where m.destinataire.id = :moi and m.expediteur.id = :autre and m.lu = false
            """)
    void marquerLus(@Param("moi") Long moi, @Param("autre") Long autre);

    @EntityGraph(attributePaths = {"expediteur", "destinataire"})
    List<Message> findByStatutOrderByDateEnvoiAsc(StatutMessage statut);

    @Modifying
    @Query("delete from Message m where m.expediteur.id = :id or m.destinataire.id = :id")
    void supprimerParUtilisateur(@Param("id") Long id);
}
