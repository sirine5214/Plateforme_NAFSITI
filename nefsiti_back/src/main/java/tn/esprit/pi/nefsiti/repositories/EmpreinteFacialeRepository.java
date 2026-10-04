package tn.esprit.pi.nefsiti.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.esprit.pi.nefsiti.entities.EmpreinteFaciale;

import java.util.Optional;

public interface EmpreinteFacialeRepository extends JpaRepository<EmpreinteFaciale, Long> {

    Optional<EmpreinteFaciale> findByUtilisateurId(Long utilisateurId);

    boolean existsByUtilisateurId(Long utilisateurId);

    @Modifying
    @Query("delete from EmpreinteFaciale e where e.utilisateur.id = :id")
    void supprimerParUtilisateur(@Param("id") Long id);
}
