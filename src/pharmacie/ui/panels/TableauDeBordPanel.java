package pharmacie.ui.panels;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.math.RoundingMode;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import pharmacie.dao.ApprovisionnementDAO;
import pharmacie.dao.MedicamentDAO;
import pharmacie.dao.TableauDeBordDAO;
import pharmacie.model.AlertePeremption;
import pharmacie.model.IndicateursTableauDeBord;
import pharmacie.model.Medicament;
import pharmacie.ui.EcranOutils;
import pharmacie.ui.theme.Badge;
import pharmacie.ui.theme.Bouton;
import pharmacie.ui.theme.Carte;
import pharmacie.ui.theme.CarteIndicateur;
import pharmacie.ui.theme.Icone;
import pharmacie.ui.theme.Tableaux;
import pharmacie.ui.theme.Theme;
import pharmacie.ui.theme.Ton;
import pharmacie.util.DAOException;

/** Tableau de bord : indicateurs et alertes. Page réservée à l'ADMIN (RG28, voir MainFrame). */
public class TableauDeBordPanel extends JPanel {

    private final TableauDeBordDAO tableauDeBordDAO = new TableauDeBordDAO();
    private final MedicamentDAO medicamentDAO = new MedicamentDAO();
    private final ApprovisionnementDAO approvisionnementDAO = new ApprovisionnementDAO();

    // Les 5 cartes d'indicateurs
    private final CarteIndicateur carteMedicaments =
            new CarteIndicateur("Médicaments", Icone.Type.MEDICAMENTS, Theme.PRIMAIRE);
    private final CarteIndicateur carteClients = new CarteIndicateur("Clients", Icone.Type.CLIENTS, Theme.INFO);
    private final CarteIndicateur carteFournisseurs =
            new CarteIndicateur("Fournisseurs", Icone.Type.FOURNISSEURS, Theme.AVERTISSEMENT);
    private final CarteIndicateur carteVentes = new CarteIndicateur("Ventes", Icone.Type.VENTES, Theme.NEUTRE);
    private final CarteIndicateur carteChiffreAffaires =
            new CarteIndicateur("Chiffre d'affaires", Icone.Type.CHIFFRE_AFFAIRES, Theme.SUCCES);

    private final DefaultTableModel modeleStockFaible =
            EcranOutils.modeleLectureSeule("Médicament", "Stock", "Seuil");
    private final DefaultTableModel modelePeremption =
            EcranOutils.modeleLectureSeule("Médicament", "Lot", "Péremption", "Jours restants");

    // Nombre d'alertes affiché dans le titre de chaque liste
    private final Badge badgeStockFaible = new Badge("0", Ton.DANGER);
    private final Badge badgePeremption = new Badge("0", Ton.AVERTISSEMENT);

    public TableauDeBordPanel() {
        super(new BorderLayout(0, 16));
        setOpaque(false); // le fond gris vient de MainFrame

        // Haut : bouton Actualiser + cartes
        Bouton boutonActualiser = new Bouton("Actualiser", Bouton.Style.SECONDAIRE, Icone.Type.ACTUALISER);
        boutonActualiser.addActionListener(e -> charger());
        JPanel barre = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        barre.setOpaque(false);
        barre.add(boutonActualiser);

        // Le chiffre d'affaires a plus de poids pour que « 1 656.00 DH » tienne
        JPanel cartes = new JPanel(new GridBagLayout());
        cartes.setOpaque(false);
        ajouterCarte(cartes, carteMedicaments, 0, 1);
        ajouterCarte(cartes, carteClients, 1, 1);
        ajouterCarte(cartes, carteFournisseurs, 2, 1);
        ajouterCarte(cartes, carteVentes, 3, 1);
        ajouterCarte(cartes, carteChiffreAffaires, 4, 1.6);

        JPanel haut = new JPanel(new BorderLayout(0, 16));
        haut.setOpaque(false);
        haut.add(barre, BorderLayout.NORTH);
        haut.add(cartes, BorderLayout.CENTER);

        // Centre : les 2 listes d'alertes
        JTable tableStock = new JTable(modeleStockFaible);
        Tableaux.styliser(tableStock);
        Tableaux.colonneEnGras(tableStock, 0);
        Tableaux.alignerADroite(tableStock, 1);
        Tableaux.alignerADroite(tableStock, 2);
        Tableaux.largeur(tableStock, 0, 220);

        JTable tablePeremption = new JTable(modelePeremption);
        Tableaux.styliser(tablePeremption);
        Tableaux.colonneEnGras(tablePeremption, 0);
        Tableaux.colonneBadge(tablePeremption, 3);
        Tableaux.largeur(tablePeremption, 0, 180);
        Tableaux.largeur(tablePeremption, 3, 130);

        JPanel listes = new JPanel(new GridLayout(1, 2, 16, 16));
        listes.setOpaque(false);
        listes.add(liste("Stock faible", badgeStockFaible, tableStock));
        listes.add(liste("Lots proches de la péremption", badgePeremption, tablePeremption));

        add(haut, BorderLayout.NORTH);
        add(listes, BorderLayout.CENTER);

        // Rechargement chaque fois que l'onglet est affiché
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                charger();
            }
        });
        charger();
    }

    private void ajouterCarte(JPanel conteneur, CarteIndicateur carte, int colonne, double poids) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = colonne;
        c.weightx = poids;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, colonne == 0 ? 0 : 16, 0, 0);
        conteneur.add(carte, c);
    }

    /** Carte d'alerte : ligne de titre (icône, titre en gras, nombre) puis le tableau. */
    private Carte liste(String titre, Badge compteur, JTable table) {
        JLabel libelle = new JLabel(titre, new Icone(Icone.Type.ALERTE, 18, Theme.AVERTISSEMENT), JLabel.LEFT);
        libelle.setIconTextGap(8);
        libelle.setFont(Theme.police(Font.BOLD, 14f));
        libelle.setForeground(Theme.TEXTE);

        JPanel ligneTitre = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        ligneTitre.setOpaque(false);
        ligneTitre.add(libelle);
        ligneTitre.add(compteur);

        Carte carte = new Carte(new BorderLayout(0, 12));
        carte.add(ligneTitre, BorderLayout.NORTH);
        carte.add(Tableaux.defilement(table), BorderLayout.CENTER);
        return carte;
    }

    /** Relit les indicateurs (vue v_tableau_de_bord) et les deux listes d'alertes. */
    private void charger() {
        try {
            IndicateursTableauDeBord i = tableauDeBordDAO.lireIndicateurs();
            carteMedicaments.setValeur(String.valueOf(i.getNbMedicaments()));
            carteClients.setValeur(String.valueOf(i.getNbClients()));
            carteFournisseurs.setValeur(String.valueOf(i.getNbFournisseurs()));
            carteVentes.setValeur(String.valueOf(i.getNbVentes()));
            // RG27 : chiffre d'affaires calculé par la vue, affiché avec 2 décimales
            carteChiffreAffaires.setValeur(i.getChiffreAffaires().setScale(2, RoundingMode.HALF_UP) + " DH");

            modeleStockFaible.setRowCount(0);
            for (Medicament m : medicamentDAO.listerStockFaible()) { // RG24
                modeleStockFaible.addRow(new Object[] {m.toString(), m.getQuantiteStock(), m.getSeuilMinimal()});
            }
            badgeStockFaible.setTexte(String.valueOf(modeleStockFaible.getRowCount()), Ton.DANGER);

            modelePeremption.setRowCount(0);
            for (AlertePeremption a : approvisionnementDAO.listerAlertesPeremption()) { // RG26
                // « N jours » : le badge prend la couleur orange de Ton.pour
                modelePeremption.addRow(new Object[] {a.getNomMedicament(), a.getNumeroLot(),
                        a.getDatePeremption(), a.getJoursRestants() + " jours"});
            }
            badgePeremption.setTexte(String.valueOf(modelePeremption.getRowCount()), Ton.AVERTISSEMENT);
        } catch (DAOException e) {
            EcranOutils.afficherErreur(this, e);
        }
    }
}
