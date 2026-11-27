package pharmacie.ui.panels;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

import pharmacie.dao.ClientDAO;
import pharmacie.model.Client;
import pharmacie.ui.OutilsEcran;
import pharmacie.util.DAOException;
import pharmacie.util.RegleGestionException;

/** Module « Clients » : liste, recherche, ajout, modification, suppression (RG12, RG13). */
public class ClientPanel extends JPanel {

    private final ClientDAO clientDAO = new ClientDAO();

    private final DefaultTableModel modele = OutilsEcran.creerModele("N°", "Nom", "Prénom", "Téléphone", "Adresse");
    private final JTable table = OutilsEcran.creerTable(modele);
    private final JTextField champRecherche = new JTextField(20);

    private final JTextField champNom = new JTextField(15);
    private final JTextField champPrenom = new JTextField(15);
    private final JTextField champTelephone = new JTextField(15);
    private final JTextField champAdresse = new JTextField(15);

    private List<Client> clients = new ArrayList<>(); // clients affichés, dans l'ordre des lignes du tableau
    private Client clientSelectionne;                 // null = aucun client choisi dans le tableau

    public ClientPanel() {
        super(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Barre de recherche (Entrée dans le champ = Rechercher)
        JPanel barreRecherche = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton boutonRechercher = new JButton("Rechercher");
        JButton boutonToutAfficher = new JButton("Tout afficher");
        barreRecherche.add(new JLabel("Nom, prénom ou téléphone :"));
        barreRecherche.add(champRecherche);
        barreRecherche.add(boutonRechercher);
        barreRecherche.add(boutonToutAfficher);

        // Formulaire de saisie
        JPanel formulaire = new JPanel(new GridBagLayout());
        formulaire.setBorder(new TitledBorder("Fiche client"));
        OutilsEcran.ajouterChamp(formulaire, 0, "Nom * :", champNom);
        OutilsEcran.ajouterChamp(formulaire, 1, "Prénom * :", champPrenom);
        OutilsEcran.ajouterChamp(formulaire, 2, "Téléphone :", champTelephone);
        OutilsEcran.ajouterChamp(formulaire, 3, "Adresse :", champAdresse);

        JPanel boutons = new JPanel(new FlowLayout());
        JButton boutonAjouter = new JButton("Ajouter");
        JButton boutonModifier = new JButton("Modifier");
        JButton boutonSupprimer = new JButton("Supprimer");
        JButton boutonVider = new JButton("Vider");
        boutons.add(boutonAjouter);
        boutons.add(boutonModifier);
        boutons.add(boutonSupprimer);
        boutons.add(boutonVider);

        JPanel droite = new JPanel(new BorderLayout());
        droite.add(formulaire, BorderLayout.NORTH);
        droite.add(boutons, BorderLayout.CENTER);

        add(barreRecherche, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(droite, BorderLayout.EAST);

        // Événements
        champRecherche.addActionListener(e -> rafraichir());
        boutonRechercher.addActionListener(e -> rafraichir());
        boutonToutAfficher.addActionListener(e -> {
            champRecherche.setText("");
            rafraichir();
        });
        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                afficherSelection();
            }
        });
        boutonAjouter.addActionListener(e -> ajouter());
        boutonModifier.addActionListener(e -> modifier());
        boutonSupprimer.addActionListener(e -> supprimer());
        boutonVider.addActionListener(e -> vider());

        // Recharger les données chaque fois que l'onglet devient visible
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                rafraichir();
            }
        });
        rafraichir();
    }

    /** Recharge le tableau : tous les clients, ou ceux qui correspondent à la recherche. */
    private void rafraichir() {
        try {
            String texte = champRecherche.getText().trim();
            clients = texte.isEmpty() ? clientDAO.lister() : clientDAO.rechercher(texte);
            modele.setRowCount(0);
            for (Client c : clients) {
                modele.addRow(new Object[] { c.getIdClient(), c.getNomClient(), c.getPrenomClient(),
                        c.getTelephoneClient(), c.getAdresseClient() });
            }
        } catch (DAOException e) {
            OutilsEcran.afficherErreur(this, e);
        }
    }

    /** Copie le client de la ligne sélectionnée dans le formulaire. */
    private void afficherSelection() {
        int ligne = table.getSelectedRow();
        if (ligne < 0) {
            return;
        }
        clientSelectionne = clients.get(ligne);
        champNom.setText(clientSelectionne.getNomClient());
        champPrenom.setText(clientSelectionne.getPrenomClient());
        champTelephone.setText(clientSelectionne.getTelephoneClient());
        champAdresse.setText(clientSelectionne.getAdresseClient());
    }

    /** Construit un client à partir du formulaire (le DAO vérifie RG12). */
    private Client lireFormulaire(int idClient) {
        return new Client(idClient, champNom.getText(), champPrenom.getText(),
                OutilsEcran.texteOuNull(champTelephone), OutilsEcran.texteOuNull(champAdresse));
    }

    private void ajouter() {
        try {
            clientDAO.ajouter(lireFormulaire(0));
            vider();
            rafraichir();
        } catch (DAOException | RegleGestionException e) {
            OutilsEcran.afficherErreur(this, e);
        }
    }

    private void modifier() {
        if (clientSelectionne == null) {
            JOptionPane.showMessageDialog(this, "Sélectionnez d'abord un client dans le tableau.");
            return;
        }
        try {
            clientDAO.modifier(lireFormulaire(clientSelectionne.getIdClient()));
            vider();
            rafraichir();
        } catch (DAOException | RegleGestionException e) {
            OutilsEcran.afficherErreur(this, e);
        }
    }

    private void supprimer() {
        if (clientSelectionne == null) {
            JOptionPane.showMessageDialog(this, "Sélectionnez d'abord un client dans le tableau.");
            return;
        }
        // RG13 : on prévient que les ventes du client sont conservées comme « client de passage »
        int choix = JOptionPane.showConfirmDialog(this,
                "Supprimer le client " + clientSelectionne.getNomClient() + " " + clientSelectionne.getPrenomClient()
                + " ?\nSes ventes seront conservées et affichées comme « client de passage ».",
                "Confirmer la suppression", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (choix != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            clientDAO.supprimer(clientSelectionne.getIdClient());
            vider();
            rafraichir();
        } catch (DAOException e) {
            OutilsEcran.afficherErreur(this, e);
        }
    }

    /** Vide le formulaire et annule la sélection. */
    private void vider() {
        clientSelectionne = null;
        table.clearSelection();
        champNom.setText("");
        champPrenom.setText("");
        champTelephone.setText("");
        champAdresse.setText("");
    }
}
