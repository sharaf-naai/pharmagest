package pharmacie.ui.theme;

import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;

import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

/** Panneau blanc aux coins arrondis avec une fine bordure : le bloc de base de chaque écran. */
public class Carte extends JPanel {

    public Carte() {
        this(new BorderLayout(), 16);
    }

    public Carte(LayoutManager layout) {
        this(layout, 16);
    }

    /** @param marge espace intérieur en pixels */
    public Carte(LayoutManager layout, int marge) {
        super(layout);
        setOpaque(false);
        setBackground(Theme.SURFACE);
        setBorder(new EmptyBorder(marge, marge, marge, marge));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int r = Theme.RAYON_CARTE;
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, r * 2, r * 2);
        g2.setColor(Theme.BORDURE);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, r * 2, r * 2);
        g2.dispose();
        super.paintComponent(g);
    }
}
