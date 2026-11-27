package pharmacie.ui.theme;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;

/** Badge coloré hors tableau (par exemple un compteur d'alertes à côté d'un titre). */
public class Badge extends JComponent {

    private String texte;
    private Ton ton;

    public Badge(String texte, Ton ton) {
        this.texte = texte;
        this.ton = ton;
        setFont(Theme.police(Font.BOLD, 12f));
        setOpaque(false);
    }

    public void setTexte(String texte, Ton ton) {
        this.texte = texte;
        this.ton = ton;
        revalidate();
        repaint();
    }

    @Override
    public Dimension getPreferredSize() {
        int w = getFontMetrics(getFont()).stringWidth(texte == null ? "" : texte) + 20;
        return new Dimension(w, 24);
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (texte == null || texte.isEmpty()) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        RenduBadge.dessinerBadge(g2, texte, ton, getFont(), 0, getHeight());
        g2.dispose();
    }
}
