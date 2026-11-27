package pharmacie.ui.panels;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** Module « Tableau de bord » : écran provisoire, réalisé en phase 3. */
public class TableauDeBordPanel extends JPanel {

    public TableauDeBordPanel() {
        super(new BorderLayout());
        add(new JLabel("Module « Tableau de bord » : écran en cours de réalisation (phase 3).",
                SwingConstants.CENTER), BorderLayout.CENTER);
    }
}
