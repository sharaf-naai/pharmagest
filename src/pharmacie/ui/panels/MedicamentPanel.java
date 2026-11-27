package pharmacie.ui.panels;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

import pharmacie.dao.CategorieDAO;
import pharmacie.dao.MedicamentDAO;
import pharmacie.model.Categorie;
import pharmacie.model.Medicament;
import pharmacie.ui.OutilsEcran;
import pharmacie.util.DAOException;
import pharmacie.util.RegleGestionException;
import pharmacie.util.Session;

/**
 * Module « Médicaments » : liste et recherche (RG08) pour tous ;
 * ajout, modification et suppression (RG05 à RG09) pour l'ADMIN seulement.
 */
public class MedicamentPanel extends JPanel {

    private final MedicamentDAO medicamentDAO = new MedicamentDAO();

    private final DefaultTableModel modele = OutilsEcran.creerModele(
            "N°", "Nom", "Forme", "Dosage", "Catégorie", "Prix", "Stock", "Seuil");
    private final JTable table = OutilsEcran.creerTable(modele);
    private final JTextField champRecherche = new JTextField(20);

    private final JTextField champNom = new JTextField(15);
    private final JTextField champForme = new JTextField(15);
    private final JTextField champDosage = new JTextField(15);
    private final JTextField champPrix = new JTextField(15);
    private final JTextField champSeuil = new JTextField(15);
    private final JTextField champStock = new JTextField(15);
    private final JComboBox<Categorie> listeCategories = new JComboBox<>();

    private List<Medicament> medicaments = new ArrayList<>(); // dans l'ordre des lignes du tableau
    private Medicament medicamentSelectionne;                 // null = aucun médicament choisi

    public MedicamentPanel() {
        super(new BorderLayout(10, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));

        // Barre de recherche (Entrée dans le champ = Rechercher)
        JPanel barreRecherche = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton boutonRechercher = new JButton("Rechercher");
        JButton boutonToutAfficher = new JButton("Tout afficher");
        barreRecherche.add(new JLabel("N° ou partie du nom :"));
        barreRecherche.add(champRecherche);
        barreRecherche.add(boutonRechercher);
        barreRecherche.add(boutonToutAfficher);

        add(barreRecherche, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);

        champRecherche.addActionListener(e -> rafraichir());
        boutonRechercher.addActionListener(e -> rafraichir());
        boutonToutAfficher.addActionListener(e -> {
            champRecherche.setText("");
            rafraichir();
        });

        // RG03 : le PHARMACIEN consulte seulement (tableau + recherche), pas de formulaire
        if (Session.estAdmin()) {
            add(creerFormulaire(), BorderLayout.EAST);
            chargerCategories();
        }

        rafraichir();
    }

    /** Formulaire et boutons de gestion, réservés à l'ADMIN. */
    private JPanel creerFormulaire() {
        // Le stock est affiché mais non modifiable : il ne change que par les ventes et les approvisionnements
        champStock.setEditable(false);
        champStock.setText("0");

        JPanel formulaire = new JPanel(new GridBagLayout());
        formulaire.setBorder(new TitledBorder("Fiche médicament"));
        OutilsEcran.ajouterChamp(formulaire, 0, "Nom * :", champNom);
        OutilsEcran.ajouterChamp(formulaire, 1, "Forme :", champForme);
        OutilsEcran.ajouterChamp(formulaire, 2, "Dosage :", champDosage);
        OutilsEcran.ajouterChamp(formulaire, 3, "Prix de vente * :", champPrix);
        OutilsEcran.ajouterChamp(formulaire, 4, "Seuil minimal * :", champSeuil);
        OutilsEcran.ajouterChamp(formulaire, 5, "Catégorie * :", listeCategories);
        OutilsEcran.ajouterChamp(formulaire, 6, "Stock (lecture seule) :", champStock);

        JPanel boutons = new JPanel(new FlowLayout());
        JButton boutonAjouter = new JButton("Ajouter");
        JButton boutonModifier = new JButton("Modifier");
        JButton boutonSupprimer = new JButton("Supprimer");
        JButton boutonVider = new JButton("Vider");
        boutons.add(boutonAjouter);
        boutons.add(boutonModifier);
        boutons.add(boutonSupprimer);
        boutons.add(boutonVider);

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                afficherSelection();
            }
        });
        boutonAjouter.addActionListener(e -> ajouter());
        boutonModifier.addActionListener(e -> modifier());
        boutonSupprimer.addActionListener(e -> supprimer());
        boutonVider.addActionListener(e -> vider());

        JPanel droite = new JPanel(new BorderLayout());
        droite.add(formulaire, BorderLayout.NORTH);
        droite.add(boutons, BorderLayout.CENTER);
        return droite;
    }

    /** RG07 : la liste déroulante propose les catégories de la base. */
    private void chargerCategories() {
        try {
            for (Categorie c : new CategorieDAO().lister()) {
                listeCategories.addItem(c);
            }
        } catch (DAOException e) {
            OutilsEcran.afficherErreur(this, e);
        }
    }

    /** RG08 : tous les médicaments, ou recherche par N° exact ou partie du nom. */
    private void rafraichir() {
        try {
            String texte = champRecherche.getText().trim();
            medicaments = texte.isEmpty() ? medicamentDAO.lister() : medicamentDAO.rechercher(texte);
            modele.setRowCount(0);
            for (Medicament m : medicaments) {
                modele.addRow(new Object[] { m.getIdMedicament(), m.getNomMedicament(), m.getForme(), m.getDosage(),
                        m.getLibelleCategorie(), m.getPrixVente(), m.getQuantiteStock(), m.getSeuilMinimal() });
            }
        } catch (DAOException e) {
            OutilsEcran.afficherErreur(this, e);
        }
    }

    /** Copie le médicament de la ligne sélectionnée dans le formulaire. */
    private void afficherSelection() {
        int ligne = table.getSelectedRow();
        if (ligne < 0) {
            return;
        }
        medicamentSelectionne = medicaments.get(ligne);
        champNom.setText(medicamentSelectionne.getNomMedicament());
        champForme.setText(medicamentSelectionne.getForme());
        champDosage.setText(medicamentSelectionne.getDosage());
        champPrix.setText(medicamentSelectionne.getPrixVente().toPlainString());
        champSeuil.setText(String.valueOf(medicamentSelectionne.getSeuilMinimal()));
        champStock.setText(String.valueOf(medicamentSelectionne.getQuantiteStock()));
        // Sélectionne dans la liste la catégorie du médicament
        for (int i = 0; i < listeCategories.getItemCount(); i++) {
            if (listeCategories.getItemAt(i).getIdCategorie() == medicamentSelectionne.getIdCategorie()) {
                listeCategories.setSelectedIndex(i);
            }
        }
    }

    /**
     * Construit un médicament à partir du formulaire. Les nombres mal saisis sont refusés ici ;
     * les autres règles (RG05, RG06, RG07) sont vérifiées par le DAO.
     */
    private Medicament lireFormulaire(int idMedicament, int stock) throws RegleGestionException {
        BigDecimal prix;
        int seuil;
        try {
            prix = new BigDecimal(champPrix.getText().trim().replace(',', '.')); // accepte 12,50 ou 12.50
        } catch (NumberFormatException e) {
            throw new RegleGestionException("Le prix de vente doit être un nombre (exemple : 25.50).");
        }
        try {
            seuil = Integer.parseInt(champSeuil.getText().trim());
        } catch (NumberFormatException e) {
            throw new RegleGestionException("Le seuil minimal doit être un nombre entier.");
        }
        Categorie categorie = (Categorie) listeCategories.getSelectedItem();
        int idCategorie = categorie == null ? 0 : categorie.getIdCategorie(); // 0 = refusé par le DAO (RG07)
        return new Medicament(idMedicament, champNom.getText(), OutilsEcran.texteOuNull(champForme),
                OutilsEcran.texteOuNull(champDosage), prix, stock, seuil, idCategorie, null);
    }

    private void ajouter() {
        try {
            medicamentDAO.ajouter(lireFormulaire(0, 0)); // un nouveau médicament commence avec un stock de 0
            vider();
            rafraichir();
        } catch (DAOException | RegleGestionException e) {
            OutilsEcran.afficherErreur(this, e);
        }
    }

    private void modifier() {
        if (medicamentSelectionne == null) {
            JOptionPane.showMessageDialog(this, "Sélectionnez d'abord un médicament dans le tableau.");
            return;
        }
        try {
            // Le stock est repris tel quel : le DAO ne le modifie pas
            medicamentDAO.modifier(lireFormulaire(medicamentSelectionne.getIdMedicament(),
                    medicamentSelectionne.getQuantiteStock()));
            vider();
            rafraichir();
        } catch (DAOException | RegleGestionException e) {
            OutilsEcran.afficherErreur(this, e);
        }
    }

    private void supprimer() {
        if (medicamentSelectionne == null) {
            JOptionPane.showMessageDialog(this, "Sélectionnez d'abord un médicament dans le tableau.");
            return;
        }
        int choix = JOptionPane.showConfirmDialog(this,
                "Supprimer le médicament « " + medicamentSelectionne + " » ?",
                "Confirmer la suppression", JOptionPane.YES_NO_OPTION);
        if (choix != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            medicamentDAO.supprimer(medicamentSelectionne.getIdMedicament());
            vider();
            rafraichir();
        } catch (DAOException | RegleGestionException e) {
            OutilsEcran.afficherErreur(this, e); // RG09 : refus si le médicament figure dans une vente ou une livraison
        }
    }

    /** Vide le formulaire et annule la sélection. */
    private void vider() {
        medicamentSelectionne = null;
        table.clearSelection();
        champNom.setText("");
        champForme.setText("");
        champDosage.setText("");
        champPrix.setText("");
        champSeuil.setText("");
        champStock.setText("0");
    }
}
