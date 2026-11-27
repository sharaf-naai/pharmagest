package pharmacie.ui.panels;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.math.RoundingMode;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

import pharmacie.dao.ApprovisionnementDAO;
import pharmacie.dao.MedicamentDAO;
import pharmacie.dao.TableauDeBordDAO;
import pharmacie.model.AlertePeremption;
import pharmacie.model.IndicateursTableauDeBord;
import pharmacie.model.Medicament;
import pharmacie.ui.EcranOutils;
import pharmacie.util.DAOException;

/** Tableau de bord : indicateurs et alertes. Onglet réservé à l'ADMIN (RG28, voir MainFrame). */
public class TableauDeBordPanel extends JPanel {

    private final TableauDeBordDAO tableauDeBordDAO = new TableauDeBordDAO();
    private final MedicamentDAO medicamentDAO = new MedicamentDAO();
    private final ApprovisionnementDAO approvisionnementDAO = new ApprovisionnementDAO();

    // Les 5 cartes d'indicateurs
    private final JLabel valeurMedicaments = new JLabel();
    private final JLabel valeurClients = new JLabel();
    private final JLabel valeurFournisseurs = new JLabel();
    private final JLabel valeurVentes = new JLabel();
    private final JLabel valeurChiffreAffaires = new JLabel();

    private final DefaultTableModel modeleStockFaible =
            EcranOutils.modeleLectureSeule("Médicament", "Stock", "Seuil");
    private final DefaultTableModel modelePeremption =
            EcranOutils.modeleLectureSeule("Médicament", "Lot", "Péremption", "Jours restants");

    public TableauDeBordPanel() {
        super(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Haut : bouton Actualiser + cartes
        JButton boutonActualiser = new JButton("Actualiser");
        boutonActualiser.addActionListener(e -> charger());
        JPanel barre = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        barre.add(boutonActualiser);

        JPanel cartes = new JPanel(new GridLayout(1, 5, 10, 10));
        cartes.add(carte("Médicaments", valeurMedicaments));
        cartes.add(carte("Clients", valeurClients));
        cartes.add(carte("Fournisseurs", valeurFournisseurs));
        cartes.add(carte("Ventes", valeurVentes));
        cartes.add(carte("Chiffre d'affaires", valeurChiffreAffaires));

        JPanel haut = new JPanel(new BorderLayout());
        haut.add(barre, BorderLayout.NORTH);
        haut.add(cartes, BorderLayout.CENTER);

        // Centre : les 2 listes d'alertes
        JPanel listes = new JPanel(new GridLayout(1, 2, 10, 10));
        listes.add(liste("Stock faible", modeleStockFaible));
        listes.add(liste("Lots proches de la péremption", modelePeremption));

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

    private JPanel carte(String titre, JLabel valeur) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(titre));
        valeur.setHorizontalAlignment(SwingConstants.CENTER);
        valeur.setFont(valeur.getFont().deriveFont(Font.BOLD, 22f));
        p.add(valeur, BorderLayout.CENTER);
        return p;
    }

    private JPanel liste(String titre, DefaultTableModel modele) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(BorderFactory.createTitledBorder(titre));
        p.add(new JScrollPane(new JTable(modele)), BorderLayout.CENTER);
        return p;
    }

    /** Relit les indicateurs (vue v_tableau_de_bord) et les deux listes d'alertes. */
    private void charger() {
        try {
            IndicateursTableauDeBord i = tableauDeBordDAO.lireIndicateurs();
            valeurMedicaments.setText(String.valueOf(i.getNbMedicaments()));
            valeurClients.setText(String.valueOf(i.getNbClients()));
            valeurFournisseurs.setText(String.valueOf(i.getNbFournisseurs()));
            valeurVentes.setText(String.valueOf(i.getNbVentes()));
            // RG27 : chiffre d'affaires calculé par la vue, affiché avec 2 décimales
            valeurChiffreAffaires.setText(i.getChiffreAffaires().setScale(2, RoundingMode.HALF_UP) + " DH");

            modeleStockFaible.setRowCount(0);
            for (Medicament m : medicamentDAO.listerStockFaible()) { // RG24
                modeleStockFaible.addRow(new Object[] {m.toString(), m.getQuantiteStock(), m.getSeuilMinimal()});
            }
            modelePeremption.setRowCount(0);
            for (AlertePeremption a : approvisionnementDAO.listerAlertesPeremption()) { // RG26
                modelePeremption.addRow(new Object[] {a.getNomMedicament(), a.getNumeroLot(),
                        a.getDatePeremption(), a.getJoursRestants()});
            }
        } catch (DAOException e) {
            EcranOutils.afficherErreur(this, e);
        }
    }
}
