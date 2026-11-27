package pharmacie.test;

import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SwingUtilities;

import pharmacie.config.DatabaseConnection;
import pharmacie.dao.ApprovisionnementDAO;
import pharmacie.dao.ClientDAO;
import pharmacie.dao.FournisseurDAO;
import pharmacie.dao.MedicamentDAO;
import pharmacie.dao.TableauDeBordDAO;
import pharmacie.dao.UtilisateurDAO;
import pharmacie.dao.VenteDAO;
import pharmacie.model.AlertePeremption;
import pharmacie.model.Client;
import pharmacie.model.IndicateursTableauDeBord;
import pharmacie.model.LigneApprovisionnement;
import pharmacie.model.LigneVente;
import pharmacie.model.Medicament;
import pharmacie.model.Role;
import pharmacie.model.Utilisateur;
import pharmacie.model.Vente;
import pharmacie.ui.LoginFrame;
import pharmacie.ui.MainFrame;
import pharmacie.util.DAOException;
import pharmacie.util.PasswordUtil;
import pharmacie.util.RegleGestionException;
import pharmacie.util.Session;

/**
 * Vérifications de la phase 2 sur la vraie base (données de démonstration fraîches).
 * Toute vente, livraison ou client créé ici est supprimé à la fin, et les stocks sont remis.
 * Lancement : java -cp "bin;lib/*" pharmacie.test.TestPhase2
 */
public class TestPhase2 {

    private static int reussis = 0;
    private static int echecs = 0;

    private static final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private static final MedicamentDAO medicamentDAO = new MedicamentDAO();
    private static final VenteDAO venteDAO = new VenteDAO();
    private static final ApprovisionnementDAO approDAO = new ApprovisionnementDAO();

    /** Une action qui peut lever n'importe quelle exception (pour tester les refus). */
    private interface Action {
        void executer() throws Exception;
    }

    public static void main(String[] args) {
        lancer("Tableau de bord et vues", TestPhase2::testerTableauDeBord);
        lancer("Authentification", TestPhase2::testerAuthentification);
        lancer("Onglets par rôle et déconnexion", TestPhase2::testerOnglets);
        lancer("Vente refusée (stock insuffisant)", TestPhase2::testerVenteRefusee);
        lancer("Vente valide", TestPhase2::testerVenteValide);
        lancer("Approvisionnement", TestPhase2::testerApprovisionnement);
        lancer("Autres règles de gestion", TestPhase2::testerAutresRegles);

        System.out.println();
        System.out.println("Résultat : " + reussis + " réussi(s), " + echecs + " échec(s).");
        System.exit(echecs == 0 ? 0 : 1); // exit nécessaire : les fenêtres Swing gardent la JVM ouverte
    }

    // ------------------------------------------------------------------ tests

    private static void testerTableauDeBord() throws Exception {
        IndicateursTableauDeBord t = new TableauDeBordDAO().lireIndicateurs();
        verifier("indicateurs = 8 / 3 / 3 / 4 / 1656.00",
                t.getNbMedicaments() == 8 && t.getNbClients() == 3 && t.getNbFournisseurs() == 3
                        && t.getNbVentes() == 4 && t.getChiffreAffaires().compareTo(new BigDecimal("1656.00")) == 0);

        List<String> faibles = new ArrayList<>();
        for (Medicament m : medicamentDAO.listerStockFaible()) {
            faibles.add(m.getNomMedicament());
        }
        verifier("stock faible = Amoxicilline, Oméprazole, Sirop contre la toux",
                faibles.equals(List.of("Amoxicilline", "Oméprazole", "Sirop contre la toux")));

        List<String> lots = new ArrayList<>();
        for (AlertePeremption a : approDAO.listerAlertesPeremption()) {
            lots.add(a.getNumeroLot());
        }
        verifier("péremption proche = VTC-A1, SIR-B1", lots.equals(List.of("VTC-A1", "SIR-B1")));
    }

    private static void testerAuthentification() throws Exception {
        verifier("SHA-256 de admin123 = empreinte en base (RG02)", PasswordUtil.hacher("admin123")
                .equals("240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9"));

        Utilisateur admin = utilisateurDAO.authentifier("admin", "admin123");
        verifier("admin se connecte avec le rôle ADMIN", admin.getRole() == Role.ADMIN);

        Utilisateur pharma = utilisateurDAO.authentifier("pharmacien", "pharma123");
        verifier("pharmacien se connecte avec le rôle PHARMACIEN", pharma.getRole() == Role.PHARMACIEN);

        verifierRefus("ancien est refusé : compte désactivé (RG04)", "désactivé",
                () -> utilisateurDAO.authentifier("ancien", "ancien123"));
        verifierRefus("mauvais mot de passe : message, pas d'erreur technique", "incorrect",
                () -> utilisateurDAO.authentifier("admin", "mauvais"));
        verifierRefus("login inconnu refusé", "incorrect",
                () -> utilisateurDAO.authentifier("personne", "x"));
        verifierRefus("login en double refusé (RG01)", "existe déjà",
                () -> utilisateurDAO.ajouter(new Utilisateur(0, "admin", "Doublon", Role.PHARMACIEN, true), "x"));
    }

    private static void testerOnglets() throws Exception {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("  [IGNORÉ] pas d'écran disponible");
            return;
        }
        Session.ouvrir(utilisateurDAO.authentifier("admin", "admin123"));
        verifier("admin voit les 7 pages", titresOnglets().equals(List.of("Tableau de bord", "Ventes",
                "Médicaments", "Stocks", "Clients", "Fournisseurs", "Utilisateurs")));

        Session.ouvrir(utilisateurDAO.authentifier("pharmacien", "pharma123"));
        verifier("pharmacien voit 4 pages", titresOnglets().equals(List.of("Ventes", "Médicaments",
                "Stocks", "Clients")));

        // Déconnexion : la session est vidée et l'écran de connexion réapparaît
        boolean[] loginAffiche = {false};
        SwingUtilities.invokeAndWait(() -> {
            MainFrame f = new MainFrame();
            f.deconnecter();
            for (Frame fenetre : Frame.getFrames()) {
                if (fenetre instanceof LoginFrame && fenetre.isVisible()) {
                    loginAffiche[0] = true;
                    fenetre.dispose();
                }
            }
        });
        verifier("Déconnexion vide la session et rouvre la connexion",
                Session.getUtilisateur() == null && loginAffiche[0]);
    }

    private static void testerVenteRefusee() throws Exception {
        Medicament para = medicamentDAO.trouverParId(1);
        Medicament sirop = medicamentDAO.trouverParId(7);
        int nbVentes = venteDAO.lister().size();

        // Ligne 1 valide, ligne 2 au-dessus du stock : la vente entière doit être refusée
        List<LigneVente> lignes = List.of(
                new LigneVente(para.getIdMedicament(), null, 1, null),
                new LigneVente(sirop.getIdMedicament(), null, sirop.getQuantiteStock() + 1, null));
        verifierRefus("vente au-dessus du stock refusée (RG17)", "Stock insuffisant",
                () -> venteDAO.enregistrer(null, 2, lignes));

        verifier("stocks inchangés après le refus",
                medicamentDAO.trouverParId(1).getQuantiteStock() == para.getQuantiteStock()
                        && medicamentDAO.trouverParId(7).getQuantiteStock() == sirop.getQuantiteStock());
        verifier("aucune vente créée", venteDAO.lister().size() == nbVentes);

        // Client inexistant : la clé étrangère échoue après le verrouillage -> rollback
        try {
            venteDAO.enregistrer(999999, 2, List.of(new LigneVente(1, null, 1, null)));
            verifier("vente avec client inexistant refusée", false);
        } catch (DAOException e) {
            verifier("vente avec client inexistant refusée (erreur technique)", true);
        }
        verifier("rollback : stock et ventes inchangés",
                medicamentDAO.trouverParId(1).getQuantiteStock() == para.getQuantiteStock()
                        && venteDAO.lister().size() == nbVentes);

        verifierRefus("vente sans ligne refusée (RG16)", "au moins une ligne",
                () -> venteDAO.enregistrer(null, 2, List.of()));
        verifierRefus("même médicament deux fois refusé (RG16)", "qu'une fois",
                () -> venteDAO.enregistrer(null, 2, List.of(new LigneVente(1, null, 1, null),
                        new LigneVente(1, null, 1, null))));
        verifierRefus("quantité 0 refusée (RG17)", "supérieure à 0",
                () -> venteDAO.enregistrer(null, 2, List.of(new LigneVente(1, null, 0, null))));
    }

    private static void testerVenteValide() throws Exception {
        Medicament para = medicamentDAO.trouverParId(1);
        Medicament ibu = medicamentDAO.trouverParId(2);
        Integer idVente = null;
        try {
            // Prix volontairement faux côté "interface" : le DAO doit les ignorer (RG18)
            idVente = venteDAO.enregistrer(null, 2, List.of(
                    new LigneVente(1, null, 2, new BigDecimal("0.01")),
                    new LigneVente(2, null, 1, new BigDecimal("999.00"))));

            verifier("stock Paracétamol diminué de 2 (RG20)",
                    medicamentDAO.trouverParId(1).getQuantiteStock() == para.getQuantiteStock() - 2);
            verifier("stock Ibuprofène diminué de 1 (RG20)",
                    medicamentDAO.trouverParId(2).getQuantiteStock() == ibu.getQuantiteStock() - 1);

            boolean prixDeLaBase = true;
            for (LigneVente l : venteDAO.listerLignes(idVente)) {
                BigDecimal attendu = l.getIdMedicament() == 1 ? para.getPrixVente() : ibu.getPrixVente();
                prixDeLaBase &= l.getPrixUnitaireVente().compareTo(attendu) == 0;
            }
            verifier("prix enregistrés = prix en base, pas ceux de l'interface (RG18)", prixDeLaBase);

            BigDecimal totalAttendu = para.getPrixVente().multiply(BigDecimal.valueOf(2)).add(ibu.getPrixVente());
            Vente v = trouverVente(idVente);
            verifier("total calculé = 2 × prix + 1 × prix (RG19)",
                    v != null && v.getMontantTotal().compareTo(totalAttendu) == 0);
            verifier("client de passage : id_client NULL (RG15)", v != null && v.getIdClient() == null);
            verifier("date et utilisateur enregistrés (RG14)",
                    v != null && v.getDateVente() != null && v.getIdUtilisateur() == 2);
        } finally {
            // Nettoyage : suppression de la vente (lignes en cascade) et remise des stocks
            if (idVente != null) {
                executerSql("DELETE FROM vente WHERE id_vente = ?", idVente);
            }
            executerSql("UPDATE medicament SET quantite_stock = ? WHERE id_medicament = ?", para.getQuantiteStock(), 1);
            executerSql("UPDATE medicament SET quantite_stock = ? WHERE id_medicament = ?", ibu.getQuantiteStock(), 2);
        }
        verifier("nettoyage : stocks remis à leur valeur initiale",
                medicamentDAO.trouverParId(1).getQuantiteStock() == para.getQuantiteStock()
                        && medicamentDAO.trouverParId(2).getQuantiteStock() == ibu.getQuantiteStock());
    }

    private static void testerApprovisionnement() throws Exception {
        Medicament amox = medicamentDAO.trouverParId(3);
        LocalDate aujourdhui = LocalDate.now();
        Integer idAppro = null;
        try {
            idAppro = approDAO.enregistrer(1, 1, aujourdhui, "TEST-PHASE2", List.of(
                    new LigneApprovisionnement(3, null, 5, new BigDecimal("30.00"), "TEST-LOT", aujourdhui.plusYears(1))));
            verifier("stock Amoxicilline augmenté de 5 (RG23)",
                    medicamentDAO.trouverParId(3).getQuantiteStock() == amox.getQuantiteStock() + 5);
            verifier("ligne enregistrée avec son lot (RG25)",
                    approDAO.listerLignes(idAppro).get(0).getNumeroLot().equals("TEST-LOT"));
        } finally {
            if (idAppro != null) {
                executerSql("DELETE FROM approvisionnement WHERE id_approvisionnement = ?", idAppro);
            }
            executerSql("UPDATE medicament SET quantite_stock = ? WHERE id_medicament = ?", amox.getQuantiteStock(), 3);
        }
        verifier("nettoyage : stock Amoxicilline remis",
                medicamentDAO.trouverParId(3).getQuantiteStock() == amox.getQuantiteStock());

        verifierRefus("péremption avant la livraison refusée (RG25)", "postérieure",
                () -> approDAO.enregistrer(1, 1, aujourdhui, null, List.of(
                        new LigneApprovisionnement(3, null, 5, new BigDecimal("30.00"), "L1", aujourdhui))));
        verifierRefus("prix d'achat 0 refusé (RG22)", "prix d'achat",
                () -> approDAO.enregistrer(1, 1, aujourdhui, null, List.of(
                        new LigneApprovisionnement(3, null, 5, BigDecimal.ZERO, "L1", aujourdhui.plusYears(1)))));
        verifierRefus("lot manquant refusé (RG25)", "numéro de lot",
                () -> approDAO.enregistrer(1, 1, aujourdhui, null, List.of(
                        new LigneApprovisionnement(3, null, 5, new BigDecimal("30.00"), " ", aujourdhui.plusYears(1)))));
    }

    private static void testerAutresRegles() throws Exception {
        verifierRefus("médicament déjà vendu non supprimable (RG09)", "ne peut pas être supprimé",
                () -> medicamentDAO.supprimer(1));
        verifierRefus("fournisseur ayant livré non supprimable (RG11)", "ne peut pas être supprimé",
                () -> new FournisseurDAO().supprimer(1));
        verifierRefus("prix de vente 0 refusé (RG06)", "supérieur à 0",
                () -> medicamentDAO.ajouter(new Medicament(0, "Test", null, null, BigDecimal.ZERO, 1, 1, 1, null)));

        // Le stock saisi est ignoré : un nouveau médicament démarre à 0 (RG23)
        int idNouveau = medicamentDAO.ajouter(
                new Medicament(0, "TestPhase2", null, null, new BigDecimal("10.00"), 50, 5, 1, null));
        boolean stockNul = medicamentDAO.trouverParId(idNouveau).getQuantiteStock() == 0;
        medicamentDAO.supprimer(idNouveau);
        verifier("nouveau médicament créé avec stock 0, puis supprimé (RG23)",
                stockNul && medicamentDAO.trouverParId(idNouveau) == null);

        verifier("recherche par partie du nom « para » (RG08)", medicamentDAO.rechercher("para").stream()
                .anyMatch(m -> m.getNomMedicament().equals("Paracétamol")));
        verifier("recherche par identifiant exact « 3 » (RG08)", medicamentDAO.rechercher("3").stream()
                .anyMatch(m -> m.getIdMedicament() == 3));

        ClientDAO clientDAO = new ClientDAO();
        verifierRefus("client sans prénom refusé (RG12)", "obligatoires",
                () -> clientDAO.ajouter(new Client(0, "Test", "", null, null)));
        int idClient = clientDAO.ajouter(new Client(0, "TestPhase2", "Client", null, null));
        clientDAO.supprimer(idClient);
        verifier("ajout puis suppression d'un client", clientDAO.rechercher("TestPhase2").isEmpty());
    }

    // ------------------------------------------------------------------ outils

    private static void lancer(String titre, Action test) {
        System.out.println("== " + titre);
        try {
            test.executer();
        } catch (Exception e) {
            echecs++;
            System.out.println("  [ÉCHEC] exception inattendue : " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    private static void verifier(String libelle, boolean condition) {
        if (condition) {
            reussis++;
            System.out.println("  [OK]    " + libelle);
        } else {
            echecs++;
            System.out.println("  [ÉCHEC] " + libelle);
        }
    }

    /** Réussit si l'action lève une RegleGestionException dont le message contient le texte attendu. */
    private static void verifierRefus(String libelle, String texteAttendu, Action action) {
        try {
            action.executer();
            verifier(libelle + " (aucun refus)", false);
        } catch (RegleGestionException e) {
            verifier(libelle + " -> « " + e.getMessage() + " »", e.getMessage().contains(texteAttendu));
        } catch (Exception e) {
            verifier(libelle + " (mauvaise exception : " + e.getClass().getSimpleName() + ")", false);
        }
    }

    private static List<String> titresOnglets() throws Exception {
        List<String> titres = new ArrayList<>();
        SwingUtilities.invokeAndWait(() -> {
            MainFrame f = new MainFrame();
            titres.addAll(f.getTitresPages());
            f.dispose();
        });
        return titres;
    }

    private static Vente trouverVente(int idVente) throws DAOException {
        for (Vente v : venteDAO.lister()) {
            if (v.getIdVente() == idVente) {
                return v;
            }
        }
        return null;
    }

    /** Nettoyage direct en SQL (réservé au test : l'application passe toujours par les DAO). */
    private static void executerSql(String sql, int... parametres) throws DAOException, SQLException {
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            for (int i = 0; i < parametres.length; i++) {
                ps.setInt(i + 1, parametres[i]);
            }
            ps.executeUpdate();
        }
    }
}
