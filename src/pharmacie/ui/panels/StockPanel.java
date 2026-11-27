package pharmacie.ui.panels;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTable;
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
import pharmacie.ui.OutilsEcran;
import pharmacie.ui.theme.Bouton;
import pharmacie.ui.theme.Carte;
import pharmacie.ui.theme.ChampTexte;
import pharmacie.ui.theme.Icone;
import pharmacie.ui.theme.OngletsSegmentes;
import pharmacie.ui.theme.Tableaux;
import pharmacie.ui.theme.Theme;
import pharmacie.util.DAOException;
import pharmacie.util.RegleGestionException;
import pharmacie.util.Session;

/**
 * Module « Stocks » : état du stock, approvisionnement (ADMIN), historique des livraisons, péremptions.
 * Pour le PHARMACIEN, l'onglet Approvisionnement est masqué : l'écran est en lecture seule.
 */
public class StockPanel extends JPanel {

    // Mots exacts des badges (couleurs données par Ton.pour)
    private static final String STOCK_FAIBLE = "Stock faible";
    private static final String EN_STOCK = "En stock";
    private static final String PERIME = "Périmé";

    private final MedicamentDAO medicamentDAO = new MedicamentDAO();
    private final FournisseurDAO fournisseurDAO = new FournisseurDAO();
    private final ApprovisionnementDAO approvisionnementDAO = new ApprovisionnementDAO();

    // --- État du stock
    private final DefaultTableModel modeleEtat =
            EcranOutils.modeleLectureSeule("N°", "Médicament", "Catégorie", "Stock", "Seuil", "État");
    private final JCheckBox caseStockFaible = new JCheckBox("Stock faible uniquement");

    // --- Approvisionnement
    private final JComboBox<Fournisseur> comboFournisseur = new JComboBox<>();
    private final ChampTexte champDate = champ(10);
    private final ChampTexte champReference = champ(10);
    private final JComboBox<Medicament> comboMedicament = new JComboBox<>();
    private final ChampTexte champQuantite = champ(5);
    private final ChampTexte champPrixAchat = champ(7);
    private final ChampTexte champLot = champ(8);
    private final ChampTexte champPeremption = champ(10);
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
        setOpaque(false); // le fond gris vient de MainFrame
        OngletsSegmentes sousOnglets = new OngletsSegmentes();
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
        Tableaux.styliser(table);
        Tableaux.largeur(table, 0, 50);
        Tableaux.largeur(table, 1, 220);
        Tableaux.colonneEnGras(table, 1);
        Tableaux.alignerADroite(table, 3);
        Tableaux.alignerADroite(table, 4);
        Tableaux.colonneBadge(table, 5); // RG24 : badge rouge « Stock faible », vert « En stock »
        Tableaux.largeur(table, 5, 130);

        caseStockFaible.setOpaque(false);
        caseStockFaible.setFont(Theme.police(13f));
        caseStockFaible.setForeground(Theme.TEXTE);
        caseStockFaible.addActionListener(e -> chargerEtat());
        Bouton boutonActualiser = new Bouton("Actualiser", Bouton.Style.SECONDAIRE, Icone.Type.ACTUALISER);
        boutonActualiser.addActionListener(e -> chargerEtat());

        JPanel haut = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        haut.setOpaque(false);
        haut.add(caseStockFaible);
        haut.add(boutonActualiser);
        Carte p = new Carte(new BorderLayout(0, 12));
        p.add(haut, BorderLayout.NORTH);
        p.add(Tableaux.defilement(table), BorderLayout.CENTER);
        return p;
    }

    private void chargerEtat() {
        try {
            List<Medicament> liste = caseStockFaible.isSelected()
                    ? medicamentDAO.listerStockFaible() : medicamentDAO.lister();
            modeleEtat.setRowCount(0);
            for (Medicament m : liste) {
                modeleEtat.addRow(new Object[] {m.getIdMedicament(), m.toString(), m.getLibelleCategorie(),
                        m.getQuantiteStock(), m.getSeuilMinimal(), m.estStockFaible() ? STOCK_FAIBLE : EN_STOCK});
            }
        } catch (DAOException e) {
            EcranOutils.afficherErreur(this, e);
        }
    }

    // ===================================================================== Approvisionnement

    private JPanel ongletApprovisionnement() {
        champDate.setText(LocalDate.now().toString()); // date du jour par défaut
        OutilsEcran.styliserListe(comboFournisseur);
        OutilsEcran.styliserListe(comboMedicament);

        JPanel entete = ligneDeChamps(
                new String[] {"Fournisseur", "Date (AAAA-MM-JJ)", "Référence du bon"},
                comboFournisseur, champDate, champReference);

        Bouton boutonAjouter = new Bouton("Ajouter la ligne", Bouton.Style.SECONDAIRE, Icone.Type.AJOUTER);
        boutonAjouter.addActionListener(e -> ajouterLigne());
        JPanel saisieLigne = ligneDeChamps(
                new String[] {"Médicament", "Qté", "Prix d'achat", "N° de lot", "Péremption (AAAA-MM-JJ)", null},
                comboMedicament, champQuantite, champPrixAchat, champLot, champPeremption, boutonAjouter);

        // Carte du haut : la livraison puis la ligne à ajouter
        JPanel blocs = new JPanel(new BorderLayout(0, 12));
        blocs.setOpaque(false);
        JPanel titreLivraison = new JPanel(new BorderLayout(0, 8));
        titreLivraison.setOpaque(false);
        titreLivraison.add(OutilsEcran.titreCarte("Livraison"), BorderLayout.NORTH);
        titreLivraison.add(entete, BorderLayout.CENTER);
        JPanel titreLigne = new JPanel(new BorderLayout(0, 8));
        titreLigne.setOpaque(false);
        titreLigne.add(OutilsEcran.titreCarte("Ligne"), BorderLayout.NORTH);
        titreLigne.add(saisieLigne, BorderLayout.CENTER);
        blocs.add(titreLivraison, BorderLayout.NORTH);
        blocs.add(titreLigne, BorderLayout.CENTER);
        Carte haut = new Carte();
        haut.add(blocs, BorderLayout.CENTER);

        Bouton boutonRetirer = new Bouton("Retirer", Bouton.Style.DANGER, Icone.Type.SUPPRIMER);
        boutonRetirer.addActionListener(e -> retirerLigne());
        Bouton boutonEnregistrer = new Bouton("Enregistrer", Bouton.Style.PRIMAIRE, Icone.Type.VALIDER);
        boutonEnregistrer.addActionListener(e -> enregistrerLivraison());

        // Total à droite : libellé gris + montant en gros
        JLabel libelleTotal = new JLabel("Total");
        libelleTotal.setFont(Theme.police(14f));
        libelleTotal.setForeground(Theme.TEXTE_SECONDAIRE);
        labelTotal.setFont(Theme.police(Font.BOLD, 22f));
        labelTotal.setForeground(Theme.TEXTE);
        JPanel droite = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        droite.setOpaque(false);
        droite.add(libelleTotal);
        droite.add(labelTotal);
        droite.add(boutonEnregistrer);
        JPanel bas = new JPanel(new BorderLayout());
        bas.setOpaque(false);
        JPanel gauche = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        gauche.setOpaque(false);
        gauche.add(boutonRetirer);
        bas.add(gauche, BorderLayout.WEST);
        bas.add(droite, BorderLayout.EAST);

        Tableaux.styliser(tableLignes);
        Tableaux.colonneEnGras(tableLignes, 0);
        Tableaux.largeur(tableLignes, 0, 200);
        Tableaux.alignerADroite(tableLignes, 1);
        Tableaux.alignerADroite(tableLignes, 2);
        Tableaux.alignerADroite(tableLignes, 5);
        Carte carteLignes = new Carte(new BorderLayout(0, 12));
        carteLignes.add(Tableaux.defilement(tableLignes), BorderLayout.CENTER);
        carteLignes.add(bas, BorderLayout.SOUTH);

        JPanel p = new JPanel(new BorderLayout(0, 16));
        p.setOpaque(false);
        p.add(haut, BorderLayout.NORTH);
        p.add(carteLignes, BorderLayout.CENTER);
        afficherTotal();
        return p;
    }

    /** Champ de saisie du thème avec une largeur en colonnes. */
    private static ChampTexte champ(int colonnes) {
        ChampTexte c = new ChampTexte();
        c.setColumns(colonnes);
        return c;
    }

    /** Une rangée de champs, chaque libellé au-dessus de son champ (libellé null = pas de libellé). */
    private static JPanel ligneDeChamps(String[] libelles, JComponent... champs) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.WEST;
        for (int i = 0; i < champs.length; i++) {
            c.gridx = i;
            c.gridy = 0;
            c.insets = new Insets(0, 0, 6, 12);
            if (libelles[i] != null) {
                p.add(OutilsEcran.etiquette(libelles[i]), c);
            }
            c.gridy = 1;
            c.insets = new Insets(0, 0, 0, 12);
            p.add(champs[i], c);
        }
        // Espace libre à droite : les champs restent collés à gauche
        c.gridx = champs.length;
        c.weightx = 1;
        p.add(Box.createHorizontalGlue(), c);
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
        labelTotal.setText(total + " DH"); // le libellé « Total » est à côté
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
        Tableaux.styliser(tableLivraisons);
        Tableaux.largeur(tableLivraisons, 0, 50);
        Tableaux.colonneEnGras(tableLivraisons, 2);
        Tableaux.alignerADroite(tableLivraisons, 4);
        tableLivraisons.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                chargerDetail();
            }
        });

        JTable tableDetail = new JTable(modeleDetail);
        Tableaux.styliser(tableDetail);
        Tableaux.colonneEnGras(tableDetail, 0);
        Tableaux.largeur(tableDetail, 0, 200);
        Tableaux.alignerADroite(tableDetail, 1);
        Tableaux.alignerADroite(tableDetail, 2);
        Tableaux.alignerADroite(tableDetail, 5);
        Carte detail = new Carte(new BorderLayout(0, 12));
        detail.add(OutilsEcran.titreCarte("Lignes de la livraison sélectionnée"), BorderLayout.NORTH);
        detail.add(Tableaux.defilement(tableDetail), BorderLayout.CENTER);

        Carte liste = new Carte();
        liste.add(Tableaux.defilement(tableLivraisons), BorderLayout.CENTER);

        // Séparateur invisible de 16 px entre les deux cartes
        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, liste, detail);
        split.setResizeWeight(0.5);
        split.setOpaque(false);
        split.setBorder(null);
        split.setDividerSize(16);
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
        Tableaux.styliser(table);
        Tableaux.colonneEnGras(table, 0);
        Tableaux.largeur(table, 0, 220);
        Tableaux.colonneBadge(table, 3); // RG26 : « Périmé » en rouge, « N jours » en orange
        Tableaux.largeur(table, 3, 130);

        JLabel aide = new JLabel("Lots qui expirent dans 30 jours ou moins, médicament encore en stock (RG26).");
        aide.setFont(Theme.police(13f));
        aide.setForeground(Theme.TEXTE_SECONDAIRE);
        Carte p = new Carte(new BorderLayout(0, 12));
        p.add(aide, BorderLayout.NORTH);
        p.add(Tableaux.defilement(table), BorderLayout.CENTER);
        return p;
    }

    private void chargerPeremptions() {
        try {
            modelePeremption.setRowCount(0);
            for (AlertePeremption a : approvisionnementDAO.listerAlertesPeremption()) {
                // Lot déjà périmé (jours restants < 0) : badge « Périmé »
                String jours = a.getJoursRestants() < 0 ? PERIME : a.getJoursRestants() + " jours";
                modelePeremption.addRow(new Object[] {a.getNomMedicament(), a.getNumeroLot(),
                        a.getDatePeremption(), jours});
            }
        } catch (DAOException e) {
            EcranOutils.afficherErreur(this, e);
        }
    }
}
