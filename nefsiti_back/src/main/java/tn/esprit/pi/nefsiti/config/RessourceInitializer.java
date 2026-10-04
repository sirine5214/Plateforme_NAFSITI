package tn.esprit.pi.nefsiti.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import tn.esprit.pi.nefsiti.entities.Ressource;
import tn.esprit.pi.nefsiti.entities.TypeRessource;
import tn.esprit.pi.nefsiti.repositories.RessourceRepository;

import java.util.List;

/** Catalogue de départ de la bibliothèque (module 4), créé si la table est vide. */
@Slf4j
@Component
@RequiredArgsConstructor
public class RessourceInitializer implements CommandLineRunner {

    private final RessourceRepository repository;

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        List<Ressource> catalogue = List.of(
                ressource("Respiration carrée", TypeRessource.RESPIRATION, 4,
                        "Technique simple pour calmer l'anxiété et le stress en quelques minutes.",
                        "Inspirez par le nez pendant 4 secondes.\nRetenez votre souffle 4 secondes.\n"
                                + "Expirez lentement par la bouche pendant 4 secondes.\nRestez poumons vides 4 secondes.\n"
                                + "Répétez le cycle 6 à 8 fois, en gardant les épaules relâchées."),
                ressource("Cohérence cardiaque 365", TypeRessource.RESPIRATION, 5,
                        "Respiration rythmée pour réduire le stress et améliorer le sommeil, 3 fois par jour.",
                        "Asseyez-vous le dos droit.\nInspirez 5 secondes, expirez 5 secondes, pendant 5 minutes.\n"
                                + "Pratiquez idéalement le matin, avant le déjeuner et en fin d'après-midi."),
                ressource("Respiration 4-7-8 pour s'endormir", TypeRessource.RESPIRATION, 3,
                        "Exercice de respiration pour apaiser le mental et favoriser l'endormissement.",
                        "Inspirez par le nez en comptant jusqu'à 4.\nRetenez votre souffle jusqu'à 7.\n"
                                + "Expirez lentement par la bouche jusqu'à 8.\nRecommencez 4 fois."),
                ressource("Scan corporel guidé", TypeRessource.MEDITATION, 10,
                        "Méditation de pleine conscience pour relâcher les tensions physiques et la fatigue.",
                        "Allongez-vous confortablement et fermez les yeux.\nPortez votre attention sur vos pieds, "
                                + "puis remontez lentement : jambes, ventre, poitrine, bras, nuque, visage.\n"
                                + "Observez chaque sensation sans la juger, puis relâchez la zone en expirant."),
                ressource("Méditation de l'ancrage", TypeRessource.MEDITATION, 5,
                        "Courte méditation pour revenir au moment présent lors d'une crise d'angoisse.",
                        "Nommez 5 choses que vous voyez, 4 que vous pouvez toucher, 3 que vous entendez, "
                                + "2 que vous sentez et 1 que vous goûtez.\nRespirez lentement entre chaque étape."),
                ressource("Méditation de bienveillance envers soi", TypeRessource.MEDITATION, 8,
                        "Pratique d'auto-compassion pour les moments de tristesse, de solitude ou d'autocritique.",
                        "Posez une main sur votre cœur.\nRépétez intérieurement : « Que je sois en paix, "
                                + "que je sois bienveillant envers moi-même, que j'accepte ce moment tel qu'il est. »"),
                ressource("Comprendre et gérer le stress au travail", TypeRessource.ARTICLE, 6,
                        "Article sur les mécanismes du stress professionnel, le burn-out et les leviers d'action.",
                        "Le stress est une réaction normale face à une exigence. Il devient problématique lorsqu'il "
                                + "est chronique : fatigue, irritabilité, troubles du sommeil.\n\nQuelques leviers : "
                                + "prioriser les tâches, poser des limites horaires, faire des pauses régulières, "
                                + "parler de sa charge de travail et demander de l'aide à un professionnel si besoin."),
                ressource("Mieux dormir : les bases de l'hygiène du sommeil", TypeRessource.ARTICLE, 5,
                        "Conseils pratiques contre l'insomnie et les troubles du sommeil liés à l'anxiété.",
                        "Gardez des horaires réguliers, même le week-end.\nÉvitez les écrans une heure avant le "
                                + "coucher.\nLimitez la caféine après 14 h.\nSi vous ne dormez pas après 20 minutes, "
                                + "levez-vous et faites une activité calme."),
                ressource("Que faire face à des pensées sombres ?", TypeRessource.ARTICLE, 4,
                        "Repères pour traverser un moment de détresse et savoir qui contacter.",
                        "Vous n'êtes pas seul·e. En cas de pensées suicidaires, appelez le 3114 (numéro national, "
                                + "24 h/24), le 15 (SAMU) ou le 112.\nParlez-en à une personne de confiance et "
                                + "prenez rendez-vous avec un professionnel. Ces pensées peuvent être soignées."));
        repository.saveAll(catalogue);
        log.info("Bibliothèque initialisée avec {} ressources", catalogue.size());
    }

    private static Ressource ressource(String titre, TypeRessource type, int duree, String description, String contenu) {
        return Ressource.builder()
                .titre(titre)
                .type(type)
                .dureeMinutes(duree)
                .description(description)
                .contenu(contenu)
                .valideParPro(true)
                .build();
    }
}
