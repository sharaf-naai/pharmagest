package pharmacie.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pharmacie.config.DatabaseConnection;
import pharmacie.model.AlertePeremption;
import pharmacie.model.Approvisionnement;
import pharmacie.model.LigneApprovisionnement;
import pharmacie.util.DAOException;
import pharmacie.util.RegleGestionException;

/** Accès aux tables approvisionnement et ligne_approvisionnement (RG21 à RG26). */
public class ApprovisionnementDAO {

    /**
     * Enregistre une livraison complète dans UNE transaction :
     * insertion de l'approvisionnement, de ses lignes, et augmentation du stock (RG23).
     *
     * @return l'identifiant de l'approvisionnement créé
     */
    public int enregistrer(int idFournisseur, int idUtilisateur, LocalDate dateApprovisionnement,
                           String referenceBon, List<LigneApprovisionnement> lignes)
            throws DAOException, RegleGestionException {
        // Contrôles avant toute requête SQL
        if (idFournisseur <= 0) {
            throw new RegleGestionException("Choisissez un fournisseur."); // RG21
        }
        if (dateApprovisionnement == null) {
            throw new RegleGestionException("La date de l'approvisionnement est obligatoire.");
        }
        if (lignes == null || lignes.isEmpty()) {
            throw new RegleGestionException("Un approvisionnement doit contenir au moins une ligne."); // RG22
        }
        Set<Integer> dejaVus = new HashSet<>();
        for (LigneApprovisionnement l : lignes) {
            if (!dejaVus.add(l.getIdMedicament())) {
                throw new RegleGestionException("Un médicament ne peut figurer qu'une fois par livraison."); // RG22
            }
            if (l.getQuantiteRecue() <= 0) {
                throw new RegleGestionException("La quantité reçue doit être supérieure à 0."); // RG22
            }
            if (l.getPrixAchatUnitaire() == null || l.getPrixAchatUnitaire().compareTo(BigDecimal.ZERO) <= 0) {
                throw new RegleGestionException("Le prix d'achat doit être supérieur à 0."); // RG22
            }
            if (l.getNumeroLot() == null || l.getNumeroLot().trim().isEmpty() || l.getDatePeremption() == null) {
                throw new RegleGestionException("Chaque ligne doit avoir un numéro de lot et une date de péremption."); // RG25
            }
            if (!l.getDatePeremption().isAfter(dateApprovisionnement)) {
                throw new RegleGestionException("La date de péremption doit être postérieure à la date de livraison."); // RG25
            }
        }

        try (Connection cn = DatabaseConnection.getConnection()) {
            cn.setAutoCommit(false); // début de la transaction (RG23)
            try {
                // 1. Verrouiller chaque médicament (FOR UPDATE) et vérifier qu'il existe
                String sqlVerrou = "SELECT id_medicament FROM medicament WHERE id_medicament = ? FOR UPDATE";
                try (PreparedStatement ps = cn.prepareStatement(sqlVerrou)) {
                    for (LigneApprovisionnement l : lignes) {
                        ps.setInt(1, l.getIdMedicament());
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                throw new RegleGestionException("Médicament introuvable (id " + l.getIdMedicament() + ").");
                            }
                        }
                    }
                }

                // 2. Créer l'approvisionnement : un fournisseur et un utilisateur (RG21)
                int idAppro;
                String sqlAppro = "INSERT INTO approvisionnement (date_approvisionnement, reference_bon, id_fournisseur, "
                                + "id_utilisateur) VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = cn.prepareStatement(sqlAppro, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setObject(1, dateApprovisionnement);
                    ps.setString(2, referenceBon);
                    ps.setInt(3, idFournisseur);
                    ps.setInt(4, idUtilisateur);
                    ps.executeUpdate();
                    try (ResultSet cles = ps.getGeneratedKeys()) {
                        cles.next();
                        idAppro = cles.getInt(1);
                    }
                }

                // 3. Insérer les lignes et augmenter le stock (RG23)
                String sqlLigne = "INSERT INTO ligne_approvisionnement (id_approvisionnement, id_medicament, quantite_recue, "
                                + "prix_achat_unitaire, numero_lot, date_peremption) VALUES (?, ?, ?, ?, ?, ?)";
                String sqlStock = "UPDATE medicament SET quantite_stock = quantite_stock + ? WHERE id_medicament = ?";
                try (PreparedStatement psLigne = cn.prepareStatement(sqlLigne);
                     PreparedStatement psStock = cn.prepareStatement(sqlStock)) {
                    for (LigneApprovisionnement l : lignes) {
                        psLigne.setInt(1, idAppro);
                        psLigne.setInt(2, l.getIdMedicament());
                        psLigne.setInt(3, l.getQuantiteRecue());
                        psLigne.setBigDecimal(4, l.getPrixAchatUnitaire());
                        psLigne.setString(5, l.getNumeroLot().trim());
                        psLigne.setObject(6, l.getDatePeremption());
                        psLigne.executeUpdate();

                        psStock.setInt(1, l.getQuantiteRecue());
                        psStock.setInt(2, l.getIdMedicament());
                        psStock.executeUpdate();
                    }
                }

                cn.commit();
                return idAppro;
            } catch (SQLException | RegleGestionException e) {
                cn.rollback(); // une étape a échoué : on annule tout
                throw e;
            }
        } catch (SQLException e) {
            throw new DAOException("Enregistrement de l'approvisionnement impossible : " + e.getMessage(), e);
        }
    }

    /** Historique des livraisons, la plus récente d'abord (montant lu dans v_approvisionnement_total). */
    public List<Approvisionnement> lister() throws DAOException {
        String sql = "SELECT a.id_approvisionnement, a.date_approvisionnement, a.reference_bon, a.id_fournisseur, "
                   + "ap.id_utilisateur, f.raison_sociale, a.montant_approvisionnement "
                   + "FROM v_approvisionnement_total a "
                   + "JOIN approvisionnement ap ON ap.id_approvisionnement = a.id_approvisionnement "
                   + "JOIN fournisseur f ON f.id_fournisseur = a.id_fournisseur "
                   + "ORDER BY a.date_approvisionnement DESC, a.id_approvisionnement DESC";
        List<Approvisionnement> liste = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                liste.add(new Approvisionnement(rs.getInt("id_approvisionnement"),
                        rs.getObject("date_approvisionnement", LocalDate.class), rs.getString("reference_bon"),
                        rs.getInt("id_fournisseur"), rs.getInt("id_utilisateur"), rs.getString("raison_sociale"),
                        rs.getBigDecimal("montant_approvisionnement")));
            }
            return liste;
        } catch (SQLException e) {
            throw new DAOException("Lecture des approvisionnements impossible : " + e.getMessage(), e);
        }
    }

    /** Détail d'une livraison. */
    public List<LigneApprovisionnement> listerLignes(int idApprovisionnement) throws DAOException {
        String sql = "SELECT l.id_medicament, m.nom_medicament, l.quantite_recue, l.prix_achat_unitaire, "
                   + "l.numero_lot, l.date_peremption "
                   + "FROM ligne_approvisionnement l JOIN medicament m ON m.id_medicament = l.id_medicament "
                   + "WHERE l.id_approvisionnement = ? ORDER BY m.nom_medicament";
        List<LigneApprovisionnement> liste = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idApprovisionnement);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    liste.add(new LigneApprovisionnement(rs.getInt("id_medicament"), rs.getString("nom_medicament"),
                            rs.getInt("quantite_recue"), rs.getBigDecimal("prix_achat_unitaire"),
                            rs.getString("numero_lot"), rs.getObject("date_peremption", LocalDate.class)));
                }
            }
            return liste;
        } catch (SQLException e) {
            throw new DAOException("Lecture du détail de l'approvisionnement impossible : " + e.getMessage(), e);
        }
    }

    /** RG26 : lots qui périment dans 30 jours ou moins, médicament encore en stock (vue v_peremption_proche). */
    public List<AlertePeremption> listerAlertesPeremption() throws DAOException {
        String sql = "SELECT id_medicament, nom_medicament, numero_lot, date_peremption, jours_restants "
                   + "FROM v_peremption_proche ORDER BY jours_restants, nom_medicament";
        List<AlertePeremption> liste = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                liste.add(new AlertePeremption(rs.getInt("id_medicament"), rs.getString("nom_medicament"),
                        rs.getString("numero_lot"), rs.getObject("date_peremption", LocalDate.class),
                        rs.getInt("jours_restants")));
            }
            return liste;
        } catch (SQLException e) {
            throw new DAOException("Lecture des alertes de péremption impossible : " + e.getMessage(), e);
        }
    }
}
