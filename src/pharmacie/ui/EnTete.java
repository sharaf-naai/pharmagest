package pharmacie.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

import pharmacie.model.Role;
import pharmacie.model.Utilisateur;
import pharmacie.ui.theme.Bouton;
import pharmacie.ui.theme.Icone;
import pharmacie.ui.theme.Theme;

/** Bandeau du haut : titre de la page affichée, utilisateur connecté et bouton Déconnexion. */
public class EnTete extends JPanel {

    private final JLabel titre = new JLabel();
    private final JLabel sousTitre = new JLabel();
    private final Bouton boutonDeconnexion = new Bouton("Déconnexion", Bouton.Style.DISCRET, Icone.Type.DECONNEXION);

    public EnTete(Utilisateur utilisateur) {
        super(new BorderLayout());
        setBackground(Theme.SURFACE);
        setBorder(new CompoundBorder(new MatteBorder(0, 0, 1, 0, Theme.BORDURE), new EmptyBorder(14, 28, 14, 20)));

        titre.setFont(Theme.police(Font.BOLD, 20f));
        titre.setForeground(Theme.TEXTE);
        sousTitre.setFont(Theme.police(13f));
        sousTitre.setForeground(Theme.TEXTE_SECONDAIRE);
        JPanel textes = new JPanel(new GridLayout(2, 1, 0, 2));
        textes.setOpaque(false);
        textes.add(titre);
        textes.add(sousTitre);
        add(textes, BorderLayout.CENTER);

        // Utilisateur connecté : initiales dans un rond, nom et rôle
        JLabel nom = new JLabel(utilisateur.getNomComplet());
        nom.setFont(Theme.police(Font.BOLD, 13f));
        nom.setForeground(Theme.TEXTE);
        JLabel role = new JLabel(utilisateur.getRole() == Role.ADMIN ? "Administrateur" : "Pharmacien");
        role.setFont(Theme.police(12f));
        role.setForeground(Theme.TEXTE_SECONDAIRE);
        JPanel identite = new JPanel(new GridLayout(2, 1));
        identite.setOpaque(false);
        identite.add(nom);
        identite.add(role);

        JPanel droite = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        droite.setOpaque(false);
        droite.add(new Avatar(initiales(utilisateur.getNomComplet())));
        droite.add(identite);
        droite.add(Box.createHorizontalStrut(8));
        droite.add(boutonDeconnexion);
        add(droite, BorderLayout.EAST);
    }

    public void setPage(String titrePage, String description) {
        titre.setText(titrePage);
        sousTitre.setText(description);
    }

    public Bouton getBoutonDeconnexion() {
        return boutonDeconnexion;
    }

    private static String initiales(String nomComplet) {
        StringBuilder sb = new StringBuilder();
        for (String mot : nomComplet.trim().split("\\s+")) {
            if (!mot.isEmpty() && sb.length() < 2) {
                sb.append(Character.toUpperCase(mot.charAt(0)));
            }
        }
        return sb.toString();
    }

    /** Rond vert pâle avec les initiales de l'utilisateur. */
    private static final class Avatar extends JComponent {
        private final String texte;

        Avatar(String texte) {
            this.texte = texte;
        }

        @Override
        public Dimension getPreferredSize() {
            return new Dimension(38, 38);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(Theme.PRIMAIRE_CLAIR);
            g2.fillOval(0, 0, 37, 37);
            g2.setColor(Theme.PRIMAIRE_FONCE);
            g2.setFont(Theme.police(Font.BOLD, 13f));
            int w = g2.getFontMetrics().stringWidth(texte);
            int y = (38 - g2.getFontMetrics().getHeight()) / 2 + g2.getFontMetrics().getAscent();
            g2.drawString(texte, (38 - w) / 2, y);
            g2.dispose();
        }
    }
}
