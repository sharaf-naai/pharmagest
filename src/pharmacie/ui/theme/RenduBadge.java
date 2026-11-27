package pharmacie.ui.theme;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;

/**
 * Affiche une cellule de tableau sous forme de badge coloré (« Stock faible », « Actif »...).
 * La couleur vient de {@link Ton#pour(String)}, ou d'une table de correspondance fournie.
 */
public class RenduBadge extends JComponent implements TableCellRenderer {

    private final Map<String, Ton> tons;
    private String texte = "";
    private Ton ton = Ton.NEUTRE;
    private boolean selectionne;

    public RenduBadge() {
        this(null);
    }

    /** @param tons couleur imposée pour certains libellés (les autres suivent Ton.pour) */
    public RenduBadge(Map<String, Ton> tons) {
        this.tons = tons;
        setFont(Theme.police(Font.BOLD, 12f));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object valeur, boolean isSelected,
                                                   boolean hasFocus, int ligne, int colonne) {
        texte = valeur == null ? "" : valeur.toString();
        ton = (tons != null && tons.containsKey(texte)) ? tons.get(texte) : Ton.pour(texte);
        selectionne = isSelected;
        return this;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setColor(selectionne ? Theme.PRIMAIRE_CLAIR : Theme.SURFACE);
        g2.fillRect(0, 0, getWidth(), getHeight());
        if (!texte.isEmpty()) {
            dessinerBadge(g2, texte, ton, getFont(), 12, getHeight());
        }
        g2.dispose();
    }

    /** Dessine un badge arrondi à partir de x, centré verticalement dans la hauteur donnée. */
    static void dessinerBadge(Graphics2D g2, String texte, Ton ton, Font police, int x, int hauteur) {
        g2.setFont(police);
        FontMetrics fm = g2.getFontMetrics();
        int w = fm.stringWidth(texte) + 20;
        int h = 22;
        int y = (hauteur - h) / 2;
        g2.setColor(ton.getFond());
        g2.fillRoundRect(x, y, w, h, h, h);
        Color point = ton.getTexte();
        g2.setColor(point);
        g2.drawString(texte, x + 10, y + (h - fm.getHeight()) / 2 + fm.getAscent());
    }
}
