package com.mtc.mutuaConseil.services.servicesImpl.assuPret;

import com.mtc.mutuaConseil.base.BasePlaywrightService;
import com.mtc.mutuaConseil.models.Compte;
import com.mtc.mutuaConseil.models.FluxData;
import com.mtc.mutuaConseil.models.Tarif;
import com.mtc.mutuaConseil.services.LaunchedService;
import com.mtc.mutuaConseil.services.servicesImpl.TypeAssuranceService;
import com.mtc.mutuaConseil.utils.TarifUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApiviaPretPlayWrightService extends BasePlaywrightService implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(ApiviaPretPlayWrightService.class);

    private final TypeAssuranceService typeAssuranceService;

    public ApiviaPretPlayWrightService(TypeAssuranceService typeAssuranceService){
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        log.info("Debut de traitement -- Apivia");
        Tarif tarifApivia = TarifUtils.createDefaultTarif(c, typeAssuranceService, 1L);

        try {
            // Initialisation simplifiée - elementLib est initialisé automatiquement
            initializeBrowser(false);
            // Navigation
            humanLikeNavigate(c.getUrlFournisseur());

            informationsGenerales(flux);

            waitThread(2);
            next(1);
//            WebElement buttonEtapeSuivant = waitForElement1(driver, By.xpath("/html/body/div[1]/form/div[2]/div[3]/button"), 20,2);
//            buttonEtapeSuivant.click();

            informationsPersonnes(flux);
            waitThread(2);
            next(2);
//            WebElement buttonEtapeSuivant1 = waitForElement(driver, By.xpath("/html/body/div[1]/form/div[3]/div[3]/button"), 20, 2);
//            buttonEtapeSuivant1.click();

            informationsPret(flux);
            waitThread(2);
            next(3);
//            WebElement buttonEtapeSuivant2 = waitForElement(driver, By.xpath("/html/body/div[1]/form/div[4]/div[3]/button"), 20, 2);
//            buttonEtapeSuivant2.click();

            informationsComplementaires(flux);
            waitThread(30);
            elementLib.clickByXpath("//button[@id=\"demande_form_comparer\"]");

            String cout = "";
            String montantSelector = "div.panel-body span.h3.infoTarifMens";

            // Attendre que les éléments soient présents
            elementLib.waitForElement(montantSelector, 30);

            // Vérifier si des éléments sont présents
            if (elementLib.isElementPresent(montantSelector)) {
                // Récupérer le texte du premier élément
                cout = elementLib.getText(montantSelector);
                log.info("Montant récupéré : " + cout);
                tarifApivia.setMontant(cout);

                String screenshotBytes = captureScreenshot(tarifApivia.getNom(), false, tarifApivia);
                if (screenshotBytes != null) {
                    tarifApivia.setCaptureImg(screenshotBytes);
                }
                tarifApivia.setExecution(true);
            } else {
                log.info("Aucun élément correspondant trouvé.");
            }
        } catch (Exception e) {
            log.error("An error occurred", e);
            tarifApivia.setErreur(e.getMessage());
            String screenshotBytesErreur = captureScreenshot(tarifApivia.getNom(), true, tarifApivia);
            tarifApivia.setCaptureImgErreur(screenshotBytesErreur);
            tarifApivia.setEtape("");
        } finally {
            cleanup();
        }
        return tarifApivia;
    }

    public void next(int page) {
        try {
            // Déterminer le sélecteur selon la page
            String buttonSelector = null;
            String expectedText = null;

            switch (page) {
                case 1:
                    expectedText = "Etape suivante : Vous";
                    // Utiliser un sélecteur CSS plus spécifique
                    buttonSelector = "button[type='button']:has-text('Etape suivante : Vous'), button:has-text('Etape suivante : Vous')";
                    break;
                case 2:
                    expectedText = "Etape suivante : Les prêts à assurer";
                    buttonSelector = "button[type='button']:has-text('Etape suivante : Les prêts à assurer'), button:has-text('Etape suivante : Les prêts à assurer')";
                    break;
                case 3:
                    expectedText = "Etape suivante : Comment vous joindre";
                    buttonSelector = "button[type='button']:has-text('Etape suivante : Comment vous joindre'), button:has-text('Etape suivante : Comment vous joindre')";
                    break;
                default:
                    log.warn("Page {} non reconnue pour la navigation", page);
                    return;
            }

            // Attendre que le bouton soit présent et visible
            elementLib.waitForElement(buttonSelector, 20);

            // Vérifier que le bouton est bien présent
            if (elementLib.isElementPresent(buttonSelector)) {
                elementLib.click(buttonSelector);
                log.info("Bouton '{}' cliqué avec succès", expectedText);
            } else {
                log.warn("Bouton '{}' non trouvé", expectedText);
            }

        } catch (Exception e) {
            log.error("Erreur lors de la navigation vers l'étape suivante pour la page {}", page, e);
            throw new RuntimeException("Échec de la navigation vers l'étape suivante", e);
        }
    }

    private void informationsGenerales(FluxData flux) {
        elementLib.clickByXpath("/html/body/div[1]/form/div[2]/div[2]/div[2]/div/div/label[1]");
        choixObjetPret(flux);
        choixBanque(flux);
    }

    private void informationsPersonnes(FluxData flux) {
        int index = 0;
        waitThread(2);
        if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur")) {
            elementLib.clickByXpath("//div[@id=\"demande_form_personnes_0_titre\"]//label[ contains(text(), 'M.')]");
        } else if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame")){
            elementLib.clickByXpath("//div[@id=\"demande_form_personnes_0_titre\"]//label[ contains(text(), 'Mme')]");
        }

        elementLib.typeById("demande_form_personnes_0_prenom", flux.getPersonnes().get(index).getPrenom());
        elementLib.typeById("demande_form_personnes_0_nom", flux.getPersonnes().get(index).getNom());
        elementLib.typeByXpath("//input[@id='demande_form_personnes_0_datenaissance']", flux.getPersonnes().get(index).getDateNaissance());
        waitThread(2);
        elementLib.typeById("demande_form_personnes_0_villenaissance", flux.getPersonnes().get(index).getVille());
        elementLib.pressEnter();
        choixFumeur(flux, index);
        choixProfession(flux, index);
        choixManutention(flux, index);
        choixDeplacementPro(flux, index);
        choixTravailHauteur(flux, index);
        choixTravailRisque(flux, index);
        if (flux.getPersonnes().size() == 2) {
            index = 1;
            elementLib.clickByXpath("//button[contains(@class, 'btn-ajoutPersonne')]");
            waitThread(1);
            if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur")) {
                elementLib.clickByXpath("//div[@id=\"demande_form_personnes_1_titre\"]//label[ contains(text(), 'M.')]");
            } else if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame")){
                elementLib.clickByXpath("//div[@id=\"demande_form_personnes_1_titre\"]//label[ contains(text(), 'Mme')]");
            }
            elementLib.typeById("demande_form_personnes_1_prenom", flux.getPersonnes().get(index).getPrenom());
            elementLib.typeById("demande_form_personnes_1_nom", flux.getPersonnes().get(index).getNom());
            elementLib.typeByXpath("//input[@id='demande_form_personnes_1_datenaissance']", flux.getPersonnes().get(index).getDateNaissance());
            waitThread(2);
            elementLib.typeById("demande_form_personnes_1_villenaissance", flux.getPersonnes().get(index).getVille());
            elementLib.pressEnter();
            choixFumeur(flux, index);
            choixProfession(flux, index);
            choixManutention(flux, index);
            choixDeplacementPro(flux, index);
            choixTravailHauteur(flux, index);
            choixTravailRisque(flux, index);
        }
    }

    private void informationsPret(FluxData flux) {
        int index = 0;
        elementLib.typeByXpath("demande_form_prets_0_montant", flux.getPrets().get(index).getMontantPret());
        elementLib.typeByXpath("demande_form_prets_0_taux", flux.getPrets().get(index).getTaux());
        elementLib.typeByXpath("demande_form_prets_0_duree", flux.getPrets().get(index).getDuree());
        if (flux.getPrets().get(index).getType().equalsIgnoreCase("Amortissable")) {
            choixTypePret(flux, index);
        }
        if (flux.getPrets().size() == 2) {
            index = 1;
            waitThread(2);
            elementLib.clickByXpath("//button[contains(@class, 'btn-ajoutPret')]");
            elementLib.typeById("demande_form_prets_1_montant", flux.getPrets().get(index).getMontantPret());
            elementLib.typeById("demande_form_prets_1_taux", flux.getPrets().get(index).getTaux());
            elementLib.typeById("demande_form_prets_1_duree", flux.getPrets().get(index).getDuree());
            if (flux.getPrets().get(index).getType().equalsIgnoreCase("Amortissable")) {
                choixTypePret(flux, index);
            }
        }
        elementLib.typeById("demande_form_fraiscourtage", "15");
    }

    private void informationsComplementaires(FluxData flux) {
        int index = 0;
        elementLib.typeById("demande_form_personnes_0_adresse1", flux.getPersonnes().get(index).getNumeroVoie() + " " + flux.getPersonnes().get(index).getNomVoie());
//        WebElement inputComplement = waitForElement(driver, By.id("demande_form_personnes_0_adresse2"), 20, 1);
//        inputComplement.sendKeys("Bravo");
        elementLib.typeById("demande_form_personnes_0_cp", flux.getPersonnes().get(index).getCodePostal());
        elementLib.typeById("demande_form_personnes_0_ville", flux.getPersonnes().get(index).getVille());
        elementLib.typeById("demande_form_personnes_0_email", flux.getPersonnes().get(index).getEmail());
        elementLib.typeById("demande_form_personnes_0_tel1", flux.getPersonnes().get(index).getTelephone());
        if (flux.getPersonnes().size() == 2) {
            index = 1;
            elementLib.typeById("demande_form_personnes_1_adresse1",flux.getPersonnes().get(index).getNumeroVoie() + " " + flux.getPersonnes().get(index).getNomVoie());
            elementLib.typeById("demande_form_personnes_1_adresse2", "");
            elementLib.typeById("demande_form_personnes_1_cp", flux.getPersonnes().get(index).getCodePostal());
            elementLib.typeById("demande_form_personnes_1_ville", flux.getPersonnes().get(index).getVille());
            elementLib.typeById("demande_form_personnes_1_email", flux.getPersonnes().get(index).getEmail());
            elementLib.typeById("demande_form_personnes_1_tel1", flux.getPersonnes().get(index).getTelephone());
        }
        choixValidationSecumut(flux);
//        try {
//            recaptchaTest(driver, "");
//        } catch (Exception e){
//            log.error(String.valueOf(e));
//        }
//        waitThread(2);
//        WebElement buttonComparer = waitForElement(driver, By.id("demande_form_comparer"), 20, 1);
//        actions.moveToElement(buttonComparer).click().perform();
    }

    private void choixValidationSecumut(FluxData flux) {
        elementLib.clickById("consentementPersonne0");
        if (flux.getPersonnes().size() == 2) {
            elementLib.clickById("consentementPersonne1");
        }
        elementLib.clickByXpath("//div[@id='demande_form_consentementcontact']//label[@for='demande_form_consentementcontact_1']");
    }

    private void choixBanque(FluxData flux) {
        try {
            // Sélecteur CSS pour le dropdown banque
            String dropdownSelector = "#demande_form_preteur";

            // Attendre que l'élément soit présent et visible
            elementLib.waitForElement(dropdownSelector, 20);

            // Vérifier que l'élément est bien présent
            if (!elementLib.isElementPresent(dropdownSelector)) {
                log.warn("L'élément dropdown banque n'est pas présent");
                return;
            }

            // Vérifier que les données de prêt sont disponibles
            if (flux.getPrets() == null || flux.getPrets().isEmpty()) {
                log.warn("Aucune donnée de prêt disponible");
                return;
            }

            // Récupérer le nom de la banque
            String banque = flux.getPrets().get(0).getBanque();

            // Déterminer l'option à sélectionner selon la logique métier
            String optionToSelect = null;

            if (banque.equalsIgnoreCase("BNP Paribas")) {
                optionToSelect = "BNP Paribas";
            } else if (banque.equalsIgnoreCase("Banque populaire")) {
                optionToSelect = "Banque Populaire Auvergne Rhône Alpes";
            } else if (banque.equalsIgnoreCase("Axa banque")) {
                optionToSelect = "Axa Banque";
            } else if (banque.equalsIgnoreCase("Caisse d\\'épargne")) {
                optionToSelect = "Caisse d'Epargne Rhône Alpes";
            } else if (banque.equalsIgnoreCase("Crédit agricole")) {
                optionToSelect = "Crédit Agricole Sud Rhône Alpes";
            } else if (banque.equalsIgnoreCase("CIC")) {
                optionToSelect = "CIC";
            } else if (banque.equalsIgnoreCase("Crédit mutuel")) {
                optionToSelect = "Crédit Mutuel Savoie Mont-Blanc";
            } else if (banque.equalsIgnoreCase("LCL")) {
                optionToSelect = "LCL";
            } else if (banque.equalsIgnoreCase("Société Générale")) {
                optionToSelect = "SG Societe Generale";
            } else if (banque.equalsIgnoreCase("BoursoBank")) {
                optionToSelect = "BoursoBank";
            } else if (banque.equalsIgnoreCase("La Banque Postale")) {
                optionToSelect = "La Banque Postale";
            }

            // Sélectionner l'option si elle a été déterminée
            if (optionToSelect != null) {
                elementLib.selectByVisibleText2(dropdownSelector, optionToSelect);
                log.info("Banque sélectionnée : {}", optionToSelect);
            } else {
                log.warn("Aucune option correspondante trouvée pour la banque : {}", banque);
            }

        } catch (Exception e) {
            log.error("Erreur lors de la sélection de la banque", e);
            throw new RuntimeException("Échec de la sélection de la banque", e);
        }
    }

    private void choixObjetPret(FluxData flux) {
        try {
            String dropdownSelector = "#demande_form_objetprojet";

            // Attendre que l'élément soit présent et visible
            elementLib.waitForElement(dropdownSelector, 30);

            // Vérifier que l'élément est bien présent
            if (!elementLib.isElementPresent(dropdownSelector)) {
                log.warn("L'élément dropdown objetprojet n'est pas présent");
                return;
            }

            // Récupérer l'objet du prêt
            String objetPret = flux.getPrets().get(0).getObjet();

            // Logique de sélection basée sur l'objet du prêt
            String optionToSelect = null;

            if (objetPret.equalsIgnoreCase("Résidence principale")) {
                optionToSelect = "Résidence principale primo accédant";
            }
            else if (objetPret.equalsIgnoreCase("Résidence secondaire") ||
                    objetPret.equalsIgnoreCase("Autre immobilier")) {
                optionToSelect = "Résidence secondaire ou autre bien immobilier";
            }
            else if (objetPret.equalsIgnoreCase("Investissement locatif")) {
                optionToSelect = "Investissement locatif";
            }
            else if (objetPret.equalsIgnoreCase("Prêt à objet professionnel")) {
                optionToSelect = "Prêts professionnels";
            }

            // Sélectionner l'option si elle a été déterminée
            if (optionToSelect != null) {
                // Utiliser la méthode robuste pour la sélection
                elementLib.selectByVisibleText2(dropdownSelector, optionToSelect);
                log.info("Option sélectionnée : {}", optionToSelect);
            } else {
                log.warn("Aucune option correspondante trouvée pour l'objet : {}", objetPret);
            }

        } catch (Exception e) {
            log.error("Erreur lors de la sélection de l'objet du prêt", e);
            throw new RuntimeException("Échec de la sélection de l'objet du prêt", e);
        }
    }

    private void choixFumeur(FluxData flux, int index) {
        try {
            // Construire le sélecteur CSS selon l'index
            String dropdownSelector = String.format("#demande_form_personnes_%d_fumeur", index);

            // Attendre que l'élément soit présent et visible
            elementLib.waitForElement(dropdownSelector, 20);

            // Vérifier que l'élément est bien présent
            if (!elementLib.isElementPresent(dropdownSelector)) {
                log.warn("L'élément dropdown fumeur pour l'index {} n'est pas présent", index);
                return;
            }

            // Vérifier que l'index est valide
            if (flux.getInfoAssureComplets() == null ||
                    index >= flux.getInfoAssureComplets().size()) {
                log.warn("Index {} invalide ou InfoAssureComplets non disponible", index);
                return;
            }

            // Récupérer le statut fumeur de la personne
            boolean estFumeur = flux.getInfoAssureComplets().get(index).getFumeur();

            // Déterminer l'option à sélectionner
            String optionToSelect = estFumeur ? "Fumeur" : "Non fumeur";

            // Vérifier que l'option existe avant de la sélectionner
            List<String> availableOptions = elementLib.getSelectOptions(dropdownSelector);
            boolean optionExists = availableOptions.stream()
                    .anyMatch(option -> option.equalsIgnoreCase(optionToSelect));

            if (!optionExists) {
                log.warn("L'option '{}' n'existe pas dans le dropdown. Options disponibles : {}",
                        optionToSelect, availableOptions);
                return;
            }

            // Sélectionner l'option
            elementLib.selectByVisibleText2(dropdownSelector, optionToSelect);
            log.info("Option sélectionnée pour l'index {} : {}", index, optionToSelect);

        } catch (Exception e) {
            log.error("Erreur lors de la sélection du statut fumeur pour l'index {}", index, e);
            throw new RuntimeException("Échec de la sélection du statut fumeur", e);
        }
    }

    private void choixProfession(FluxData flux, int index) {
        try {
            // Construire le sélecteur CSS selon l'index
            String dropdownSelector = String.format("#demande_form_personnes_%d_situationpro", index);

            // Attendre que l'élément soit présent et visible
            elementLib.waitForElement(dropdownSelector, 20);

            // Vérifier que l'élément est bien présent
            if (!elementLib.isElementPresent(dropdownSelector)) {
                log.warn("L'élément dropdown profession pour l'index {} n'est pas présent", index);
                return;
            }

            // Vérifier que l'index est valide
            if (flux.getPersonnes() == null ||
                    index >= flux.getPersonnes().size()) {
                log.warn("Index {} invalide ou Personnes non disponible", index);
                return;
            }

            // Récupérer la profession spécifique
            String professionSpecifique = flux.getPersonnes().get(index).getProfessionSpecifique();

            // Déterminer l'option à sélectionner selon la logique métier
            String optionToSelect = null;

            if (professionSpecifique.equalsIgnoreCase("Commerçant")) {
                optionToSelect = "Commerçant";
            } else if (professionSpecifique.equalsIgnoreCase("Artisan")) {
                optionToSelect = "Artisan";
            } else if (professionSpecifique.equalsIgnoreCase("Agriculteur")) {
                optionToSelect = "Profession agricole";
            } else if (professionSpecifique.equalsIgnoreCase("Profession libérale")) {
                optionToSelect = "Profession libérale (hors paramédical)";
            } else if (professionSpecifique.equalsIgnoreCase("Fonctionnaire classe a")) {
                optionToSelect = "Fonctionnaire catégorie A";
            } else if (professionSpecifique.equalsIgnoreCase("Ouvrier")) {
                optionToSelect = "Ouvrier";
            } else if (professionSpecifique.equalsIgnoreCase("Salarié cadre") ||
                    professionSpecifique.equalsIgnoreCase("Chef d\\'entreprise")) {
                optionToSelect = "Cadre / Chef d'entreprise (hors BTP)";
            } else if (professionSpecifique.equalsIgnoreCase("Salarié non cadre : employé")) {
                optionToSelect = "Employé (hors paramédical)";
            }

            // Sélectionner l'option si elle a été déterminée
            if (optionToSelect != null) {
                elementLib.selectByVisibleText2(dropdownSelector, optionToSelect);
                log.info("Option sélectionnée pour l'index {} : {}", index, optionToSelect);
            } else {
                log.warn("Aucune option correspondante trouvée pour la profession : {}", professionSpecifique);
            }

        } catch (Exception e) {
            log.error("Erreur lors de la sélection de la profession pour l'index {}", index, e);
            throw new RuntimeException("Échec de la sélection de la profession", e);
        }
    }

    private void choixManutention(FluxData flux, int index) {
        try {
            // Construire le sélecteur CSS selon l'index
            String dropdownSelector = String.format("#demande_form_personnes_%d_manutention", index);

            // Attendre que l'élément soit présent et visible
            elementLib.waitForElement(dropdownSelector, 20);

            // Vérifier que l'élément est bien présent
            if (!elementLib.isElementPresent(dropdownSelector)) {
                log.warn("L'élément dropdown manutention pour l'index {} n'est pas présent", index);
                return;
            }

            // Vérifier que l'index est valide
            if (flux.getInfoAssureComplets() == null ||
                    index >= flux.getInfoAssureComplets().size()) {
                log.warn("Index {} invalide ou InfoAssureComplets non disponible", index);
                return;
            }

            // Récupérer les informations de manutention
            boolean travailManuel = flux.getInfoAssureComplets().get(index).getTravailManuel();
            boolean travailManuelManuLourde = flux.getInfoAssureComplets().get(index).getTravailManuelManuLourde();

            // Déterminer l'option à sélectionner selon la logique métier
            String optionToSelect;
            if (travailManuel) {
                optionToSelect = "Légère";
            } else if (travailManuelManuLourde) {
                optionToSelect = "Lourde ou avec outillage";
            } else {
                optionToSelect = "Pas de manutention";
            }

            // Vérifier que l'option existe avant de la sélectionner
            List<String> availableOptions = elementLib.getSelectOptions(dropdownSelector);
            boolean optionExists = availableOptions.stream()
                    .anyMatch(option -> option.equalsIgnoreCase(optionToSelect));

            if (!optionExists) {
                log.warn("L'option '{}' n'existe pas dans le dropdown. Options disponibles : {}",
                        optionToSelect, availableOptions);
                return;
            }

            // Sélectionner l'option
            elementLib.selectByVisibleText2(dropdownSelector, optionToSelect);
            log.info("Option sélectionnée pour l'index {} : {}", index, optionToSelect);

        } catch (Exception e) {
            log.error("Erreur lors de la sélection de la manutention pour l'index {}", index, e);
            throw new RuntimeException("Échec de la sélection de la manutention", e);
        }
    }

    private void choixDeplacementPro(FluxData flux, int index) {
        try {
            // Construire le sélecteur CSS selon l'index
            String dropdownSelector = String.format("#demande_form_personnes_%d_deplacements", index);

            // Attendre que l'élément soit présent et visible
            elementLib.waitForElement(dropdownSelector, 20);

            // Vérifier que l'élément est bien présent
            if (!elementLib.isElementPresent(dropdownSelector)) {
                log.warn("L'élément dropdown déplacement professionnel pour l'index {} n'est pas présent", index);
                return;
            }

            // Vérifier que l'index est valide
            if (flux.getInfoAssureComplets() == null ||
                    index >= flux.getInfoAssureComplets().size()) {
                log.warn("Index {} invalide ou InfoAssureComplets non disponible", index);
                return;
            }

            // Récupérer l'information sur les déplacements professionnels
            boolean deplacementPro20000 = flux.getInfoAssureComplets().get(index).getDeplacementPro20000();

            // Déterminer l'option à sélectionner selon la logique métier
            String optionToSelect;
            if (deplacementPro20000) {
                optionToSelect = "Plus de 30 000 Km";
            } else {
                optionToSelect = "De 15 000 Km à 30 000 Km";
            }

            // Vérifier que l'option existe avant de la sélectionner
            List<String> availableOptions = elementLib.getSelectOptions(dropdownSelector);
            boolean optionExists = availableOptions.stream()
                    .anyMatch(option -> option.equalsIgnoreCase(optionToSelect));

            if (!optionExists) {
                log.warn("L'option '{}' n'existe pas dans le dropdown. Options disponibles : {}",
                        optionToSelect, availableOptions);
                return;
            }

            // Sélectionner l'option
            elementLib.selectByVisibleText2(dropdownSelector, optionToSelect);
            log.info("Option sélectionnée pour l'index {} : {}", index, optionToSelect);

        } catch (Exception e) {
            log.error("Erreur lors de la sélection du déplacement professionnel pour l'index {}", index, e);
            throw new RuntimeException("Échec de la sélection du déplacement professionnel", e);
        }
    }

    private void choixTravailHauteur(FluxData flux, int index) {
        try {
            // Construire le sélecteur CSS selon l'index
            String dropdownSelector = String.format("#demande_form_personnes_%d_hauteur", index);

            // Attendre que l'élément soit présent et visible
            elementLib.waitForElement(dropdownSelector, 20);

            // Vérifier que l'élément est bien présent
            if (!elementLib.isElementPresent(dropdownSelector)) {
                log.warn("L'élément dropdown travail en hauteur pour l'index {} n'est pas présent", index);
                return;
            }

            // Vérifier que l'index est valide
            if (flux.getInfoAssureComplets() == null ||
                    index >= flux.getInfoAssureComplets().size()) {
                log.warn("Index {} invalide ou InfoAssureComplets non disponible", index);
                return;
            }

            // Récupérer l'information sur le travail en hauteur
            boolean travailHauteur = flux.getInfoAssureComplets().get(index).getTravailHauteur();

            // Déterminer l'option à sélectionner selon la logique métier
            String optionToSelect;
            if (travailHauteur) {
                optionToSelect = "De 3 à 20 mètres";
            } else {
                optionToSelect = "Non";
            }

            // Vérifier que l'option existe avant de la sélectionner
            List<String> availableOptions = elementLib.getSelectOptions(dropdownSelector);
            boolean optionExists = availableOptions.stream()
                    .anyMatch(option -> option.equalsIgnoreCase(optionToSelect));

            if (!optionExists) {
                log.warn("L'option '{}' n'existe pas dans le dropdown. Options disponibles : {}",
                        optionToSelect, availableOptions);
                return;
            }

            // Sélectionner l'option
            elementLib.selectByVisibleText2(dropdownSelector, optionToSelect);
            log.info("Option sélectionnée pour l'index {} : {}", index, optionToSelect);

        } catch (Exception e) {
            log.error("Erreur lors de la sélection du travail en hauteur pour l'index {}", index, e);
            throw new RuntimeException("Échec de la sélection du travail en hauteur", e);
        }
    }

    private void choixTravailRisque(FluxData flux, int index) {
        try {
            // Construire le sélecteur CSS selon l'index
            String dropdownSelector = String.format("#demande_form_personnes_%d_profrisque", index);

            // Attendre que l'élément soit présent et visible
            elementLib.waitForElement(dropdownSelector, 20);

            // Vérifier que l'élément est bien présent
            if (!elementLib.isElementPresent(dropdownSelector)) {
                log.warn("L'élément dropdown profession à risque pour l'index {} n'est pas présent", index);
                return;
            }

            // Vérifier que l'index est valide
            if (flux.getInfoAssureComplets() == null ||
                    index >= flux.getInfoAssureComplets().size()) {
                log.warn("Index {} invalide ou InfoAssureComplets non disponible", index);
                return;
            }

            // Récupérer les informations sur les risques professionnels
            boolean metierExpose = flux.getInfoAssureComplets().get(index).getMetierExpose();
            boolean produitDanger = flux.getInfoAssureComplets().get(index).getProduitDanger();

            // Déterminer l'option à sélectionner selon la logique métier
            String optionToSelect;
            if (!metierExpose || !produitDanger) {
                optionToSelect = "Non";
            } else {
                optionToSelect = "Oui";
            }

            // Vérifier que l'option existe avant de la sélectionner
            List<String> availableOptions = elementLib.getSelectOptions(dropdownSelector);
            boolean optionExists = availableOptions.stream()
                    .anyMatch(option -> option.equalsIgnoreCase(optionToSelect));

            if (!optionExists) {
                log.warn("L'option '{}' n'existe pas dans le dropdown. Options disponibles : {}",
                        optionToSelect, availableOptions);
                return;
            }

            // Sélectionner l'option
            elementLib.selectByVisibleText2(dropdownSelector, optionToSelect);
            log.info("Option sélectionnée pour l'index {} : {}", index, optionToSelect);

        } catch (Exception e) {
            log.error("Erreur lors de la sélection de la profession à risque pour l'index {}", index, e);
            throw new RuntimeException("Échec de la sélection de la profession à risque", e);
        }
    }

    private void choixTypePret(FluxData flux, int index) {
        try {
            // Construire le sélecteur CSS selon l'index
            String dropdownSelector = String.format("#demande_form_prets_%d_type", index);

            // Attendre que l'élément soit présent et visible
            elementLib.waitForElement(dropdownSelector, 20);

            // Vérifier que l'élément est bien présent
            if (!elementLib.isElementPresent(dropdownSelector)) {
                log.warn("L'élément dropdown type de prêt pour l'index {} n'est pas présent", index);
                return;
            }

            // Vérifier que l'index est valide
            if (flux.getPrets() == null ||
                    index >= flux.getPrets().size()) {
                log.warn("Index {} invalide ou Prets non disponible", index);
                return;
            }

            // Récupérer le type de prêt
            String typePret = flux.getPrets().get(index).getType();

            // Déterminer l'option à sélectionner selon la logique métier
            String optionToSelect = null;

            if (typePret.equalsIgnoreCase("Amortissable")) {
                optionToSelect = "Prêt amortissable classique";
            } else if (typePret.equalsIgnoreCase("Prêt à paliers")) {
                optionToSelect = "Prêt à paliers";
            }

            // Sélectionner l'option si elle a été déterminée
            if (optionToSelect != null) {
                elementLib.selectByVisibleText2(dropdownSelector, optionToSelect);
                log.info("Option sélectionnée pour l'index {} : {}", index, optionToSelect);
            } else {
                log.warn("Aucune option correspondante trouvée pour le type : {}", typePret);
            }

        } catch (Exception e) {
            log.error("Erreur lors de la sélection du type de prêt pour l'index {}", index, e);
            throw new RuntimeException("Échec de la sélection du type de prêt", e);
        }
    }
}
