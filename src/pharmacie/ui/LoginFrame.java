package pharmacie.ui;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import pharmacie.dao.UtilisateurDAO;
import pharmacie.model.Utilisateur;
import pharmacie.util.DAOException;
import pharmacie.util.RegleGestionException;
import pharmacie.util.Session;

/** Écran de connexion (RG01 à RG04). */
public class LoginFrame extends JFrame {

    private final JTextField champLogin = new JTextField(15);
    private final JPasswordField champMotDePasse = new JPasswordField(15);
    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();

    public LoginFrame() {
        super("Pharmacie - Connexion");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panneau = new JPanel(new GridBagLayout());
        panneau.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 5, 5, 5);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0;
        panneau.add(new JLabel("Login :"), c);
        c.gridx = 1;
        panneau.add(champLogin, c);

        c.gridx = 0; c.gridy = 1;
        panneau.add(new JLabel("Mot de passe :"), c);
        c.gridx = 1;
        panneau.add(champMotDePasse, c);

        JButton boutonConnexion = new JButton("Se connecter");
        c.gridx = 1; c.gridy = 2;
        c.anchor = GridBagConstraints.EAST;
        panneau.add(boutonConnexion, c);

        // Événement : clic sur le bouton (ou touche Entrée, grâce au bouton par défaut)
        boutonConnexion.addActionListener(e -> seConnecter());
        getRootPane().setDefaultButton(boutonConnexion);

        setContentPane(panneau);
        pack();
        setResizable(false);
        setLocationRelativeTo(null); // centré à l'écran
    }

    private void seConnecter() {
        try {
            Utilisateur u = utilisateurDAO.authentifier(champLogin.getText(),
                    new String(champMotDePasse.getPassword()));
            Session.ouvrir(u);
            new MainFrame().setVisible(true);
            dispose();
        } catch (RegleGestionException e) {
            // Mauvais identifiants ou compte désactivé (RG04) : message clair, pas de plantage
            JOptionPane.showMessageDialog(this, e.getMessage(), "Connexion refusée", JOptionPane.WARNING_MESSAGE);
            champMotDePasse.setText("");
            champMotDePasse.requestFocusInWindow();
        } catch (DAOException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Erreur de base de données", JOptionPane.ERROR_MESSAGE);
        }
    }
}
