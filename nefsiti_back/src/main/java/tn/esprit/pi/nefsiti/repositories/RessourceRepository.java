package tn.esprit.pi.nefsiti.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.esprit.pi.nefsiti.entities.Ressource;

import java.util.List;

public interface RessourceRepository extends JpaRepository<Ressource, Long> {

    List<Ressource> findAllByOrderByTypeAscTitreAsc();
}
