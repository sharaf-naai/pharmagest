package pharmacie;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import pharmacie.ui.LoginFrame;

/** Point d'entrée : applique l'apparence du système puis ouvre l'écran de connexion. */
public class Main {

    public static void main(String[] args) {
        // Toute l'interface Swing est créée sur le thread graphique (EDT)
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                // Pas grave : on garde l'apparence Swing par défaut
                System.err.println("Apparence système indisponible : " + e.getMessage());
            }
            new LoginFrame().setVisible(true);
        });
    }
}
