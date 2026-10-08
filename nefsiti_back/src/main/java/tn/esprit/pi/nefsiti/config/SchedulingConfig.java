package tn.esprit.pi.nefsiti.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Tâches planifiées (ré-entraînement hebdomadaire du détecteur d'anomalies) et envois asynchrones (e-mails). */
@Configuration
@EnableScheduling
@EnableAsync
public class SchedulingConfig {
}
