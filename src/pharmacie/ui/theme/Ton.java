package pharmacie.ui.theme;

import java.awt.Color;

/** Couleur d'un badge selon ce qu'il signale. */
public enum Ton {
    SUCCES(Theme.SUCCES_CLAIR, Theme.SUCCES),
    DANGER(Theme.DANGER_CLAIR, Theme.DANGER_FONCE),
    AVERTISSEMENT(Theme.AVERTISSEMENT_CLAIR, Theme.AVERTISSEMENT),
    INFO(Theme.INFO_CLAIR, Theme.INFO),
    NEUTRE(Theme.NEUTRE_CLAIR, Theme.NEUTRE);

    private final Color fond;
    private final Color texte;

    Ton(Color fond, Color texte) {
        this.fond = fond;
        this.texte = texte;
    }

    public Color getFond() {
        return fond;
    }

    public Color getTexte() {
        return texte;
    }

    /**
     * Ton par défaut d'un libellé courant de l'application, pour que le même mot
     * ait toujours la même couleur d'un écran à l'autre.
     */
    public static Ton pour(String libelle) {
        if (libelle == null) {
            return NEUTRE;
        }
        String t = libelle.trim().toLowerCase();
        if (t.contains("stock faible") || t.contains("périmé") || t.contains("perime")
                || t.contains("rupture") || t.contains("inactif") || t.contains("désactivé")) {
            return DANGER;
        }
        if (t.contains("bientôt") || t.contains("proche") || t.contains("jour")) {
            return AVERTISSEMENT;
        }
        if (t.equals("ok") || t.contains("normal") || t.contains("en stock") || t.equals("actif")
                || t.equals("oui")) {
            return SUCCES;
        }
        if (t.contains("admin")) {
            return INFO;
        }
        return NEUTRE;
    }
}
