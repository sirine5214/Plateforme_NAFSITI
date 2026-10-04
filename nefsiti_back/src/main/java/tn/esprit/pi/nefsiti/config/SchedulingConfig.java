package tn.esprit.pi.nefsiti.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Tâches planifiées (ré-entraînement hebdomadaire du détecteur d'anomalies de connexion). */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
