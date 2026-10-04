package tn.esprit.pi.nefsiti.repositories;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.pi.nefsiti.entities.InteractionRessource;

import java.util.List;
import java.util.Optional;

public interface InteractionRessourceRepository extends JpaRepository<InteractionRessource, Long> {

    Optional<InteractionRessource> findByUtilisateurIdAndRessourceId(Long utilisateurId, Long ressourceId);

    @EntityGraph(attributePaths = "ressource")
    List<InteractionRessource> findByUtilisateurId(Long utilisateurId);

    @Modifying
    @Query("delete from InteractionRessource i where i.utilisateur.id = :id")
    void supprimerParUtilisateur(@Param("id") Long id);

    @Modifying
    @Query("delete from InteractionRessource i where i.ressource.id = :id")
    void supprimerParRessource(@Param("id") Long id);
}
