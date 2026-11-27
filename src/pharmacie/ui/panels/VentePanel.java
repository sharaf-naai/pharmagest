package pharmacie.ui.panels;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** Module « Ventes » : écran provisoire, réalisé en phase 3. */
public class VentePanel extends JPanel {

    public VentePanel() {
        super(new BorderLayout());
        add(new JLabel("Module « Ventes » : écran en cours de réalisation (phase 3).",
                SwingConstants.CENTER), BorderLayout.CENTER);
    }
}
