package pharmacie.dao;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import pharmacie.config.DatabaseConnection;
import pharmacie.model.Medicament;
import pharmacie.util.DAOException;
import pharmacie.util.RegleGestionException;

/** Accès à la table medicament (RG05 à RG09, RG24). */
public class MedicamentDAO {

    // Code d'erreur MySQL : ligne encore référencée par une clé étrangère
    private static final int ERREUR_REFERENCE = 1451;

    private static final String SELECT_MEDICAMENT =
            "SELECT m.id_medicament, m.nom_medicament, m.forme, m.dosage, m.prix_vente, m.quantite_stock, "
          + "m.seuil_minimal, m.id_categorie, c.libelle_categorie "
          + "FROM medicament m JOIN categorie c ON c.id_categorie = m.id_categorie ";

    public List<Medicament> lister() throws DAOException {
        return listerAvec(SELECT_MEDICAMENT + "ORDER BY m.nom_medicament", null, null);
    }

    /** RG08 : recherche par identifiant exact OU par une partie du nom. */
    public List<Medicament> rechercher(String texte) throws DAOException {
        String t = texte == null ? "" : texte.trim();
        int id;
        try {
            id = Integer.parseInt(t);
        } catch (NumberFormatException e) {
            id = -1; // le texte n'est pas un nombre : aucun identifiant ne correspondra
        }
        return listerAvec(SELECT_MEDICAMENT + "WHERE m.id_medicament = ? OR m.nom_medicament LIKE ? "
                + "ORDER BY m.nom_medicament", id, "%" + t + "%");
    }

    /** RG24 : médicaments dont le stock est sous le seuil (vue v_stock_faible). */
    public List<Medicament> listerStockFaible() throws DAOException {
        return listerAvec(SELECT_MEDICAMENT
                + "WHERE m.id_medicament IN (SELECT id_medicament FROM v_stock_faible) ORDER BY m.nom_medicament",
                null, null);
    }

    /** Retourne le médicament, ou null s'il n'existe pas. */
    public Medicament trouverParId(int idMedicament) throws DAOException {
        List<Medicament> liste = listerAvec(SELECT_MEDICAMENT + "WHERE m.id_medicament = ?", idMedicament, null);
        return liste.isEmpty() ? null : liste.get(0);
    }

    /**
     * Ajoute un médicament et retourne son identifiant.
     * Le stock initial vaut toujours 0 : il n'augmente que par un approvisionnement (RG23).
     */
    public int ajouter(Medicament m) throws DAOException, RegleGestionException {
        valider(m);
        String sql = "INSERT INTO medicament (nom_medicament, forme, dosage, prix_vente, quantite_stock, "
                   + "seuil_minimal, id_categorie) VALUES (?, ?, ?, ?, 0, ?, ?)"; // RG23 : stock initial = 0
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, m.getNomMedicament().trim());
            ps.setString(2, m.getForme());
            ps.setString(3, m.getDosage());
            ps.setBigDecimal(4, m.getPrixVente());
            ps.setInt(5, m.getSeuilMinimal());
            ps.setInt(6, m.getIdCategorie());
            ps.executeUpdate();
            try (ResultSet cles = ps.getGeneratedKeys()) {
                cles.next();
                return cles.getInt(1);
            }
        } catch (SQLException e) {
            throw new DAOException("Ajout du médicament impossible : " + e.getMessage(), e);
        }
    }

    /**
     * Modifie la fiche du médicament. La quantité en stock n'est PAS modifiée ici :
     * elle évolue uniquement par les ventes (RG20) et les approvisionnements (RG23).
     */
    public void modifier(Medicament m) throws DAOException, RegleGestionException {
        valider(m);
        String sql = "UPDATE medicament SET nom_medicament = ?, forme = ?, dosage = ?, prix_vente = ?, "
                   + "seuil_minimal = ?, id_categorie = ? WHERE id_medicament = ?";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, m.getNomMedicament().trim());
            ps.setString(2, m.getForme());
            ps.setString(3, m.getDosage());
            ps.setBigDecimal(4, m.getPrixVente());
            ps.setInt(5, m.getSeuilMinimal());
            ps.setInt(6, m.getIdCategorie());
            ps.setInt(7, m.getIdMedicament());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("Modification du médicament impossible : " + e.getMessage(), e);
        }
    }

    public void supprimer(int idMedicament) throws DAOException, RegleGestionException {
        String sql = "DELETE FROM medicament WHERE id_medicament = ?";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idMedicament);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == ERREUR_REFERENCE) {
                // RG09 : la clé étrangère de ligne_vente / ligne_approvisionnement refuse la suppression
                throw new RegleGestionException("Ce médicament figure dans une vente ou un approvisionnement : "
                        + "il ne peut pas être supprimé.");
            }
            throw new DAOException("Suppression du médicament impossible : " + e.getMessage(), e);
        }
    }

    /** RG05, RG06, RG07 : contrôles avant toute requête SQL. */
    private void valider(Medicament m) throws RegleGestionException {
        if (m.getNomMedicament() == null || m.getNomMedicament().trim().isEmpty()) {
            throw new RegleGestionException("Le nom du médicament est obligatoire."); // RG05
        }
        if (m.getPrixVente() == null || m.getPrixVente().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RegleGestionException("Le prix de vente doit être supérieur à 0."); // RG06
        }
        if (m.getQuantiteStock() < 0 || m.getSeuilMinimal() < 0) {
            throw new RegleGestionException("Le stock et le seuil minimal doivent être positifs ou nuls."); // RG06
        }
        if (m.getIdCategorie() <= 0) {
            throw new RegleGestionException("Choisissez une catégorie."); // RG07
        }
    }

    /** Exécute un SELECT de médicaments avec au plus un paramètre entier et un paramètre texte. */
    private List<Medicament> listerAvec(String sql, Integer paramEntier, String paramTexte) throws DAOException {
        List<Medicament> liste = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            int i = 1;
            if (paramEntier != null) {
                ps.setInt(i++, paramEntier);
            }
            if (paramTexte != null) {
                ps.setString(i, paramTexte);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    liste.add(new Medicament(rs.getInt("id_medicament"), rs.getString("nom_medicament"),
                            rs.getString("forme"), rs.getString("dosage"), rs.getBigDecimal("prix_vente"),
                            rs.getInt("quantite_stock"), rs.getInt("seuil_minimal"),
                            rs.getInt("id_categorie"), rs.getString("libelle_categorie")));
                }
            }
            return liste;
        } catch (SQLException e) {
            throw new DAOException("Lecture des médicaments impossible : " + e.getMessage(), e);
        }
    }
}
