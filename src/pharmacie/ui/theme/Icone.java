package pharmacie.ui.theme;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

import javax.swing.Icon;

/**
 * Petites icônes au trait, dessinées en Java2D (aucun fichier image à fournir).
 * Chaque icône est tracée sur une grille de 24 x 24 puis mise à l'échelle.
 */
public class Icone implements Icon {

    public enum Type {
        LOGO, TABLEAU_DE_BORD, VENTES, MEDICAMENTS, STOCKS, CLIENTS, FOURNISSEURS, UTILISATEURS,
        DECONNEXION, RECHERCHE, AJOUTER, MODIFIER, SUPPRIMER, VALIDER, ACTUALISER, ALERTE,
        CALENDRIER, CHIFFRE_AFFAIRES
    }

    private final Type type;
    private final int taille;
    private final Color couleur;

    public Icone(Type type, int taille, Color couleur) {
        this.type = type;
        this.taille = taille;
        this.couleur = couleur;
    }

    @Override
    public int getIconWidth() {
        return taille;
    }

    @Override
    public int getIconHeight() {
        return taille;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.translate(x, y);
        g2.scale(taille / 24.0, taille / 24.0);
        g2.setColor(couleur);
        g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        dessiner(g2);
        g2.dispose();
    }

    private void dessiner(Graphics2D g) {
        switch (type) {
            case LOGO: {
                g.fill(new RoundRectangle2D.Double(0, 0, 24, 24, 12, 12));
                g.setColor(Color.WHITE);
                g.fill(new RoundRectangle2D.Double(10, 5, 4, 14, 2, 2));
                g.fill(new RoundRectangle2D.Double(5, 10, 14, 4, 2, 2));
                break;
            }
            case TABLEAU_DE_BORD: {
                g.draw(new RoundRectangle2D.Double(3.5, 3.5, 7, 7, 3, 3));
                g.draw(new RoundRectangle2D.Double(13.5, 3.5, 7, 7, 3, 3));
                g.draw(new RoundRectangle2D.Double(3.5, 13.5, 7, 7, 3, 3));
                g.draw(new RoundRectangle2D.Double(13.5, 13.5, 7, 7, 3, 3));
                break;
            }
            case VENTES: {
                g.draw(new RoundRectangle2D.Double(4, 8, 16, 13, 4, 4));
                g.draw(new Arc2D.Double(8, 3, 8, 9, 0, 180, Arc2D.OPEN));
                break;
            }
            case MEDICAMENTS: {
                Graphics2D r = (Graphics2D) g.create();
                r.rotate(Math.toRadians(-45), 12, 12);
                r.draw(new RoundRectangle2D.Double(2.5, 8.5, 19, 7, 7, 7));
                r.draw(new Line2D.Double(12, 8.5, 12, 15.5));
                r.dispose();
                break;
            }
            case STOCKS: {
                Path2D p = new Path2D.Double();
                p.moveTo(12, 3); p.lineTo(20.5, 7.5); p.lineTo(12, 12); p.lineTo(3.5, 7.5); p.closePath();
                g.draw(p);
                Path2D cotes = new Path2D.Double();
                cotes.moveTo(3.5, 7.5); cotes.lineTo(3.5, 16.5); cotes.lineTo(12, 21);
                cotes.lineTo(20.5, 16.5); cotes.lineTo(20.5, 7.5);
                g.draw(cotes);
                g.draw(new Line2D.Double(12, 12, 12, 21));
                break;
            }
            case CLIENTS: {
                g.draw(new Ellipse2D.Double(5.5, 4, 7, 7));
                g.draw(new Arc2D.Double(2.5, 13, 13, 14, 0, 180, Arc2D.OPEN));
                g.draw(new Arc2D.Double(14, 5, 6, 6, -60, 240, Arc2D.OPEN));
                g.draw(new Arc2D.Double(15, 13.5, 7, 13, 0, 90, Arc2D.OPEN));
                break;
            }
            case FOURNISSEURS: {
                g.draw(new RoundRectangle2D.Double(2, 6, 12, 10.5, 2, 2));
                Path2D cabine = new Path2D.Double();
                cabine.moveTo(14, 9.5); cabine.lineTo(18, 9.5); cabine.lineTo(21.5, 13); cabine.lineTo(21.5, 16.5);
                cabine.lineTo(14, 16.5);
                g.draw(cabine);
                g.draw(new Ellipse2D.Double(4.5, 16, 4, 4));
                g.draw(new Ellipse2D.Double(15, 16, 4, 4));
                break;
            }
            case UTILISATEURS: {
                Path2D bouclier = new Path2D.Double();
                bouclier.moveTo(12, 3); bouclier.lineTo(19.5, 6); bouclier.lineTo(19.5, 11.5);
                bouclier.curveTo(19.5, 16, 16, 19.5, 12, 21);
                bouclier.curveTo(8, 19.5, 4.5, 16, 4.5, 11.5);
                bouclier.lineTo(4.5, 6); bouclier.closePath();
                g.draw(bouclier);
                Path2D coche = new Path2D.Double();
                coche.moveTo(9, 12); coche.lineTo(11.2, 14.2); coche.lineTo(15.2, 10);
                g.draw(coche);
                break;
            }
            case DECONNEXION: {
                Path2D porte = new Path2D.Double();
                porte.moveTo(10, 4); porte.lineTo(5, 4); porte.lineTo(5, 20); porte.lineTo(10, 20);
                g.draw(porte);
                g.draw(new Line2D.Double(10, 12, 20, 12));
                Path2D fleche = new Path2D.Double();
                fleche.moveTo(16, 8); fleche.lineTo(20, 12); fleche.lineTo(16, 16);
                g.draw(fleche);
                break;
            }
            case RECHERCHE: {
                g.draw(new Ellipse2D.Double(4, 4, 12, 12));
                g.draw(new Line2D.Double(14.8, 14.8, 20, 20));
                break;
            }
            case AJOUTER: {
                g.draw(new Line2D.Double(12, 5, 12, 19));
                g.draw(new Line2D.Double(5, 12, 19, 12));
                break;
            }
            case MODIFIER: {
                Path2D crayon = new Path2D.Double();
                crayon.moveTo(4, 20); crayon.lineTo(5, 15.5); crayon.lineTo(15.5, 5); crayon.lineTo(19, 8.5);
                crayon.lineTo(8.5, 19); crayon.closePath();
                g.draw(crayon);
                g.draw(new Line2D.Double(13, 7.5, 16.5, 11));
                break;
            }
            case SUPPRIMER: {
                g.draw(new Line2D.Double(4, 7, 20, 7));
                Path2D anse = new Path2D.Double();
                anse.moveTo(9, 7); anse.lineTo(9, 4); anse.lineTo(15, 4); anse.lineTo(15, 7);
                g.draw(anse);
                Path2D corps = new Path2D.Double();
                corps.moveTo(6, 7); corps.lineTo(7, 20); corps.lineTo(17, 20); corps.lineTo(18, 7);
                g.draw(corps);
                g.draw(new Line2D.Double(10, 11, 10, 16));
                g.draw(new Line2D.Double(14, 11, 14, 16));
                break;
            }
            case VALIDER: {
                Path2D coche = new Path2D.Double();
                coche.moveTo(5, 12.5); coche.lineTo(10, 17.5); coche.lineTo(19, 7);
                g.draw(coche);
                break;
            }
            case ACTUALISER: {
                g.draw(new Arc2D.Double(4.5, 4.5, 15, 15, 60, 280, Arc2D.OPEN));
                Path2D fleche = new Path2D.Double();
                fleche.moveTo(19.5, 3.5); fleche.lineTo(19.5, 8.5); fleche.lineTo(14.5, 8.5);
                g.draw(fleche);
                break;
            }
            case ALERTE: {
                Path2D triangle = new Path2D.Double();
                triangle.moveTo(12, 3.5); triangle.lineTo(21, 19.5); triangle.lineTo(3, 19.5); triangle.closePath();
                g.draw(triangle);
                g.draw(new Line2D.Double(12, 9.5, 12, 13.5));
                g.fill(new Ellipse2D.Double(11, 15.5, 2, 2));
                break;
            }
            case CALENDRIER: {
                g.draw(new RoundRectangle2D.Double(3.5, 5, 17, 15.5, 4, 4));
                g.draw(new Line2D.Double(3.5, 9.5, 20.5, 9.5));
                g.draw(new Line2D.Double(8, 3, 8, 7));
                g.draw(new Line2D.Double(16, 3, 16, 7));
                break;
            }
            case CHIFFRE_AFFAIRES: {
                Path2D courbe = new Path2D.Double();
                courbe.moveTo(3, 17); courbe.lineTo(9, 11); courbe.lineTo(13, 15); courbe.lineTo(21, 7);
                g.draw(courbe);
                Path2D fleche = new Path2D.Double();
                fleche.moveTo(16, 7); fleche.lineTo(21, 7); fleche.lineTo(21, 12);
                g.draw(fleche);
                break;
            }
            default:
                break;
        }
    }
}
