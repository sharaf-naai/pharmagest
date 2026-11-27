package pharmacie.ui.panels;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

import pharmacie.dao.ClientDAO;
import pharmacie.dao.MedicamentDAO;
import pharmacie.dao.VenteDAO;
import pharmacie.model.Client;
import pharmacie.model.LigneVente;
import pharmacie.model.Medicament;
import pharmacie.model.Vente;
import pharmacie.util.DAOException;
import pharmacie.util.RegleGestionException;
import pharmacie.util.Session;

/** Module « Ventes » : saisie d'une nouvelle vente et historique des ventes (RG14 à RG20). */
public class VentePanel extends JPanel {

    private static final String CLIENT_DE_PASSAGE = "Client de passage";
    private static final DateTimeFormatter FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final VenteDAO venteDAO = new VenteDAO();
    private final MedicamentDAO medicamentDAO = new MedicamentDAO();
    private final ClientDAO clientDAO = new ClientDAO();

    // --- Onglet « Nouvelle vente » ---
    private final JComboBox<Object> comboClient = new JComboBox<>(); // contient le texte « Client de passage » puis des Client
    private final JComboBox<Medicament> comboMedicament = new JComboBox<>();
    private final JTextField champQuantite = new JTextField("1", 5);
    private final List<LigneVente> panier = new ArrayList<>();
    private final DefaultTableModel modelePanier =
            modeleNonEditable("Médicament", "Prix unitaire", "Quantité", "Sous-total");
    private final JTable tablePanier = new JTable(modelePanier);
    private final JLabel labelTotal = new JLabel();

    // --- Onglet « Historique » ---
    private List<Vente> ventes = new ArrayList<>(); // même ordre que les lignes de tableVentes
    private final DefaultTableModel modeleVentes =
            modeleNonEditable("N°", "Date-heure", "Client", "Vendeur", "Total");
    private final JTable tableVentes = new JTable(modeleVentes);
    private final DefaultTableModel modeleLignes =
            modeleNonEditable("Médicament", "Quantité", "Prix unitaire", "Sous-total");
    private final JLabel labelTotalDetail = new JLabel(" ");

    public VentePanel() {
        super(new BorderLayout());
        JTabbedPane sousOnglets = new JTabbedPane();
        sousOnglets.addTab("Nouvelle vente", construireNouvelleVente());
        sousOnglets.addTab("Historique", construireHistorique());
        // Recharger l'historique chaque fois qu'on ouvre son onglet
        sousOnglets.addChangeListener(e -> {
            if (sousOnglets.getSelectedIndex() == 1) {
                chargerHistorique();
            }
        });
        add(sousOnglets, BorderLayout.CENTER);

        chargerClients();
        chargerMedicaments();
        rafraichirPanier();
    }

    // ===================== Nouvelle vente =====================

    private JPanel construireNouvelleVente() {
        // Afficher le nom, le prix et le stock disponible dans la liste des médicaments
        comboMedicament.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> liste, Object valeur, int index,
                                                          boolean selectionne, boolean focus) {
                super.getListCellRendererComponent(liste, valeur, index, selectionne, focus);
                if (valeur instanceof Medicament m) {
                    setText(m + " — " + enDH(m.getPrixVente()) + " — stock : " + m.getQuantiteStock());
                }
                return this;
            }
        });

        JPanel haut = new JPanel(new FlowLayout(FlowLayout.LEFT));
        haut.add(new JLabel("Client :"));
        haut.add(comboClient);
        haut.add(new JLabel("   Médicament :"));
        haut.add(comboMedicament);
        haut.add(new JLabel("Quantité :"));
        haut.add(champQuantite);
        JButton boutonAjouter = new JButton("Ajouter");
        haut.add(boutonAjouter);

        tablePanier.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        labelTotal.setFont(labelTotal.getFont().deriveFont(Font.BOLD, 22f));
        JButton boutonRetirer = new JButton("Retirer");
        JButton boutonValider = new JButton("Valider la vente");
        JButton boutonAnnuler = new JButton("Annuler");
        JPanel boutons = new JPanel(new FlowLayout(FlowLayout.LEFT));
        boutons.add(boutonRetirer);
        boutons.add(boutonValider);
        boutons.add(boutonAnnuler);
        JPanel bas = new JPanel(new BorderLayout());
        bas.add(boutons, BorderLayout.WEST);
        bas.add(labelTotal, BorderLayout.EAST);
        bas.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 10));

        // Événements
        boutonAjouter.addActionListener(e -> ajouterLigne());
        champQuantite.addActionListener(e -> ajouterLigne()); // touche Entrée dans le champ = Ajouter
        boutonRetirer.addActionListener(e -> retirerLigne());
        boutonValider.addActionListener(e -> validerVente());
        boutonAnnuler.addActionListener(e -> viderPanier());

        JPanel panneau = new JPanel(new BorderLayout());
        panneau.add(haut, BorderLayout.NORTH);
        panneau.add(new JScrollPane(tablePanier), BorderLayout.CENTER);
        panneau.add(bas, BorderLayout.SOUTH);
        return panneau;
    }

    private void chargerClients() {
        comboClient.removeAllItems();
        comboClient.addItem(CLIENT_DE_PASSAGE); // RG15 : zéro ou un client
        try {
            for (Client c : clientDAO.lister()) {
                comboClient.addItem(c);
            }
        } catch (DAOException e) {
            afficherErreurBase(e);
        }
    }

    private void chargerMedicaments() {
        comboMedicament.removeAllItems();
        try {
            for (Medicament m : medicamentDAO.lister()) {
                comboMedicament.addItem(m);
            }
        } catch (DAOException e) {
            afficherErreurBase(e);
        }
    }

    /** Ajoute le médicament choisi au panier, ou augmente sa quantité s'il y est déjà (RG16). */
    private void ajouterLigne() {
        Medicament m = (Medicament) comboMedicament.getSelectedItem();
        if (m == null) {
            avertir("Choisissez un médicament.");
            return;
        }
        int quantite;
        try {
            quantite = Integer.parseInt(champQuantite.getText().trim());
        } catch (NumberFormatException e) {
            avertir("La quantité doit être un nombre entier.");
            return;
        }
        if (quantite <= 0) { // RG17
            avertir("La quantité doit être supérieure à 0.");
            return;
        }
        LigneVente existante = trouverDansPanier(m.getIdMedicament());
        int totalDemande = quantite + (existante == null ? 0 : existante.getQuantiteVendue());
        if (totalDemande > m.getQuantiteStock()) { // RG17 : VenteDAO refait ce contrôle en base
            avertir("Stock insuffisant pour " + m + " : " + m.getQuantiteStock()
                    + " disponible(s), " + totalDemande + " demandé(s).");
            return;
        }
        if (existante != null) {
            existante.setQuantiteVendue(totalDemande); // RG16 : une seule ligne par médicament
        } else {
            // Le prix sert seulement à l'affichage : VenteDAO relit le vrai prix en base (RG18)
            panier.add(new LigneVente(m.getIdMedicament(), m.toString(), quantite, m.getPrixVente()));
        }
        champQuantite.setText("1");
        rafraichirPanier();
    }

    private LigneVente trouverDansPanier(int idMedicament) {
        for (LigneVente l : panier) {
            if (l.getIdMedicament() == idMedicament) {
                return l;
            }
        }
        return null;
    }

    private void retirerLigne() {
        int ligne = tablePanier.getSelectedRow();
        if (ligne < 0) {
            avertir("Sélectionnez une ligne du panier à retirer.");
            return;
        }
        panier.remove(ligne); // les lignes du tableau sont dans le même ordre que le panier
        rafraichirPanier();
    }

    private void viderPanier() {
        panier.clear();
        rafraichirPanier();
    }

    /** Réaffiche le panier et met à jour le total en direct. */
    private void rafraichirPanier() {
        modelePanier.setRowCount(0);
        for (LigneVente l : panier) {
            modelePanier.addRow(new Object[] {l.getNomMedicament(), enDH(l.getPrixUnitaireVente()),
                    l.getQuantiteVendue(), enDH(l.getSousTotal())});
        }
        labelTotal.setText("Total : " + enDH(calculerTotalPanier()));
    }

    /** RG19 : total = somme des quantités × prix unitaires, calculé et jamais saisi. */
    private BigDecimal calculerTotalPanier() {
        BigDecimal total = BigDecimal.ZERO;
        for (LigneVente l : panier) {
            total = total.add(l.getSousTotal());
        }
        return total;
    }

    private void validerVente() {
        if (panier.isEmpty()) { // RG16
            avertir("Le panier est vide : ajoutez au moins un médicament.");
            return;
        }
        int choix = JOptionPane.showConfirmDialog(this, "Confirmer la vente (" + labelTotal.getText() + ") ?",
                "Valider la vente", JOptionPane.YES_NO_OPTION);
        if (choix != JOptionPane.YES_OPTION) {
            return;
        }
        Object choixClient = comboClient.getSelectedItem();
        Integer idClient = (choixClient instanceof Client c) ? c.getIdClient() : null; // RG15 : null = client de passage
        try {
            // RG14 : vente faite par l'utilisateur connecté ; RG20 : transaction gérée par le DAO
            int idVente = venteDAO.enregistrer(idClient, Session.getUtilisateur().getIdUtilisateur(), panier);
            JOptionPane.showMessageDialog(this, "Vente n° " + idVente + " enregistrée, total "
                    + enDH(calculerTotalPanier()), "Vente enregistrée", JOptionPane.INFORMATION_MESSAGE);
            viderPanier();
            comboClient.setSelectedIndex(0);
            chargerMedicaments(); // les stocks ont changé
        } catch (RegleGestionException e) {
            avertir(e.getMessage()); // le panier est conservé
        } catch (DAOException e) {
            afficherErreurBase(e);
        }
    }

    // ===================== Historique =====================

    private JSplitPane construireHistorique() {
        tableVentes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Clic sur une vente : afficher son détail en dessous
        tableVentes.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                afficherDetail();
            }
        });

        labelTotalDetail.setFont(labelTotalDetail.getFont().deriveFont(Font.BOLD, 16f));
        labelTotalDetail.setHorizontalAlignment(SwingConstants.RIGHT);
        labelTotalDetail.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 10));
        JPanel detail = new JPanel(new BorderLayout());
        detail.setBorder(BorderFactory.createTitledBorder("Détail de la vente sélectionnée"));
        detail.add(new JScrollPane(new JTable(modeleLignes)), BorderLayout.CENTER);
        detail.add(labelTotalDetail, BorderLayout.SOUTH);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(tableVentes), detail);
        split.setResizeWeight(0.6);
        return split;
    }

    /** Liste des ventes, la plus récente d'abord (tri fait par VenteDAO.lister). */
    private void chargerHistorique() {
        modeleVentes.setRowCount(0); // vide aussi la sélection, donc le détail
        try {
            ventes = venteDAO.lister();
        } catch (DAOException e) {
            ventes = new ArrayList<>();
            afficherErreurBase(e);
        }
        for (Vente v : ventes) {
            String client = (v.getIdClient() == null) ? CLIENT_DE_PASSAGE : v.getNomClient();
            modeleVentes.addRow(new Object[] {v.getIdVente(), v.getDateVente().format(FORMAT_DATE),
                    client, v.getNomUtilisateur(), enDH(v.getMontantTotal())});
        }
    }

    private void afficherDetail() {
        modeleLignes.setRowCount(0);
        labelTotalDetail.setText(" ");
        int ligne = tableVentes.getSelectedRow();
        if (ligne < 0) {
            return;
        }
        Vente v = ventes.get(ligne);
        try {
            for (LigneVente l : venteDAO.listerLignes(v.getIdVente())) {
                modeleLignes.addRow(new Object[] {l.getNomMedicament(), l.getQuantiteVendue(),
                        enDH(l.getPrixUnitaireVente()), enDH(l.getSousTotal())});
            }
            labelTotalDetail.setText("Total de la vente n° " + v.getIdVente() + " : " + enDH(v.getMontantTotal()));
        } catch (DAOException e) {
            afficherErreurBase(e);
        }
    }

    // ===================== Outils =====================

    /** Modèle de tableau en lecture seule (on ne modifie pas les cellules à la main). */
    private static DefaultTableModel modeleNonEditable(String... colonnes) {
        return new DefaultTableModel(colonnes, 0) {
            @Override
            public boolean isCellEditable(int ligne, int colonne) {
                return false;
            }
        };
    }

    private static String enDH(BigDecimal montant) {
        return montant.setScale(2, RoundingMode.HALF_UP) + " DH";
    }

    private void avertir(String message) {
        JOptionPane.showMessageDialog(this, message, "Attention", JOptionPane.WARNING_MESSAGE);
    }

    private void afficherErreurBase(DAOException e) {
        JOptionPane.showMessageDialog(this, e.getMessage(), "Erreur de base de données", JOptionPane.ERROR_MESSAGE);
    }
}
