package pharmacie.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import pharmacie.config.DatabaseConnection;
import pharmacie.model.IndicateursTableauDeBord;
import pharmacie.util.DAOException;

/** Lecture des indicateurs (vue v_tableau_de_bord). RG27 ; affiché aux ADMIN seulement (RG28). */
public class TableauDeBordDAO {

    public IndicateursTableauDeBord lireIndicateurs() throws DAOException {
        String sql = "SELECT nb_medicaments, nb_clients, nb_fournisseurs, nb_ventes, chiffre_affaires FROM v_tableau_de_bord";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next(); // la vue renvoie toujours exactement une ligne
            return new IndicateursTableauDeBord(rs.getInt("nb_medicaments"), rs.getInt("nb_clients"),
                    rs.getInt("nb_fournisseurs"), rs.getInt("nb_ventes"), rs.getBigDecimal("chiffre_affaires"));
        } catch (SQLException e) {
            throw new DAOException("Lecture du tableau de bord impossible : " + e.getMessage(), e);
        }
    }
}
