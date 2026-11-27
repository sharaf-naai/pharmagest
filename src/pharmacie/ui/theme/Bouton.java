package pharmacie.ui.theme;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JButton;
import javax.swing.border.EmptyBorder;

/** Bouton arrondi aux couleurs de l'application. */
public class Bouton extends JButton {

    /** PRIMAIRE = action principale (une par zone), SECONDAIRE = autres actions, DANGER = supprimer, DISCRET = lien. */
    public enum Style { PRIMAIRE, SECONDAIRE, DANGER, DISCRET }

    private final Style style;

    public Bouton(String texte) {
        this(texte, Style.SECONDAIRE, null);
    }

    public Bouton(String texte, Style style) {
        this(texte, style, null);
    }

    public Bouton(String texte, Style style, Icone.Type icone) {
        super(texte);
        this.style = style;
        setFont(Theme.police(Font.BOLD, 13f));
        setForeground(couleurTexte());
        setContentAreaFilled(false);
        setFocusPainted(false);
        setBorderPainted(false);
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(0, 14, 0, 14));
        if (icone != null) {
            setIcon(new Icone(icone, 16, couleurTexte()));
            setIconTextGap(8);
        }
    }

    public Style getStyle() {
        return style;
    }

    private Color couleurTexte() {
        switch (style) {
            case PRIMAIRE:
                return Color.WHITE;
            case DANGER:
                return Theme.DANGER_FONCE;
            case DISCRET:
                return Theme.TEXTE_SECONDAIRE;
            default:
                return Theme.TEXTE;
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(Math.max(d.width, 64), Theme.HAUTEUR_CONTROLE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boolean survol = getModel().isRollover();
        boolean enfonce = getModel().isPressed();
        int w = getWidth() - 1;
        int h = getHeight() - 1;
        int r = Theme.RAYON_CONTROLE * 2;

        Color fond = null;
        Color bord = null;
        switch (style) {
            case PRIMAIRE:
                fond = enfonce ? Theme.PRIMAIRE_FONCE.darker() : survol ? Theme.PRIMAIRE_FONCE : Theme.PRIMAIRE;
                break;
            case SECONDAIRE:
                fond = enfonce ? Theme.NEUTRE_CLAIR : survol ? Theme.SURFACE_DOUCE : Theme.SURFACE;
                bord = Theme.BORDURE_FORTE;
                break;
            case DANGER:
                fond = enfonce ? Theme.DANGER_CLAIR.darker() : survol ? Theme.DANGER_CLAIR : Theme.SURFACE;
                bord = survol ? Theme.DANGER_CLAIR.darker() : Theme.BORDURE_FORTE;
                break;
            case DISCRET:
                fond = survol || enfonce ? Theme.NEUTRE_CLAIR : null;
                break;
            default:
                break;
        }
        if (!isEnabled()) {
            g2.setComposite(java.awt.AlphaComposite.getInstance(java.awt.AlphaComposite.SRC_OVER, 0.45f));
        }
        if (fond != null) {
            g2.setColor(fond);
            g2.fillRoundRect(0, 0, w, h, r, r);
        }
        if (bord != null) {
            g2.setColor(bord);
            g2.drawRoundRect(0, 0, w, h, r, r);
        }
        g2.dispose();

        // Le texte et l'icône sont dessinés par Swing, atténués si le bouton est désactivé
        setForeground(isEnabled() ? couleurTexte() : Theme.TEXTE_DISCRET);
        super.paintComponent(g);
    }
}
