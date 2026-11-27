package pharmacie.util;

/** Erreur technique d'accès aux données (SQL, connexion...). */
public class DAOException extends Exception {

    public DAOException(String message, Throwable cause) {
        super(message, cause);
    }
}
