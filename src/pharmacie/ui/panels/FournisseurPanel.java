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

import pharmacie.dao.FournisseurDAO;
import pharmacie.model.Fournisseur;
import pharmacie.ui.OutilsEcran;
import pharmacie.util.DAOException;
import pharmacie.util.RegleGestionException;

/** Module « Fournisseurs » (ADMIN) : liste, recherche, ajout, modification, suppression (RG10, RG11). */
public class FournisseurPanel extends JPanel {

    private final FournisseurDAO fournisseurDAO = new FournisseurDAO();

    private final DefaultTableModel modele =
            OutilsEcran.creerModele("N°", "Raison sociale", "Téléphone", "Email", "Ville", "Adresse");
    private final JTable table = OutilsEcran.creerTable(modele);
    private final JTextField champRecherche = new JTextField(20);

    private final JTextField champRaisonSociale = new JTextField(15);
    private final JTextField champTelephone = new JTextField(15);
    private final JTextField champEmail = new JTextField(15);
    private final JTextField champAdresse = new JTextField(15);
    private final JTextField champVille = new JTextField(15);

    private List<Fournisseur> fournisseurs = new ArrayList<>(); // dans l'ordre des lignes du tableau
    private Fournisseur fournisseurSelectionne;                 // null = aucun fournisseur choisi

    public FournisseurPanel() {
        super(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Barre de recherche (Entrée dans le champ = Rechercher)
        JPanel barreRecherche = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton boutonRechercher = new JButton("Rechercher");
        JButton boutonToutAfficher = new JButton("Tout afficher");
        barreRecherche.add(new JLabel("Raison sociale ou ville :"));
        barreRecherche.add(champRecherche);
        barreRecherche.add(boutonRechercher);
        barreRecherche.add(boutonToutAfficher);

        // Formulaire de saisie
        JPanel formulaire = new JPanel(new GridBagLayout());
        formulaire.setBorder(new TitledBorder("Fiche fournisseur"));
        OutilsEcran.ajouterChamp(formulaire, 0, "Raison sociale * :", champRaisonSociale);
        OutilsEcran.ajouterChamp(formulaire, 1, "Téléphone * :", champTelephone);
        OutilsEcran.ajouterChamp(formulaire, 2, "Email :", champEmail);
        OutilsEcran.ajouterChamp(formulaire, 3, "Adresse :", champAdresse);
        OutilsEcran.ajouterChamp(formulaire, 4, "Ville :", champVille);

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

    /** Recharge le tableau : tous les fournisseurs, ou ceux qui correspondent à la recherche. */
    private void rafraichir() {
        try {
            String texte = champRecherche.getText().trim();
            fournisseurs = texte.isEmpty() ? fournisseurDAO.lister() : fournisseurDAO.rechercher(texte);
            modele.setRowCount(0);
            for (Fournisseur f : fournisseurs) {
                modele.addRow(new Object[] { f.getIdFournisseur(), f.getRaisonSociale(), f.getTelephoneFournisseur(),
                        f.getEmailFournisseur(), f.getVilleFournisseur(), f.getAdresseFournisseur() });
            }
        } catch (DAOException e) {
            OutilsEcran.afficherErreur(this, e);
        }
    }

    /** Copie le fournisseur de la ligne sélectionnée dans le formulaire. */
    private void afficherSelection() {
        int ligne = table.getSelectedRow();
        if (ligne < 0) {
            return;
        }
        fournisseurSelectionne = fournisseurs.get(ligne);
        champRaisonSociale.setText(fournisseurSelectionne.getRaisonSociale());
        champTelephone.setText(fournisseurSelectionne.getTelephoneFournisseur());
        champEmail.setText(fournisseurSelectionne.getEmailFournisseur());
        champAdresse.setText(fournisseurSelectionne.getAdresseFournisseur());
        champVille.setText(fournisseurSelectionne.getVilleFournisseur());
    }

    /** Construit un fournisseur à partir du formulaire (le DAO vérifie RG10). */
    private Fournisseur lireFormulaire(int idFournisseur) {
        return new Fournisseur(idFournisseur, champRaisonSociale.getText(), champTelephone.getText(),
                OutilsEcran.texteOuNull(champEmail), OutilsEcran.texteOuNull(champAdresse),
                OutilsEcran.texteOuNull(champVille));
    }

    private void ajouter() {
        try {
            fournisseurDAO.ajouter(lireFormulaire(0));
            vider();
            rafraichir();
        } catch (DAOException | RegleGestionException e) {
            OutilsEcran.afficherErreur(this, e);
        }
    }

    private void modifier() {
        if (fournisseurSelectionne == null) {
            JOptionPane.showMessageDialog(this, "Sélectionnez d'abord un fournisseur dans le tableau.");
            return;
        }
        try {
            fournisseurDAO.modifier(lireFormulaire(fournisseurSelectionne.getIdFournisseur()));
            vider();
            rafraichir();
        } catch (DAOException | RegleGestionException e) {
            OutilsEcran.afficherErreur(this, e);
        }
    }

    private void supprimer() {
        if (fournisseurSelectionne == null) {
            JOptionPane.showMessageDialog(this, "Sélectionnez d'abord un fournisseur dans le tableau.");
            return;
        }
        int choix = JOptionPane.showConfirmDialog(this,
                "Supprimer le fournisseur « " + fournisseurSelectionne.getRaisonSociale() + " » ?",
                "Confirmer la suppression", JOptionPane.YES_NO_OPTION);
        if (choix != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            fournisseurDAO.supprimer(fournisseurSelectionne.getIdFournisseur());
            vider();
            rafraichir();
        } catch (DAOException | RegleGestionException e) {
            OutilsEcran.afficherErreur(this, e); // RG11 : refus si le fournisseur a des approvisionnements
        }
    }

    /** Vide le formulaire et annule la sélection. */
    private void vider() {
        fournisseurSelectionne = null;
        table.clearSelection();
        champRaisonSociale.setText("");
        champTelephone.setText("");
        champEmail.setText("");
        champAdresse.setText("");
        champVille.setText("");
    }
}
