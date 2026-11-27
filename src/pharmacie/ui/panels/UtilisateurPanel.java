package pharmacie.ui.panels;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** Module « Utilisateurs » : écran provisoire, réalisé en phase 3. */
public class UtilisateurPanel extends JPanel {

    public UtilisateurPanel() {
        super(new BorderLayout());
        add(new JLabel("Module « Utilisateurs » : écran en cours de réalisation (phase 3).",
                SwingConstants.CENTER), BorderLayout.CENTER);
    }
}
