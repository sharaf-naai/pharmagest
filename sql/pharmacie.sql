-- =====================================================================
--  Application de gestion d'une pharmacie : script de création MySQL
--  Conforme au MLD du dossier MERISE (section 5)
--  Cible : MySQL 8.0.16 ou plus (les contraintes CHECK y sont appliquées)
--  Les éléments marqués ★ sont des ajouts au cahier des charges.
-- =====================================================================

DROP DATABASE IF EXISTS pharmacie;
CREATE DATABASE pharmacie CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE pharmacie;

-- ---------------------------------------------------------------------
--  1. Tables issues des entités (règle de passage R1)
-- ---------------------------------------------------------------------

-- UTILISATEUR (RG01 à RG04)
CREATE TABLE utilisateur (
    id_utilisateur  INT          NOT NULL AUTO_INCREMENT,
    login           VARCHAR(50)  NOT NULL,
    mot_de_passe    CHAR(64)     NOT NULL,                -- ★ empreinte SHA-256 en hexadécimal (RG02)
    nom_complet     VARCHAR(100) NOT NULL,
    role            ENUM('ADMIN','PHARMACIEN') NOT NULL DEFAULT 'PHARMACIEN', -- ★ RG03
    actif           BOOLEAN      NOT NULL DEFAULT TRUE,   -- ★ RG04
    CONSTRAINT pk_utilisateur PRIMARY KEY (id_utilisateur),
    CONSTRAINT uq_utilisateur_login UNIQUE (login)
) ENGINE = InnoDB;

-- ★ CATEGORIE (RG07)
CREATE TABLE categorie (
    id_categorie      INT         NOT NULL AUTO_INCREMENT,
    libelle_categorie VARCHAR(50) NOT NULL,
    CONSTRAINT pk_categorie PRIMARY KEY (id_categorie),
    CONSTRAINT uq_categorie_libelle UNIQUE (libelle_categorie)
) ENGINE = InnoDB;

-- MEDICAMENT (RG05 à RG09) ; #id_categorie vient de APPARTENIR (R2)
CREATE TABLE medicament (
    id_medicament   INT            NOT NULL AUTO_INCREMENT,
    nom_medicament  VARCHAR(100)   NOT NULL,
    forme           VARCHAR(30)    NULL,
    dosage          VARCHAR(30)    NULL,
    prix_vente      DECIMAL(10,2)  NOT NULL,
    quantite_stock  INT            NOT NULL DEFAULT 0,
    seuil_minimal   INT            NOT NULL DEFAULT 0,
    id_categorie    INT            NOT NULL,
    CONSTRAINT pk_medicament PRIMARY KEY (id_medicament),
    CONSTRAINT chk_medicament_prix  CHECK (prix_vente > 0),
    CONSTRAINT chk_medicament_stock CHECK (quantite_stock >= 0),
    CONSTRAINT chk_medicament_seuil CHECK (seuil_minimal >= 0),
    CONSTRAINT fk_medicament_categorie FOREIGN KEY (id_categorie)
        REFERENCES categorie (id_categorie) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB;

CREATE INDEX idx_medicament_nom ON medicament (nom_medicament);   -- recherche par nom (RG08)

-- CLIENT (RG12, RG13)
CREATE TABLE client (
    id_client         INT          NOT NULL AUTO_INCREMENT,
    nom_client        VARCHAR(50)  NOT NULL,
    prenom_client     VARCHAR(50)  NOT NULL,
    telephone_client  VARCHAR(20)  NULL,
    adresse_client    VARCHAR(255) NULL,
    CONSTRAINT pk_client PRIMARY KEY (id_client)
) ENGINE = InnoDB;

-- FOURNISSEUR (RG10, RG11)
CREATE TABLE fournisseur (
    id_fournisseur         INT          NOT NULL AUTO_INCREMENT,
    raison_sociale         VARCHAR(100) NOT NULL,
    telephone_fournisseur  VARCHAR(20)  NOT NULL,
    email_fournisseur      VARCHAR(100) NULL,
    adresse_fournisseur    VARCHAR(255) NULL,
    ville_fournisseur      VARCHAR(50)  NULL,
    CONSTRAINT pk_fournisseur PRIMARY KEY (id_fournisseur)
) ENGINE = InnoDB;

-- VENTE (RG14, RG15) ; #id_client vient de ACHETER (R2 + R4 : NULL = client de passage)
--                       #id_utilisateur vient de EFFECTUER (R2)
CREATE TABLE vente (
    id_vente        INT       NOT NULL AUTO_INCREMENT,
    date_vente      DATETIME  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    id_client       INT       NULL,
    id_utilisateur  INT       NOT NULL,
    CONSTRAINT pk_vente PRIMARY KEY (id_vente),
    CONSTRAINT fk_vente_client FOREIGN KEY (id_client)
        REFERENCES client (id_client) ON DELETE SET NULL ON UPDATE CASCADE,          -- RG13
    CONSTRAINT fk_vente_utilisateur FOREIGN KEY (id_utilisateur)
        REFERENCES utilisateur (id_utilisateur) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB;

-- APPROVISIONNEMENT (RG21) ; #id_fournisseur vient de LIVRER, #id_utilisateur de ENREGISTRER (R2)
CREATE TABLE approvisionnement (
    id_approvisionnement    INT         NOT NULL AUTO_INCREMENT,
    date_approvisionnement  DATE        NOT NULL,
    reference_bon           VARCHAR(50) NULL,
    id_fournisseur          INT         NOT NULL,
    id_utilisateur          INT         NOT NULL,
    CONSTRAINT pk_approvisionnement PRIMARY KEY (id_approvisionnement),
    CONSTRAINT fk_appro_fournisseur FOREIGN KEY (id_fournisseur)
        REFERENCES fournisseur (id_fournisseur) ON DELETE RESTRICT ON UPDATE CASCADE, -- RG11
    CONSTRAINT fk_appro_utilisateur FOREIGN KEY (id_utilisateur)
        REFERENCES utilisateur (id_utilisateur) ON DELETE RESTRICT ON UPDATE CASCADE
) ENGINE = InnoDB;

-- ---------------------------------------------------------------------
--  2. Tables issues des associations (x,n)-(x,n) (règle de passage R3)
-- ---------------------------------------------------------------------

-- LIGNE_VENTE = association CONTENIR (RG16 à RG18)
CREATE TABLE ligne_vente (
    id_vente             INT           NOT NULL,
    id_medicament        INT           NOT NULL,
    quantite_vendue      INT           NOT NULL,
    prix_unitaire_vente  DECIMAL(10,2) NOT NULL,   -- prix appliqué au moment de la vente (RG18)
    CONSTRAINT pk_ligne_vente PRIMARY KEY (id_vente, id_medicament),
    CONSTRAINT chk_ligne_vente_qte  CHECK (quantite_vendue > 0),
    CONSTRAINT chk_ligne_vente_prix CHECK (prix_unitaire_vente > 0),
    CONSTRAINT fk_ligne_vente_vente FOREIGN KEY (id_vente)
        REFERENCES vente (id_vente) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_ligne_vente_medicament FOREIGN KEY (id_medicament)
        REFERENCES medicament (id_medicament) ON DELETE RESTRICT ON UPDATE CASCADE  -- RG09
) ENGINE = InnoDB;

-- LIGNE_APPROVISIONNEMENT = association COMPORTER (RG22, RG25)
CREATE TABLE ligne_approvisionnement (
    id_approvisionnement  INT           NOT NULL,
    id_medicament         INT           NOT NULL,
    quantite_recue        INT           NOT NULL,
    prix_achat_unitaire   DECIMAL(10,2) NOT NULL,
    numero_lot            VARCHAR(30)   NOT NULL,   -- ★ RG25
    date_peremption       DATE          NOT NULL,   -- ★ RG25
    CONSTRAINT pk_ligne_approvisionnement PRIMARY KEY (id_approvisionnement, id_medicament),
    CONSTRAINT chk_ligne_appro_qte  CHECK (quantite_recue > 0),
    CONSTRAINT chk_ligne_appro_prix CHECK (prix_achat_unitaire > 0),
    CONSTRAINT fk_ligne_appro_appro FOREIGN KEY (id_approvisionnement)
        REFERENCES approvisionnement (id_approvisionnement) ON DELETE CASCADE ON UPDATE CASCADE,
    CONSTRAINT fk_ligne_appro_medicament FOREIGN KEY (id_medicament)
        REFERENCES medicament (id_medicament) ON DELETE RESTRICT ON UPDATE CASCADE  -- RG09
) ENGINE = InnoDB;

-- Contrôlés par l'application Java, pas par le schéma :
--   RG16 au moins une ligne par vente, RG17 quantité vendue <= stock,
--   RG20 / RG23 mise à jour du stock dans la même transaction JDBC que la vente
--   ou l'approvisionnement, date_peremption postérieure à date_approvisionnement.

-- ---------------------------------------------------------------------
--  3. Vues : propriétés calculées et tableau de bord
-- ---------------------------------------------------------------------

-- montant_total d'une vente (propriété calculée, RG19)
CREATE VIEW v_vente_total AS
SELECT v.id_vente,
       v.date_vente,
       v.id_client,
       v.id_utilisateur,
       COALESCE(SUM(l.quantite_vendue * l.prix_unitaire_vente), 0) AS montant_total
FROM vente v
LEFT JOIN ligne_vente l ON l.id_vente = v.id_vente
GROUP BY v.id_vente, v.date_vente, v.id_client, v.id_utilisateur;

-- montant_approvisionnement (propriété calculée ★)
CREATE VIEW v_approvisionnement_total AS
SELECT a.id_approvisionnement,
       a.date_approvisionnement,
       a.reference_bon,
       a.id_fournisseur,
       COALESCE(SUM(l.quantite_recue * l.prix_achat_unitaire), 0) AS montant_approvisionnement
FROM approvisionnement a
LEFT JOIN ligne_approvisionnement l ON l.id_approvisionnement = a.id_approvisionnement
GROUP BY a.id_approvisionnement, a.date_approvisionnement, a.reference_bon, a.id_fournisseur;

-- Médicaments en stock faible (RG24)
CREATE VIEW v_stock_faible AS
SELECT id_medicament, nom_medicament, quantite_stock, seuil_minimal
FROM medicament
WHERE quantite_stock < seuil_minimal;

-- ★ Lots proches de la péremption : 30 jours ou moins, médicament encore en stock (RG26)
CREATE VIEW v_peremption_proche AS
SELECT m.id_medicament,
       m.nom_medicament,
       l.numero_lot,
       l.date_peremption,
       DATEDIFF(l.date_peremption, CURDATE()) AS jours_restants
FROM ligne_approvisionnement l
JOIN medicament m ON m.id_medicament = l.id_medicament
WHERE m.quantite_stock > 0
  AND l.date_peremption <= DATE_ADD(CURDATE(), INTERVAL 30 DAY);

-- Indicateurs du tableau de bord (RG27)
CREATE VIEW v_tableau_de_bord AS
SELECT (SELECT COUNT(*) FROM medicament)  AS nb_medicaments,
       (SELECT COUNT(*) FROM client)      AS nb_clients,
       (SELECT COUNT(*) FROM fournisseur) AS nb_fournisseurs,
       (SELECT COUNT(*) FROM vente)       AS nb_ventes,
       (SELECT COALESCE(SUM(quantite_vendue * prix_unitaire_vente), 0) FROM ligne_vente) AS chiffre_affaires;

-- ---------------------------------------------------------------------
--  4. Jeu de données de démonstration
--     Comptes : admin / admin123 (ADMIN), pharmacien / pharma123 (PHARMACIEN),
--               ancien / ancien123 (compte désactivé, pour tester RG04)
--     Les dates sont relatives au jour d'exécution du script, pour que les
--     alertes de péremption s'affichent toujours pendant la démonstration.
--     Les stocks valent exactement : quantités reçues - quantités vendues.
-- ---------------------------------------------------------------------

INSERT INTO utilisateur (id_utilisateur, login, mot_de_passe, nom_complet, role, actif) VALUES
  (1, 'admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9', 'Administrateur Pharmacie', 'ADMIN', 1),
  (2, 'pharmacien', '1c7d7f2a7668a1de0ea8f04a0ce6ff072e14781b052c51ee506a41b05d28b5cb', 'Pharmacien Comptoir', 'PHARMACIEN', 1),
  (3, 'ancien', 'edace8c66d6a063236d56ed611ae2ade13aa141b0ec785272408523e14461134', 'Ancien Employé', 'PHARMACIEN', 0);

INSERT INTO categorie (id_categorie, libelle_categorie) VALUES
  (1, 'Antalgiques'),
  (2, 'Antibiotiques'),
  (3, 'Anti-inflammatoires'),
  (4, 'Vitamines'),
  (5, 'Antihistaminiques'),
  (6, 'Gastro-entérologie'),
  (7, 'Voies respiratoires');

INSERT INTO medicament (id_medicament, nom_medicament, forme, dosage, prix_vente, quantite_stock, seuil_minimal, id_categorie) VALUES
  (1, 'Paracétamol', 'Comprimé', '500 mg', 12.50, 94, 20, 1),
  (2, 'Ibuprofène', 'Comprimé', '400 mg', 18.00, 59, 15, 3),
  (3, 'Amoxicilline', 'Gélule', '1 g', 45.00, 8, 10, 2),
  (4, 'Vitamine C', 'Comprimé effervescent', '1000 mg', 30.00, 38, 10, 4),
  (5, 'Cétirizine', 'Comprimé', '10 mg', 25.00, 22, 10, 5),
  (6, 'Oméprazole', 'Gélule', '20 mg', 38.00, 6, 8, 6),
  (7, 'Sirop contre la toux', 'Sirop', '150 ml', 35.00, 4, 5, 7),
  (8, 'Diclofénac', 'Gel', '1 %', 42.00, 8, 5, 3);

INSERT INTO client (id_client, nom_client, prenom_client, telephone_client, adresse_client) VALUES
  (1, 'Bennani', 'Youssef', '0600000001', 'Rue des Orangers, Marrakech'),
  (2, 'El Idrissi', 'Salma', '0600000002', 'Avenue Mohammed V, Marrakech'),
  (3, 'Ouazzani', 'Karim', '0600000003', NULL);

INSERT INTO fournisseur (id_fournisseur, raison_sociale, telephone_fournisseur, email_fournisseur, adresse_fournisseur, ville_fournisseur) VALUES
  (1, 'Atlas Pharma Distribution', '0522000001', 'contact@atlaspharma.example', 'Boulevard Zerktouni', 'Casablanca'),
  (2, 'MediSud Répartition', '0524000002', 'commandes@medisud.example', 'Zone industrielle Sidi Ghanem', 'Marrakech'),
  (3, 'Nord Santé Distribution', '0539000003', NULL, NULL, 'Tanger');

INSERT INTO approvisionnement (id_approvisionnement, date_approvisionnement, reference_bon, id_fournisseur, id_utilisateur) VALUES
  (1, DATE_SUB(CURDATE(), INTERVAL 30 DAY), 'BL-2026-001', 1, 1),
  (2, DATE_SUB(CURDATE(), INTERVAL 10 DAY), 'BL-2026-002', 2, 1);

INSERT INTO ligne_approvisionnement (id_approvisionnement, id_medicament, quantite_recue, prix_achat_unitaire, numero_lot, date_peremption) VALUES
  (1, 1, 100, 8.00, 'PAR-A1', DATE_ADD(CURDATE(), INTERVAL 2 YEAR)),
  (1, 2, 60, 12.00, 'IBU-A1', DATE_ADD(CURDATE(), INTERVAL 18 MONTH)),
  (1, 3, 30, 32.00, 'AMX-A1', DATE_ADD(CURDATE(), INTERVAL 1 YEAR)),
  (1, 4, 40, 20.00, 'VTC-A1', DATE_ADD(CURDATE(), INTERVAL 20 DAY)),
  (2, 5, 25, 16.00, 'CET-B1', DATE_ADD(CURDATE(), INTERVAL 2 YEAR)),
  (2, 6, 12, 26.00, 'OME-B1', DATE_ADD(CURDATE(), INTERVAL 1 YEAR)),
  (2, 7, 10, 24.00, 'SIR-B1', DATE_ADD(CURDATE(), INTERVAL 25 DAY)),
  (2, 8, 8, 30.00, 'DIC-B1', DATE_ADD(CURDATE(), INTERVAL 1 YEAR));

INSERT INTO vente (id_vente, date_vente, id_client, id_utilisateur) VALUES
  (1, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 7 DAY), '10:15:00'), 1, 2),
  (2, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 3 DAY), '16:40:00'), NULL, 2),
  (3, TIMESTAMP(DATE_SUB(CURDATE(), INTERVAL 1 DAY), '11:05:00'), 2, 1),
  (4, TIMESTAMP(CURDATE(), '09:30:00'), 3, 2);

INSERT INTO ligne_vente (id_vente, id_medicament, quantite_vendue, prix_unitaire_vente) VALUES
  (1, 1, 4, 12.50),
  (1, 4, 2, 30.00),
  (2, 3, 22, 45.00),
  (2, 2, 1, 18.00),
  (3, 6, 6, 38.00),
  (3, 7, 6, 35.00),
  (4, 5, 3, 25.00),
  (4, 1, 2, 12.50);

-- ---------------------------------------------------------------------
--  5. Vérifications (résultats attendus en commentaire)
-- ---------------------------------------------------------------------
-- SELECT * FROM v_tableau_de_bord;
--   -> 8 médicaments, 3 clients, 3 fournisseurs, 4 ventes, chiffre d'affaires 1656.00
-- SELECT * FROM v_stock_faible;
--   -> Amoxicilline (8 < 10), Oméprazole (6 < 8), Sirop contre la toux (4 < 5)
-- SELECT * FROM v_peremption_proche;
--   -> Vitamine C lot VTC-A1 (20 jours), Sirop contre la toux lot SIR-B1 (25 jours)
-- SELECT * FROM v_vente_total;
--   -> ventes 1 à 4 : 110.00, 1008.00, 438.00, 100.00
