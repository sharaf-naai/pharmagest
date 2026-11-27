package pharmacie.ui;

import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.Insets;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

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
        formulaire.add(new JLabel(libelle), c);
        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        formulaire.add(champ, c);
    }

    /** Texte saisi sans espaces autour, ou null si le champ est vide (colonne facultative). */
    public static String texteOuNull(JTextField champ) {
        String t = champ.getText().trim();
        return t.isEmpty() ? null : t;
    }
}
