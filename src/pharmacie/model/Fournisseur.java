package pharmacie.model;

/** Table fournisseur (RG10, RG11). */
public class Fournisseur {

    private int idFournisseur;
    private String raisonSociale;
    private String telephoneFournisseur;
    private String emailFournisseur;
    private String adresseFournisseur;
    private String villeFournisseur;

    public Fournisseur(int idFournisseur, String raisonSociale, String telephoneFournisseur,
                       String emailFournisseur, String adresseFournisseur, String villeFournisseur) {
        this.idFournisseur = idFournisseur;
        this.raisonSociale = raisonSociale;
        this.telephoneFournisseur = telephoneFournisseur;
        this.emailFournisseur = emailFournisseur;
        this.adresseFournisseur = adresseFournisseur;
        this.villeFournisseur = villeFournisseur;
    }

    public int getIdFournisseur() { return idFournisseur; }
    public void setIdFournisseur(int idFournisseur) { this.idFournisseur = idFournisseur; }
    public String getRaisonSociale() { return raisonSociale; }
    public void setRaisonSociale(String raisonSociale) { this.raisonSociale = raisonSociale; }
    public String getTelephoneFournisseur() { return telephoneFournisseur; }
    public void setTelephoneFournisseur(String telephoneFournisseur) { this.telephoneFournisseur = telephoneFournisseur; }
    public String getEmailFournisseur() { return emailFournisseur; }
    public void setEmailFournisseur(String emailFournisseur) { this.emailFournisseur = emailFournisseur; }
    public String getAdresseFournisseur() { return adresseFournisseur; }
    public void setAdresseFournisseur(String adresseFournisseur) { this.adresseFournisseur = adresseFournisseur; }
    public String getVilleFournisseur() { return villeFournisseur; }
    public void setVilleFournisseur(String villeFournisseur) { this.villeFournisseur = villeFournisseur; }

    @Override
    public String toString() {
        return raisonSociale;
    }
}
