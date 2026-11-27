package pharmacie.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Table vente (RG14, RG15) + champs lus dans v_vente_total pour l'historique. */
public class Vente {

    private int idVente;
    private LocalDateTime dateVente;
    private Integer idClient;        // null = client de passage (RG15)
    private int idUtilisateur;
    private String nomClient;        // affichage
    private String nomUtilisateur;   // affichage
    private BigDecimal montantTotal; // RG19 : calculé par la vue, jamais saisi

    public Vente(int idVente, LocalDateTime dateVente, Integer idClient, int idUtilisateur,
                 String nomClient, String nomUtilisateur, BigDecimal montantTotal) {
        this.idVente = idVente;
        this.dateVente = dateVente;
        this.idClient = idClient;
        this.idUtilisateur = idUtilisateur;
        this.nomClient = nomClient;
        this.nomUtilisateur = nomUtilisateur;
        this.montantTotal = montantTotal;
    }

    public int getIdVente() { return idVente; }
    public LocalDateTime getDateVente() { return dateVente; }
    public Integer getIdClient() { return idClient; }
    public int getIdUtilisateur() { return idUtilisateur; }
    public String getNomClient() { return nomClient; }
    public String getNomUtilisateur() { return nomUtilisateur; }
    public BigDecimal getMontantTotal() { return montantTotal; }
}
