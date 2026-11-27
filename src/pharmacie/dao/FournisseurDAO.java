package pharmacie.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import pharmacie.config.DatabaseConnection;
import pharmacie.model.Fournisseur;
import pharmacie.util.DAOException;
import pharmacie.util.RegleGestionException;

/** Accès à la table fournisseur (RG10, RG11). */
public class FournisseurDAO {

    // Code d'erreur MySQL : ligne encore référencée par une clé étrangère
    private static final int ERREUR_REFERENCE = 1451;

    private static final String SELECT_FOURNISSEUR =
            "SELECT id_fournisseur, raison_sociale, telephone_fournisseur, email_fournisseur, "
          + "adresse_fournisseur, ville_fournisseur FROM fournisseur ";

    public List<Fournisseur> lister() throws DAOException {
        return listerAvec(SELECT_FOURNISSEUR + "ORDER BY raison_sociale", null);
    }

    /** Recherche sur une partie de la raison sociale ou de la ville. */
    public List<Fournisseur> rechercher(String texte) throws DAOException {
        String t = "%" + (texte == null ? "" : texte.trim()) + "%";
        return listerAvec(SELECT_FOURNISSEUR + "WHERE raison_sociale LIKE ? OR ville_fournisseur LIKE ? "
                + "ORDER BY raison_sociale", t);
    }

    public int ajouter(Fournisseur f) throws DAOException, RegleGestionException {
        valider(f);
        String sql = "INSERT INTO fournisseur (raison_sociale, telephone_fournisseur, email_fournisseur, "
                   + "adresse_fournisseur, ville_fournisseur) VALUES (?, ?, ?, ?, ?)";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            remplir(ps, f);
            ps.executeUpdate();
            try (ResultSet cles = ps.getGeneratedKeys()) {
                cles.next();
                return cles.getInt(1);
            }
        } catch (SQLException e) {
            throw new DAOException("Ajout du fournisseur impossible : " + e.getMessage(), e);
        }
    }

    public void modifier(Fournisseur f) throws DAOException, RegleGestionException {
        valider(f);
        String sql = "UPDATE fournisseur SET raison_sociale = ?, telephone_fournisseur = ?, email_fournisseur = ?, "
                   + "adresse_fournisseur = ?, ville_fournisseur = ? WHERE id_fournisseur = ?";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            remplir(ps, f);
            ps.setInt(6, f.getIdFournisseur());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("Modification du fournisseur impossible : " + e.getMessage(), e);
        }
    }

    public void supprimer(int idFournisseur) throws DAOException, RegleGestionException {
        String sql = "DELETE FROM fournisseur WHERE id_fournisseur = ?";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idFournisseur);
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == ERREUR_REFERENCE) {
                // RG11 : un fournisseur qui a livré ne peut pas être supprimé
                throw new RegleGestionException("Ce fournisseur a des approvisionnements : il ne peut pas être supprimé.");
            }
            throw new DAOException("Suppression du fournisseur impossible : " + e.getMessage(), e);
        }
    }

    /** RG10 : raison sociale et téléphone obligatoires. */
    private void valider(Fournisseur f) throws RegleGestionException {
        if (f.getRaisonSociale() == null || f.getRaisonSociale().trim().isEmpty()
                || f.getTelephoneFournisseur() == null || f.getTelephoneFournisseur().trim().isEmpty()) {
            throw new RegleGestionException("La raison sociale et le téléphone du fournisseur sont obligatoires."); // RG10
        }
    }

    private void remplir(PreparedStatement ps, Fournisseur f) throws SQLException {
        ps.setString(1, f.getRaisonSociale().trim());
        ps.setString(2, f.getTelephoneFournisseur().trim());
        ps.setString(3, f.getEmailFournisseur());
        ps.setString(4, f.getAdresseFournisseur());
        ps.setString(5, f.getVilleFournisseur());
    }

    /** Exécute un SELECT de fournisseurs ; si texte != null, il remplit les 2 paramètres de recherche. */
    private List<Fournisseur> listerAvec(String sql, String texte) throws DAOException {
        List<Fournisseur> liste = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            if (texte != null) {
                ps.setString(1, texte);
                ps.setString(2, texte);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    liste.add(new Fournisseur(rs.getInt("id_fournisseur"), rs.getString("raison_sociale"),
                            rs.getString("telephone_fournisseur"), rs.getString("email_fournisseur"),
                            rs.getString("adresse_fournisseur"), rs.getString("ville_fournisseur")));
                }
            }
            return liste;
        } catch (SQLException e) {
            throw new DAOException("Lecture des fournisseurs impossible : " + e.getMessage(), e);
        }
    }
}
