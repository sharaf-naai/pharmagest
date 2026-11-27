package pharmacie.ui.panels;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** Module « Médicaments » : écran provisoire, réalisé en phase 3. */
public class MedicamentPanel extends JPanel {

    public MedicamentPanel() {
        super(new BorderLayout());
        add(new JLabel("Module « Médicaments » : écran en cours de réalisation (phase 3).",
                SwingConstants.CENTER), BorderLayout.CENTER);
    }
}
