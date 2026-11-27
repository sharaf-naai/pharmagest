package pharmacie.model;

import java.math.BigDecimal;

/** Table ligne_vente (RG16 à RG18). */
public class LigneVente {

    private int idMedicament;
    private String nomMedicament;          // affichage
    private int quantiteVendue;
    private BigDecimal prixUnitaireVente;  // RG18 : VenteDAO le relit en base au moment de la vente

    public LigneVente(int idMedicament, String nomMedicament, int quantiteVendue, BigDecimal prixUnitaireVente) {
        this.idMedicament = idMedicament;
        this.nomMedicament = nomMedicament;
        this.quantiteVendue = quantiteVendue;
        this.prixUnitaireVente = prixUnitaireVente;
    }

    /** RG19 : sous-total = quantité × prix unitaire. */
    public BigDecimal getSousTotal() {
        return prixUnitaireVente.multiply(BigDecimal.valueOf(quantiteVendue));
    }

    public int getIdMedicament() { return idMedicament; }
    public String getNomMedicament() { return nomMedicament; }
    public int getQuantiteVendue() { return quantiteVendue; }
    public void setQuantiteVendue(int quantiteVendue) { this.quantiteVendue = quantiteVendue; }
    public BigDecimal getPrixUnitaireVente() { return prixUnitaireVente; }
}
