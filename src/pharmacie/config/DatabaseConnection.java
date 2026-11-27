package pharmacie.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import pharmacie.util.DAOException;

/**
 * Fournit les connexions JDBC à la base "pharmacie".
 * Les paramètres (url, user, password) sont lus dans db.properties, cherché :
 * 1. dans le dossier courant (lancement depuis VS Code ou la ligne de commande) ;
 * 2. à côté de PharmaGest.exe (version installée) ;
 * 3. à côté de pharmagest.jar ou dans le dossier au-dessus (lancement par runtime\bin\javaw.exe).
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
            throw new DAOException("Connexion à MySQL impossible : " + e.getMessage()
                    + "\nVérifiez que MySQL est démarré et que le mot de passe de " + FICHIER + " est correct.", e);
        }
    }

    private static synchronized Properties chargerParametres() throws DAOException {
        if (parametres == null) {
            Path fichier = trouverFichier();
            if (fichier == null) {
                throw new DAOException("Fichier " + FICHIER + " introuvable.\nCopiez " + FICHIER
                        + ".example en " + FICHIER + " (à côté de PharmaGest.exe) et indiquez votre mot de passe MySQL.",
                        null);
            }
            Properties p = new Properties();
            try (InputStream in = new FileInputStream(fichier.toFile())) {
                p.load(in);
            } catch (IOException e) {
                throw new DAOException("Fichier " + fichier + " illisible.", e);
            }
            if (p.getProperty("db.url") == null || p.getProperty("db.user") == null) {
                throw new DAOException("Fichier " + FICHIER + " incomplet (db.url, db.user, db.password).", null);
            }
            parametres = p;
        }
        return parametres;
    }

    /** Premier db.properties trouvé parmi les emplacements possibles, ou null. */
    private static Path trouverFichier() {
        List<Path> candidats = new ArrayList<>();
        candidats.add(Paths.get(FICHIER));
        // jpackage indique le chemin de PharmaGest.exe dans cette propriété
        String exe = System.getProperty("jpackage.app-path");
        if (exe != null) {
            Path dossier = Paths.get(exe).toAbsolutePath().getParent();
            if (dossier != null) {
                candidats.add(dossier.resolve(FICHIER));
            }
        }
        // Dossier du .jar (app\) et dossier parent (PharmaGest\)
        try {
            Path jar = Paths.get(DatabaseConnection.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            Path dossierJar = Files.isDirectory(jar) ? jar : jar.getParent();
            if (dossierJar != null) {
                candidats.add(dossierJar.resolve(FICHIER));
                if (dossierJar.getParent() != null) {
                    candidats.add(dossierJar.getParent().resolve(FICHIER));
                }
            }
        } catch (Exception e) {
            // emplacement du .jar inconnu : on garde les autres possibilités
        }
        for (Path c : candidats) {
            if (Files.isReadable(c)) {
                return c;
            }
        }
        return null;
    }
}
