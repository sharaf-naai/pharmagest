package pharmacie.model;

import java.math.BigDecimal;

/** Table medicament (RG05 à RG09). */
public class Medicament {

    private int idMedicament;
    private String nomMedicament;
    private String forme;
    private String dosage;
    private BigDecimal prixVente;
    private int quantiteStock;
    private int seuilMinimal;
    private int idCategorie;
    private String libelleCategorie; // lu par jointure, pour l'affichage

    public Medicament(int idMedicament, String nomMedicament, String forme, String dosage,
                      BigDecimal prixVente, int quantiteStock, int seuilMinimal,
                      int idCategorie, String libelleCategorie) {
        this.idMedicament = idMedicament;
        this.nomMedicament = nomMedicament;
        this.forme = forme;
        this.dosage = dosage;
        this.prixVente = prixVente;
        this.quantiteStock = quantiteStock;
        this.seuilMinimal = seuilMinimal;
        this.idCategorie = idCategorie;
        this.libelleCategorie = libelleCategorie;
    }

    /** RG24 : stock faible si quantité < seuil minimal. */
    public boolean estStockFaible() {
        return quantiteStock < seuilMinimal;
    }

    public int getIdMedicament() { return idMedicament; }
    public void setIdMedicament(int idMedicament) { this.idMedicament = idMedicament; }
    public String getNomMedicament() { return nomMedicament; }
    public void setNomMedicament(String nomMedicament) { this.nomMedicament = nomMedicament; }
    public String getForme() { return forme; }
    public void setForme(String forme) { this.forme = forme; }
    public String getDosage() { return dosage; }
    public void setDosage(String dosage) { this.dosage = dosage; }
    public BigDecimal getPrixVente() { return prixVente; }
    public void setPrixVente(BigDecimal prixVente) { this.prixVente = prixVente; }
    public int getQuantiteStock() { return quantiteStock; }
    public void setQuantiteStock(int quantiteStock) { this.quantiteStock = quantiteStock; }
    public int getSeuilMinimal() { return seuilMinimal; }
    public void setSeuilMinimal(int seuilMinimal) { this.seuilMinimal = seuilMinimal; }
    public int getIdCategorie() { return idCategorie; }
    public void setIdCategorie(int idCategorie) { this.idCategorie = idCategorie; }
    public String getLibelleCategorie() { return libelleCategorie; }

    @Override
    public String toString() {
        return nomMedicament + (dosage == null ? "" : " " + dosage);
    }
}
