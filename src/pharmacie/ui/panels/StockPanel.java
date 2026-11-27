package pharmacie.ui.panels;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import pharmacie.dao.ApprovisionnementDAO;
import pharmacie.dao.FournisseurDAO;
import pharmacie.dao.MedicamentDAO;
import pharmacie.model.AlertePeremption;
import pharmacie.model.Approvisionnement;
import pharmacie.model.Fournisseur;
import pharmacie.model.LigneApprovisionnement;
import pharmacie.model.Medicament;
import pharmacie.ui.EcranOutils;
import pharmacie.util.DAOException;
import pharmacie.util.RegleGestionException;
import pharmacie.util.Session;

/**
 * Module « Stocks » : état du stock, approvisionnement (ADMIN), historique des livraisons, péremptions.
 * Pour le PHARMACIEN, l'onglet Approvisionnement est masqué : l'écran est en lecture seule.
 */
public class StockPanel extends JPanel {

    private static final String STOCK_FAIBLE = "Stock faible";

    private final MedicamentDAO medicamentDAO = new MedicamentDAO();
    private final FournisseurDAO fournisseurDAO = new FournisseurDAO();
    private final ApprovisionnementDAO approvisionnementDAO = new ApprovisionnementDAO();

    // --- État du stock
    private final DefaultTableModel modeleEtat =
            EcranOutils.modeleLectureSeule("N°", "Médicament", "Catégorie", "Stock", "Seuil", "État");
    private final JCheckBox caseStockFaible = new JCheckBox("Stock faible uniquement");

    // --- Approvisionnement
    private final JComboBox<Fournisseur> comboFournisseur = new JComboBox<>();
    private final JTextField champDate = new JTextField(10);
    private final JTextField champReference = new JTextField(10);
    private final JComboBox<Medicament> comboMedicament = new JComboBox<>();
    private final JTextField champQuantite = new JTextField(5);
    private final JTextField champPrixAchat = new JTextField(7);
    private final JTextField champLot = new JTextField(8);
    private final JTextField champPeremption = new JTextField(10);
    private final DefaultTableModel modeleLignes = EcranOutils.modeleLectureSeule(
            "Médicament", "Quantité", "Prix d'achat", "Lot", "Péremption", "Sous-total");
    private final JTable tableLignes = new JTable(modeleLignes);
    private final List<LigneApprovisionnement> lignes = new ArrayList<>(); // même ordre que tableLignes
    private final JLabel labelTotal = new JLabel();

    // --- Historique des livraisons
    private final DefaultTableModel modeleLivraisons =
            EcranOutils.modeleLectureSeule("N°", "Date", "Fournisseur", "Bon", "Montant (DH)");
    private final JTable tableLivraisons = new JTable(modeleLivraisons);
    private List<Approvisionnement> livraisons = new ArrayList<>(); // même ordre que tableLivraisons
    private final DefaultTableModel modeleDetail = EcranOutils.modeleLectureSeule(
            "Médicament", "Quantité", "Prix d'achat", "Lot", "Péremption", "Sous-total");

    // --- Péremptions
    private final DefaultTableModel modelePeremption =
            EcranOutils.modeleLectureSeule("Médicament", "Lot", "Date de péremption", "Jours restants");

    public StockPanel() {
        super(new BorderLayout());
        JTabbedPane sousOnglets = new JTabbedPane();
        sousOnglets.addTab("État du stock", ongletEtat());
        if (Session.estAdmin()) { // RG03 : le pharmacien ne saisit pas de livraison
            sousOnglets.addTab("Approvisionnement", ongletApprovisionnement());
        }
        sousOnglets.addTab("Historique des livraisons", ongletHistorique());
        sousOnglets.addTab("Péremptions", ongletPeremptions());
        add(sousOnglets, BorderLayout.CENTER);

        // Rechargement chaque fois que l'onglet Stocks est affiché (une vente a pu changer le stock)
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                toutCharger();
            }
        });
        toutCharger();
    }

    private void toutCharger() {
        chargerEtat();
        chargerHistorique();
        chargerPeremptions();
        if (Session.estAdmin()) {
            chargerListesApprovisionnement();
        }
    }

    // ===================================================================== État du stock

    private JPanel ongletEtat() {
        JTable table = new JTable(modeleEtat);
        // RG24 : la colonne « État » est écrite en rouge quand le stock est faible
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object valeur, boolean choisi,
                                                           boolean focus, int ligne, int colonne) {
                Component c = super.getTableCellRendererComponent(t, valeur, choisi, focus, ligne, colonne);
                c.setForeground(STOCK_FAIBLE.equals(valeur) ? Color.RED
                        : (choisi ? t.getSelectionForeground() : t.getForeground()));
                return c;
            }
        });
        caseStockFaible.addActionListener(e -> chargerEtat());
        JButton boutonActualiser = new JButton("Actualiser");
        boutonActualiser.addActionListener(e -> chargerEtat());

        JPanel haut = new JPanel(new FlowLayout(FlowLayout.LEFT));
        haut.add(caseStockFaible);
        haut.add(boutonActualiser);
        JPanel p = new JPanel(new BorderLayout());
        p.add(haut, BorderLayout.NORTH);
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        return p;
    }

    private void chargerEtat() {
        try {
            List<Medicament> liste = caseStockFaible.isSelected()
                    ? medicamentDAO.listerStockFaible() : medicamentDAO.lister();
            modeleEtat.setRowCount(0);
            for (Medicament m : liste) {
                modeleEtat.addRow(new Object[] {m.getIdMedicament(), m.toString(), m.getLibelleCategorie(),
                        m.getQuantiteStock(), m.getSeuilMinimal(), m.estStockFaible() ? STOCK_FAIBLE : "OK"});
            }
        } catch (DAOException e) {
            EcranOutils.afficherErreur(this, e);
        }
    }

    // ===================================================================== Approvisionnement

    private JPanel ongletApprovisionnement() {
        champDate.setText(LocalDate.now().toString()); // date du jour par défaut

        JPanel entete = new JPanel(new FlowLayout(FlowLayout.LEFT));
        entete.setBorder(BorderFactory.createTitledBorder("Livraison"));
        entete.add(new JLabel("Fournisseur :"));
        entete.add(comboFournisseur);
        entete.add(new JLabel("Date (AAAA-MM-JJ) :"));
        entete.add(champDate);
        entete.add(new JLabel("Référence du bon :"));
        entete.add(champReference);

        JButton boutonAjouter = new JButton("Ajouter la ligne");
        boutonAjouter.addActionListener(e -> ajouterLigne());
        JPanel saisieLigne = new JPanel(new FlowLayout(FlowLayout.LEFT));
        saisieLigne.setBorder(BorderFactory.createTitledBorder("Ligne"));
        saisieLigne.add(new JLabel("Médicament :"));
        saisieLigne.add(comboMedicament);
        saisieLigne.add(new JLabel("Qté :"));
        saisieLigne.add(champQuantite);
        saisieLigne.add(new JLabel("Prix d'achat :"));
        saisieLigne.add(champPrixAchat);
        saisieLigne.add(new JLabel("N° de lot :"));
        saisieLigne.add(champLot);
        saisieLigne.add(new JLabel("Péremption (AAAA-MM-JJ) :"));
        saisieLigne.add(champPeremption);
        saisieLigne.add(boutonAjouter);

        JPanel haut = new JPanel(new BorderLayout());
        haut.add(entete, BorderLayout.NORTH);
        haut.add(saisieLigne, BorderLayout.CENTER);

        JButton boutonRetirer = new JButton("Retirer");
        boutonRetirer.addActionListener(e -> retirerLigne());
        JButton boutonEnregistrer = new JButton("Enregistrer");
        boutonEnregistrer.addActionListener(e -> enregistrerLivraison());
        JPanel bas = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        bas.add(boutonRetirer);
        bas.add(labelTotal);
        bas.add(boutonEnregistrer);

        tableLignes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JPanel p = new JPanel(new BorderLayout());
        p.add(haut, BorderLayout.NORTH);
        p.add(new JScrollPane(tableLignes), BorderLayout.CENTER);
        p.add(bas, BorderLayout.SOUTH);
        afficherTotal();
        return p;
    }

    private void chargerListesApprovisionnement() {
        try {
            comboFournisseur.removeAllItems();
            for (Fournisseur f : fournisseurDAO.lister()) {
                comboFournisseur.addItem(f);
            }
            comboMedicament.removeAllItems();
            for (Medicament m : medicamentDAO.lister()) {
                comboMedicament.addItem(m);
            }
        } catch (DAOException e) {
            EcranOutils.afficherErreur(this, e);
        }
    }

    /** Ajoute la ligne saisie au tableau. Les règles RG22 / RG25 sont vérifiées par le DAO à l'enregistrement. */
    private void ajouterLigne() {
        Medicament m = (Medicament) comboMedicament.getSelectedItem();
        if (m == null) {
            JOptionPane.showMessageDialog(this, "Choisissez un médicament.", "Attention", JOptionPane.WARNING_MESSAGE);
            return;
        }
        try {
            int quantite = Integer.parseInt(champQuantite.getText().trim());
            BigDecimal prix = new BigDecimal(champPrixAchat.getText().trim().replace(',', '.'));
            LocalDate peremption = LocalDate.parse(champPeremption.getText().trim());
            LigneApprovisionnement l = new LigneApprovisionnement(m.getIdMedicament(), m.toString(),
                    quantite, prix, champLot.getText().trim(), peremption);
            lignes.add(l);
            modeleLignes.addRow(new Object[] {l.getNomMedicament(), quantite, prix, l.getNumeroLot(),
                    peremption, l.getSousTotal()});
            champQuantite.setText("");
            champPrixAchat.setText("");
            champLot.setText("");
            champPeremption.setText("");
            afficherTotal();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Quantité et prix d'achat doivent être des nombres.",
                    "Attention", JOptionPane.WARNING_MESSAGE);
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Date de péremption invalide (format AAAA-MM-JJ).",
                    "Attention", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void retirerLigne() {
        int ligne = tableLignes.getSelectedRow();
        if (ligne < 0) {
            JOptionPane.showMessageDialog(this, "Sélectionnez une ligne à retirer.", "Attention",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        lignes.remove(ligne);
        modeleLignes.removeRow(ligne);
        afficherTotal();
    }

    /** Total recalculé à chaque changement de lignes, jamais saisi. */
    private void afficherTotal() {
        BigDecimal total = BigDecimal.ZERO;
        for (LigneApprovisionnement l : lignes) {
            total = total.add(l.getSousTotal());
        }
        labelTotal.setText("Total : " + total + " DH");
    }

    private void enregistrerLivraison() {
        Fournisseur f = (Fournisseur) comboFournisseur.getSelectedItem();
        LocalDate date;
        try {
            date = LocalDate.parse(champDate.getText().trim());
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Date de livraison invalide (format AAAA-MM-JJ).",
                    "Attention", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int choix = JOptionPane.showConfirmDialog(this, "Enregistrer cette livraison ? Le stock sera augmenté.",
                "Confirmation", JOptionPane.YES_NO_OPTION);
        if (choix != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            // RG21 à RG23, RG25 : vérifications + transaction (lignes et hausse du stock) dans le DAO
            int id = approvisionnementDAO.enregistrer(f == null ? 0 : f.getIdFournisseur(),
                    Session.getUtilisateur().getIdUtilisateur(), date, champReference.getText().trim(), lignes);
            JOptionPane.showMessageDialog(this, "Livraison n° " + id + " enregistrée.", "Approvisionnement",
                    JOptionPane.INFORMATION_MESSAGE);
            lignes.clear();
            modeleLignes.setRowCount(0);
            champReference.setText("");
            champDate.setText(LocalDate.now().toString());
            afficherTotal();
            toutCharger();
        } catch (RegleGestionException | DAOException e) {
            EcranOutils.afficherErreur(this, e);
        }
    }

    // ===================================================================== Historique

    private JSplitPane ongletHistorique() {
        tableLivraisons.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tableLivraisons.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                chargerDetail();
            }
        });
        JPanel detail = new JPanel(new BorderLayout());
        detail.setBorder(BorderFactory.createTitledBorder("Lignes de la livraison sélectionnée"));
        detail.add(new JScrollPane(new JTable(modeleDetail)), BorderLayout.CENTER);
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(tableLivraisons), detail);
        split.setResizeWeight(0.5);
        return split;
    }

    private void chargerHistorique() {
        try {
            livraisons = approvisionnementDAO.lister();
            modeleLivraisons.setRowCount(0); // vide aussi la sélection, donc le détail
            for (Approvisionnement a : livraisons) {
                modeleLivraisons.addRow(new Object[] {a.getIdApprovisionnement(), a.getDateApprovisionnement(),
                        a.getRaisonSociale(), a.getReferenceBon(), a.getMontantTotal()});
            }
        } catch (DAOException e) {
            EcranOutils.afficherErreur(this, e);
        }
    }

    private void chargerDetail() {
        modeleDetail.setRowCount(0);
        int ligne = tableLivraisons.getSelectedRow();
        if (ligne < 0) {
            return;
        }
        try {
            for (LigneApprovisionnement l : approvisionnementDAO.listerLignes(
                    livraisons.get(ligne).getIdApprovisionnement())) {
                modeleDetail.addRow(new Object[] {l.getNomMedicament(), l.getQuantiteRecue(),
                        l.getPrixAchatUnitaire(), l.getNumeroLot(), l.getDatePeremption(), l.getSousTotal()});
            }
        } catch (DAOException e) {
            EcranOutils.afficherErreur(this, e);
        }
    }

    // ===================================================================== Péremptions

    private JPanel ongletPeremptions() {
        JTable table = new JTable(modelePeremption);
        // RG26 : lot déjà périmé (jours restants < 0) affiché en rouge
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object valeur, boolean choisi,
                                                           boolean focus, int ligne, int colonne) {
                Component c = super.getTableCellRendererComponent(t, valeur, choisi, focus, ligne, colonne);
                boolean perime = (Integer) modelePeremption.getValueAt(ligne, 3) < 0;
                c.setForeground(perime ? Color.RED : (choisi ? t.getSelectionForeground() : t.getForeground()));
                return c;
            }
        });
        JPanel p = new JPanel(new BorderLayout());
        p.add(new JLabel("  Lots qui expirent dans 30 jours ou moins, médicament encore en stock (RG26)."),
                BorderLayout.NORTH);
        p.add(new JScrollPane(table), BorderLayout.CENTER);
        return p;
    }

    private void chargerPeremptions() {
        try {
            modelePeremption.setRowCount(0);
            for (AlertePeremption a : approvisionnementDAO.listerAlertesPeremption()) {
                modelePeremption.addRow(new Object[] {a.getNomMedicament(), a.getNumeroLot(),
                        a.getDatePeremption(), a.getJoursRestants()});
            }
        } catch (DAOException e) {
            EcranOutils.afficherErreur(this, e);
        }
    }
}
