package pharmacie.ui.panels;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import pharmacie.dao.CategorieDAO;
import pharmacie.dao.MedicamentDAO;
import pharmacie.model.Categorie;
import pharmacie.model.Medicament;
import pharmacie.ui.OutilsEcran;
import pharmacie.ui.theme.Bouton;
import pharmacie.ui.theme.Carte;
import pharmacie.ui.theme.ChampTexte;
import pharmacie.ui.theme.Icone;
import pharmacie.ui.theme.Tableaux;
import pharmacie.ui.theme.Theme;
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
    private final ChampTexte champRecherche = new ChampTexte("Rechercher par N° ou partie du nom", true);

    private final ChampTexte champNom = new ChampTexte();
    private final ChampTexte champForme = new ChampTexte();
    private final ChampTexte champDosage = new ChampTexte();
    private final ChampTexte champPrix = new ChampTexte();
    private final ChampTexte champSeuil = new ChampTexte();
    private final ChampTexte champStock = new ChampTexte();
    private final JComboBox<Categorie> listeCategories = new JComboBox<>();

    private List<Medicament> medicaments = new ArrayList<>(); // dans l'ordre des lignes du tableau
    private Medicament medicamentSelectionne;                 // null = aucun médicament choisi

    public MedicamentPanel() {
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

        // Tableau : nom en gras, nombres à droite
        Tableaux.styliser(table);
        Tableaux.largeur(table, 0, 50);
        Tableaux.largeur(table, 1, 200);
        Tableaux.colonneEnGras(table, 1);
        for (int col = 5; col <= 7; col++) {
            Tableaux.alignerADroite(table, col);
        }

        Carte carteListe = new Carte(new BorderLayout(0, 12));
        carteListe.add(barreRecherche, BorderLayout.NORTH);
        carteListe.add(Tableaux.defilement(table), BorderLayout.CENTER);
        add(carteListe, BorderLayout.CENTER);

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

        // Recharger les données chaque fois que l'onglet devient visible
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                rafraichir();
            }
        });
        rafraichir();
    }

    /** Formulaire et boutons de gestion, réservés à l'ADMIN. */
    private JPanel creerFormulaire() {
        // Le stock est affiché mais non modifiable : il ne change que par les ventes et les approvisionnements
        champStock.setEditable(false);
        champStock.setText("0");
        champNom.setColumns(16);
        OutilsEcran.styliserListe(listeCategories);

        JPanel formulaire = new JPanel(new GridBagLayout());
        formulaire.setOpaque(false);
        OutilsEcran.ajouterChamp(formulaire, 0, "Nom * :", champNom);
        OutilsEcran.ajouterChamp(formulaire, 1, "Forme :", champForme);
        OutilsEcran.ajouterChamp(formulaire, 2, "Dosage :", champDosage);
        OutilsEcran.ajouterChamp(formulaire, 3, "Prix de vente * :", champPrix);
        OutilsEcran.ajouterChamp(formulaire, 4, "Seuil minimal * :", champSeuil);
        OutilsEcran.ajouterChamp(formulaire, 5, "Catégorie * :", listeCategories);
        OutilsEcran.ajouterChamp(formulaire, 6, "Stock (lecture seule) :", champStock);

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

        table.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                afficherSelection();
            }
        });
        boutonAjouter.addActionListener(e -> ajouter());
        boutonModifier.addActionListener(e -> modifier());
        boutonSupprimer.addActionListener(e -> supprimer());
        boutonVider.addActionListener(e -> vider());

        // Carte de droite : titre de la fiche, champs, puis boutons
        return OutilsEcran.carteFormulaire("Fiche médicament", formulaire, boutons);
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
