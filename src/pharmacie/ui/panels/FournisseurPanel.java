package pharmacie.ui.panels;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** Module « Fournisseurs » : écran provisoire, réalisé en phase 3. */
public class FournisseurPanel extends JPanel {

    public FournisseurPanel() {
        super(new BorderLayout());
        add(new JLabel("Module « Fournisseurs » : écran en cours de réalisation (phase 3).",
                SwingConstants.CENTER), BorderLayout.CENTER);
    }
}
