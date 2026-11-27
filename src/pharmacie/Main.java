package pharmacie;

import javax.swing.SwingUtilities;

import pharmacie.ui.LoginFrame;
import pharmacie.ui.theme.Theme;

/** Point d'entrée : applique la charte graphique puis ouvre l'écran de connexion. */
public class Main {

    public static void main(String[] args) {
        // Toute l'interface Swing est créée sur le thread graphique (EDT)
        SwingUtilities.invokeLater(() -> {
            Theme.installer();
            new LoginFrame().setVisible(true);
        });
    }
}
