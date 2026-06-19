package com.mtc.mutuaConseil.services.servicesImpl.assuPret;

import com.mtc.mutuaConseil.base.BaseAutomationService;
import com.mtc.mutuaConseil.base.BrowserType;
import com.mtc.mutuaConseil.base.WaitUtils;
import com.mtc.mutuaConseil.models.Compte;
import com.mtc.mutuaConseil.models.FluxData;
import com.mtc.mutuaConseil.models.Tarif;
import com.mtc.mutuaConseil.services.LaunchedService;
import com.mtc.mutuaConseil.services.servicesImpl.TypeAssuranceService;
import com.mtc.mutuaConseil.utils.TarifUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class AlptisService extends BaseAutomationService implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(AlptisService.class);
    public static WebDriver driver;
    private JavascriptExecutor js = null;
    private final TypeAssuranceService typeAssuranceService;

    public AlptisService(TypeAssuranceService typeAssuranceService){
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        log.info("Debut de traitement -- Alptis");
        Tarif tarifAlptisPret = TarifUtils.createDefaultTarif(c, typeAssuranceService, 1L);
        // Déterminer le navigateur à partir du compte si possible (sinon Chromedriver Selenium par défaut)
        BrowserType browserType = BrowserType.SELENIUM_FIREFOX;
        try {
             // Essayer de récupérer un nom de navigateur dans le compte (méthode optionnelle)
             try {
                 String browserName = (String) (c.getClass().getMethod("getBrowser").invoke(c));
                 if (browserName != null) {
                     String normalized = browserName.trim().toLowerCase();
                     if (normalized.contains("firefox")) browserType = BrowserType.SELENIUM_FIREFOX;
                     else if (normalized.contains("edge")) browserType = BrowserType.SELENIUM_EDGE;
                     else browserType = BrowserType.SELENIUM_CHROME;
                 }
             } catch (NoSuchMethodException nsme) {
                // getBrowser() non présent : ignorer (utiliser le défaut)
             } catch (Exception ignore) {
                // si invocation échoue, on continue avec défaut
             }

             // Initialise le navigateur via la factory (headless = false pour imiter ton ancien comportement)
             init(browserType, false);
             // Initialiser le driver depuis le BrowserAdapter pour compatibilité avec le code existant
             driver = getWebDriver();
             if (driver == null) {
                 log.error("Impossible d'obtenir le WebDriver depuis le BrowserAdapter");
             }
             // Initialiser le JavascriptExecutor depuis le BrowserAdapter
             js = getJavascriptExecutor();
             if (js == null) {
                 log.error("Impossible d'obtenir le JavascriptExecutor depuis le BrowserAdapter");
             }
             connexion(c);
             WaitUtils.humanDelay();
             element.clickByXpath("//button[@class='rubrique-list-card rubrique-list-card--emprunteur-financement']");
             WaitUtils.humanDelay();
             element.clickByXpath("//a[@href='/offres/comparateur/emprunteur']");
             // Changement de page
             switchPage();
             element.clickByXpath("//a[normalize-space()='Nouveau projet']");
             element.clickByXpath("//div[@class='new-project-bloc']");
             remplirInformationsPersonne(flux);
             scrollDown(0, 300);
             WaitUtils.sleepMs(2000);
             remplirInformationsPret(flux);
//             choixGaranties(flux);
            //Valider
             element.clickByXpath("//div[@class=\"wrap-submit-form\"]//button[text()='Valider le formulaire']");

             WaitUtils.sleepMs(15000);

             WebElement firstCell = waitForElement(driver, By.xpath("//td[3]//div"), 15, 3);
             tarifAlptisPret.setMontant(firstCell.getText());
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
        humanNavigate(c.getUrlFournisseur());
        WaitUtils.sleepMs(3000);
        element.clickById("axeptio_btn_dismiss");
        element.typeById("username", c.getUsername());
        element.typeById("password", c.getPassword());
        element.clickByName("login");
    }

    private void remplirInformationsPersonne(FluxData flux) {
        int index = 0;
        //Civilite
        if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur")) {
            WaitUtils.sleepMs(3000);
            element.clickByXpath(".//select[contains(@id, 'insured_insdGender')]");
            element.clickByXpath(".//option[text()='Monsieur']");
        } else if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame")) {
            WaitUtils.sleepMs(3000);
            element.clickByXpath(".//select[contains(@id, 'insured_insdGender')]");
            element.clickByXpath(".//option[text()='Madame']");
        }
        //prenom
        element.typeByXpath(".//input[contains(@id, 'insured_lastname')]", flux.getPersonnes().get(index).getPrenom());
        //nom
        element.typeByXpath(".//input[contains(@id, 'insured_firstname')]", flux.getPersonnes().get(index).getNom());
        //Date naissance
        element.typeByXpath(".//input[contains(@id, 'insured_insdDateOfBirthDt')]", flux.getPersonnes().get(index).getDateNaissance());
        //Ville naissance
        element.typeByXpath(".//input[contains(@id, 'insured_cityOfBirth')]", flux.getPersonnes().get(index).getVille());
        //Email
        element.typeByXpath(".//input[contains(@id, 'insured_email')]", flux.getPersonnes().get(index).getEmail());
        //Telephone
        element.typeByXpath(".//input[contains(@id, 'insured_mobilePhoneNumber')]", flux.getPersonnes().get(index).getTelephone());
        //adresse
        element.typeByXpath(".//input[contains(@id, 'addressline1')]", flux.getPersonnes().get(index).getNumeroVoie() + " " + flux.getPersonnes().get(index).getNomVoie());
        element.typeByXpath(".//input[contains(@id, 'addresscity')]", flux.getPersonnes().get(index).getVille());
        element.typeByXpath(".//input[contains(@id, 'addresszipcode')]", flux.getPersonnes().get(index).getCodePostal());
        element.typeByXpath(".//input[contains(@id, 'addresscountry')]", flux.getPersonnes().get(index).getPays());
        WaitUtils.sleepMs(2000);
        elementNeutre();
        scrollDown(0, 100);
        if (flux.getPersonnes().get(index).getNationalite().equalsIgnoreCase("Française")) {
            element.typeByXpath("//input[contains(@id, 'label.nationality')]", "France");
        }
        WaitUtils.sleepMs(1000);
        elementNeutre();
        // Habitudes de vie
        //Profession
        scrollDown(0, 500);
        choixCategorieProfession(flux, index);
        //Profession exacte
        element.typeByXpath(".//input[contains(@id, 'insured_exactJob')]", flux.getPersonnes().get(index).getProfession());
        //Regime social
        WaitUtils.sleepMs(3000);
        choixRegime(flux, index);
        //Déplacements professionnels
        choixDeplacementProfessionnelle(flux, index);
        //Travail hauteur
        choixTravailHauteur(flux, index);

        if (flux.getInfoAssureComplets().get(index).getFumeur()) {
            element.clickByXpath(".//input[contains(@id, 'insured_insdSmoker_0')]");
        }
        if (flux.getInfoAssureComplets().get(index).getFumeurSansNico()) {
            element.clickByXpath(".//input[contains(@id, 'insured_insdESmoker_0')]");
        }
        if (flux.getInfoAssureComplets().get(index).getFumeurElecNico()) {
            element.clickByXpath(".//input[contains(@id, 'insured_esmokerNoNicotine_0')]");
        }
        if (flux.getInfoAssureComplets().get(index).getTravailManuel()) {
            element.clickByXpath(".//input[contains(@id, 'insured_insdManualWork_0')]");
        }
        if (flux.getInfoAssureComplets().get(index).getProduitDanger()) {
            element.clickByXpath(".//input[contains(@id, 'insured_dangerousProduct_0')]");
        }
        if (flux.getInfoAssureComplets().get(index).getTravailManuelManuLourde()) {
            element.clickByXpath(".//input[contains(@id, 'insured_insdManualWorkRisk_0')]");
        }
        if (flux.getInfoAssureComplets().get(index).getMetierExpose()) {
            element.clickByXpath(".//input[contains(@id, 'insured_workRisk_0')]");
        }
        if (flux.getInfoAssureComplets().get(index).getSportRisque()) {
            element.clickByXpath(".//input[contains(@id, 'insured_insdSportsActivities_0')]");
        }

        if (flux.getPersonnes().size() == 2) {
            index = 1;
            element.clickByXpath("//button[@class=\"btn\" and text()=\" Ajouter un assuré \"]");
            if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur")) {
                element.clickByXpath("//select[contains(@id, 'insured_insdGender_1')]");
                WaitUtils.sleepMs(3000);
                element.clickByXpath(".//option[text()='Monsieur']");
            } else if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame")) {
                WaitUtils.sleepMs(3000);
                element.clickByXpath("//select[contains(@id, 'insured_insdGender_1')]");
                element.clickByXpath(".//option[text()='Madame']");
            }
            //Nom
            element.typeByXpath("//input[contains(@id, 'insured_lastname_1')]", flux.getPersonnes().get(index).getPrenom());
            //Prenom
            element.typeByXpath("//input[contains(@id, 'insured_firstname_1')]", flux.getPersonnes().get(index).getNom());
            //Date naissance
            element.typeByXpath("//input[contains(@id, 'insured_insdDateOfBirthDt_1')]", flux.getPersonnes().get(index).getDateNaissance());
            //Ville naissance
            element.typeByXpath("//input[contains(@id, 'insured_cityOfBirth_1')]", flux.getPersonnes().get(index).getVille());
            //Email
            element.typeByXpath("//input[contains(@id, 'insured_email_1')]", flux.getPersonnes().get(index).getEmail());
            //Telephone
            element.typeByXpath("//input[contains(@id, 'insured_mobilePhoneNumber_1')]", flux.getPersonnes().get(index).getTelephone());
            //adresse
            element.typeByXpath("//input[contains(@id, 'insured_address1') and contains(@id, 'addressline1')]", flux.getPersonnes().get(index).getNumeroVoie() + " " + flux.getPersonnes().get(index).getNomVoie());
            element.typeByXpath("//input[contains(@id, 'insured_address1') and contains(@id, 'addresscity')]", flux.getPersonnes().get(index).getVille());
            element.typeByXpath("//input[contains(@id, 'insured_address1') and contains(@id, 'addresszipcode')]", flux.getPersonnes().get(index).getCodePostal());
            element.typeByXpath("//input[contains(@id, 'insured_address1') and contains(@id, 'addresscountry')]", flux.getPersonnes().get(index).getPays());
            WaitUtils.sleepMs(2000);
            elementNeutre();
            if (flux.getPersonnes().get(index).getNationalite().equalsIgnoreCase("Française")) {
                element.typeByXpath("/html[1]/body[1]/form[1]/main[1]/form[1]/div[2]/form[1]/form[1]/form[1]/form[1]/form[1]/div[1]/div[3]/div[1]/div[2]/div[2]/form[1]/div[2]/div[4]/div[1]/div[1]/div[1]/div[1]/div[1]/input[1]", "France");
            }
            WaitUtils.sleepMs(1000);
            elementNeutre();
            // Habitudes de vie
            //Profession
            scrollDown(0, 500);
            choixCategorieProfession(flux, index);
            //Profession exacte
            element.typeByXpath("//input[contains(@id, 'insured_exactJob_1')]", flux.getPersonnes().get(index).getProfession());
            //Regime social
            WaitUtils.sleepMs(2000);
            choixRegime(flux, index);
            //Déplacements professionnels
            choixDeplacementProfessionnelle(flux, index);
            //Travail hauteur
            choixTravailHauteur(flux, index);

            if (flux.getInfoAssureComplets().get(index).getFumeur()) {
                element.clickByXpath(".//input[contains(@id, 'insured_insdSmoker_1')]");
            }
            if (flux.getInfoAssureComplets().get(index).getFumeurSansNico()) {
                element.clickByXpath(".//input[contains(@id, 'insured_insdESmoker_1')]");
            }
            if (flux.getInfoAssureComplets().get(index).getFumeurElecNico()) {
                element.clickByXpath(".//input[contains(@id, 'insured_esmokerNoNicotine_1')]");
            }
            if (flux.getInfoAssureComplets().get(index).getTravailManuel()) {
                element.clickByXpath(".//input[contains(@id, 'insured_insdManualWork_1')]");
            }
            if (flux.getInfoAssureComplets().get(index).getProduitDanger()) {
                element.clickByXpath(".//input[contains(@id, 'insured_dangerousProduct_1')]");
            }
            if (flux.getInfoAssureComplets().get(index).getTravailManuelManuLourde()) {
                element.clickByXpath(".//input[contains(@id, 'insured_insdManualWorkRisk_1')]");
            }
            if (flux.getInfoAssureComplets().get(index).getMetierExpose()) {
                element.clickByXpath(".//input[contains(@id, 'insured_workRisk_1')]");
            }
            if (flux.getInfoAssureComplets().get(index).getSportRisque()) {
                element.clickByXpath(".//input[contains(@id, 'insured_insdSportsActivities_1')]");
            }
        }
    }

    private void remplirInformationsPret(FluxData flux) {
        int index = 0;
        //Type de pret
        choixTypePret(flux, index);
        //Montant
        element.typeByXpath(".//input[contains(@id, 'loan_loanAmount_0')]", flux.getPrets().get(index).getMontantPret());
        //Duree
        element.typeByXpath(".//input[contains(@id, 'loan_loanDuration_0')]", flux.getPrets().get(index).getDuree());
        //Taux
        element.typeByXpath(".//input[contains(@id, 'loan_loanRate_0')]", flux.getPrets().get(index).getTaux());
        //Date effet
        element.typeByXpath(".//input[contains(@id, 'loan_effectiveDate_0')]", flux.getPrets().get(index).getDateEffet());
        //Type de différé
        choixTypeDiffere(flux, index);

        if (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Partiel")) {
            element.typeByXpath(".//input[contains(@id, 'loan_loanDeferredDuration2')]", flux.getPrets().get(index).getDureeDiffere());
        }
        if (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Total")) {
            element.typeByXpath(".//input[contains(@id, 'loan_loanDeferredDuration2')]", flux.getPrets().get(index).getDureeDiffere());
        }

        //Objet financement
        choixObjetFinancement(flux, index);

        //Banque
        choixBanque(flux, index);
        choixFiliale(flux, index);
        scrollDown(0, 800);
        WaitUtils.sleepMs(1000);
        if (flux.getPrets().size() == 2) {
            WaitUtils.sleepMs(3000);
            element.clickByXpath("//button[contains(@class, 'btn') and contains(text(), 'Ajouter un prêt')]");
            index = 1;
            choixTypePret(flux, index);
            //Montant
            element.typeByXpath(".//input[contains(@id, 'loan_loanAmount_1')]", flux.getPrets().get(index).getMontantPret());
            //Duree
            element.typeByXpath(".//input[contains(@id, 'loan_loanDuration_1')]", flux.getPrets().get(index).getDuree());
            //Taux
            element.typeByXpath(".//input[contains(@id, 'loan_loanRate_1')]", flux.getPrets().get(index).getTaux());
            //Date effet
            element.typeByXpath(".//input[contains(@id, 'loan_effectiveDate_1')]", flux.getPrets().get(index).getDateEffet());
            //Type de différé
            choixTypeDiffere(flux, index);

            if (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Partiel")) {
                element.typeByXpath(".//input[contains(@id, 'loan_loanDeferredDuration2_1')]", flux.getPrets().get(index).getDureeDiffere());
            }
            if (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Total")) {
                element.typeByXpath(".//input[contains(@id, 'loan_loanDeferredDuration2_1')]", flux.getPrets().get(index).getDureeDiffere());
            }

            choixObjetFinancement(flux, index);
            //Banque
            choixBanque(flux, index);
            choixFiliale(flux, index);
        }
    }

    private void choixDeplacementProfessionnelle(FluxData flux, int index) {
        WebElement dropdownDepPro = null;
        if (index == 0){
            dropdownDepPro = waitForElement(driver, By.xpath(".//select[contains(@id, 'insured_insdAnnualMilage')]"), 10, 1);
        }
        if (index == 1) {
            dropdownDepPro = waitForElement(driver, By.xpath("//select[contains(@id, 'insured_insdAnnualMilage_1')]"), 10, 1);
        }
        if (dropdownDepPro != null) {
            Select selectDepPro = new Select(dropdownDepPro);
            if (!flux.getInfoAssureComplets().get(index).getDeplacementPro20000())
                selectDepPro.selectByVisibleText("Moins de 10 000");
            if (flux.getInfoAssureComplets().get(index).getDeplacementPro20000())
                selectDepPro.selectByVisibleText("20 001 - 25 000");
        }
    }

    private void choixTravailHauteur(FluxData flux, int index) {
        WebElement dropdownTravailHauteur = null;
        if (index == 0){
            WaitUtils.sleepMs(1000);
            dropdownTravailHauteur = waitForElement(driver, By.xpath("//select[contains(@id, 'insured_insdWorkAtHeight')]"), 10, 1);
        }
        if (index == 1) {
            WaitUtils.sleepMs(1000);
            dropdownTravailHauteur = waitForElement(driver, By.xpath("//select[contains(@id, 'insured_insdWorkAtHeight_1')]"), 10, 1);
        }
        if (dropdownTravailHauteur != null) {
            if (flux.getInfoAssureComplets().get(index).getTravailHauteur()) {
                Select selectTravailHauteur = new Select(dropdownTravailHauteur);
                if (flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("0-3"))
                    selectTravailHauteur.selectByVisibleText("0 - 3");
                else if (flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("3-10"))
                    selectTravailHauteur.selectByVisibleText("3 - 10");
                else if (flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("10-12m"))
                    selectTravailHauteur.selectByVisibleText("10 - 12");
                else if (flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("12-15m"))
                    selectTravailHauteur.selectByVisibleText("12 - 15");
                else if (flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("15-20m"))
                    selectTravailHauteur.selectByVisibleText("15 - 20");
                else if (flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("Plus de 20"))
                    selectTravailHauteur.selectByVisibleText("Plus de 20m");
            }
        }
    }

    private void choixRegime(FluxData flux, int index) {
        if (flux.getPersonnes().get(index).getRegime() != null) {
            WebElement dropdownRegimeSocial = null;
            WaitUtils.sleepMs(2000);
            if (index == 0) {
                WaitUtils.sleepMs(1000);
                dropdownRegimeSocial = waitForElement(driver, By.xpath(".//select[contains(@id, 'insured_socialRegime')]"), 10, 1);
            }
            if (index == 1) {
                WaitUtils.sleepMs(1000);
                dropdownRegimeSocial = waitForElement(driver, By.xpath("//select[contains(@id, 'insured_socialRegime_1')]"), 10, 1);
            }
            if (dropdownRegimeSocial != null) {
                Select selectRegimeSocial = new Select(dropdownRegimeSocial);
                WaitUtils.sleepMs(2000);
                selectRegimeSocial.selectByVisibleText(flux.getPersonnes().get(index).getRegime());
            }
        }
    }

    private void choixCategorieProfession(FluxData flux, int index) {
        WebElement dropdownProfession = null;
        if (index == 0){
            WaitUtils.sleepMs(1000);
            dropdownProfession = waitForElement1(driver, By.xpath(".//select[contains(@id, 'insured_professionalCategory')]"), 10, 1);
        }
        if (index == 1) {
            WaitUtils.sleepMs(1000);
            dropdownProfession = waitForElement1(driver, By.xpath("//select[contains(@id, 'insured_professionalCategory_1')]"), 10, 1);
        }
        if (dropdownProfession != null) {
            Select selectProfession = new Select(dropdownProfession);
            WaitUtils.sleepMs(2000);
            selectProfession.selectByVisibleText(flux.getPersonnes().get(index).getProfessionSpecifique());
        }
    }

    private void choixTypePret(FluxData flux, int index) {
        WebElement dropdownTypePret = null;
        if (index == 0){
            dropdownTypePret = waitForElement1(driver, By.xpath(".//select[contains(@id, 'loan_loanType_0')]"), 30, 1);
        }
        if (index == 1) {
            dropdownTypePret = waitForElement1(driver, By.xpath(".//select[contains(@id, 'loan_loanType_1')]"), 30, 1);
        }
        if (dropdownTypePret != null) {
            Select selectTypePret = new Select(dropdownTypePret);
            WaitUtils.sleepMs(1000);
            selectTypePret.selectByVisibleText(flux.getPrets().get(index).getType());
        }
    }

    private void choixObjetFinancement(FluxData flux, int index) {
        WebElement dropdownObjetFinancement = null;
        if (index == 0){
            dropdownObjetFinancement = waitForElement(driver, By.xpath(".//select[contains(@id, 'loan_loanPurposeOfFinancing_0')]"), 30, 1);
        }
        if (index == 1) {
            dropdownObjetFinancement = waitForElement(driver, By.xpath(".//select[contains(@id, 'loan_loanPurposeOfFinancing_1')]"), 30, 1);
        }
        if (dropdownObjetFinancement != null) {
            Select selectObjetFinancement = new Select(dropdownObjetFinancement);
            WaitUtils.sleepMs(1000);
            selectObjetFinancement.selectByVisibleText(flux.getPrets().get(index).getObjet());
        }
    }

    private void choixBanque(FluxData flux, int index) {
        WebElement dropdownBanque = null;
        if (index == 0){
            dropdownBanque = waitForElement(driver, By.xpath(".//select[contains(@id, 'loan_loan.bankInformation_bankCode_0')]"), 30, 1);
        }
        if (index == 1) {
            dropdownBanque = waitForElement(driver,By.xpath(".//select[contains(@id, 'loan_loan.bankInformation_bankCode_1')]"), 30, 1);
        }
        if (dropdownBanque != null) {
            Select selectBanque = new Select(dropdownBanque);
            List<WebElement> options = selectBanque.getOptions();
            for (WebElement option : options) {
                if ((option.getText().contains("AXA") || option.getText().equalsIgnoreCase("AXA")) && flux.getPrets().get(index).getBanque().equalsIgnoreCase("Axa banque")) {
                    js.executeScript("arguments[0].scrollIntoView(true);", option);
                    option.click();
                    scrollDown(0,-450);
                    break;
                }
                if ((option.getText().contains("BNP Paribas") || option.getText().equalsIgnoreCase("BNP Paribas")) && flux.getPrets().get(index).getBanque().equalsIgnoreCase("BNP Paribas")) {
                    js.executeScript("arguments[0].scrollIntoView(true);", option);
                    option.click();
                    scrollDown(0,-450);
                    break;
                }
                if ((option.getText().contains("Caisse d\\'épargne") || option.getText().equalsIgnoreCase("Caisse d\\'épargne")) && flux.getPrets().get(index).getBanque().equalsIgnoreCase("Caisse d\\'épargne")) {
                    js.executeScript("arguments[0].scrollIntoView(true);", option);
                    option.click();
                    scrollDown(0,-450);
                    break;
                }
                if ((option.getText().contains("Caisse agricole") || option.getText().equalsIgnoreCase("Caisse agricole")) && flux.getPrets().get(index).getBanque().equalsIgnoreCase("Caisse agricole")) {
                    js.executeScript("arguments[0].scrollIntoView(true);", option);
                    option.click();
                    scrollDown(0,-450);
                    break;
                }
                if ((option.getText().contains("Crédit Mutuel") || option.getText().equalsIgnoreCase("Crédit Mutuel")) && flux.getPrets().get(index).getBanque().equalsIgnoreCase("Crédit Mutuel")) {
                    js.executeScript("arguments[0].scrollIntoView(true);", option);
                    option.click();
                    scrollDown(0,-450);
                    break;
                }
                if ((option.getText().contains("LCL") || option.getText().equalsIgnoreCase("LCL")) && flux.getPrets().get(index).getBanque().equalsIgnoreCase("LCL")) {
                    js.executeScript("arguments[0].scrollIntoView(true);", option);
                    option.click();
                    scrollDown(0,-450);
                    break;
                }
                if ((option.getText().contains("CIC") || option.getText().equalsIgnoreCase("CIC")) && flux.getPrets().get(index).getBanque().equalsIgnoreCase("CIC")) {
                    js.executeScript("arguments[0].scrollIntoView(true);", option);
                    option.click();
                    scrollDown(0,-450);
                    break;
                }
                if ((option.getText().contains("La Banque Postale") || option.getText().equalsIgnoreCase("La Banque Postale")) && flux.getPrets().get(index).getBanque().equalsIgnoreCase("La Banque Postale")) {
                    js.executeScript("arguments[0].scrollIntoView(true);", option);
                    option.click();
                    scrollDown(0,-450);
                    break;
                }
                if ((option.getText().contains("Société Générale") || option.getText().equalsIgnoreCase("Société Générale")) && flux.getPrets().get(index).getBanque().equalsIgnoreCase("Société Générale")) {
                    js.executeScript("arguments[0].scrollIntoView(true);", option);
                    option.click();
                    scrollDown(0,-450);
                    break;
                }
            }
        }
    }

    private static final Set<String> BANQUES_SANS_FILIALE = Set.of(
            "Axa banque", "AXA", "BoursoBank", "FORTUNEO", "La Banque Postale"
    );

    private void choixFiliale(FluxData flux, int index) {

        String banque = flux.getPrets().get(index).getBanque().trim();

        // Si la banque n'a pas de filiale à choisir → on ne fait rien
        if (BANQUES_SANS_FILIALE.contains(banque)) {
            return;
        }

        // Sinon : choisir la première option du select
        String xpath = index == 0
                ? ".//select[contains(@id, 'loan_bankInformation_agencyCode_0')]"
                : ".//select[contains(@id, 'loan_bankInformation_agencyCode_1')]";

        WebElement selectElement = waitForElement(driver, By.xpath(xpath), 30, 1);
        Select select = new Select(selectElement);
        // Sélection de la première option
        select.selectByIndex(0);
    }

    private void choixTypeDiffere(FluxData flux, int index) {
        WebElement dropdownTypeDiffere = null;
        if (index == 0){
            WaitUtils.sleepMs(1000);
            dropdownTypeDiffere = driver.findElement(By.xpath(".//select[contains(@id, 'loan_loanDeferredType2_0')]"));
        }
        if (index == 1) {
            WaitUtils.sleepMs(1000);
            dropdownTypeDiffere = driver.findElement(By.xpath(".//select[contains(@id, 'loan_loanDeferredType2_1')]"));
        }
        if (dropdownTypeDiffere != null) {
            Select selectTypeDiffere = new Select(dropdownTypeDiffere);
            if (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Partiel")) {
                selectTypeDiffere.selectByValue("AUCUN");
            } else if (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Total")) {
                selectTypeDiffere.selectByValue("AUCUN");
            }
        }

    }

    private void choixGaranties(FluxData flux) {
        if (flux.getInfoAssureComplets().get(0).getGarantie().equalsIgnoreCase("IPT")){

        } else if (flux.getInfoAssureComplets().get(0).getGarantie().equalsIgnoreCase("IPT, ITT")){
            element.clickByXpath("//td[@cov-code='IPP']");
        }
//        WebElement couvertureOption = waitForElement(By.xpath("//input[contains(@name,'DOS_PSY')]"), 10, 1);
//        couvertureOption.click();
    }

}
