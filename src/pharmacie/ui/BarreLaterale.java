package pharmacie.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import pharmacie.ui.theme.Icone;
import pharmacie.ui.theme.Theme;

/** Menu de navigation à gauche de la fenêtre principale : logo, rubriques et pages. */
public class BarreLaterale extends JPanel {

    private final JPanel liste = new JPanel();
    private final Map<String, ElementNavigation> elements = new LinkedHashMap<>();
    private String pageActive;

    public BarreLaterale() {
        super(new BorderLayout());
        setBackground(Theme.SURFACE);
        setBorder(new MatteBorder(0, 0, 0, 1, Theme.BORDURE));
        setPreferredSize(new Dimension(236, 0));

        // Logo + nom de l'application
        JLabel logo = new JLabel("PharmaGest", new Icone(Icone.Type.LOGO, 30, Theme.PRIMAIRE), JLabel.LEFT);
        logo.setIconTextGap(10);
        logo.setFont(Theme.police(Font.BOLD, 18f));
        logo.setForeground(Theme.TEXTE);
        logo.setBorder(new EmptyBorder(22, 22, 18, 16));
        add(logo, BorderLayout.NORTH);

        liste.setLayout(new BoxLayout(liste, BoxLayout.Y_AXIS));
        liste.setOpaque(false);
        liste.setBorder(new EmptyBorder(0, 12, 0, 12));
        add(liste, BorderLayout.CENTER);

        JLabel pied = new JLabel("Projet SI · v1.0");
        pied.setFont(Theme.police(12f));
        pied.setForeground(Theme.TEXTE_DISCRET);
        pied.setBorder(new CompoundBorder(new MatteBorder(1, 0, 0, 0, Theme.BORDURE), new EmptyBorder(14, 22, 16, 16)));
        add(pied, BorderLayout.SOUTH);
    }

    /** Titre de rubrique en petites capitales grises (« GESTION »...). */
    public void ajouterSection(String titre) {
        if (liste.getComponentCount() > 0) {
            liste.add(Box.createVerticalStrut(14));
        }
        JLabel l = new JLabel(titre.toUpperCase());
        l.setFont(Theme.police(Font.BOLD, 11f));
        l.setForeground(Theme.TEXTE_DISCRET);
        l.setBorder(new EmptyBorder(6, 10, 6, 0));
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        liste.add(l);
    }

    /** Ajoute une page au menu ; l'action est exécutée au clic. */
    public void ajouterElement(String titre, Icone.Type icone, Runnable action) {
        ElementNavigation e = new ElementNavigation(titre, icone, action);
        e.setAlignmentX(Component.LEFT_ALIGNMENT);
        elements.put(titre, e);
        liste.add(e);
        liste.add(Box.createVerticalStrut(2));
    }

    /** Met en surbrillance la page affichée. */
    public void selectionner(String titre) {
        pageActive = titre;
        for (ElementNavigation e : elements.values()) {
            e.repaint();
        }
    }

    /** Une ligne cliquable du menu : icône + libellé ; fond vert quand la page est affichée. */
    private final class ElementNavigation extends JComponent {
        private final String titre;
        private final Icone.Type icone;
        private boolean survol;

        ElementNavigation(String titre, Icone.Type icone, Runnable action) {
            this.titre = titre;
            this.icone = icone;
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setToolTipText(titre);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    action.run();
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
            return new Dimension(212, 40);
        }

        @Override
        public Dimension getMaximumSize() {
            return new Dimension(Integer.MAX_VALUE, 40);
        }

        @Override
        protected void paintComponent(Graphics g) {
            boolean actif = titre.equals(pageActive);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            if (actif) {
                g2.setColor(Theme.PRIMAIRE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            } else if (survol) {
                g2.setColor(Theme.NEUTRE_CLAIR);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
            }
            new Icone(icone, 18, actif ? java.awt.Color.WHITE : Theme.TEXTE_SECONDAIRE)
                    .paintIcon(this, g2, 12, (getHeight() - 18) / 2);
            g2.setFont(Theme.police(actif ? Font.BOLD : Font.PLAIN, 13.5f));
            g2.setColor(actif ? java.awt.Color.WHITE : Theme.TEXTE);
            int y = (getHeight() - g2.getFontMetrics().getHeight()) / 2 + g2.getFontMetrics().getAscent();
            g2.drawString(titre, 42, y);
            g2.dispose();
        }
    }
}
