package pharmacie.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import pharmacie.ui.theme.Carte;
import pharmacie.ui.theme.Theme;
import pharmacie.util.RegleGestionException;

/** Petites méthodes communes aux écrans (affichage des erreurs, tableau, formulaire). */
public final class OutilsEcran {

    private OutilsEcran() {
    }

    /**
     * Affiche une erreur à l'utilisateur, jamais de trace technique :
     * règle de gestion = avertissement avec son message ; sinon = erreur de base de données.
     */
    public static void afficherErreur(Component parent, Exception e) {
        if (e instanceof RegleGestionException) {
            JOptionPane.showMessageDialog(parent, e.getMessage(), "Règle de gestion", JOptionPane.WARNING_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(parent, e.getMessage(), "Erreur de base de données", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Tableau en lecture seule, une seule ligne sélectionnable à la fois. */
    public static JTable creerTable(DefaultTableModel modele) {
        JTable table = new JTable(modele);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getTableHeader().setReorderingAllowed(false);
        return table;
    }

    /** Modèle de tableau dont les cellules ne sont pas modifiables à la main. */
    public static DefaultTableModel creerModele(String... colonnes) {
        return new DefaultTableModel(colonnes, 0) {
            @Override
            public boolean isCellEditable(int ligne, int colonne) {
                return false;
            }
        };
    }

    /** Ajoute « libellé : champ » sur la ligne donnée d'un formulaire en GridBagLayout. */
    public static void ajouterChamp(JPanel formulaire, int ligne, String libelle, JComponent champ) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.gridy = ligne;
        c.gridx = 0;
        c.anchor = GridBagConstraints.WEST;
        formulaire.add(etiquette(libelle), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        formulaire.add(champ, c);
    }

    /** Libellé d'un champ de formulaire (12.5 gras, couleur du texte). */
    public static JLabel etiquette(String texte) {
        JLabel l = new JLabel(texte);
        l.setFont(Theme.police(Font.BOLD, 12.5f));
        l.setForeground(Theme.TEXTE);
        return l;
    }

    /** Titre en gras en haut d'une carte (par exemple « Fiche client »). */
    public static JLabel titreCarte(String texte) {
        JLabel titre = new JLabel(texte);
        titre.setFont(Theme.police(Font.BOLD, 14f));
        titre.setForeground(Theme.TEXTE);
        return titre;
    }

    /** Carte de formulaire : titre, champs, puis boutons, collés en haut de la carte. */
    public static Carte carteFormulaire(String titre, JComponent formulaire, JComponent boutons) {
        JPanel contenu = new JPanel(new BorderLayout(0, 12));
        contenu.setOpaque(false);
        contenu.add(titreCarte(titre), BorderLayout.NORTH);
        contenu.add(formulaire, BorderLayout.CENTER);
        contenu.add(boutons, BorderLayout.SOUTH);
        Carte carte = new Carte(new BorderLayout());
        carte.add(contenu, BorderLayout.NORTH);
        return carte;
    }

    /** Liste déroulante à la police et à la hauteur des autres champs. */
    public static void styliserListe(JComboBox<?> liste) {
        liste.setFont(Theme.police(13f));
        liste.setPreferredSize(new Dimension(liste.getPreferredSize().width, Theme.HAUTEUR_CONTROLE));
    }

    /** Texte saisi sans espaces autour, ou null si le champ est vide (colonne facultative). */
    public static String texteOuNull(JTextField champ) {
        String t = champ.getText().trim();
        return t.isEmpty() ? null : t;
    }
}
