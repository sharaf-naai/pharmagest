package pharmacie.ui.panels;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import pharmacie.dao.ClientDAO;
import pharmacie.model.Client;
import pharmacie.ui.OutilsEcran;
import pharmacie.ui.theme.Bouton;
import pharmacie.ui.theme.Carte;
import pharmacie.ui.theme.ChampTexte;
import pharmacie.ui.theme.Icone;
import pharmacie.ui.theme.Tableaux;
import pharmacie.ui.theme.Theme;
import pharmacie.util.DAOException;
import pharmacie.util.RegleGestionException;

/** Module « Clients » : liste, recherche, ajout, modification, suppression (RG12, RG13). */
public class ClientPanel extends JPanel {

    private final ClientDAO clientDAO = new ClientDAO();

    private final DefaultTableModel modele = OutilsEcran.creerModele("N°", "Nom", "Prénom", "Téléphone", "Adresse");
    private final JTable table = OutilsEcran.creerTable(modele);
    private final ChampTexte champRecherche = new ChampTexte("Rechercher par nom, prénom ou téléphone", true);

    private final ChampTexte champNom = new ChampTexte();
    private final ChampTexte champPrenom = new ChampTexte();
    private final ChampTexte champTelephone = new ChampTexte();
    private final ChampTexte champAdresse = new ChampTexte();

    private List<Client> clients = new ArrayList<>(); // clients affichés, dans l'ordre des lignes du tableau
    private Client clientSelectionne;                 // null = aucun client choisi dans le tableau

    public ClientPanel() {
        super(new BorderLayout(16, 16));
        setOpaque(false); // le fond gris vient de MainFrame

        // Barre de recherche (Entrée dans le champ = Rechercher)
        champRecherche.setPreferredSize(new Dimension(320, Theme.HAUTEUR_CONTROLE));
        JPanel barreRecherche = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        barreRecherche.setOpaque(false);
        Bouton boutonRechercher = new Bouton("Rechercher", Bouton.Style.SECONDAIRE, Icone.Type.RECHERCHE);
        Bouton boutonToutAfficher = new Bouton("Tout afficher", Bouton.Style.DISCRET);
        barreRecherche.add(champRecherche);
        barreRecherche.add(boutonRechercher);
        barreRecherche.add(boutonToutAfficher);

        // Tableau : nom en gras
        Tableaux.styliser(table);
        Tableaux.largeur(table, 0, 50);
        Tableaux.colonneEnGras(table, 1);
        Tableaux.largeur(table, 4, 220);

        // Formulaire de saisie
        champNom.setColumns(16);
        JPanel formulaire = new JPanel(new GridBagLayout());
        formulaire.setOpaque(false);
        OutilsEcran.ajouterChamp(formulaire, 0, "Nom * :", champNom);
        OutilsEcran.ajouterChamp(formulaire, 1, "Prénom * :", champPrenom);
        OutilsEcran.ajouterChamp(formulaire, 2, "Téléphone :", champTelephone);
        OutilsEcran.ajouterChamp(formulaire, 3, "Adresse :", champAdresse);

        JPanel boutons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 12));
        boutons.setOpaque(false);
        Bouton boutonAjouter = new Bouton("Ajouter", Bouton.Style.PRIMAIRE, Icone.Type.AJOUTER);
        Bouton boutonModifier = new Bouton("Modifier", Bouton.Style.SECONDAIRE, Icone.Type.MODIFIER);
        Bouton boutonSupprimer = new Bouton("Supprimer", Bouton.Style.DANGER, Icone.Type.SUPPRIMER);
        Bouton boutonVider = new Bouton("Vider", Bouton.Style.SECONDAIRE);
        boutons.add(boutonAjouter);
        boutons.add(boutonModifier);
        boutons.add(boutonSupprimer);
        boutons.add(boutonVider);

        Carte carteListe = new Carte(new BorderLayout(0, 12));
        carteListe.add(barreRecherche, BorderLayout.NORTH);
        carteListe.add(Tableaux.defilement(table), BorderLayout.CENTER);
        add(carteListe, BorderLayout.CENTER);
        add(OutilsEcran.carteFormulaire("Fiche client", formulaire, boutons), BorderLayout.EAST);

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
