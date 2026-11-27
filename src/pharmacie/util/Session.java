package pharmacie.util;

import pharmacie.model.Role;
import pharmacie.model.Utilisateur;

/** Mémorise l'utilisateur connecté pendant l'exécution de l'application. */
public final class Session {

    private static Utilisateur utilisateurCourant;

    private Session() {
    }

    public static void ouvrir(Utilisateur utilisateur) {
        utilisateurCourant = utilisateur;
    }

    public static void fermer() {
        utilisateurCourant = null;
    }

    public static Utilisateur getUtilisateur() {
        return utilisateurCourant;
    }

    /** RG03 : le rôle décide des onglets et des droits. */
    public static boolean estAdmin() {
        return utilisateurCourant != null && utilisateurCourant.getRole() == Role.ADMIN;
    }
}
