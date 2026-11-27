# CLAUDE.md : Application de gestion d'une pharmacie

University project (Cadi Ayyad University, Computer Science). Desktop app in **Java Swing + JDBC + MySQL**, designed with **MERISE**.
The student must be able to explain every line during the oral demo, so keep code simple, readable and commented in French.

## Hard constraints (graded, from the subject PDF)

- Object-oriented Java, code split into several classes and packages.
- GUI with **Java Swing** only, using **events** (ActionListener, KeyListener, etc.).
- MySQL access with **JDBC** and **PreparedStatement only**. Never build SQL by concatenating user values.
- **Exception handling** everywhere: no empty catch blocks, no stack traces shown to the user.
- No frameworks (no Spring, Hibernate, JavaFX, FlatLaf or other UI libraries). The only external jar is the MySQL driver.

## Environment (Windows)

- JDK 27, VS Code with the Java extension pack. **No Maven/Gradle**: plain VS Code Java project.
- Source root: `src/`. Compiled classes: `bin/`. Driver: `lib/mysql-connector-j-9.7.0.jar`.
- MySQL 8.4 on `localhost:3306`, database `pharmacie`, created by `sql/pharmacie.sql`.
- DB credentials in `db.properties` at the project root (gitignored). Keep `db.properties.example` committed with a placeholder password:
  ```
  db.url=jdbc:mysql://localhost:3306/pharmacie?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=UTF-8
  db.user=root
  db.password=CHANGE_ME
  ```
- Compile and run from the project root (PowerShell):
  ```powershell
  javac -encoding UTF-8 -d bin -cp "lib/*" (Get-ChildItem -Recurse src -Filter *.java).FullName
  java -cp "bin;lib/*" pharmacie.Main
  ```
- Reset the database to the demo data: `mysql -u root -p --default-character-set=utf8mb4 -e "source sql/pharmacie.sql"`

## Demo accounts (from sql/pharmacie.sql)

| Login | Password | Role | Note |
| --- | --- | --- | --- |
| admin | admin123 | ADMIN | sees every tab |
| pharmacien | pharma123 | PHARMACIEN | limited tabs |
| ancien | ancien123 | PHARMACIEN | **disabled**: login must be refused |

Passwords are stored as **SHA-256 hex** (64 chars) in `utilisateur.mot_de_passe`.

## Database (source of truth: `sql/pharmacie.sql`)

**Do not change the schema or the seed data without asking the user first.** The MERISE dossier (MCD/MLD) must stay consistent with it.

Tables (PK in bold, # = FK):
- utilisateur (**id_utilisateur**, login UNIQUE, mot_de_passe, nom_complet, role ENUM('ADMIN','PHARMACIEN'), actif BOOLEAN)
- categorie (**id_categorie**, libelle_categorie UNIQUE)
- medicament (**id_medicament**, nom_medicament, forme, dosage, prix_vente DECIMAL(10,2), quantite_stock, seuil_minimal, #id_categorie)
- client (**id_client**, nom_client, prenom_client, telephone_client, adresse_client)
- fournisseur (**id_fournisseur**, raison_sociale, telephone_fournisseur, email_fournisseur, adresse_fournisseur, ville_fournisseur)
- vente (**id_vente**, date_vente DATETIME, #id_client NULLABLE = walk-in client, #id_utilisateur)
- ligne_vente (**#id_vente, #id_medicament**, quantite_vendue, prix_unitaire_vente)
- approvisionnement (**id_approvisionnement**, date_approvisionnement DATE, reference_bon, #id_fournisseur, #id_utilisateur)
- ligne_approvisionnement (**#id_approvisionnement, #id_medicament**, quantite_recue, prix_achat_unitaire, numero_lot, date_peremption)

FK behaviour: medicament and fournisseur referenced → DELETE refused (MySQL error 1451). Client deleted → vente.id_client SET NULL. Lines are deleted in CASCADE with their vente/approvisionnement.

Views (use them instead of recomputing in Java): `v_vente_total` (montant_total per sale), `v_approvisionnement_total`, `v_stock_faible`, `v_peremption_proche` (lots expiring ≤ 30 days, medicine still in stock, with jours_restants), `v_tableau_de_bord` (nb_medicaments, nb_clients, nb_fournisseurs, nb_ventes, chiffre_affaires).

Expected values on fresh demo data: dashboard 8 / 3 / 3 / 4 / 1656.00; low stock = Amoxicilline, Oméprazole, Sirop contre la toux; near expiry = Vitamine C (VTC-A1), Sirop contre la toux (SIR-B1).

## Business rules the code must enforce

Reference the rule number in a comment where it is enforced (e.g. `// RG17`).

- RG01 login unique · RG02 password hashed SHA-256 · RG03 one role, ADMIN or PHARMACIEN, decides visible tabs · RG04 disabled account cannot log in
- RG05/RG06 medicine: name, price > 0, stock ≥ 0, threshold ≥ 0 required · RG07 one category per medicine · RG08 search by exact id or part of the name · RG09 medicine used in a sale or delivery cannot be deleted
- RG10 supplier: company name + phone required · RG11 supplier with deliveries cannot be deleted · RG12 client: last + first name required · RG13 deleting a client keeps their sales as walk-in
- RG14 a sale has one user and a date/time · RG15 zero or one client · RG16 at least one line, each medicine once per sale · RG17 quantity > 0 and ≤ stock · RG18 the price stored in ligne_vente is the medicine's price **read from the DB at sale time**, never trusted from the UI · RG19 total = Σ qty × unit price, computed, never typed · RG20 insert sale + lines + stock decrease in **one transaction** (commit/rollback)
- RG21 a delivery has one supplier and one user · RG22 at least one line, qty > 0, purchase price > 0, each medicine once · RG23 stock increases in the same transaction · RG24 low stock = quantite_stock < seuil_minimal · RG25 each delivered line has a lot number and expiry date (expiry must be after the delivery date) · RG26 near-expiry alert = ≤ 30 days and still in stock
- RG27 revenue = Σ of all sale totals · RG28 dashboard (revenue) is ADMIN only

## Code architecture

```
src/pharmacie/
  Main.java                 look & feel + open LoginFrame on the EDT
  config/DatabaseConnection getConnection() reads db.properties, NEW connection per call
  util/                     PasswordUtil (SHA-256), Session (current user, estAdmin()),
                            DAOException (technical, wraps SQLException),
                            RegleGestionException (business rule, message shown to the user)
  model/                    one class per table + Role enum + AlertePeremption + IndicateursTableauDeBord
  dao/                      one DAO per table + TableauDeBordDAO
  ui/LoginFrame + PanneauConnexion   login screen
  ui/MainFrame + BarreLaterale + EnTete   sidebar menu, top bar, one page per module (CardLayout)
  ui/theme/                 design system: Theme, Carte, Bouton, ChampTexte, ChampMotDePasse, BordureChamp,
                            Tableaux, RenduBadge, Badge, Ton, OngletsSegmentes, CarteIndicateur, Icone
  ui/panels/                one JPanel per module (TableauDeBordPanel, MedicamentPanel, FournisseurPanel,
                            ClientPanel, VentePanel, StockPanel, UtilisateurPanel)
```

Conventions:
- Identifiers in **French**, matching the MLD (Medicament, quantiteStock, VenteDAO.enregistrer...). Comments in French.
- Money = `BigDecimal`, dates = `LocalDate` / `LocalDateTime` (`rs.getObject(col, LocalDate.class)`).
- Every JDBC resource in **try-with-resources**. One connection per DAO call, except transactions, which use one connection with `setAutoCommit(false)`, `commit()`, `rollback()` in the catch, and lock stock rows with `SELECT ... FOR UPDATE`.
- DAO methods throw `DAOException` (technical) and `RegleGestionException` (business). Validate inputs in the DAO before any SQL.
- The UI **never** uses JDBC directly: panels call DAOs only.
- UI shows errors with `JOptionPane`: RegleGestionException → its message as a warning; DAOException → "Erreur de base de données" + short message.
- Swing work on the EDT (`SwingUtilities.invokeLater`). Keep layouts simple (BorderLayout, GridBagLayout, JTable + form). No GUI builder code.
- Pages by role (sidebar order): ADMIN = Tableau de bord, Ventes, Médicaments, Stocks, Clients, Fournisseurs, Utilisateurs. PHARMACIEN = Ventes, Médicaments (read-only), Stocks (read-only), Clients.

## Phases and file ownership

- **Phase 2 (foundation, one agent)**: config, util, model, all DAOs (including the sale and delivery transactions), LoginFrame, MainFrame with role-based tabs + "Déconnexion", one **placeholder** panel per module, and `src/pharmacie/test/TestPhase2.java` (console checks against the real DB that clean up after themselves).
  Done when: admin logs in and sees 7 tabs; pharmacien sees 4; ancien is refused ("compte désactivé"); a wrong password shows a message, not a crash; Déconnexion returns to login; a sale above stock is refused and stock is unchanged; a valid sale decreases stock; TestPhase2 passes.
- **Phase 4 (UI polish)**: apply the design system below to every panel without changing any behaviour, DAO call or business rule.
- **Phase 3 (screens, 3 agents in parallel, each in its own git worktree and branch)**: each agent edits **only its own panel files** in `ui/panels/`, and puts any new helper classes in **its own package** (A: `pharmacie.ui.referentiels`, B: `pharmacie.ui.ventes`, C: `pharmacie.ui.stock`). It must not change DAOs, models, util, MainFrame, CLAUDE.md or the SQL. If a DAO method is missing or wrong, **stop and tell the user** instead of editing the DAO.
  - Agent A: MedicamentPanel, FournisseurPanel, ClientPanel (CRUD + search + JTable)
  - Agent B: VentePanel (new sale, lines, live total, sale history and detail)
  - Agent C: StockPanel (quantities, low-stock and expiry alerts, new delivery, delivery history), TableauDeBordPanel, UtilisateurPanel
  - The 3 agents share the same MySQL database: never reload `sql/pharmacie.sql` and never leave test data behind (delete what you create).

## Screen conventions (Phase 3, all agents)

Same look everywhere so the app feels like one program:
- CRUD screens: search bar at the top (JTextField + "Rechercher" button, Enter also searches), JTable in the center (not editable, single selection, sortable with `setAutoCreateRowSorter(true)`), form on the right or bottom with buttons **Ajouter / Modifier / Supprimer / Vider**. Clicking a row fills the form.
- Use a custom `AbstractTableModel` per table (not DefaultTableModel with Object[][]).
- Ask for confirmation (`JOptionPane.showConfirmDialog`) before every delete and before validating a sale or a delivery.
- Validate number and date fields in the panel before calling the DAO (clear message if the user types letters in a quantity).
- Formats: money `0.00 DH` (BigDecimal, 2 decimals), dates `dd/MM/yyyy`, date-time `dd/MM/yyyy HH:mm`.
- Each panel reloads its data every time its tab becomes visible (`addComponentListener` → `componentShown`), so a sale made in Ventes shows up at once in Stocks and the dashboard.
- Role rules (RG03): PHARMACIEN sees Médicaments and Stocks **read-only** (hide the form buttons and the new-delivery form when `!Session.estAdmin()`). The user id for a sale or delivery always comes from `Session.getUtilisateur()`.
- Errors: `RegleGestionException` → `JOptionPane` warning with its message; `DAOException` → error dialog "Erreur de base de données" + message. Never let an exception reach the console.
- Labels and messages in French, no emoji. `Main` calls `Theme.installer()` before the first window.

## Design system (`pharmacie.ui.theme`): use it on every screen

Look: light gray workspace, white rounded cards, green accent (pharmacy), clean tables with colored status badges. Never hard-code colors or fonts in a panel: take them from `Theme`.

- **Page layout**: each module panel is a page shown by `MainFrame` (the top bar already shows the page title, so panels have NO title of their own). The panel itself is transparent (`setOpaque(false)`) on the gray `Theme.FOND` background, and puts its content in one or more `Carte` (white rounded card). Space between cards: 16 px.
- **Sub-tabs**: use `OngletsSegmentes` instead of `JTabbedPane` (same methods: `addTab`, `getSelectedIndex`, `setSelectedIndex`, `addChangeListener`).
- **Toolbar** (first row inside the main card): `ChampTexte("Rechercher par …", true)` (search icon, about 320 px wide) + `Bouton("Rechercher")` on the left; action buttons on the right.
- **Buttons**: `Bouton(texte, Bouton.Style.X, Icone.Type.Y)`. PRIMAIRE = the one main action of the area (Ajouter, Valider la vente, Enregistrer), SECONDAIRE = Modifier, Vider, Annuler, Rechercher, DANGER = Supprimer, Retirer, DISCRET = minor links. One PRIMAIRE per area.
- **Tables**: keep the existing models, then `Tableaux.styliser(table)`, put it in `Tableaux.defilement(table)`, set column widths with `Tableaux.largeur` so nothing important is cut, the name column in bold (`Tableaux.colonneEnGras`), amounts and quantities right-aligned (`Tableaux.alignerADroite`).
- **Status columns as badges**: `Tableaux.colonneBadge(table, col)`; give badge columns at least 130 px. Use these exact words so colors are consistent (see `Ton.pour`): "En stock" (green), "Stock faible" (red), "Périmé" (red), "N jours" for expiry (amber), "Actif" / "Inactif", "Client de passage" (gray), role "Administrateur" (blue) / "Pharmacien" (gray).
- **Forms**: inside a Carte, GridBagLayout, label above or left of each field (12.5 bold, `Theme.TEXTE`), fields = `ChampTexte` / `ChampMotDePasse`, or `BordureChamp.appliquer(field)` on an existing JTextField, JSpinner editor or similar. Combo boxes: just set `Theme.police(13f)` and preferred height `Theme.HAUTEUR_CONTROLE`.
- **Dashboard**: `CarteIndicateur(libellé, Icone.Type, couleur)` + `setValeur(...)`; the revenue card must be wide enough for "1 656.00 DH" (give it more weight in a GridBagLayout). Alert lists in Cartes with a title row: `Icone` ALERTE + bold title + `Badge(count, Ton)`.
- **Totals** (sale cart, delivery): right-aligned, label in `TEXTE_SECONDAIRE`, amount in `Theme.police(Font.BOLD, 22f)`.
- **Icons**: `new Icone(Icone.Type.X, size, color)`; never load image files.
- **Dialogs** stay `JOptionPane` (already themed by `Theme.installer()`).

## Rules for the agent

- Build only what this file and the subject ask for. The ★ extras already decided are: roles, hashed passwords, disabled accounts, categories, lots + expiry alerts, user management. **Do not invent new features.**
- Never edit `sql/pharmacie.sql`, never commit `db.properties`, never print or log passwords.
- Compile after every change with the command above and fix all errors before saying you're done.
- Do not commit to git unless the user asks.
- When done, give the user a short list of what changed and how to test it in the app.
