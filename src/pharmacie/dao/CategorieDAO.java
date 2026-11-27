package pharmacie.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import pharmacie.config.DatabaseConnection;
import pharmacie.model.Categorie;
import pharmacie.util.DAOException;

/** Lecture des catégories (pour la liste déroulante des médicaments, RG07). */
public class CategorieDAO {

    public List<Categorie> lister() throws DAOException {
        String sql = "SELECT id_categorie, libelle_categorie FROM categorie ORDER BY libelle_categorie";
        List<Categorie> liste = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                liste.add(new Categorie(rs.getInt("id_categorie"), rs.getString("libelle_categorie")));
            }
            return liste;
        } catch (SQLException e) {
            throw new DAOException("Lecture des catégories impossible : " + e.getMessage(), e);
        }
    }
}
