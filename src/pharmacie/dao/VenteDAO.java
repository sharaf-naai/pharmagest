package pharmacie.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import pharmacie.config.DatabaseConnection;
import pharmacie.model.LigneVente;
import pharmacie.model.Vente;
import pharmacie.util.DAOException;
import pharmacie.util.RegleGestionException;

/** Accès aux tables vente et ligne_vente (RG14 à RG20). */
public class VenteDAO {

    /**
     * Enregistre une vente complète dans UNE transaction (RG20) :
     * insertion de la vente, de ses lignes, et diminution du stock.
     * Si une seule étape échoue, tout est annulé (rollback).
     *
     * @param idClient      null pour un client de passage (RG15)
     * @param idUtilisateur l'utilisateur connecté qui effectue la vente (RG14)
     * @param lignes        les lignes ; leur prix unitaire est ignoré et relu en base (RG18)
     * @return l'identifiant de la vente créée
     */
    public int enregistrer(Integer idClient, int idUtilisateur, List<LigneVente> lignes)
            throws DAOException, RegleGestionException {
        // Contrôles avant toute requête SQL
        if (lignes == null || lignes.isEmpty()) {
            throw new RegleGestionException("Une vente doit contenir au moins une ligne."); // RG16
        }
        Set<Integer> dejaVus = new HashSet<>();
        for (LigneVente l : lignes) {
            if (!dejaVus.add(l.getIdMedicament())) {
                throw new RegleGestionException("Un médicament ne peut figurer qu'une fois par vente."); // RG16
            }
            if (l.getQuantiteVendue() <= 0) {
                throw new RegleGestionException("La quantité vendue doit être supérieure à 0."); // RG17
            }
        }

        try (Connection cn = DatabaseConnection.getConnection()) {
            cn.setAutoCommit(false); // début de la transaction (RG20)
            try {
                // 1. Verrouiller chaque médicament (FOR UPDATE) puis relire son prix et son stock
                List<BigDecimal> prixEnBase = new ArrayList<>();
                String sqlLire = "SELECT nom_medicament, prix_vente, quantite_stock FROM medicament "
                               + "WHERE id_medicament = ? FOR UPDATE";
                try (PreparedStatement ps = cn.prepareStatement(sqlLire)) {
                    for (LigneVente l : lignes) {
                        ps.setInt(1, l.getIdMedicament());
                        try (ResultSet rs = ps.executeQuery()) {
                            if (!rs.next()) {
                                throw new RegleGestionException("Médicament introuvable (id " + l.getIdMedicament() + ").");
                            }
                            int stock = rs.getInt("quantite_stock");
                            if (l.getQuantiteVendue() > stock) { // RG17
                                throw new RegleGestionException("Stock insuffisant pour " + rs.getString("nom_medicament")
                                        + " : " + stock + " disponible(s), " + l.getQuantiteVendue() + " demandé(s).");
                            }
                            prixEnBase.add(rs.getBigDecimal("prix_vente")); // RG18 : prix lu en base
                        }
                    }
                }

                // 2. Créer la vente : date/heure actuelle + utilisateur (RG14), client facultatif (RG15)
                int idVente;
                String sqlVente = "INSERT INTO vente (date_vente, id_client, id_utilisateur) VALUES (?, ?, ?)";
                try (PreparedStatement ps = cn.prepareStatement(sqlVente, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setObject(1, LocalDateTime.now().withNano(0));
                    if (idClient == null) {
                        ps.setNull(2, Types.INTEGER);
                    } else {
                        ps.setInt(2, idClient);
                    }
                    ps.setInt(3, idUtilisateur);
                    ps.executeUpdate();
                    try (ResultSet cles = ps.getGeneratedKeys()) {
                        cles.next();
                        idVente = cles.getInt(1);
                    }
                }

                // 3. Insérer les lignes et diminuer le stock
                String sqlLigne = "INSERT INTO ligne_vente (id_vente, id_medicament, quantite_vendue, prix_unitaire_vente) "
                                + "VALUES (?, ?, ?, ?)";
                String sqlStock = "UPDATE medicament SET quantite_stock = quantite_stock - ? WHERE id_medicament = ?";
                try (PreparedStatement psLigne = cn.prepareStatement(sqlLigne);
                     PreparedStatement psStock = cn.prepareStatement(sqlStock)) {
                    for (int i = 0; i < lignes.size(); i++) {
                        LigneVente l = lignes.get(i);
                        psLigne.setInt(1, idVente);
                        psLigne.setInt(2, l.getIdMedicament());
                        psLigne.setInt(3, l.getQuantiteVendue());
                        psLigne.setBigDecimal(4, prixEnBase.get(i));
                        psLigne.executeUpdate();

                        psStock.setInt(1, l.getQuantiteVendue());
                        psStock.setInt(2, l.getIdMedicament());
                        psStock.executeUpdate();
                    }
                }

                cn.commit(); // tout s'est bien passé : on valide
                return idVente;
            } catch (SQLException | RegleGestionException e) {
                cn.rollback(); // une étape a échoué : on annule tout (RG20)
                throw e;
            }
        } catch (SQLException e) {
            throw new DAOException("Enregistrement de la vente impossible : " + e.getMessage(), e);
        }
    }

    /** Historique des ventes, la plus récente d'abord ; total lu dans v_vente_total (RG19). */
    public List<Vente> lister() throws DAOException {
        String sql = "SELECT t.id_vente, t.date_vente, t.id_client, t.id_utilisateur, t.montant_total, "
                   + "CONCAT(c.prenom_client, ' ', c.nom_client) AS nom_client, u.nom_complet "
                   + "FROM v_vente_total t "
                   + "LEFT JOIN client c ON c.id_client = t.id_client "
                   + "JOIN utilisateur u ON u.id_utilisateur = t.id_utilisateur "
                   + "ORDER BY t.date_vente DESC, t.id_vente DESC";
        List<Vente> liste = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                liste.add(new Vente(rs.getInt("id_vente"), rs.getObject("date_vente", LocalDateTime.class),
                        rs.getObject("id_client", Integer.class), rs.getInt("id_utilisateur"),
                        rs.getString("nom_client"), rs.getString("nom_complet"), rs.getBigDecimal("montant_total")));
            }
            return liste;
        } catch (SQLException e) {
            throw new DAOException("Lecture des ventes impossible : " + e.getMessage(), e);
        }
    }

    /** Détail d'une vente. */
    public List<LigneVente> listerLignes(int idVente) throws DAOException {
        String sql = "SELECT l.id_medicament, m.nom_medicament, l.quantite_vendue, l.prix_unitaire_vente "
                   + "FROM ligne_vente l JOIN medicament m ON m.id_medicament = l.id_medicament "
                   + "WHERE l.id_vente = ? ORDER BY m.nom_medicament";
        List<LigneVente> liste = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idVente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    liste.add(new LigneVente(rs.getInt("id_medicament"), rs.getString("nom_medicament"),
                            rs.getInt("quantite_vendue"), rs.getBigDecimal("prix_unitaire_vente")));
                }
            }
            return liste;
        } catch (SQLException e) {
            throw new DAOException("Lecture du détail de la vente impossible : " + e.getMessage(), e);
        }
    }
}
