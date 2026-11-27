package pharmacie.ui.theme;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.Enumeration;

import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

/**
 * Charte graphique de l'application : couleurs, polices et réglages Swing communs.
 * Toutes les couleurs de l'interface viennent d'ici, pour que les écrans restent cohérents.
 */
public final class Theme {

    // ---- Couleurs de fond et de texte ----
    public static final Color FOND = new Color(0xF4F6F8);          // fond de l'espace de travail
    public static final Color SURFACE = Color.WHITE;                // cartes, barre latérale, en-tête
    public static final Color SURFACE_DOUCE = new Color(0xF9FAFB);  // en-têtes de tableau, zones calmes
    public static final Color BORDURE = new Color(0xE5E7EB);
    public static final Color BORDURE_FORTE = new Color(0xD1D5DB);
    public static final Color TEXTE = new Color(0x111827);
    public static final Color TEXTE_SECONDAIRE = new Color(0x6B7280);
    public static final Color TEXTE_DISCRET = new Color(0x9CA3AF);

    // ---- Couleur principale : vert pharmacie ----
    public static final Color PRIMAIRE = new Color(0x0E9F6E);
    public static final Color PRIMAIRE_FONCE = new Color(0x057A55);
    public static final Color PRIMAIRE_CLAIR = new Color(0xE6F6EF);

    // ---- Couleurs d'état (badges, messages) ----
    public static final Color SUCCES = new Color(0x057A55);
    public static final Color SUCCES_CLAIR = new Color(0xDEF7EC);
    public static final Color DANGER = new Color(0xE02424);
    public static final Color DANGER_FONCE = new Color(0xC81E1E);
    public static final Color DANGER_CLAIR = new Color(0xFDE8E8);
    public static final Color AVERTISSEMENT = new Color(0xB45309);
    public static final Color AVERTISSEMENT_CLAIR = new Color(0xFEF3C7);
    public static final Color INFO = new Color(0x1C64F2);
    public static final Color INFO_CLAIR = new Color(0xE1EFFE);
    public static final Color NEUTRE = new Color(0x4B5563);
    public static final Color NEUTRE_CLAIR = new Color(0xF3F4F6);

    // ---- Dimensions ----
    public static final int RAYON_CARTE = 12;
    public static final int RAYON_CONTROLE = 8;
    public static final int HAUTEUR_CONTROLE = 36;
    public static final int MARGE_PAGE = 24;

    private static final String FAMILLE = choisirFamille();

    private Theme() {
    }

    /** Police de l'application dans le style et la taille demandés. */
    public static Font police(int style, float taille) {
        return new Font(FAMILLE, style, 12).deriveFont(style, taille);
    }

    public static Font police(float taille) {
        return police(Font.PLAIN, taille);
    }

    /** Segoe UI sous Windows ; à défaut une police sans empattement disponible. */
    private static String choisirFamille() {
        String[] disponibles;
        try {
            disponibles = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
        } catch (Exception e) {
            return Font.SANS_SERIF;
        }
        for (String voulue : new String[] {"Segoe UI", "Inter", "Roboto", "Helvetica Neue", "Arial"}) {
            if (Arrays.asList(disponibles).contains(voulue)) {
                return voulue;
            }
        }
        return Font.SANS_SERIF;
    }

    /**
     * À appeler une seule fois au démarrage, avant de créer la première fenêtre :
     * apparence du système, police par défaut, couleurs de sélection et des boîtes de dialogue.
     */
    public static void installer() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            // On garde l'apparence Swing par défaut : l'application reste utilisable
        }

        // Même police partout
        FontUIResource base = new FontUIResource(police(13f));
        Enumeration<Object> cles = UIManager.getDefaults().keys();
        while (cles.hasMoreElements()) {
            Object cle = cles.nextElement();
            if (UIManager.get(cle) instanceof FontUIResource) {
                UIManager.put(cle, base);
            }
        }

        UIManager.put("Panel.background", SURFACE);
        UIManager.put("OptionPane.background", SURFACE);
        UIManager.put("OptionPane.messageFont", new FontUIResource(police(13f)));
        UIManager.put("OptionPane.buttonFont", new FontUIResource(police(13f)));
        UIManager.put("Table.selectionBackground", PRIMAIRE_CLAIR);
        UIManager.put("Table.selectionForeground", TEXTE);
        UIManager.put("List.selectionBackground", PRIMAIRE_CLAIR);
        UIManager.put("List.selectionForeground", TEXTE);
        UIManager.put("ComboBox.selectionBackground", PRIMAIRE_CLAIR);
        UIManager.put("ComboBox.selectionForeground", TEXTE);
        UIManager.put("TextField.selectionBackground", PRIMAIRE_CLAIR);
        UIManager.put("TextField.selectionForeground", TEXTE);
        UIManager.put("ScrollPane.background", SURFACE);
        UIManager.put("Viewport.background", SURFACE);
        UIManager.put("ToolTip.background", TEXTE);
        UIManager.put("ToolTip.foreground", Color.WHITE);
    }
}
