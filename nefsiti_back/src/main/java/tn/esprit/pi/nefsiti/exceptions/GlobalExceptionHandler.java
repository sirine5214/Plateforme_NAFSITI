package tn.esprit.pi.nefsiti.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tn.esprit.pi.nefsiti.dto.ApiError;
import tn.esprit.pi.nefsiti.ia.IaIndisponibleException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApi(ApiException ex) {
        return build(ex.getStatus(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> erreurs = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> erreurs.putIfAbsent(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(new ApiError(400, "Données invalides", erreurs));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiError> handleUnreadable(Exception ex) {
        return build(HttpStatus.BAD_REQUEST, "Requête invalide");
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, "Email ou mot de passe incorrect");
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ApiError> handleDisabled(DisabledException ex) {
        return build(HttpStatus.FORBIDDEN, "Votre compte est désactivé. Contactez un administrateur.");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NoResourceFoundException ex) {
        return build(HttpStatus.NOT_FOUND, "Ressource introuvable");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "Méthode non autorisée");
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> handleConcurrence(OptimisticLockingFailureException ex) {
        return build(HttpStatus.CONFLICT, "Ce créneau vient d'être modifié par quelqu'un d'autre. Actualisez et réessayez.");
    }

    @ExceptionHandler(IaIndisponibleException.class)
    public ResponseEntity<ApiError> handleIa(IaIndisponibleException ex) {
        log.warn("Service IA indisponible : {}", ex.getMessage());
        return build(HttpStatus.SERVICE_UNAVAILABLE, "Le service d'intelligence artificielle est momentanément indisponible");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleOther(Exception ex) {
        log.error("Erreur inattendue", ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur interne du serveur");
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(new ApiError(status.value(), message));
    }
}
