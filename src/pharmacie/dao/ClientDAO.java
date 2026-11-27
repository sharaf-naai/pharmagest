package pharmacie.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import pharmacie.config.DatabaseConnection;
import pharmacie.model.Client;
import pharmacie.util.DAOException;
import pharmacie.util.RegleGestionException;

/** Accès à la table client (RG12, RG13). */
public class ClientDAO {

    private static final String SELECT_CLIENT =
            "SELECT id_client, nom_client, prenom_client, telephone_client, adresse_client FROM client ";

    public List<Client> lister() throws DAOException {
        return listerAvec(SELECT_CLIENT + "ORDER BY nom_client, prenom_client", null);
    }

    /** Recherche sur une partie du nom, du prénom ou du téléphone. */
    public List<Client> rechercher(String texte) throws DAOException {
        String t = "%" + (texte == null ? "" : texte.trim()) + "%";
        return listerAvec(SELECT_CLIENT + "WHERE nom_client LIKE ? OR prenom_client LIKE ? OR telephone_client LIKE ? "
                + "ORDER BY nom_client, prenom_client", t);
    }

    public int ajouter(Client c) throws DAOException, RegleGestionException {
        valider(c);
        String sql = "INSERT INTO client (nom_client, prenom_client, telephone_client, adresse_client) VALUES (?, ?, ?, ?)";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            remplir(ps, c);
            ps.executeUpdate();
            try (ResultSet cles = ps.getGeneratedKeys()) {
                cles.next();
                return cles.getInt(1);
            }
        } catch (SQLException e) {
            throw new DAOException("Ajout du client impossible : " + e.getMessage(), e);
        }
    }

    public void modifier(Client c) throws DAOException, RegleGestionException {
        valider(c);
        String sql = "UPDATE client SET nom_client = ?, prenom_client = ?, telephone_client = ?, adresse_client = ? "
                   + "WHERE id_client = ?";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            remplir(ps, c);
            ps.setInt(5, c.getIdClient());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("Modification du client impossible : " + e.getMessage(), e);
        }
    }

    /** RG13 : les ventes du client sont conservées (ON DELETE SET NULL = client de passage). */
    public void supprimer(int idClient) throws DAOException {
        String sql = "DELETE FROM client WHERE id_client = ?";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setInt(1, idClient);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("Suppression du client impossible : " + e.getMessage(), e);
        }
    }

    /** RG12 : nom et prénom obligatoires. */
    private void valider(Client c) throws RegleGestionException {
        if (c.getNomClient() == null || c.getNomClient().trim().isEmpty()
                || c.getPrenomClient() == null || c.getPrenomClient().trim().isEmpty()) {
            throw new RegleGestionException("Le nom et le prénom du client sont obligatoires."); // RG12
        }
    }

    private void remplir(PreparedStatement ps, Client c) throws SQLException {
        ps.setString(1, c.getNomClient().trim());
        ps.setString(2, c.getPrenomClient().trim());
        ps.setString(3, c.getTelephoneClient());
        ps.setString(4, c.getAdresseClient());
    }

    /** Exécute un SELECT de clients ; si texte != null, il remplit les 3 paramètres de recherche. */
    private List<Client> listerAvec(String sql, String texte) throws DAOException {
        List<Client> liste = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            if (texte != null) {
                ps.setString(1, texte);
                ps.setString(2, texte);
                ps.setString(3, texte);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    liste.add(new Client(rs.getInt("id_client"), rs.getString("nom_client"),
                            rs.getString("prenom_client"), rs.getString("telephone_client"),
                            rs.getString("adresse_client")));
                }
            }
            return liste;
        } catch (SQLException e) {
            throw new DAOException("Lecture des clients impossible : " + e.getMessage(), e);
        }
    }
}
