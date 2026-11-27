package pharmacie.model;

/** Table categorie (RG07). */
public class Categorie {

    private int idCategorie;
    private String libelleCategorie;

    public Categorie(int idCategorie, String libelleCategorie) {
        this.idCategorie = idCategorie;
        this.libelleCategorie = libelleCategorie;
    }

    public int getIdCategorie() { return idCategorie; }
    public String getLibelleCategorie() { return libelleCategorie; }

    /** Affiché tel quel dans une JComboBox. */
    @Override
    public String toString() {
        return libelleCategorie;
    }
}
