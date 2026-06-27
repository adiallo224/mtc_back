--Ordre de création des tables dans la base de données
-------- **************** ----------
--->  Table type_assurance <---
CREATE TABLE type_assurance (
   id SERIAL PRIMARY KEY,
   type_assurance VARCHAR(255) NOT NULL UNIQUE,
   assu_pret BOOLEAN DEFAULT FALSE,
   assu_auto BOOLEAN DEFAULT FALSE,
   assu_mutuel_indiv BOOLEAN DEFAULT FALSE,
   assu_mutuel_pro BOOLEAN DEFAULT FALSE
);

--->  Table flux_data <---
CREATE TABLE flux_data (
   id SERIAL PRIMARY KEY,
   type_assurance_id BIGINT NOT NULL,
   entreprise_id BIGINT,
   date_creation TIMESTAMP,
   date_modification TIMESTAMP,
   FOREIGN KEY (type_assurance_id) REFERENCES type_assurance(id)
);

--->  Table personne <---
CREATE TABLE personne (
   id SERIAL PRIMARY KEY,
   civilite VARCHAR(255),
   nom VARCHAR(255),
   prenom VARCHAR(255),
   date_naissance VARCHAR(255),
   nom_voie VARCHAR(255),
   numero_voie VARCHAR(255),
   code_postal VARCHAR(255),
   ville VARCHAR(255),
   pays VARCHAR(255),
   telephone VARCHAR(255),
   email VARCHAR(255),
   regime VARCHAR(255),
   nationalite VARCHAR(255),
   statut_profession VARCHAR(255),
   profession_specifique VARCHAR(255),
   profession VARCHAR(255),
   flux_data_id BIGINT NOT NULL,
   FOREIGN KEY (flux_data_id) REFERENCES flux_data(id)
);

--->  Table enfant <---
CREATE TABLE enfant (
   id SERIAL PRIMARY KEY,
   civilite VARCHAR(255),
   nom VARCHAR(255),
   prenom VARCHAR(255),
   date_naissance VARCHAR(255),
   flux_data_id BIGINT NOT NULL,
   FOREIGN KEY (flux_data_id) REFERENCES flux_data(id)
);

--->  Table pret <---
CREATE TABLE pret (
   id SERIAL PRIMARY KEY,
   objet VARCHAR(255),
   banque VARCHAR(255),
   type VARCHAR(255),
   montant_pret VARCHAR(255),
   duree VARCHAR(255),
   differe VARCHAR(255),
   duree_differe VARCHAR(255),
   type_taux VARCHAR(255),
   taux VARCHAR(255),
   nouveau_ou_reprise VARCHAR(255),
   date_effet VARCHAR(255),
   duree_amort VARCHAR(255),
   flux_data_id BIGINT NOT NULL,
   FOREIGN KEY (flux_data_id) REFERENCES flux_data(id)
);

--->  Table info_assure_complet <---
CREATE TABLE info_assure_complet (
   id SERIAL PRIMARY KEY,
   travail_manuel BOOLEAN,
   travail_manuel_manu_lourde BOOLEAN,
   travail_hauteur BOOLEAN,
   produit_danger BOOLEAN,
   metier_expose BOOLEAN,
   sport_risque BOOLEAN,
   fumeur BOOLEAN,
   fumeur_elec_nico BOOLEAN,
   fumeur_sans_nico BOOLEAN,
   deplacement_pro20000 BOOLEAN,
   deplacement_etranger60 BOOLEAN,
   deplacement_pays_risque BOOLEAN,
   instrument_precis BOOLEAN,
   garantie VARCHAR(255),
   quotite_deces VARCHAR(255),
   quotite VARCHAR(255),
   garantie_chomage BOOLEAN,
   hauteur VARCHAR(255),
   flux_data_id BIGINT NOT NULL,
   FOREIGN KEY (flux_data_id) REFERENCES flux_data(id)
);

--->  Table tarif <---
CREATE TABLE tarif (
   id SERIAL PRIMARY KEY,
   nom VARCHAR(255),
   montant VARCHAR(255),
   type_assurance_id BIGINT,
   execution BOOLEAN,
   erreur TEXT,
   etape VARCHAR(255),
   capture_img_path VARCHAR(255),
   capture_img_erreur_path VARCHAR(255),
   temp_id VARCHAR(255),
   flux_data_id BIGINT,
   FOREIGN KEY (type_assurance_id) REFERENCES type_assurance(id),
   FOREIGN KEY (flux_data_id) REFERENCES flux_data(id)
);

--->  Table parametre_general <---
CREATE TABLE parametre_general (
   id SERIAL PRIMARY KEY,
   name VARCHAR(255) UNIQUE DEFAULT 'unique_parametre_general',
   dossier_image VARCHAR(255),
   mode_recherche VARCHAR(255),
   heure_purge TIME,
   nb_jours INT
);

--->  Table entreprise <---
CREATE TABLE entreprise (
   id SERIAL PRIMARY KEY,
   nom VARCHAR(255) NOT NULL,
   siret VARCHAR(255) UNIQUE,
   adresse VARCHAR(255),
   code_postal VARCHAR(255),
   ville VARCHAR(255),
   pays VARCHAR(255),
   telephone VARCHAR(255),
   email VARCHAR(255),
   site_web VARCHAR(255),
   date_creation TIMESTAMP,
   date_modification TIMESTAMP,
   flux_data_id BIGINT NOT NULL,
   FOREIGN KEY (flux_data_id) REFERENCES flux_data(id)
);

--->  Table info_assure_complet <---
CREATE TABLE info_assure_complet (
   id SERIAL PRIMARY KEY,
   travail_manuel BOOLEAN,
   travail_manuel_manu_lourde BOOLEAN,
   travail_hauteur BOOLEAN,
   produit_danger BOOLEAN,
   metier_expose BOOLEAN,
   sport_risque BOOLEAN,
   fumeur BOOLEAN,
   fumeur_elec_nico BOOLEAN,
   fumeur_sans_nico BOOLEAN,
   deplacement_pro20000 BOOLEAN,
   deplacement_etranger60 BOOLEAN,
   deplacement_pays_risque BOOLEAN,
   instrument_precis BOOLEAN,
   garantie VARCHAR(255),
   quotite_deces VARCHAR(255),
   quotite VARCHAR(255),
   garantie_chomage BOOLEAN,
   hauteur VARCHAR(255),
   flux_data_id BIGINT NOT NULL,
   FOREIGN KEY (flux_data_id) REFERENCES flux_data(id)
);


ALTER TABLE public.compte ADD COLUMN niveau integer default 0;
ALTER TABLE public.compte ADD COLUMN ordre integer;
ALTER TABLE public.parametre_general ADD COLUMN navigateur_playwright VARCHAR(50);


