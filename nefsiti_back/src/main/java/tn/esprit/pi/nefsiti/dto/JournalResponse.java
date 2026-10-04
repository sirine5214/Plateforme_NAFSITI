package tn.esprit.pi.nefsiti.dto;

import tn.esprit.pi.nefsiti.entities.EntreeJournal;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public record JournalResponse(Long id, int humeur, String texte, List<String> tags, Double valence,
                              Integer niveauRisque, LocalDateTime dateCreation) {

    public static JournalResponse from(EntreeJournal e) {
        List<String> tags = e.getTags() == null || e.getTags().isBlank()
                ? List.of()
                : Arrays.stream(e.getTags().split(",")).toList();
        return new JournalResponse(e.getId(), e.getHumeur(), e.getTexte(), tags, e.getValence(),
                e.getNiveauRisque(), e.getDateCreation());
    }
}
