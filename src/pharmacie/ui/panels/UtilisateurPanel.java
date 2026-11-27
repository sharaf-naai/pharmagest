package pharmacie.ui.panels;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import pharmacie.dao.UtilisateurDAO;
import pharmacie.model.Role;
import pharmacie.model.Utilisateur;
import pharmacie.ui.EcranOutils;
import pharmacie.ui.OutilsEcran;
import pharmacie.ui.theme.ListeDeroulante;
import pharmacie.ui.theme.Bouton;
import pharmacie.ui.theme.Carte;
import pharmacie.ui.theme.ChampMotDePasse;
import pharmacie.ui.theme.ChampTexte;
import pharmacie.ui.theme.Icone;
import pharmacie.ui.theme.Tableaux;
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
    private final ChampTexte champLogin = new ChampTexte();
    private final ChampTexte champNom = new ChampTexte();
    private final JComboBox<Role> comboRole = new ListeDeroulante<>(Role.values(), 200);
    private final ChampMotDePasse champMotDePasse = new ChampMotDePasse("");
    private final ChampMotDePasse champConfirmation = new ChampMotDePasse("");

    public UtilisateurPanel() {
        super(new BorderLayout(16, 16));
        setOpaque(false); // le fond gris vient de MainFrame

        // Tableau : login en gras, rôle et état en badges
        Tableaux.styliser(table);
        Tableaux.colonneEnGras(table, 0);
        Tableaux.colonneBadge(table, 2);
        Tableaux.largeur(table, 2, 140);
        Tableaux.colonneBadge(table, 3);
        Tableaux.largeur(table, 3, 130);

        // Boutons qui agissent sur le compte sélectionné (barre d'outils en haut de la carte)
        Bouton boutonRole = new Bouton("Changer le rôle", Bouton.Style.SECONDAIRE, Icone.Type.MODIFIER);
        Bouton boutonActif = new Bouton("Activer / Désactiver", Bouton.Style.SECONDAIRE);
        Bouton boutonMotDePasse = new Bouton("Réinitialiser le mot de passe", Bouton.Style.SECONDAIRE);
        boutonRole.addActionListener(e -> changerRole());
        boutonActif.addActionListener(e -> basculerActif());
        boutonMotDePasse.addActionListener(e -> reinitialiserMotDePasse());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.add(boutonRole);
        actions.add(boutonActif);
        actions.add(boutonMotDePasse);

        Carte centre = new Carte(new BorderLayout(0, 12));
        centre.add(actions, BorderLayout.NORTH);
        centre.add(Tableaux.defilement(table), BorderLayout.CENTER);

        add(centre, BorderLayout.CENTER);
        add(formulaireCreation(), BorderLayout.EAST);
        // Recharger les données chaque fois que l'onglet devient visible
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentShown(ComponentEvent e) {
                charger();
            }
        });
        charger();
    }

    private JPanel formulaireCreation() {
        champLogin.setColumns(15);
        OutilsEcran.styliserListe(comboRole);
        JPanel f = new JPanel(new GridBagLayout());
        f.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        String[] libelles = {"Login :", "Nom complet :", "Rôle :", "Mot de passe :", "Confirmation :"};
        JComponent[] champs = {champLogin, champNom, comboRole, champMotDePasse, champConfirmation};
        for (int i = 0; i < libelles.length; i++) {
            c.gridy = i;
            c.gridx = 0;
            c.fill = GridBagConstraints.NONE;
            f.add(OutilsEcran.etiquette(libelles[i]), c);
            c.gridx = 1;
            c.fill = GridBagConstraints.HORIZONTAL; // tous les champs à la même largeur
            f.add(champs[i], c);
        }
        Bouton boutonCreer = new Bouton("Créer le compte", Bouton.Style.PRIMAIRE, Icone.Type.AJOUTER);
        boutonCreer.addActionListener(e -> creer());
        JPanel boutons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 12));
        boutons.setOpaque(false);
        boutons.add(boutonCreer);
        return OutilsEcran.carteFormulaire("Nouveau compte", f, boutons);
    }

    private void charger() {
        try {
            utilisateurs = utilisateurDAO.lister();
            modele.setRowCount(0);
            for (Utilisateur u : utilisateurs) {
                // Mots exacts des badges : bleu « Administrateur », gris « Pharmacien », vert/rouge Actif/Inactif
                String role = u.getRole() == Role.ADMIN ? "Administrateur" : "Pharmacien";
                modele.addRow(new Object[] {u.getLogin(), u.getNomComplet(), role, u.isActif() ? "Actif" : "Inactif"});
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
        ChampMotDePasse mdp = new ChampMotDePasse("");
        ChampMotDePasse confirmation = new ChampMotDePasse("");
        mdp.setColumns(15);
        JPanel saisie = new JPanel(new GridLayout(2, 2, 8, 8));
        saisie.add(OutilsEcran.etiquette("Nouveau mot de passe :"));
        saisie.add(mdp);
        saisie.add(OutilsEcran.etiquette("Confirmation :"));
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
