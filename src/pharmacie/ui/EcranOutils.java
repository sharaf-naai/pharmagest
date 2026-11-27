package pharmacie.ui;

import java.awt.Component;

import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

import pharmacie.util.RegleGestionException;

/** Petites méthodes communes aux écrans Stocks, Tableau de bord et Utilisateurs. */
public final class EcranOutils {

    private EcranOutils() {
    }

    /** Modèle de JTable dont les cellules ne sont pas modifiables à la souris. */
    public static DefaultTableModel modeleLectureSeule(String... colonnes) {
        return new DefaultTableModel(colonnes, 0) {
            @Override
            public boolean isCellEditable(int ligne, int colonne) {
                return false;
            }
        };
    }

    /** Règle de gestion : message en avertissement. Erreur technique : "Erreur de base de données". */
    public static void afficherErreur(Component parent, Exception e) {
        if (e instanceof RegleGestionException) {
            JOptionPane.showMessageDialog(parent, e.getMessage(), "Attention", JOptionPane.WARNING_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(parent, e.getMessage(), "Erreur de base de données",
                    JOptionPane.ERROR_MESSAGE);
        }
    }
}
