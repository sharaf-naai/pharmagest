package pharmacie.model;

/** Table utilisateur. Le mot de passe n'est jamais gardé dans cet objet. */
public class Utilisateur {

    private int idUtilisateur;
    private String login;
    private String nomComplet;
    private Role role;
    private boolean actif;

    public Utilisateur(int idUtilisateur, String login, String nomComplet, Role role, boolean actif) {
        this.idUtilisateur = idUtilisateur;
        this.login = login;
        this.nomComplet = nomComplet;
        this.role = role;
        this.actif = actif;
    }

    public int getIdUtilisateur() { return idUtilisateur; }
    public void setIdUtilisateur(int idUtilisateur) { this.idUtilisateur = idUtilisateur; }
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getNomComplet() { return nomComplet; }
    public void setNomComplet(String nomComplet) { this.nomComplet = nomComplet; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public boolean isActif() { return actif; }
    public void setActif(boolean actif) { this.actif = actif; }

    @Override
    public String toString() {
        return nomComplet + " (" + login + ")";
    }
}
