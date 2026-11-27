package pharmacie.ui.theme;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.JPasswordField;

/** Champ de mot de passe arrondi, avec un texte d'aide grisé quand il est vide. */
public class ChampMotDePasse extends JPasswordField {

    private final String aide;

    public ChampMotDePasse(String aide) {
        this.aide = aide;
        BordureChamp.appliquer(this);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        return new Dimension(Math.max(d.width, 120), Theme.HAUTEUR_CONTROLE);
    }

    @Override
    protected void paintComponent(Graphics g) {
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

        if (getPassword().length == 0 && aide != null && !aide.isEmpty()) {
            Graphics2D g3 = (Graphics2D) g.create();
            g3.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            Insets in = getInsets();
            g3.setColor(Theme.TEXTE_DISCRET);
            g3.setFont(getFont());
            int y = (getHeight() - g3.getFontMetrics().getHeight()) / 2 + g3.getFontMetrics().getAscent();
            g3.drawString(aide, in.left, y);
            g3.dispose();
        }
    }
}
