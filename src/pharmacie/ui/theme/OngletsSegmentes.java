package pharmacie.ui.theme;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Remplace un JTabbedPane pour les sous-onglets d'un écran (« Nouvelle vente / Historique ») :
 * une barre de boutons en pilule au-dessus, et le contenu choisi en dessous.
 * Mêmes méthodes utiles qu'un JTabbedPane : addTab, getSelectedIndex, setSelectedIndex, addChangeListener.
 */
public class OngletsSegmentes extends JPanel {

    private final JPanel barre = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4)) {
        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.NEUTRE_CLAIR);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            g2.dispose();
        }
    };
    private final CardLayout cartes = new CardLayout();
    private final JPanel contenu = new JPanel(cartes);
    private final List<Segment> segments = new ArrayList<>();
    private final List<ChangeListener> ecouteurs = new ArrayList<>();
    private int selection = -1;

    public OngletsSegmentes() {
        super(new BorderLayout(0, 16));
        setOpaque(false);
        barre.setOpaque(false);
        contenu.setOpaque(false);
        JPanel haut = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        haut.setOpaque(false);
        haut.add(barre);
        add(haut, BorderLayout.NORTH);
        add(contenu, BorderLayout.CENTER);
    }

    public void addTab(String titre, JComponent composant) {
        int index = segments.size();
        Segment s = new Segment(titre, index);
        segments.add(s);
        barre.add(s);
        contenu.add(composant, String.valueOf(index));
        if (selection < 0) {
            setSelectedIndex(0);
        }
    }

    public int getSelectedIndex() {
        return selection;
    }

    public int getTabCount() {
        return segments.size();
    }

    public void setSelectedIndex(int index) {
        if (index == selection || index < 0 || index >= segments.size()) {
            return;
        }
        selection = index;
        cartes.show(contenu, String.valueOf(index));
        for (Segment s : segments) {
            s.repaint();
        }
        ChangeEvent evt = new ChangeEvent(this);
        for (ChangeListener l : ecouteurs) {
            l.stateChanged(evt);
        }
    }

    public void addChangeListener(ChangeListener l) {
        ecouteurs.add(l);
    }

    /** Un bouton de la barre. */
    private final class Segment extends JLabel {
        private final int index;
        private boolean survol;

        Segment(String titre, int index) {
            super(titre, SwingConstants.CENTER);
            this.index = index;
            setBorder(new EmptyBorder(0, 16, 0, 16));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    setSelectedIndex(Segment.this.index);
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    survol = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    survol = false;
                    repaint();
                }
            });
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            return new Dimension(d.width, 30);
        }

        @Override
        protected void paintComponent(Graphics g) {
            boolean actif = index == selection;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (actif) {
                g2.setColor(Theme.SURFACE);
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(Theme.BORDURE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
            }
            g2.dispose();
            setFont(Theme.police(actif ? Font.BOLD : Font.PLAIN, 13f));
            setForeground(actif ? Theme.PRIMAIRE_FONCE : survol ? Theme.TEXTE : Theme.TEXTE_SECONDAIRE);
            super.paintComponent(g);
        }
    }
}
