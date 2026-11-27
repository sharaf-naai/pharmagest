package pharmacie.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import pharmacie.config.DatabaseConnection;
import pharmacie.model.Role;
import pharmacie.model.Utilisateur;
import pharmacie.util.DAOException;
import pharmacie.util.PasswordUtil;
import pharmacie.util.RegleGestionException;

/** Accès à la table utilisateur (RG01 à RG04). */
public class UtilisateurDAO {

    // Code d'erreur MySQL : valeur en double sur une contrainte UNIQUE
    private static final int ERREUR_DOUBLON = 1062;

    /**
     * Vérifie login + mot de passe et retourne l'utilisateur connecté.
     * Lève RegleGestionException si les identifiants sont faux ou le compte désactivé.
     */
    public Utilisateur authentifier(String login, String motDePasse) throws DAOException, RegleGestionException {
        if (estVide(login) || motDePasse == null || motDePasse.isEmpty()) {
            throw new RegleGestionException("Veuillez saisir le login et le mot de passe.");
        }
        String sql = "SELECT id_utilisateur, login, mot_de_passe, nom_complet, role, actif "
                   + "FROM utilisateur WHERE login = ?";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, login.trim());
            try (ResultSet rs = ps.executeQuery()) {
                // RG02 : on compare les empreintes SHA-256, jamais le mot de passe en clair
                if (!rs.next() || !rs.getString("mot_de_passe").equals(PasswordUtil.hacher(motDePasse))) {
                    throw new RegleGestionException("Login ou mot de passe incorrect.");
                }
                Utilisateur u = lire(rs);
                if (!u.isActif()) {
                    throw new RegleGestionException("Ce compte est désactivé. Contactez l'administrateur."); // RG04
                }
                return u;
            }
        } catch (SQLException e) {
            throw new DAOException("Authentification impossible : " + e.getMessage(), e);
        }
    }

    public List<Utilisateur> lister() throws DAOException {
        String sql = "SELECT id_utilisateur, login, nom_complet, role, actif FROM utilisateur ORDER BY login";
        List<Utilisateur> liste = new ArrayList<>();
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                liste.add(lire(rs));
            }
            return liste;
        } catch (SQLException e) {
            throw new DAOException("Lecture des utilisateurs impossible : " + e.getMessage(), e);
        }
    }

    /** Ajoute un utilisateur ; le mot de passe est haché avant l'insertion (RG02). Retourne l'id créé. */
    public int ajouter(Utilisateur u, String motDePasse) throws DAOException, RegleGestionException {
        valider(u);
        if (motDePasse == null || motDePasse.isEmpty()) {
            throw new RegleGestionException("Le mot de passe est obligatoire.");
        }
        String sql = "INSERT INTO utilisateur (login, mot_de_passe, nom_complet, role, actif) VALUES (?, ?, ?, ?, ?)";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getLogin().trim());
            ps.setString(2, PasswordUtil.hacher(motDePasse));
            ps.setString(3, u.getNomComplet().trim());
            ps.setString(4, u.getRole().name());
            ps.setBoolean(5, u.isActif());
            ps.executeUpdate();
            try (ResultSet cles = ps.getGeneratedKeys()) {
                cles.next();
                return cles.getInt(1);
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == ERREUR_DOUBLON) {
                throw new RegleGestionException("Ce login existe déjà. Choisissez-en un autre."); // RG01
            }
            throw new DAOException("Enregistrement de l'utilisateur impossible : " + e.getMessage(), e);
        }
    }

    /** Modifie login, nom, rôle et état actif (pas le mot de passe). */
    public void modifier(Utilisateur u) throws DAOException, RegleGestionException {
        valider(u);
        String sql = "UPDATE utilisateur SET login = ?, nom_complet = ?, role = ?, actif = ? WHERE id_utilisateur = ?";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, u.getLogin().trim());
            ps.setString(2, u.getNomComplet().trim());
            ps.setString(3, u.getRole().name());
            ps.setBoolean(4, u.isActif());
            ps.setInt(5, u.getIdUtilisateur());
            ps.executeUpdate();
        } catch (SQLException e) {
            if (e.getErrorCode() == ERREUR_DOUBLON) {
                throw new RegleGestionException("Ce login existe déjà. Choisissez-en un autre."); // RG01
            }
            throw new DAOException("Enregistrement de l'utilisateur impossible : " + e.getMessage(), e);
        }
    }

    public void changerMotDePasse(int idUtilisateur, String nouveauMotDePasse) throws DAOException, RegleGestionException {
        if (nouveauMotDePasse == null || nouveauMotDePasse.isEmpty()) {
            throw new RegleGestionException("Le mot de passe est obligatoire.");
        }
        String sql = "UPDATE utilisateur SET mot_de_passe = ? WHERE id_utilisateur = ?";
        try (Connection cn = DatabaseConnection.getConnection();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, PasswordUtil.hacher(nouveauMotDePasse)); // RG02
            ps.setInt(2, idUtilisateur);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DAOException("Changement de mot de passe impossible : " + e.getMessage(), e);
        }
    }

    private void valider(Utilisateur u) throws RegleGestionException {
        if (estVide(u.getLogin()) || estVide(u.getNomComplet())) {
            throw new RegleGestionException("Le login et le nom complet sont obligatoires.");
        }
        if (u.getRole() == null) {
            throw new RegleGestionException("Le rôle est obligatoire (ADMIN ou PHARMACIEN)."); // RG03
        }
    }

    private static Utilisateur lire(ResultSet rs) throws SQLException {
        return new Utilisateur(rs.getInt("id_utilisateur"), rs.getString("login"), rs.getString("nom_complet"),
                Role.valueOf(rs.getString("role")), rs.getBoolean("actif"));
    }

    private static boolean estVide(String s) {
        return s == null || s.trim().isEmpty();
    }
}
