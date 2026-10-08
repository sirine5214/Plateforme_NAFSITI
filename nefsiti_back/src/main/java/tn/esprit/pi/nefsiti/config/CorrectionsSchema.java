package tn.esprit.pi.nefsiti.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Corrections que ddl-auto=update ne sait pas faire seul.
 * Hibernate ajoute une contrainte CHECK sur les colonnes d'enum (ex. source in ('JOURNAL','MESSAGE'))
 * mais ne la met jamais à jour : une nouvelle valeur (CHATBOT) serait refusée sur une base existante.
 * La validation reste assurée par l'enum Java. Chaque instruction est idempotente.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CorrectionsSchema implements ApplicationRunner {

    private final JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        executer("alter table if exists alertes_risque drop constraint if exists alertes_risque_source_check");
    }

    private void executer(String sql) {
        try {
            jdbc.execute(sql);
        } catch (DataAccessException e) {
            log.warn("Correction de schéma ignorée ({}) : {}", sql, e.getMessage());
        }
    }
}
