package pharmacie.ui.theme;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

/** Carte du tableau de bord : libellé et icône en haut, grande valeur en dessous. */
public class CarteIndicateur extends Carte {

    private final JLabel valeur = new JLabel("—");

    public CarteIndicateur(String libelle, Icone.Type icone, Color teinte) {
        super(new BorderLayout(0, 10), 18);

        JLabel titre = new JLabel(libelle);
        titre.setFont(Theme.police(13f));
        titre.setForeground(Theme.TEXTE_SECONDAIRE);
        valeur.setFont(Theme.police(Font.BOLD, 22f));
        valeur.setForeground(Theme.TEXTE);

        // En haut : libellé à gauche, pastille à droite ; en dessous : la valeur sur toute la largeur
        JPanel haut = new JPanel(new BorderLayout(8, 0));
        haut.setOpaque(false);
        haut.add(titre, BorderLayout.CENTER);
        haut.add(new Pastille(icone, teinte), BorderLayout.EAST);

        add(haut, BorderLayout.NORTH);
        add(valeur, BorderLayout.CENTER);
    }

    public void setValeur(String texte) {
        valeur.setText(texte);
    }

    /** Rond coloré pâle avec l'icône au centre. */
    private static final class Pastille extends JComponent {
        private final Icone icone;
        private final Color teinte;

        Pastille(Icone.Type type, Color teinte) {
            this.teinte = teinte;
            this.icone = new Icone(type, 18, teinte);
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(36, 36);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int y = (getHeight() - 36) / 2;
            g2.setColor(new Color(teinte.getRed(), teinte.getGreen(), teinte.getBlue(), 28));
            g2.fillOval(0, y, 36, 36);
            icone.paintIcon(this, g2, 9, y + 9);
            g2.dispose();
        }
    }
}
