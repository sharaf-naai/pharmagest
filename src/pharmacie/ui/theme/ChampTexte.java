package pharmacie.ui.theme;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.JTextField;

/**
 * Champ de texte arrondi avec un texte d'aide grisé quand il est vide (« Rechercher... »).
 * Avec une icône de recherche si demandé.
 */
public class ChampTexte extends JTextField {

    private final String aide;
    private final boolean recherche;

    public ChampTexte() {
        this("", false);
    }

    public ChampTexte(String aide) {
        this(aide, false);
    }

    /** @param recherche affiche une loupe à gauche du texte */
    public ChampTexte(String aide, boolean recherche) {
        this.aide = aide;
        this.recherche = recherche;
        BordureChamp.appliquer(this);
        if (recherche) {
            setBorder(new BordureChamp(22));
        }
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(Math.max(d.width, 120), Theme.HAUTEUR_CONTROLE);
    }

    @Override
    protected void paintComponent(Graphics g) {
        // Fond blanc limité à l'intérieur de la bordure arrondie
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getParent() != null ? getParent().getBackground() : Theme.SURFACE);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.setColor(getBackground());
        int r = Theme.RAYON_CONTROLE * 2;
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, r, r);
        g2.dispose();

        setOpaque(false);
        super.paintComponent(g);
        setOpaque(true);

        Insets in = getInsets();
        Graphics2D g3 = (Graphics2D) g.create();
        g3.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        if (recherche) {
            new Icone(Icone.Type.RECHERCHE, 16, Theme.TEXTE_DISCRET)
                    .paintIcon(this, g3, 12, (getHeight() - 16) / 2);
        }
        if (getText().isEmpty() && aide != null && !aide.isEmpty()) {
            g3.setColor(Theme.TEXTE_DISCRET);
            g3.setFont(getFont());
            int y = (getHeight() - g3.getFontMetrics().getHeight()) / 2 + g3.getFontMetrics().getAscent();
            g3.drawString(aide, in.left, y);
        }
        g3.dispose();
    }
}
