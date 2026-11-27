package pharmacie.ui.theme;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

/** Mise en forme commune des JTable : lignes aérées, séparateurs fins, en-tête discret, badges. */
public final class Tableaux {

    private Tableaux() {
    }

    /** Applique le style de l'application à un tableau existant (n'importe quel TableModel). */
    public static void styliser(JTable table) {
        table.setFont(Theme.police(13f));
        table.setRowHeight(42);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(true);
        table.setGridColor(new Color(0xEEF0F2));
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setBackground(Theme.SURFACE);
        table.setForeground(Theme.TEXTE);
        table.setSelectionBackground(Theme.PRIMAIRE_CLAIR);
        table.setSelectionForeground(Theme.TEXTE);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.setBorder(null);
        table.setDefaultRenderer(Object.class, new RenduCellule(SwingConstants.LEFT));
        table.setDefaultRenderer(Number.class, new RenduCellule(SwingConstants.RIGHT));
        table.setDefaultRenderer(Integer.class, new RenduCellule(SwingConstants.RIGHT));

        JTableHeader entete = table.getTableHeader();
        entete.setReorderingAllowed(false);
        entete.setPreferredSize(new Dimension(entete.getPreferredSize().width, 40));
        entete.setDefaultRenderer(new RenduEntete());
        entete.setBackground(Theme.SURFACE_DOUCE);
    }

    /** Le tableau dans un défilement sans cadre, prêt à poser dans une Carte. */
    public static JScrollPane defilement(JTable table) {
        JScrollPane sp = new JScrollPane(table);
        sp.setColumnHeaderView(table.getTableHeader()); // en-tête toujours visible
        sp.setBorder(BorderFactory.createLineBorder(Theme.BORDURE));
        sp.getViewport().setBackground(Theme.SURFACE);
        sp.setBackground(Theme.SURFACE);
        return sp;
    }

    /** Affiche la colonne sous forme de badges colorés (couleurs par défaut de Ton.pour). */
    public static void colonneBadge(JTable table, int colonne) {
        colonneBadge(table, colonne, null);
    }

    /** Badges avec des couleurs imposées pour certains libellés. */
    public static void colonneBadge(JTable table, int colonne, Map<String, Ton> tons) {
        table.getColumnModel().getColumn(colonne).setCellRenderer(new RenduBadge(tons));
    }

    /** Aligne une colonne à droite (montants, quantités). */
    public static void alignerADroite(JTable table, int colonne) {
        table.getColumnModel().getColumn(colonne).setCellRenderer(new RenduCellule(SwingConstants.RIGHT));
    }

    /** Écrit une colonne en gras (par exemple le nom d'un médicament). */
    public static void colonneEnGras(JTable table, int colonne) {
        RenduCellule rendu = new RenduCellule(SwingConstants.LEFT);
        rendu.gras = true;
        table.getColumnModel().getColumn(colonne).setCellRenderer(rendu);
    }

    /** Largeur préférée d'une colonne, en pixels. */
    public static void largeur(JTable table, int colonne, int pixels) {
        table.getColumnModel().getColumn(colonne).setPreferredWidth(pixels);
    }

    /** Cellule ordinaire : marge intérieure, sélection verte pâle. */
    private static final class RenduCellule extends DefaultTableCellRenderer {
        private final int alignement;
        private boolean gras;

        RenduCellule(int alignement) {
            this.alignement = alignement;
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object valeur, boolean isSelected,
                                                       boolean hasFocus, int ligne, int colonne) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, valeur, isSelected, false, ligne, colonne);
            l.setHorizontalAlignment(alignement);
            l.setBorder(BorderFactory.createEmptyBorder(0, 14, 0, 14));
            l.setFont(gras ? Theme.police(Font.BOLD, 13f) : Theme.police(13f));
            l.setBackground(isSelected ? Theme.PRIMAIRE_CLAIR : Theme.SURFACE);
            l.setForeground(Theme.TEXTE);
            return l;
        }
    }

    /** En-tête de colonne : petit texte gris sur fond clair. */
    private static final class RenduEntete extends DefaultTableCellRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object valeur, boolean isSelected,
                                                       boolean hasFocus, int ligne, int colonne) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(table, valeur, false, false, ligne, colonne);
            l.setFont(Theme.police(Font.BOLD, 12f));
            l.setForeground(Theme.TEXTE_SECONDAIRE);
            l.setBackground(Theme.SURFACE_DOUCE);
            l.setHorizontalAlignment(SwingConstants.LEFT);
            l.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Theme.BORDURE),
                    BorderFactory.createEmptyBorder(0, 14, 0, 14)));
            return l;
        }
    }
}
