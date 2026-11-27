package pharmacie.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Table ligne_approvisionnement (RG22, RG25). */
public class LigneApprovisionnement {

    private int idMedicament;
    private String nomMedicament;  // affichage
    private int quantiteRecue;
    private BigDecimal prixAchatUnitaire;
    private String numeroLot;
    private LocalDate datePeremption;

    public LigneApprovisionnement(int idMedicament, String nomMedicament, int quantiteRecue,
                                  BigDecimal prixAchatUnitaire, String numeroLot, LocalDate datePeremption) {
        this.idMedicament = idMedicament;
        this.nomMedicament = nomMedicament;
        this.quantiteRecue = quantiteRecue;
        this.prixAchatUnitaire = prixAchatUnitaire;
        this.numeroLot = numeroLot;
        this.datePeremption = datePeremption;
    }

    public BigDecimal getSousTotal() {
        return prixAchatUnitaire.multiply(BigDecimal.valueOf(quantiteRecue));
    }

    public int getIdMedicament() { return idMedicament; }
    public String getNomMedicament() { return nomMedicament; }
    public int getQuantiteRecue() { return quantiteRecue; }
    public BigDecimal getPrixAchatUnitaire() { return prixAchatUnitaire; }
    public String getNumeroLot() { return numeroLot; }
    public LocalDate getDatePeremption() { return datePeremption; }
}
