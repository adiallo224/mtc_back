package com.mtc.mutuaConseil.services.servicesImpl.assuPret;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.SelectOption;
import com.microsoft.playwright.options.WaitForSelectorState;
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

import java.util.Arrays;
import java.util.Map;

@Service
public class AlptisPretPlayWrightService extends BasePlaywrightService implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(AlptisPretPlayWrightService.class);
    private final String source = "AlptisPret";
    private final TypeAssuranceService typeAssuranceService;

    public AlptisPretPlayWrightService(TypeAssuranceService typeAssuranceService){
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        log.info("Debut de traitement -- Alptis");
        Tarif tarifAlptisPret = TarifUtils.createDefaultTarif(c, typeAssuranceService, 1L);

        waitThread(1);
        try {
            // Initialisation simplifiée - elementLib est initialisé automatiquement
            initializeBrowser(false);
            // Navigation
            humanLikeNavigate(c.getUrlFournisseur());
            // Connexion
            connexion(c);
            waitThread(1);
            elementLib.clickByXpath("//button[@class='rubrique-list-card rubrique-list-card--emprunteur-financement']");
            waitThread(1);
            elementLib.clickByXpath("//a[@href='/offres/comparateur/emprunteur']");
            // Changement de page
            elementLib.switchToNewWindow();
            waitThread(1);
            elementLib.clickByXpath("//a[normalize-space()='Nouveau projet']");
            waitThread(1);
            elementLib.clickByXpath("//div[@class='new-project-bloc']");
            waitThread(1);

            remplirInformationsPersonne(flux, source);

            elementLib.scrollDown(300);

            waitThread(1);
            remplirInformationsPret(flux, source);

            // waitThread(1);
            // choixGaranties(flux);

            //Valider
            elementLib.clickByXpath("//div[@class=\"wrap-submit-form\"]//button[text()='Valider le formulaire']");

            waitThread(15);
            String cout = elementLib.getElementText("//td[3]//div");
            log.info("Cout {}", cout);
            tarifAlptisPret.setMontant(cout);
            String screenshotBytes = captureScreenshot(tarifAlptisPret.getNom(), false, tarifAlptisPret);
            if (screenshotBytes != null) {
                tarifAlptisPret.setCaptureImg(screenshotBytes);
            }
            tarifAlptisPret.setExecution(true);
            log.info("Fin de traitement -- Alptis");
        } catch (Exception e) {
            log.error("An error occurred", e);
            tarifAlptisPret.setErreur(e.getMessage());
            String screenshotBytesErreur = captureScreenshot(tarifAlptisPret.getNom(), true, tarifAlptisPret);
            tarifAlptisPret.setCaptureImgErreur(screenshotBytesErreur);
            tarifAlptisPret.setEtape("");
        } finally {
            cleanup();
        }
        return tarifAlptisPret;
    }

    public void connexion(Compte c) {
        elementLib.clickByXpath("//div//button[@aria-label=\"Non merci\"]");
        elementLib.humanTypeById("username", c.getUsername());
        elementLib.humanTypeById("password", c.getPassword());
        clickIfExists("//*[@name=\"login\"]");
    }

    private void remplirInformationsPersonne(FluxData flux, String source) {
        int index = 0;
        //Civilite
        if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur")) {
            elementLib.selectByVisibleText2("select[id*='insured_insdGender']", "Monsieur");
        } else if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame")) {
            elementLib.selectByVisibleText2("select[id*='insured_insdGender']", "Madame");
        }
        elementLib.selectByVisibleText2("select[id*='insured_insdGender']", "Monsieur");
        //prenom
        elementLib.humanTypeByXpath(".//input[contains(@id, 'insured_lastname')]", flux.getPersonnes().get(index).getPrenom());
        //nom
        elementLib.humanTypeByXpath(".//input[contains(@id, 'insured_firstname')]", flux.getPersonnes().get(index).getNom());
        //Date naissance
        elementLib.humanTypeByXpath(".//input[contains(@id, 'insured_insdDateOfBirthDt')]", flux.getPersonnes().get(index).getDateNaissance());
        //Ville naissance
        elementLib.humanTypeByXpath(".//input[contains(@id, 'insured_cityOfBirth')]", flux.getPersonnes().get(index).getVille());
        //Email
        elementLib.humanTypeByXpath(".//input[contains(@id, 'insured_email')]", flux.getPersonnes().get(index).getEmail());
        //Telephone
        elementLib.humanTypeByXpath(".//input[contains(@id, 'insured_mobilePhoneNumber')]", flux.getPersonnes().get(index).getTelephone());
        //adresse
        elementLib.humanTypeByXpath(".//input[contains(@id, 'addressline1')]", flux.getPersonnes().get(index).getNumeroVoie() + " " + flux.getPersonnes().get(index).getNomVoie());
        elementLib.humanTypeByXpath(".//input[contains(@id, 'addresscity')]", flux.getPersonnes().get(index).getVille());
        elementLib.humanTypeByXpath(".//input[contains(@id, 'addresszipcode')]", flux.getPersonnes().get(index).getCodePostal());
        elementLib.humanTypeByXpath(".//input[contains(@id, 'addresscountry')]", flux.getPersonnes().get(index).getPays());
        waitThread(2);
        elementLib.pressEnter();
        elementLib.scrollDown(100);
        if (flux.getPersonnes().get(index).getNationalite().equalsIgnoreCase("Française")) {
            elementLib.humanTypeByXpath("//input[contains(@id, 'label.nationality')]", "France");
        }
        waitThread(1);
        elementLib.pressEnterOnBody();
        // Habitudes de vie
        //Profession
        elementLib.scrollDown(500);
        System.out.println("Frames: " + page.frames().size());
        for (Frame f : page.frames()) {
            System.out.println("Frame URL = " + f.url());
        }
        choixCategorieProfession(flux, index);
        //Profession exacte
        elementLib.humanTypeByXpath(".//input[contains(@id, 'insured_exactJob')]", flux.getPersonnes().get(index).getProfession());
        //Regime social
        waitThread(3);
        choixRegime(flux, index);
        //Déplacements professionnels
        choixDeplacementProfessionnelle(flux, index);
        //Travail hauteur
        choixTravailHauteur(flux, index);

        if (flux.getInfoAssureComplets().get(index).getFumeur()) {
            elementLib.clickByXpath(".//input[contains(@id, 'insured_insdSmoker_0')]");
        }
        if (flux.getInfoAssureComplets().get(index).getFumeurSansNico()) {
            elementLib.clickByXpath(".//input[contains(@id, 'insured_insdESmoker_0')]");
        }
        if (flux.getInfoAssureComplets().get(index).getFumeurElecNico()) {
            elementLib.clickByXpath(".//input[contains(@id, 'insured_esmokerNoNicotine_0')]");
        }
        if (flux.getInfoAssureComplets().get(index).getTravailManuel()) {
            elementLib.clickByXpath(".//input[contains(@id, 'insured_insdManualWork_0')]");
        }
        if (flux.getInfoAssureComplets().get(index).getProduitDanger()) {
            elementLib.clickByXpath(".//input[contains(@id, 'insured_dangerousProduct_0')]");
        }
        if (flux.getInfoAssureComplets().get(index).getTravailManuelManuLourde()) {
            elementLib.clickByXpath(".//input[contains(@id, 'insured_insdManualWorkRisk_0')]");
        }
        if (flux.getInfoAssureComplets().get(index).getMetierExpose()) {
            elementLib.clickByXpath(".//input[contains(@id, 'insured_workRisk_0')]");
        }
        if (flux.getInfoAssureComplets().get(index).getSportRisque()) {
            elementLib.clickByXpath(".//input[contains(@id, 'insured_insdSportsActivities_0')]");
        }

        if (flux.getPersonnes().size() == 2) {
            index = 1;
            elementLib.clickByXpath("//button[@class=\"btn\" and text()=\" Ajouter un assuré \"]");
            if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur")) {
                elementLib.selectByVisibleText2("select[id*='insured_insdGender_1']", "Monsieur");
            } else if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame")) {
                elementLib.selectByVisibleText2("select[id*='insured_insdGender_1']", "Madame");
            }

            //Nom
            elementLib.humanTypeByXpath("//input[contains(@id, 'insured_lastname_1')]", flux.getPersonnes().get(index).getPrenom());
            //Prenom
            elementLib.humanTypeByXpath("//input[contains(@id, 'insured_firstname_1')]", flux.getPersonnes().get(index).getNom());
            //Date naissance
            elementLib.humanTypeByXpath("//input[contains(@id, 'insured_insdDateOfBirthDt_1')]", flux.getPersonnes().get(index).getDateNaissance());
            //Ville naissance
            elementLib.humanTypeByXpath("//input[contains(@id, 'insured_cityOfBirth_1')]", flux.getPersonnes().get(index).getVille());
            //Email
            elementLib.humanTypeByXpath("//input[contains(@id, 'insured_email_1')]", flux.getPersonnes().get(index).getEmail());
            //Telephone
            elementLib.humanTypeByXpath("//input[contains(@id, 'insured_mobilePhoneNumber_1')]", flux.getPersonnes().get(index).getTelephone());
            //adresse
            elementLib.humanTypeByXpath("//input[contains(@id, 'insured_address1') and contains(@id, 'addressline1')]", flux.getPersonnes().get(index).getNumeroVoie() + " " + flux.getPersonnes().get(index).getNomVoie());
            elementLib.humanTypeByXpath("//input[contains(@id, 'insured_address1') and contains(@id, 'addresscity')]", flux.getPersonnes().get(index).getVille());
            elementLib.humanTypeByXpath("//input[contains(@id, 'insured_address1') and contains(@id, 'addresszipcode')]", flux.getPersonnes().get(index).getCodePostal());
            elementLib.humanTypeByXpath("//input[contains(@id, 'insured_address1') and contains(@id, 'addresscountry')]", flux.getPersonnes().get(index).getPays());
            waitThread(2);
            elementLib.pressEnter();
            if (flux.getPersonnes().get(index).getNationalite().equalsIgnoreCase("Française")) {
                elementLib.humanTypeByXpath("/html[1]/body[1]/form[1]/main[1]/form[1]/div[2]/form[1]/form[1]/form[1]/form[1]/form[1]/div[1]/div[3]/div[1]/div[2]/div[2]/form[1]/div[2]/div[4]/div[1]/div[1]/div[1]/div[1]/div[1]/input[1]", "France");
            }
            waitThread(1);
            elementLib.pressEnter();
            // Habitudes de vie
            //Profession
            elementLib.scrollDown(500);

            System.out.println("Frames: " + page.frames().size());
            for (Frame f : page.frames()) {
                System.out.println("Frame URL = " + f.url());
            }

            choixCategorieProfession(flux, index);
            //Profession exacte
            elementLib.humanTypeByXpath("//input[contains(@id, 'insured_exactJob_1')]", flux.getPersonnes().get(index).getProfession());
            //Regime social
            waitThread(2);
            choixRegime(flux, index);
            //Déplacements professionnels
            choixDeplacementProfessionnelle(flux, index);
            //Travail hauteur
            choixTravailHauteur(flux, index);

            if (flux.getInfoAssureComplets().get(index).getFumeur()) {
                elementLib.clickByXpath(".//input[contains(@id, 'insured_insdSmoker_1')]");
            }
            if (flux.getInfoAssureComplets().get(index).getFumeurSansNico()) {
                elementLib.clickByXpath(".//input[contains(@id, 'insured_insdESmoker_1')]");
            }
            if (flux.getInfoAssureComplets().get(index).getFumeurElecNico()) {
                elementLib.clickByXpath(".//input[contains(@id, 'insured_esmokerNoNicotine_1')]");
            }
            if (flux.getInfoAssureComplets().get(index).getTravailManuel()) {
                elementLib.clickByXpath(".//input[contains(@id, 'insured_insdManualWork_1')]");
            }
            if (flux.getInfoAssureComplets().get(index).getProduitDanger()) {
                elementLib.clickByXpath(".//input[contains(@id, 'insured_dangerousProduct_1')]");
            }
            if (flux.getInfoAssureComplets().get(index).getTravailManuelManuLourde()) {
                elementLib.clickByXpath(".//input[contains(@id, 'insured_insdManualWorkRisk_1')]");
            }
            if (flux.getInfoAssureComplets().get(index).getMetierExpose()) {
                elementLib.clickByXpath(".//input[contains(@id, 'insured_workRisk_1')]");
            }
            if (flux.getInfoAssureComplets().get(index).getSportRisque()) {
                elementLib.clickByXpath(".//input[contains(@id, 'insured_insdSportsActivities_1')]");
            }
        }
    }

    private void remplirInformationsPret(FluxData flux, String source) {
        int index = 0;
        //Type de pret
        choixTypePret(flux, index);
        //Montant
        elementLib.humanTypeByXpath(".//input[contains(@id, 'loan_loanAmount_0')]", flux.getPrets().get(index).getMontantPret());
        //Duree
        elementLib.humanTypeByXpath(".//input[contains(@id, 'loan_loanDuration_0')]", flux.getPrets().get(index).getDuree());
        //Taux
        elementLib.humanTypeByXpath(".//input[contains(@id, 'loan_loanRate_0')]", flux.getPrets().get(index).getTaux());
        //Date effet
        elementLib.humanTypeByXpath(".//input[contains(@id, 'loan_effectiveDate_0')]", flux.getPrets().get(index).getDateEffet());
        //Type de différé
        choixTypeDiffere(flux, index);

        if (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Partiel")) {
            elementLib.humanTypeByXpath(".//input[contains(@id, 'loan_loanDeferredDuration2')]", flux.getPrets().get(index).getDureeDiffere());
        }
        if (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Total")) {
            elementLib.humanTypeByXpath(".//input[contains(@id, 'loan_loanDeferredDuration2')]", flux.getPrets().get(index).getDureeDiffere());
        }
        //Périodicité pret
//        WebElement dropdownPeriodicite = driver.findElement(By.xpath(".//select[contains(@id, 'loan_loanPeriodicityRefund_0')]"));
//        Select selectPeriodicite = new Select(dropdownPeriodicite);
//        selectPeriodicite.selectByValue("MENSUELLE");
//        //Périodicité assurance
//        WebElement dropdownPeriodiciteAss = driver.findElement(By.xpath(".//select[contains(@id, 'loan_loanPeriodicityInsurance_0')]"));
//        Select selectPeriodiciteAss = new Select(dropdownPeriodiciteAss);
//        selectPeriodiciteAss.selectByValue("MENSUELLE");
        //Objet financement
        choixObjetFinancement(flux, index);

        //Banque
        choixBanque(flux, index);
        elementLib.scrollDown(800);
        waitThread(1);
        if (flux.getPrets().size() == 2) {
            waitThread(3);
            elementLib.clickByXpath("//button[contains(@class, 'btn') and contains(text(), 'Ajouter un prêt')]");
            index = 1;
            choixTypePret(flux, index);
            //Montant
            elementLib.humanTypeByXpath(".//input[contains(@id, 'loan_loanAmount_1')]", flux.getPrets().get(index).getMontantPret());
            //Duree
            elementLib.humanTypeByXpath(".//input[contains(@id, 'loan_loanDuration_1')]", flux.getPrets().get(index).getDuree());
            //Taux
            elementLib.humanTypeByXpath(".//input[contains(@id, 'loan_loanRate_1')]", flux.getPrets().get(index).getTaux());
            //Date effet
            elementLib.humanTypeByXpath(".//input[contains(@id, 'loan_effectiveDate_1')]", flux.getPrets().get(index).getDateEffet());
            //Type de différé
            choixTypeDiffere(flux, index);

            if (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Partiel")) {
                elementLib.humanTypeByXpath((".//input[contains(@id, 'loan_loanDeferredDuration2_1')]"), flux.getPrets().get(index).getDureeDiffere());
            }
            if (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Total")) {
                elementLib.humanTypeByXpath(".//input[contains(@id, 'loan_loanDeferredDuration2_1')]", flux.getPrets().get(index).getDureeDiffere());
            }
//            //Périodicité pret
//            WebElement dropdownPeriodicite1 = driver.findElement(By.xpath(".//select[contains(@id, 'loan_loanPeriodicityRefund_1')]"));
//            Select selectPeriodicite1 = new Select(dropdownPeriodicite1);
//            selectPeriodicite1.selectByValue("MENSUELLE");
//            //Périodicité assurance
//            WebElement dropdownPeriodiciteAss1 = driver.findElement(By.xpath(".//select[contains(@id, 'loan_loanPeriodicityInsurance_1')]"));
//            Select selectPeriodiciteAss1 = new Select(dropdownPeriodiciteAss1);
//            selectPeriodiciteAss1.selectByValue("MENSUELLE");
            //Objet financement
            choixObjetFinancement(flux, index);
            //Banque
            choixBanque(flux, index);
        }
    }

    private void choixDeplacementProfessionnelle(FluxData flux, int index) {
        String xpathSelector = null;

        if (index == 0) {
            xpathSelector = ".//select[contains(@id, 'insured_insdAnnualMilage')]";
        }
        if (index == 1) {
            xpathSelector = "//select[contains(@id, 'insured_insdAnnualMilage_1')]";
        }

        if (xpathSelector != null && isElementPresentByXpath(xpathSelector)) {
            try {
                // Attendre que l'élément soit disponible
                waitForElementByXpath(xpathSelector, 10);

                // Sélection conditionnelle
                if (!flux.getInfoAssureComplets().get(index).getDeplacementPro20000()) {
                    selectOptionByXpath(xpathSelector, "Moins de 10 000");
                } else if (flux.getInfoAssureComplets().get(index).getDeplacementPro20000()) {
                    selectOptionByXpath(xpathSelector, "20 001 - 25 000");
                }
            } catch (Exception e) {
                log.error("Erreur lors de la sélection du déplacement professionnel pour l'index {}: {}", index, e.getMessage());
            }
        }
    }

    private void choixTravailHauteur(FluxData flux, int index) {
        String xpathSelector = null;

        if (index == 0) {
            xpathSelector = "//select[contains(@id, 'insured_insdWorkAtHeight')]";
        }
        if (index == 1) {
            xpathSelector = "//select[contains(@id, 'insured_insdWorkAtHeight_1')]";
        }

        if (xpathSelector != null && isElementPresentByXpath(xpathSelector)) {
            try {
                waitThread(1);
                waitForElementByXpath(xpathSelector, 10);

                if (flux.getInfoAssureComplets().get(index).getTravailHauteur()) {
                    String hauteur = flux.getInfoAssureComplets().get(index).getHauteur();

                    // Mapping des valeurs d'entrée vers les valeurs de sélection
                    Map<String, String> hauteurMapping = Map.of(
                            "0 à 3m", "0 - 3",
                            "3 à 10m", "3 - 10",
                            "10 à 12m", "10 - 12",
                            "12 à 15m", "12 - 15",
                            "15 à 20m", "15 - 20",
                            "Plus de 20m", "Plus de 20m"
                    );

                    String optionToSelect = hauteurMapping.get(hauteur);
                    if (optionToSelect != null) {
                        selectOptionByXpath(xpathSelector, optionToSelect);
                    } else {
                        log.warn("Valeur de hauteur non reconnue : {}", hauteur);
                    }
                }
            } catch (Exception e) {
                log.error("Erreur lors de la sélection du travail en hauteur pour l'index {}: {}", index, e.getMessage());
            }
        }
    }

    private void choixRegime(FluxData flux, int index) {
        String xpathSelector;

        if (index == 0) {
            xpathSelector = "//select[contains(@id, 'insured_socialRegime')]";
        } else {
            xpathSelector = String.format("//select[contains(@id, 'insured_socialRegime_%d')]", index);
        }

        try {
            String regime = flux.getPersonnes().get(index).getRegime();

            if (regime == null || regime.isEmpty()) {
                log.warn("Aucun régime trouvé pour l'index {}", index);
                return;
            }

            // Préfixer avec "xpath=" pour waitForSelector et selectOption
            String selectorWithPrefix = "xpath=" + xpathSelector;
            page.waitForSelector(selectorWithPrefix, new Page.WaitForSelectorOptions()
                    .setState(WaitForSelectorState.ATTACHED)
                    .setTimeout(15000));
            
            page.selectOption(selectorWithPrefix, new SelectOption().setLabel(regime));

            log.info("Régime '{}' sélectionné pour l'index {}", regime, index);
            waitThread(1);

        } catch (Exception e) {
            log.error("Erreur lors de la sélection du régime social pour l'index {}: {}", index, e.getMessage());
        }
    }


    private void choixCategorieProfession(FluxData flux, int index) {
        String categorie = flux.getPersonnes().get(index).getProfessionSpecifique();
        if (categorie == null || categorie.isEmpty()) return;

        try {
            // 1. Cibler l'iframe (par son URL ou un attribut unique)
            FrameLocator frameLocator = page.frameLocator("iframe[src*='pro.alptis.org/offres/rubrique/emprunteur-financement']");

            // 2. Localiser le select DANS l'iframe
            Locator select = frameLocator.locator("select.form-control[name^='insured_professionalCategory_']").first();

            // 3. Attendre que le select soit visible et actionnable
            select.waitFor(new Locator.WaitForOptions()
                    .setState(WaitForSelectorState.VISIBLE)
                    .setTimeout(10000));

            // 4. Sélectionner l'option
            select.selectOption(categorie);

        } catch (Exception e) {
            log.error("Erreur sélection catégorie professionnelle : {}", e.getMessage());
        }
    }







    private void choixTypePret(FluxData flux, int index) {
        String xpathSelector = null;

        if (index == 0) {
            xpathSelector = ".//select[contains(@id, 'loan_loanType_0')]";
        }
        if (index == 1) {
            xpathSelector = ".//select[contains(@id, 'loan_loanType_1')]";
        }

        if (xpathSelector != null) {
            try {
                waitForElementByXpath(xpathSelector, 15);
                waitThread(1);
                selectOptionByXpath(xpathSelector, flux.getPrets().get(index).getType());
            } catch (Exception e) {
                log.error("Erreur lors de la sélection du type de prêt pour l'index {}: {}", index, e.getMessage());
            }
        }
    }

    private void choixObjetFinancement(FluxData flux, int index) {
        String xpathSelector = null;

        if (index == 0) {
            xpathSelector = ".//select[contains(@id, 'loan_loanPurposeOfFinancing_0')]";
        }
        if (index == 1) {
            xpathSelector = ".//select[contains(@id, 'loan_loanPurposeOfFinancing_1')]";
        }

        if (xpathSelector != null && isElementPresentByXpath(xpathSelector)) {
            try {
                waitForElementByXpath(xpathSelector, 10);
                waitThread(1);
                selectOptionByXpath(xpathSelector, flux.getPrets().get(index).getObjet());
            } catch (Exception e) {
                log.error("Erreur lors de la sélection de l'objet de financement pour l'index {}: {}", index, e.getMessage());
            }
        }
    }

    private void choixBanque(FluxData flux, int index) {
        String xpathSelector = null;

        if (index == 0) {
            xpathSelector = ".//select[contains(@id, 'loan_loan.bankInformation_bankCode_0')]";
        }
        if (index == 1) {
            xpathSelector = ".//select[contains(@id, 'loan_loan.bankInformation_bankCode_1')]";
        }

        if (xpathSelector != null) {
            try {
                waitForElementByXpath(xpathSelector, 30);

                String banqueToSelect = flux.getPrets().get(index).getBanque();

                // Essayer d'abord avec le mapping exact
                String optionToSelect = mapBanqueToOption(banqueToSelect);

                if (optionToSelect != null) {
                    try {
                        selectOptionByXpath(xpathSelector, optionToSelect);
                        scrollDown(-450);
                        return;
                    } catch (Exception e) {
                        log.warn("Impossible de sélectionner l'option exacte '{}', tentative avec recherche partielle", optionToSelect);
                    }
                }

                // Si la sélection exacte échoue, utiliser une approche plus flexible
                selectBanqueByPartialMatch(xpathSelector, banqueToSelect);
                scrollDown(-450);

            } catch (Exception e) {
                log.error("Erreur lors de la sélection de la banque pour l'index {}: {}", index, e.getMessage());
            }
        }
    }

    /**
     * Sélection par correspondance partielle si l'option exacte n'est pas trouvée
     */
    private void selectBanqueByPartialMatch(String xpathSelector, String banque) {
        try {
            // Récupérer toutes les options de la dropdown
            Locator selectElement = page.locator("xpath=" + xpathSelector);
            Locator options = selectElement.locator("option");

            int optionCount = options.count();
            String banqueLower = banque.toLowerCase();

            for (int i = 0; i < optionCount; i++) {
                String optionText = options.nth(i).textContent().toLowerCase();

                // Vérifier les correspondances partielles
                if ((optionText.contains("axa") && banqueLower.contains("axa")) ||
                        (optionText.contains("bnp") && banqueLower.contains("bnp")) ||
                        (optionText.contains("épargne") && banqueLower.contains("épargne")) ||
                        (optionText.contains("agricole") && banqueLower.contains("agricole")) ||
                        (optionText.contains("crédit mutuel") && banqueLower.contains("crédit mutuel")) ||
                        (optionText.contains("lcl") && banqueLower.contains("lcl")) ||
                        (optionText.contains("cic") && banqueLower.contains("cic")) ||
                        (optionText.contains("postale") && banqueLower.contains("postale")) ||
                        (optionText.contains("société générale") && banqueLower.contains("société générale"))) {

                    // Scroller vers l'option et la sélectionner
                    options.nth(i).scrollIntoViewIfNeeded();
                    options.nth(i).click();
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Erreur lors de la sélection par correspondance partielle : {}", e.getMessage());
        }
    }

    /**
     * Méthode utilitaire pour mapper les noms de banques aux options de la dropdown
     */
    private String mapBanqueToOption(String banque) {
        if (banque == null) return null;

        String banqueLower = banque.toLowerCase();

        // Mapping basé sur la logique originale
        if (banqueLower.contains("axa") || banqueLower.equals("axa banque")) {
            return "AXA";
        } else if (banqueLower.contains("bnp paribas")) {
            return "BNP Paribas";
        } else if (banqueLower.contains("caisse d'épargne")) {
            return "Caisse d'épargne";
        } else if (banqueLower.contains("caisse agricole")) {
            return "Caisse agricole";
        } else if (banqueLower.contains("crédit mutuel")) {
            return "Crédit Mutuel";
        } else if (banqueLower.contains("lcl")) {
            return "LCL";
        } else if (banqueLower.contains("cic")) {
            return "CIC";
        } else if (banqueLower.contains("la banque postale")) {
            return "La Banque Postale";
        } else if (banqueLower.contains("société générale")) {
            return "Société Générale";
        }

        return null;
    }

    private void choixTypeDiffere(FluxData flux, int index) {
        String xpathSelector = null;

        if (index == 0) {
            xpathSelector = ".//select[contains(@id, 'loan_loanDeferredType2_0')]";
        }
        if (index == 1) {
            xpathSelector = ".//select[contains(@id, 'loan_loanDeferredType2_1')]";
        }

        if (xpathSelector != null && isElementPresentByXpath(xpathSelector)) {
            try {
                waitThread(1);
                waitForElementByXpath(xpathSelector, 10);

                String differeType = flux.getPrets().get(index).getDiffere();

                if (differeType.equalsIgnoreCase("Partiel") || differeType.equalsIgnoreCase("Total")) {
                    selectOptionByValue("xpath=" + xpathSelector, "AUCUN");
                }
            } catch (Exception e) {
                log.error("Erreur lors de la sélection du type de différé pour l'index {}: {}", index, e.getMessage());
            }
        }
    }

    private void choixGaranties(FluxData flux) {
        if (flux.getInfoAssureComplets().get(0).getGarantie().equalsIgnoreCase("IPT")){

        } else if (flux.getInfoAssureComplets().get(0).getGarantie().equalsIgnoreCase("IPT, ITT")){
            elementLib.clickByXpath("//td[@cov-code='IPP']");
        }
    }
}
