package pharmacie.ui;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import pharmacie.ui.panels.ClientPanel;
import pharmacie.ui.panels.FournisseurPanel;
import pharmacie.ui.panels.MedicamentPanel;
import pharmacie.ui.panels.StockPanel;
import pharmacie.ui.panels.TableauDeBordPanel;
import pharmacie.ui.panels.UtilisateurPanel;
import pharmacie.ui.panels.VentePanel;
import pharmacie.ui.theme.Icone;
import pharmacie.ui.theme.Theme;
import pharmacie.util.Session;

/**
 * Fenêtre principale : menu à gauche, bandeau en haut, page choisie au centre.
 * Les pages visibles dépendent du rôle de l'utilisateur connecté (RG03).
 */
public class MainFrame extends JFrame {

    private final CardLayout cartes = new CardLayout();
    private final JPanel zonePages = new JPanel(cartes);
    private final BarreLaterale barre = new BarreLaterale();
    private final EnTete entete;
    private final List<String> titresPages = new ArrayList<>();
    private final Map<String, String> descriptions = new HashMap<>();

    public MainFrame() {
        super("PharmaGest - Gestion de pharmacie");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        boolean admin = Session.estAdmin();

        entete = new EnTete(Session.getUtilisateur());
        entete.getBoutonDeconnexion().addActionListener(e -> deconnecter());

        // Chaque page est une carte du CardLayout : quand elle s'affiche, son componentShown
        // se déclenche et elle recharge ses données.
        zonePages.setBackground(Theme.FOND);
        zonePages.setBorder(new EmptyBorder(Theme.MARGE_PAGE, Theme.MARGE_PAGE, Theme.MARGE_PAGE, Theme.MARGE_PAGE));

        barre.ajouterSection("Principal");
        if (admin) {
            ajouterPage("Tableau de bord", "Vue d'ensemble de l'activité de la pharmacie",
                    Icone.Type.TABLEAU_DE_BORD, new TableauDeBordPanel()); // RG28 : ADMIN seulement
        }
        ajouterPage("Ventes", "Créer une vente et consulter l'historique", Icone.Type.VENTES, new VentePanel());

        barre.ajouterSection("Catalogue");
        ajouterPage("Médicaments", "Catalogue, prix et quantités disponibles", Icone.Type.MEDICAMENTS,
                new MedicamentPanel());
        ajouterPage("Stocks", "État du stock, approvisionnements et péremptions", Icone.Type.STOCKS,
                new StockPanel());

        barre.ajouterSection("Contacts");
        ajouterPage("Clients", "Fiches des clients de la pharmacie", Icone.Type.CLIENTS, new ClientPanel());
        if (admin) {
            ajouterPage("Fournisseurs", "Fiches des fournisseurs", Icone.Type.FOURNISSEURS, new FournisseurPanel());

            barre.ajouterSection("Administration");
            ajouterPage("Utilisateurs", "Comptes et rôles d'accès", Icone.Type.UTILISATEURS, new UtilisateurPanel());
        }

        JPanel droite = new JPanel(new BorderLayout());
        droite.add(entete, BorderLayout.NORTH);
        droite.add(zonePages, BorderLayout.CENTER);

        JPanel racine = new JPanel(new BorderLayout());
        racine.add(barre, BorderLayout.WEST);
        racine.add(droite, BorderLayout.CENTER);
        setContentPane(racine);

        afficherPage(titresPages.get(0));
        setMinimumSize(new Dimension(1100, 680));
        setSize(1280, 780);
        setLocationRelativeTo(null);
    }

    private void ajouterPage(String titre, String description, Icone.Type icone, JComponent page) {
        page.setOpaque(false); // le fond gris de la zone des pages reste visible autour des cartes
        zonePages.add(page, titre);
        titresPages.add(titre);
        descriptions.put(titre, description);
        barre.ajouterElement(titre, icone, () -> afficherPage(titre));
    }

    /** Affiche la page demandée et met à jour le menu et le bandeau. */
    public void afficherPage(String titre) {
        cartes.show(zonePages, titre);
        barre.selectionner(titre);
        entete.setPage(titre, descriptions.get(titre));
    }

    /** Titres des pages accessibles, dans l'ordre du menu (utilisé par TestPhase2). */
    public List<String> getTitresPages() {
        return new ArrayList<>(titresPages);
    }

    /** Ferme la session et revient à l'écran de connexion. */
    public void deconnecter() {
        Session.fermer();
        dispose();
        new LoginFrame().setVisible(true);
    }
}
