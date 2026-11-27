package pharmacie.ui.theme;

import java.awt.Dimension;

import javax.accessibility.Accessible;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

/**
 * Liste déroulante de l'application :
 * - sa largeur suit l'élément le plus long (jamais plus étroite que la largeur minimale demandée),
 *   même quand la liste est remplie après sa création ;
 * - même hauteur que les champs de saisie ;
 * - la liste ouverte est au moins aussi large que son élément le plus long, pour que tout soit lisible.
 */
public class ListeDeroulante<T> extends JComboBox<T> {

    private final int largeurMin;

    public ListeDeroulante() {
        this(200);
    }

    /** @param largeurMin largeur minimale en pixels */
    public ListeDeroulante(int largeurMin) {
        super();
        this.largeurMin = largeurMin;
        configurer();
    }

    public ListeDeroulante(T[] elements, int largeurMin) {
        super(elements);
        this.largeurMin = largeurMin;
        configurer();
    }

    private void configurer() {
        setFont(Theme.police(13f));
        setMaximumRowCount(12);
        addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                elargirListeOuverte();
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                // rien à faire
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                // rien à faire
            }
        });
    }

    @Override
    public Dimension getPreferredSize() {
        // Largeur calculée par Swing à partir du contenu actuel (elle se met à jour quand on ajoute des éléments)
        Dimension naturelle = super.getPreferredSize();
        return new Dimension(Math.max(naturelle.width + 8, largeurMin), Theme.HAUTEUR_CONTROLE);
    }

    @Override
    public Dimension getMinimumSize() {
        return new Dimension(largeurMin, Theme.HAUTEUR_CONTROLE);
    }

    /** Agrandit la liste ouverte à la largeur de son élément le plus long. */
    private void elargirListeOuverte() {
        Accessible enfant = getUI().getAccessibleChild(this, 0);
        if (!(enfant instanceof JPopupMenu)) {
            return;
        }
        JPopupMenu popup = (JPopupMenu) enfant;
        if (popup.getComponentCount() == 0 || !(popup.getComponent(0) instanceof JComponent)) {
            return;
        }
        JComponent defilement = (JComponent) popup.getComponent(0);
        Dimension taille = defilement.getPreferredSize();
        taille.width = Math.max(getWidth(), super.getPreferredSize().width + 24);
        defilement.setPreferredSize(taille);
        defilement.setMaximumSize(taille);
    }
}
