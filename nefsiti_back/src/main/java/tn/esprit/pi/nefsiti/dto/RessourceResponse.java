package tn.esprit.pi.nefsiti.dto;

import tn.esprit.pi.nefsiti.entities.Ressource;
import tn.esprit.pi.nefsiti.entities.TypeRessource;

public record RessourceResponse(Long id, String titre, String description, TypeRessource type, String contenu,
                                Integer dureeMinutes, String url, boolean valideParPro, int popularite,
                                boolean aime, boolean consultee) {

    public static RessourceResponse from(Ressource r, boolean aime, boolean consultee) {
        return new RessourceResponse(r.getId(), r.getTitre(), r.getDescription(), r.getType(), r.getContenu(),
                r.getDureeMinutes(), r.getUrl(), r.isValideParPro(), r.getPopularite(), aime, consultee);
    }
}
