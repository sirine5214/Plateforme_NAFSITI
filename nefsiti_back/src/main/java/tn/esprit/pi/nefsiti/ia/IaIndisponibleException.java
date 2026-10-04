package tn.esprit.pi.nefsiti.ia;

/** Service IA injoignable, en erreur ou module non chargé : l'appelant décide du mode dégradé. */
public class IaIndisponibleException extends RuntimeException {

    public IaIndisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
