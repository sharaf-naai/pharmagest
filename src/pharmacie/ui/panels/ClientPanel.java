package pharmacie.ui.panels;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** Module « Clients » : écran provisoire, réalisé en phase 3. */
public class ClientPanel extends JPanel {

    public ClientPanel() {
        super(new BorderLayout());
        add(new JLabel("Module « Clients » : écran en cours de réalisation (phase 3).",
                SwingConstants.CENTER), BorderLayout.CENTER);
    }
}
