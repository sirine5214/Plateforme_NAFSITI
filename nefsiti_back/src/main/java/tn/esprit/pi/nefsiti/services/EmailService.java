package tn.esprit.pi.nefsiti.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * E-mails de sécurité et d'alerte. Envoi asynchrone : une connexion n'attend jamais le serveur SMTP.
 * Sans serveur configuré (spring.mail.host), le message est seulement journalisé.
 * Les e-mails ne contiennent jamais de donnée de santé : ils invitent à se connecter.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final ObjectProvider<JavaMailSender> mailSender;

    @Value("${app.mail.expediteur}")
    private String expediteur;

    @Value("${spring.mail.host:}")
    private String hoteSmtp;

    @Value("${app.front-url}")
    private String frontUrl;

    /** Envoi synchrone : passer par les méthodes @Async ci-dessous depuis les autres services. */
    public void envoyer(String destinataire, String sujet, String texte) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || hoteSmtp.isBlank()) {
            log.info("E-mail non envoyé (aucun serveur SMTP configuré) : « {} » à {}", sujet, destinataire);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(expediteur);
        message.setTo(destinataire);
        message.setSubject("[Nafsiti] " + sujet);
        message.setText(texte + "\n\n— L'équipe Nafsiti\n" + frontUrl);
        try {
            sender.send(message);
        } catch (MailException e) {
            log.warn("Échec de l'envoi de l'e-mail « {} » à {} : {}", sujet, destinataire, e.getMessage());
        }
    }

    @Async
    public void connexionBloquee(String destinataire, String prenom, String ip) {
        envoyer(destinataire, "Connexion inhabituelle bloquée",
                "Bonjour " + prenom + ",\n\n"
                        + "Une tentative de connexion à votre compte a été jugée inhabituelle (adresse IP " + ip + ") "
                        + "et bloquée temporairement par mesure de sécurité.\n\n"
                        + "Si c'était vous, réessayez dans quelques minutes. Sinon, changez votre mot de passe dès "
                        + "votre prochaine connexion (Mon profil → Changer le mot de passe).");
    }

    @Async
    public void nouvelleAlerte(String destinataire, String prenom) {
        envoyer(destinataire, "Nouvelle alerte à consulter",
                "Bonjour " + prenom + ",\n\n"
                        + "Un signal de détresse a été détecté chez l'un de vos patients. "
                        + "Connectez-vous à Nafsiti (menu « Alertes de détresse ») pour le consulter et le contacter.");
    }
}
