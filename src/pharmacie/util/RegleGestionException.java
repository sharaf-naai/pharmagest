package pharmacie.util;

/** Violation d'une règle de gestion : le message est destiné à l'utilisateur. */
public class RegleGestionException extends Exception {

    public RegleGestionException(String message) {
        super(message);
    }
}
