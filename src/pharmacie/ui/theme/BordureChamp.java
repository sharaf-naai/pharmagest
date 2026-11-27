package pharmacie.ui.theme;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JComponent;
import javax.swing.border.AbstractBorder;

/** Bordure arrondie des champs de saisie ; elle passe en vert quand le champ a le focus. */
public class BordureChamp extends AbstractBorder {

    private final int gaucheEnPlus;

    public BordureChamp() {
        this(0);
    }

    /** @param gaucheEnPlus place réservée à gauche, par exemple pour une icône de recherche */
    public BordureChamp(int gaucheEnPlus) {
        this.gaucheEnPlus = gaucheEnPlus;
    }

    @Override
    public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boolean focus = c.hasFocus();
        int r = Theme.RAYON_CONTROLE * 2;
        if (focus) {
            g2.setColor(Theme.PRIMAIRE_CLAIR);
            g2.setStroke(new java.awt.BasicStroke(3f));
            g2.drawRoundRect(x + 1, y + 1, width - 3, height - 3, r, r);
        }
        g2.setStroke(new java.awt.BasicStroke(1f));
        g2.setColor(focus ? Theme.PRIMAIRE : Theme.BORDURE_FORTE);
        g2.drawRoundRect(x, y, width - 1, height - 1, r, r);
        g2.dispose();
    }

    @Override
    public Insets getBorderInsets(Component c) {
        return new Insets(7, 12 + gaucheEnPlus, 7, 12);
    }

    @Override
    public Insets getBorderInsets(Component c, Insets insets) {
        Insets i = getBorderInsets(c);
        insets.set(i.top, i.left, i.bottom, i.right);
        return insets;
    }

    /** Applique la bordure à un champ existant (JTextField, JPasswordField...) et le redessine au focus. */
    public static void appliquer(JComponent champ) {
        champ.setBorder(new BordureChamp());
        champ.setFont(Theme.police(13f));
        champ.setBackground(Theme.SURFACE);
        champ.setForeground(Theme.TEXTE);
        champ.setOpaque(true);
        champ.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                champ.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                champ.repaint();
            }
        });
    }
}
