package pharmacie.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Table approvisionnement (RG21) + montant lu dans v_approvisionnement_total. */
public class Approvisionnement {

    private int idApprovisionnement;
    private LocalDate dateApprovisionnement;
    private String referenceBon;
    private int idFournisseur;
    private int idUtilisateur;
    private String raisonSociale;     // affichage
    private BigDecimal montantTotal;  // calculé par la vue

    public Approvisionnement(int idApprovisionnement, LocalDate dateApprovisionnement, String referenceBon,
                             int idFournisseur, int idUtilisateur, String raisonSociale, BigDecimal montantTotal) {
        this.idApprovisionnement = idApprovisionnement;
        this.dateApprovisionnement = dateApprovisionnement;
        this.referenceBon = referenceBon;
        this.idFournisseur = idFournisseur;
        this.idUtilisateur = idUtilisateur;
        this.raisonSociale = raisonSociale;
        this.montantTotal = montantTotal;
    }

    public int getIdApprovisionnement() { return idApprovisionnement; }
    public LocalDate getDateApprovisionnement() { return dateApprovisionnement; }
    public String getReferenceBon() { return referenceBon; }
    public int getIdFournisseur() { return idFournisseur; }
    public int getIdUtilisateur() { return idUtilisateur; }
    public String getRaisonSociale() { return raisonSociale; }
    public BigDecimal getMontantTotal() { return montantTotal; }
}
