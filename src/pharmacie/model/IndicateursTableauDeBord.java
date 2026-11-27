package pharmacie.model;

import java.math.BigDecimal;

/** Une ligne de la vue v_tableau_de_bord (RG27). */
public class IndicateursTableauDeBord {

    private final int nbMedicaments;
    private final int nbClients;
    private final int nbFournisseurs;
    private final int nbVentes;
    private final BigDecimal chiffreAffaires;

    public IndicateursTableauDeBord(int nbMedicaments, int nbClients, int nbFournisseurs,
                                    int nbVentes, BigDecimal chiffreAffaires) {
        this.nbMedicaments = nbMedicaments;
        this.nbClients = nbClients;
        this.nbFournisseurs = nbFournisseurs;
        this.nbVentes = nbVentes;
        this.chiffreAffaires = chiffreAffaires;
    }

    public int getNbMedicaments() { return nbMedicaments; }
    public int getNbClients() { return nbClients; }
    public int getNbFournisseurs() { return nbFournisseurs; }
    public int getNbVentes() { return nbVentes; }
    public BigDecimal getChiffreAffaires() { return chiffreAffaires; }
}
