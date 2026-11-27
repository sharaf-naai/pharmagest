package pharmacie.model;

import java.time.LocalDate;

/** Une ligne de la vue v_peremption_proche (RG26). */
public class AlertePeremption {

    private final int idMedicament;
    private final String nomMedicament;
    private final String numeroLot;
    private final LocalDate datePeremption;
    private final int joursRestants;

    public AlertePeremption(int idMedicament, String nomMedicament, String numeroLot,
                            LocalDate datePeremption, int joursRestants) {
        this.idMedicament = idMedicament;
        this.nomMedicament = nomMedicament;
        this.numeroLot = numeroLot;
        this.datePeremption = datePeremption;
        this.joursRestants = joursRestants;
    }

    public int getIdMedicament() { return idMedicament; }
    public String getNomMedicament() { return nomMedicament; }
    public String getNumeroLot() { return numeroLot; }
    public LocalDate getDatePeremption() { return datePeremption; }
    public int getJoursRestants() { return joursRestants; }
}
