package pharmacie.model;

/** Table client (RG12, RG13). */
public class Client {

    private int idClient;
    private String nomClient;
    private String prenomClient;
    private String telephoneClient;
    private String adresseClient;

    public Client(int idClient, String nomClient, String prenomClient,
                  String telephoneClient, String adresseClient) {
        this.idClient = idClient;
        this.nomClient = nomClient;
        this.prenomClient = prenomClient;
        this.telephoneClient = telephoneClient;
        this.adresseClient = adresseClient;
    }

    public int getIdClient() { return idClient; }
    public void setIdClient(int idClient) { this.idClient = idClient; }
    public String getNomClient() { return nomClient; }
    public void setNomClient(String nomClient) { this.nomClient = nomClient; }
    public String getPrenomClient() { return prenomClient; }
    public void setPrenomClient(String prenomClient) { this.prenomClient = prenomClient; }
    public String getTelephoneClient() { return telephoneClient; }
    public void setTelephoneClient(String telephoneClient) { this.telephoneClient = telephoneClient; }
    public String getAdresseClient() { return adresseClient; }
    public void setAdresseClient(String adresseClient) { this.adresseClient = adresseClient; }

    @Override
    public String toString() {
        return prenomClient + " " + nomClient;
    }
}
