package pharmacie.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.RenderingHints;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import pharmacie.ui.theme.Bouton;
import pharmacie.ui.theme.ChampMotDePasse;
import pharmacie.ui.theme.ChampTexte;
import pharmacie.ui.theme.Icone;
import pharmacie.ui.theme.Theme;

/** Contenu de l'écran de connexion : présentation à gauche, formulaire à droite. */
public class PanneauConnexion extends JPanel {

    final ChampTexte champLogin = new ChampTexte("ex. admin");
    final ChampMotDePasse champMotDePasse = new ChampMotDePasse("Votre mot de passe");
    final Bouton boutonConnexion = new Bouton("Se connecter", Bouton.Style.PRIMAIRE);
    final JLabel messageErreur = new JLabel(" ");

    public PanneauConnexion() {
        super(new GridLayout(1, 2));
        add(new Presentation());
        add(construireFormulaire());
    }

    private JPanel construireFormulaire() {
        JPanel colonne = new JPanel();
        colonne.setLayout(new BoxLayout(colonne, BoxLayout.Y_AXIS));
        colonne.setOpaque(false);

        JLabel titre = new JLabel("Connexion");
        titre.setFont(Theme.police(Font.BOLD, 24f));
        titre.setForeground(Theme.TEXTE);
        JLabel aide = new JLabel("Entrez vos identifiants pour accéder à l'application.");
        aide.setFont(Theme.police(13f));
        aide.setForeground(Theme.TEXTE_SECONDAIRE);

        messageErreur.setFont(Theme.police(13f));
        messageErreur.setForeground(Theme.DANGER_FONCE);

        ajouter(colonne, titre, 6);
        ajouter(colonne, aide, 28);
        ajouter(colonne, libelle("Login"), 6);
        ajouter(colonne, champLogin, 16);
        ajouter(colonne, libelle("Mot de passe"), 6);
        ajouter(colonne, champMotDePasse, 10);
        ajouter(colonne, messageErreur, 14);
        ajouter(colonne, boutonConnexion, 0);
        boutonConnexion.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.HAUTEUR_CONTROLE + 4));
        champLogin.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.HAUTEUR_CONTROLE + 4));
        champMotDePasse.setMaximumSize(new Dimension(Integer.MAX_VALUE, Theme.HAUTEUR_CONTROLE + 4));
        colonne.setPreferredSize(new Dimension(330, colonne.getPreferredSize().height));

        JPanel centre = new JPanel(new GridBagLayout());
        centre.setBackground(Theme.SURFACE);
        centre.add(colonne, new GridBagConstraints());
        return centre;
    }

    private static JLabel libelle(String texte) {
        JLabel l = new JLabel(texte);
        l.setFont(Theme.police(Font.BOLD, 12.5f));
        l.setForeground(Theme.TEXTE);
        return l;
    }

    private static void ajouter(JPanel colonne, Component c, int espaceApres) {
        ((javax.swing.JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT);
        colonne.add(c);
        if (espaceApres > 0) {
            colonne.add(Box.createVerticalStrut(espaceApres));
        }
    }

    /** Moitié gauche : fond vert, logo, nom de l'application et ce qu'elle permet de faire. */
    private static final class Presentation extends JPanel {

        Presentation() {
            super(new BorderLayout());
            setBorder(new EmptyBorder(48, 48, 36, 48));

            JPanel haut = new JPanel();
            haut.setLayout(new BoxLayout(haut, BoxLayout.Y_AXIS));
            haut.setOpaque(false);

            JLabel nom = new JLabel("PharmaGest");
            nom.setFont(Theme.police(Font.BOLD, 30f));
            nom.setForeground(Color.WHITE);
            JLabel slogan = new JLabel("La gestion de votre pharmacie, simplement.");
            slogan.setFont(Theme.police(15f));
            slogan.setForeground(new Color(255, 255, 255, 215));

            ajouter(haut, Box.createVerticalStrut(64), 0);
            ajouter(haut, nom, 8);
            ajouter(haut, slogan, 34);
            ajouter(haut, point("Ventes avec calcul automatique du total"), 14);
            ajouter(haut, point("Stock mis à jour à chaque vente et livraison"), 14);
            ajouter(haut, point("Alertes de stock faible et de péremption"), 0);
            add(haut, BorderLayout.NORTH);

            JLabel pied = new JLabel("Projet SI · Université Cadi Ayyad");
            pied.setFont(Theme.police(12f));
            pied.setForeground(new Color(255, 255, 255, 170));
            add(pied, BorderLayout.SOUTH);
        }

        private static JLabel point(String texte) {
            JLabel l = new JLabel(texte, new Icone(Icone.Type.VALIDER, 18, Color.WHITE), JLabel.LEFT);
            l.setIconTextGap(10);
            l.setFont(Theme.police(14f));
            l.setForeground(Color.WHITE);
            return l;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setPaint(new java.awt.GradientPaint(0, 0, Theme.PRIMAIRE, getWidth(), getHeight(), Theme.PRIMAIRE_FONCE));
            g2.fillRect(0, 0, getWidth(), getHeight());
            // Deux grands cercles très pâles pour donner du relief
            g2.setColor(new Color(255, 255, 255, 18));
            g2.fillOval(getWidth() - 220, -120, 360, 360);
            g2.fillOval(-140, getHeight() - 200, 320, 320);
            // Logo : carré blanc arrondi avec la croix verte
            g2.setColor(Color.WHITE);
            g2.fillRoundRect(48, 48, 52, 52, 16, 16);
            g2.setColor(Theme.PRIMAIRE);
            g2.fillRoundRect(48 + 22, 48 + 11, 8, 30, 4, 4);
            g2.fillRoundRect(48 + 11, 48 + 22, 30, 8, 4, 4);
            g2.dispose();
        }

        private static void ajouter(JPanel colonne, Component c, int espaceApres) {
            if (c instanceof javax.swing.JComponent) {
                ((javax.swing.JComponent) c).setAlignmentX(Component.LEFT_ALIGNMENT);
            }
            colonne.add(c);
            if (espaceApres > 0) {
                colonne.add(Box.createVerticalStrut(espaceApres));
            }
        }
    }

    @Override
    public Insets getInsets() {
        return new Insets(0, 0, 0, 0);
    }
}
