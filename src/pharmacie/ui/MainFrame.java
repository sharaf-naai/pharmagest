package pharmacie.ui;

import java.awt.BorderLayout;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import pharmacie.model.Utilisateur;
import pharmacie.ui.panels.ClientPanel;
import pharmacie.ui.panels.FournisseurPanel;
import pharmacie.ui.panels.MedicamentPanel;
import pharmacie.ui.panels.StockPanel;
import pharmacie.ui.panels.TableauDeBordPanel;
import pharmacie.ui.panels.UtilisateurPanel;
import pharmacie.ui.panels.VentePanel;
import pharmacie.util.Session;

/** Fenêtre principale : un onglet par module, selon le rôle de l'utilisateur connecté (RG03). */
public class MainFrame extends JFrame {

    private final JTabbedPane onglets = new JTabbedPane();

    public MainFrame() {
        super("Pharmacie - Gestion");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        Utilisateur u = Session.getUtilisateur();
        boolean admin = Session.estAdmin();

        // Bandeau du haut : utilisateur connecté + bouton Déconnexion
        JPanel bandeau = new JPanel(new BorderLayout());
        bandeau.setBorder(new EmptyBorder(5, 10, 5, 10));
        bandeau.add(new JLabel("Connecté : " + u.getNomComplet() + " (" + u.getRole() + ")"), BorderLayout.WEST);
        JButton boutonDeconnexion = new JButton("Déconnexion");
        boutonDeconnexion.addActionListener(e -> deconnecter());
        bandeau.add(boutonDeconnexion, BorderLayout.EAST);

        // RG03 : ADMIN voit tout ; PHARMACIEN voit Médicaments, Clients, Ventes, Stocks
        if (admin) {
            onglets.addTab("Tableau de bord", new TableauDeBordPanel()); // RG28 : ADMIN seulement
        }
        onglets.addTab("Médicaments", new MedicamentPanel());
        if (admin) {
            onglets.addTab("Fournisseurs", new FournisseurPanel());
        }
        onglets.addTab("Clients", new ClientPanel());
        onglets.addTab("Ventes", new VentePanel());
        onglets.addTab("Stocks", new StockPanel());
        if (admin) {
            onglets.addTab("Utilisateurs", new UtilisateurPanel());
        }

        add(bandeau, BorderLayout.NORTH);
        add(onglets, BorderLayout.CENTER);
        setSize(1000, 650);
        setLocationRelativeTo(null);
    }

    /** Ferme la session et revient à l'écran de connexion. */
    public void deconnecter() {
        Session.fermer();
        dispose();
        new LoginFrame().setVisible(true);
    }

    public JTabbedPane getOnglets() {
        return onglets;
    }
}
