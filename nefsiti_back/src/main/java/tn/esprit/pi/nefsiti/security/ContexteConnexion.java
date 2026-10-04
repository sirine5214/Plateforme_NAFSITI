package tn.esprit.pi.nefsiti.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

/** Informations réseau d'une tentative de connexion, utilisées par le détecteur d'anomalies. */
public record ContexteConnexion(String ip, String userAgent) {

    public static ContexteConnexion from(HttpServletRequest request) {
        String ua = request.getHeader(HttpHeaders.USER_AGENT);
        return new ContexteConnexion(request.getRemoteAddr(), ua == null ? "" : ua);
    }
}
