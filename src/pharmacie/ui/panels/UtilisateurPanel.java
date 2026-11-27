package pharmacie.ui.panels;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

import pharmacie.dao.UtilisateurDAO;
import pharmacie.model.Role;
import pharmacie.model.Utilisateur;
import pharmacie.ui.EcranOutils;
import pharmacie.util.DAOException;
import pharmacie.util.RegleGestionException;
import pharmacie.util.Session;

/** Gestion des comptes utilisateurs (onglet réservé à l'ADMIN). */
public class UtilisateurPanel extends JPanel {

    private final UtilisateurDAO utilisateurDAO = new UtilisateurDAO();
    private final DefaultTableModel modele = EcranOutils.modeleLectureSeule("Login", "Nom complet", "Rôle", "Actif");
    private final JTable table = new JTable(modele);
    private List<Utilisateur> utilisateurs = new ArrayList<>(); // même ordre que les lignes du tableau

    // Formulaire de création
    private final JTextField champLogin = new JTextField(15);
    private final JTextField champNom = new JTextField(15);
    private final JComboBox<Role> comboRole = new JComboBox<>(Role.values());
    private final JPasswordField champMotDePasse = new JPasswordField(15);
    private final JPasswordField champConfirmation = new JPasswordField(15);

    public UtilisateurPanel() {
        super(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // Boutons qui agissent sur le compte sélectionné
        JButton boutonRole = new JButton("Changer le rôle");
        JButton boutonActif = new JButton("Activer / Désactiver");
        JButton boutonMotDePasse = new JButton("Réinitialiser le mot de passe");
        boutonRole.addActionListener(e -> changerRole());
        boutonActif.addActionListener(e -> basculerActif());
        boutonMotDePasse.addActionListener(e -> reinitialiserMotDePasse());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT));
        actions.add(boutonRole);
        actions.add(boutonActif);
        actions.add(boutonMotDePasse);

        JPanel centre = new JPanel(new BorderLayout());
        centre.add(new JScrollPane(table), BorderLayout.CENTER);
        centre.add(actions, BorderLayout.SOUTH);

        add(centre, BorderLayout.CENTER);
        add(formulaireCreation(), BorderLayout.EAST);
        charger();
    }

    private JPanel formulaireCreation() {
        JPanel f = new JPanel(new GridBagLayout());
        f.setBorder(BorderFactory.createTitledBorder("Nouveau compte"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        String[] libelles = {"Login :", "Nom complet :", "Rôle :", "Mot de passe :", "Confirmation :"};
        JComponent[] champs = {champLogin, champNom, comboRole, champMotDePasse, champConfirmation};
        for (int i = 0; i < libelles.length; i++) {
            c.gridy = i;
            c.gridx = 0;
            f.add(new JLabel(libelles[i]), c);
            c.gridx = 1;
            f.add(champs[i], c);
        }
        JButton boutonCreer = new JButton("Créer le compte");
        boutonCreer.addActionListener(e -> creer());
        c.gridy = libelles.length;
        f.add(boutonCreer, c);
        return f;
    }

    private void charger() {
        try {
            utilisateurs = utilisateurDAO.lister();
            modele.setRowCount(0);
            for (Utilisateur u : utilisateurs) {
                modele.addRow(new Object[] {u.getLogin(), u.getNomComplet(), u.getRole(), u.isActif() ? "Oui" : "Non"});
            }
        } catch (DAOException e) {
            EcranOutils.afficherErreur(this, e);
        }
    }

    private void creer() {
        char[] mdp = champMotDePasse.getPassword();
        char[] confirmation = champConfirmation.getPassword();
        try {
            if (!Arrays.equals(mdp, confirmation)) {
                throw new RegleGestionException("Les deux mots de passe ne sont pas identiques.");
            }
            Utilisateur u = new Utilisateur(0, champLogin.getText(), champNom.getText(),
                    (Role) comboRole.getSelectedItem(), true);
            utilisateurDAO.ajouter(u, new String(mdp)); // RG01 (login unique) et RG02 (hachage) dans le DAO
            champLogin.setText("");
            champNom.setText("");
            champMotDePasse.setText("");
            champConfirmation.setText("");
            charger();
        } catch (RegleGestionException | DAOException e) {
            EcranOutils.afficherErreur(this, e);
        }
    }

    /** Compte sélectionné dans le tableau, ou null (avec un message) si aucune ligne n'est choisie. */
    private Utilisateur selection() {
        int ligne = table.getSelectedRow();
        if (ligne < 0) {
            JOptionPane.showMessageDialog(this, "Sélectionnez un compte dans le tableau.", "Attention",
                    JOptionPane.WARNING_MESSAGE);
            return null;
        }
        return utilisateurs.get(ligne);
    }

    private boolean estMoi(Utilisateur u) {
        return u.getIdUtilisateur() == Session.getUtilisateur().getIdUtilisateur();
    }

    private void changerRole() {
        Utilisateur u = selection();
        if (u == null) {
            return;
        }
        Role nouveau = (Role) JOptionPane.showInputDialog(this, "Nouveau rôle de " + u.getLogin() + " :",
                "Changer le rôle", JOptionPane.QUESTION_MESSAGE, null, Role.values(), u.getRole());
        if (nouveau == null || nouveau == u.getRole()) {
            return;
        }
        // Un admin ne peut pas se rétrograder lui-même (il perdrait l'accès à cet écran)
        if (estMoi(u) && nouveau != Role.ADMIN) {
            JOptionPane.showMessageDialog(this, "Vous ne pouvez pas retirer le rôle ADMIN à votre propre compte.",
                    "Attention", JOptionPane.WARNING_MESSAGE);
            return;
        }
        u.setRole(nouveau); // RG03
        enregistrer(u);
    }

    private void basculerActif() {
        Utilisateur u = selection();
        if (u == null) {
            return;
        }
        if (estMoi(u) && u.isActif()) {
            JOptionPane.showMessageDialog(this, "Vous ne pouvez pas désactiver votre propre compte.",
                    "Attention", JOptionPane.WARNING_MESSAGE);
            return;
        }
        u.setActif(!u.isActif()); // RG04 : un compte désactivé ne pourra plus se connecter
        enregistrer(u);
    }

    private void enregistrer(Utilisateur u) {
        try {
            utilisateurDAO.modifier(u);
        } catch (RegleGestionException | DAOException e) {
            EcranOutils.afficherErreur(this, e);
        }
        charger(); // recharge aussi en cas d'échec, pour réafficher l'état réel de la base
    }

    private void reinitialiserMotDePasse() {
        Utilisateur u = selection();
        if (u == null) {
            return;
        }
        JPasswordField mdp = new JPasswordField(15);
        JPasswordField confirmation = new JPasswordField(15);
        JPanel saisie = new JPanel(new GridLayout(2, 2, 4, 4));
        saisie.add(new JLabel("Nouveau mot de passe :"));
        saisie.add(mdp);
        saisie.add(new JLabel("Confirmation :"));
        saisie.add(confirmation);
        int choix = JOptionPane.showConfirmDialog(this, saisie, "Mot de passe de " + u.getLogin(),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (choix != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            if (!Arrays.equals(mdp.getPassword(), confirmation.getPassword())) {
                throw new RegleGestionException("Les deux mots de passe ne sont pas identiques.");
            }
            utilisateurDAO.changerMotDePasse(u.getIdUtilisateur(), new String(mdp.getPassword())); // RG02
            JOptionPane.showMessageDialog(this, "Mot de passe modifié.", "Utilisateurs",
                    JOptionPane.INFORMATION_MESSAGE);
        } catch (RegleGestionException | DAOException e) {
            EcranOutils.afficherErreur(this, e);
        }
    }
}
