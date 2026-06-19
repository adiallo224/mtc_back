navigation:
  activity: ".rubrique-list-card--sante-individuelle"
  offer: "#sante-select-senior"

  # Bouton "Tarifer l'offre" dans la carte Senior
  pricing_button: "#sante-select-senior button"

form:
  replace_contract: ".sel-c-toggle-form-element"

  date_effet: "#dateEffet"

  civilite:
    selector: "#civilite"
    options:
      MONSIEUR: "Monsieur"
      MADAME: "Madame"

  nom: "#nom"

  prenom: "#prenom"

  birth_date: "#date-naissance-adherent"

  csp:
    selector: "#categories-socio-professionnelles-adherent"
    options:
      AGRICULTEURS_EXPLOITANTS: "Agriculteurs exploitants"
      ARTISANS: "Artisans"
      CADRES: "Cadres"
      CADRES_ET_EMPLOYES_DE_LA_FONCTION_PUBLIQUE: "Cadres et employés de la fonction publique"
      CHEFS_D_ENTREPRISE: "Chefs d'entreprise"
      COMMERCANTS_ET_ASSIMILES: "Commerçants et assimilés"
      EMPLOYES_AGENTS_DE_MAITRISE: "Employés, agents de maîtrise"
      OUVRIERS: "Ouvriers"
      PERSONNES_SANS_ACTIVITE_PROFESSIONNELLE: "Personnes sans activité professionnelle"
      PROFESSIONS_LIBERALES_ET_ASSIMILES: "Professions libérales et assimilés"
      RETRAITES: "Retraités"

  regime:
    selector: "#regime-obligatoire-adherent"
    options:
      ALSACE_MOSELLE: "Alsace / Moselle"
      AMEXA: "Amexa"
      REGIME_SALARIES_AGRICOLES: "Régime des salariés agricoles"
      SECURITE_SOCIALE: "Sécurité sociale"
      SECURITE_SOCIALE_INDEPENDANTS: "Sécurité sociale des indépendants"

  zipcode: ".sel-c-form-profile-prospect__zip-code"

  conjoint_toggle: ".sel-c-section-conjoint"

  conjoint_birth_date: ".sel-c-form-profile-conjoint__birth-date"

  conjoint_regime:
    selector: "#regime-obligatoire-conjoint"
    options:
      ALSACE_MOSELLE: "Alsace / Moselle"
      AMEXA: "Amexa"
      REGIME_SALARIES_AGRICOLES: "Régime des salariés agricoles"
      SECURITE_SOCIALE: "Sécurité sociale"
      SECURITE_SOCIALE_INDEPENDANTS: "Sécurité sociale des indépendants"

  enfant_add: ".sel-c-enfants-content__button-wrapper button"

  enfant_regime:
    selector_pattern: "#regime-obligatoire-enfant-{index}"
    options:
      ALSACE_MOSELLE: "Alsace / Moselle"
      AMEXA: "Amexa"
      REGIME_SALARIES_AGRICOLES: "Régime des salariés agricoles"
      SECURITE_SOCIALE: "Sécurité sociale"
      SECURITE_SOCIALE_INDEPENDANTS: "Sécurité sociale des indépendants"

  submit: ".sel-c-form-profile__submit-button"
  