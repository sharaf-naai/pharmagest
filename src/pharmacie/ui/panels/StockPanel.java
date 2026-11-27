package pharmacie.ui.panels;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** Module « Stocks » : écran provisoire, réalisé en phase 3. */
public class StockPanel extends JPanel {

    public StockPanel() {
        super(new BorderLayout());
        add(new JLabel("Module « Stocks » : écran en cours de réalisation (phase 3).",
                SwingConstants.CENTER), BorderLayout.CENTER);
    }
}
