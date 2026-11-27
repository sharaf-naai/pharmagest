package pharmacie.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import pharmacie.util.DAOException;

/**
 * Fournit les connexions JDBC à la base "pharmacie".
 * Les paramètres (url, user, password) sont lus dans db.properties à la racine du projet.
 */
public final class DatabaseConnection {

    private static final String FICHIER = "db.properties";

    // Paramètres chargés une seule fois, au premier appel
    private static Properties parametres;

    private DatabaseConnection() {
    }

    /** Ouvre une NOUVELLE connexion à chaque appel (à fermer par l'appelant, try-with-resources). */
    public static Connection getConnection() throws DAOException {
        Properties p = chargerParametres();
        try {
            return DriverManager.getConnection(p.getProperty("db.url"),
                    p.getProperty("db.user"), p.getProperty("db.password"));
        } catch (SQLException e) {
            throw new DAOException("Connexion à MySQL impossible : " + e.getMessage(), e);
        }
    }

    private static synchronized Properties chargerParametres() throws DAOException {
        if (parametres == null) {
            Properties p = new Properties();
            try (InputStream in = new FileInputStream(FICHIER)) {
                p.load(in);
            } catch (IOException e) {
                throw new DAOException("Fichier " + FICHIER + " introuvable ou illisible.", e);
            }
            if (p.getProperty("db.url") == null || p.getProperty("db.user") == null) {
                throw new DAOException("Fichier " + FICHIER + " incomplet (db.url, db.user, db.password).", null);
            }
            parametres = p;
        }
        return parametres;
    }
}
