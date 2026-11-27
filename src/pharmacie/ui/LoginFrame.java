package pharmacie.ui;

import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import pharmacie.dao.UtilisateurDAO;
import pharmacie.model.Utilisateur;
import pharmacie.util.DAOException;
import pharmacie.util.RegleGestionException;
import pharmacie.util.Session;

/** Écran de connexion (RG01 à RG04). */
public class LoginFrame extends JFrame {

    private final PanneauConnexion panneau = new PanneauConnexion();
    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();

    public LoginFrame() {
        super("PharmaGest - Connexion");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setContentPane(panneau);

        // Événement : clic sur le bouton, ou touche Entrée grâce au bouton par défaut
        panneau.boutonConnexion.addActionListener(e -> seConnecter());
        getRootPane().setDefaultButton(panneau.boutonConnexion);

        setSize(new Dimension(920, 560));
        setResizable(false);
        setLocationRelativeTo(null); // centré à l'écran
    }

    private void seConnecter() {
        try {
            Utilisateur u = utilisateurDAO.authentifier(panneau.champLogin.getText(),
                    new String(panneau.champMotDePasse.getPassword()));
            Session.ouvrir(u);
            new MainFrame().setVisible(true);
            dispose();
        } catch (RegleGestionException e) {
            // Mauvais identifiants ou compte désactivé (RG04) : message sous le formulaire, pas de plantage
            panneau.messageErreur.setText(e.getMessage());
            panneau.champMotDePasse.setText("");
            panneau.champMotDePasse.requestFocusInWindow();
        } catch (DAOException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erreur de base de données", JOptionPane.ERROR_MESSAGE);
        }
    }
}
