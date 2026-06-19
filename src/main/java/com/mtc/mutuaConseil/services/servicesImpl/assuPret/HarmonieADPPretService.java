package com.mtc.mutuaConseil.services.servicesImpl.assuPret;

import com.mtc.mutuaConseil.models.Compte;
import com.mtc.mutuaConseil.models.FluxData;
import com.mtc.mutuaConseil.models.Tarif;
import com.mtc.mutuaConseil.services.LaunchedService;
import com.mtc.mutuaConseil.services.servicesImpl.TypeAssuranceService;
import com.mtc.mutuaConseil.utils.*;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

import static com.mtc.mutuaConseil.utils.Shared.stringToDouble;

@Service
public class HarmonieADPPretService extends PageElementInteraction implements LaunchedService {

    private static final Logger log = LoggerFactory.getLogger(HarmonieADPPretService.class);
    private final String source ="Harmonie";
    private static WebDriver driver;
    private static Actions actions;
    private JavascriptExecutor js = null;
    private final TypeAssuranceService typeAssuranceService;

    public HarmonieADPPretService(TypeAssuranceService typeAssuranceService){
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        log.info("Debut de traitement -- Harmonie");
        Tarif tarifHarmonie = TarifUtils.createDefaultTarif(c, typeAssuranceService, 1L);
        driver = new EdgeDriver();
        driver.manage().window().maximize();
        actions = new Actions(driver);
        js = (JavascriptExecutor) driver;
        driver.get(c.getUrlFournisseur());
        try {
            WebElement inputlogin = waitForElementExplicit(driver, By.id("edit-name"), 60);
            inputlogin.sendKeys(c.getUsername());
            WebElement inputPassword = waitForElementExplicit(driver, By.id("edit-pass"), 60);
            inputPassword.sendKeys(c.getPassword());
            clicButtonByXpath(driver, "//span[normalize-space()='Se connecter']", actions);
            WebElement dropdownProduit = waitForElementExplicit(driver, By.id("edit-produit"), 60);
            Select selectProduit = new Select(dropdownProduit);
            List<WebElement> optionsProduit = selectProduit.getOptions();
            for (WebElement webElement: optionsProduit){
                if (webElement.getText().contains("EMPRUNTEUR")){
                    selectProduit.selectByValue("14995");
                    break;
                }
            }
            waitThread(1);
            WebElement buttonSelectionner = waitForElementExplicit(driver, By.xpath("//span[normalize-space()='Sélectionner']"), 60);
            buttonSelectionner.click();

            informationsGenerales(flux);

            nextPage(driver, By.id("edit-next"), 60, 1);

            remplirInformationsPersonne(flux, source);

            waitThread(1);
            nextPage(driver, By.id("edit-next"), 60, 1);

            remplirInformationsPretEtGaranties(flux, source);

            waitThread(7);
            WebElement buttonCalculer = waitForElement1(driver, By.xpath("//*[@name=\"calculer\"]"), 60, 1);
            waitThread(2);
            if(buttonCalculer != null  && buttonCalculer.isEnabled())
               buttonCalculer.click();
            else {
                log.warn("buttonCalculer is not found");
                return tarifHarmonie;
            }
            waitThread(12);
            WebElement strongCotisation = waitForElement1(driver, By.xpath("//*[@id=\"edit-tarificateur\"]/div[3]/div/strong[1]"), 60, 1);
            String cout ="";
            if (strongCotisation != null && strongCotisation.isEnabled()) {
                cout = strongCotisation.getText();
                log.info("Cout {} : ", cout);
            } else {
                log.warn("strongCotisation is not found");
                return tarifHarmonie;
            }
            if (cout != null){
                int startIndex = cout.indexOf(":") + 2;
                int endIndex = cout.indexOf(" €");
                if (startIndex > 0 && endIndex > startIndex) {
                    cout = cout.substring(startIndex, endIndex);
                    log.info("Le prix est : {}", cout);
                }
            }
            tarifHarmonie.setMontant(cout);
            String screenshotBytes = captureScreenshot(driver, tarifHarmonie.getNom(), false, tarifHarmonie);
            if (screenshotBytes != null) {
                tarifHarmonie.setCaptureImg(screenshotBytes);
            }
            tarifHarmonie.setExecution(true);
        } catch (Exception e) {
            log.error("An error occurred : ", e);
            tarifHarmonie.setErreur(e.getMessage());
            String screenshotBytesErreur = captureScreenshot(driver, tarifHarmonie.getNom(), true, tarifHarmonie);
            tarifHarmonie.setCaptureImgErreur(screenshotBytesErreur);
            tarifHarmonie.setEtape("");
        } finally {
            driver.quit();
        }
        return tarifHarmonie;
    }

    private void informationsGenerales(FluxData flux){
        choixObjetPret(flux);
        WebElement dropdownAvanceProjet = waitForElementExplicit(driver, By.id("edit-field-de-avancement-projet"), 60);
        Select selectAvanceProjet = new Select(dropdownAvanceProjet);
        List<WebElement> optionsAvanceProjet = selectAvanceProjet.getOptions();
        for (WebElement webElement: optionsAvanceProjet){
            if (webElement.getText().contains("Demande d'information")){
                selectAvanceProjet.selectByValue("demande_info");
                break;
            }
        }

        waitThread(2);

        if (flux.getPersonnes().size() == 2) {
            WebElement radioNbAssure2 = waitForElementExplicit(driver, By.xpath("//div[@id=\"edit-field-de-nombre-emprunteur\"]//div[2]"), 60);
            actions.moveToElement(radioNbAssure2).click().perform();
        } else {
            WebElement radioNbAssure1 = waitForElementExplicit(driver, By.xpath("//div[@id=\"edit-field-de-nombre-emprunteur\"]//div[1]"), 60);
            actions.moveToElement(radioNbAssure1).click().perform();
        }

        waitThread(1);

        if (flux.getPrets().size() == 1) {
            WebElement nbPretAssure = waitForElementExplicit(driver, By.xpath("//div[@id=\"edit-field-de-nombre-pret\"]//div[1]"), 60);
            nbPretAssure.click();
        } else if (flux.getPrets().size() == 2) {
            WebElement nbPretAssure = waitForElementExplicit(driver, By.xpath("//div[@id=\"edit-field-de-nombre-pret\"]//div[2]"), 60);
            nbPretAssure.click();
        } else if (flux.getPrets().size() == 3) {
            WebElement nbPretAssure = waitForElementExplicit(driver, By.xpath("//div[@id=\"edit-field-de-nombre-pret\"]//div[3]"), 60);
            nbPretAssure.click();
        }

        WebElement radioPretAssure = waitForElementExplicit(driver, By.xpath("//div[@id=\"edit-field-de-p-est-il-assure\"]//div[1]"), 60);
        radioPretAssure.click();

        waitThread(3);
        WebElement radioProposition = waitForElementExplicit(driver, By.xpath("//div[@id=\"edit-field-de-nature-proposition\"]//div[1]"), 60);
        radioProposition.click();
    }

    private void remplirInformationsPersonne(FluxData flux, String source) {
        int index = 0;
        if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur")) {
            waitThread(2);
            WebElement radioCivilite = waitForElement(driver,By.xpath("//div[@id=\"edit-field-de-civilite-assure-1\"]//div[1]"), 60, 1);
            actions.moveToElement(radioCivilite).click().perform();
        } else if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame")){
            waitThread(2);
            WebElement radioCivilite = LibSelenium.waitForElement(driver,By.xpath("//div[@id=\"edit-field-de-civilite-assure-1\"]//div[2]"), 60, 1);
            actions.moveToElement(radioCivilite).click().perform();
        }
       // InformationsPersonne.infoCivilite(driver, By.xpath("//div[@id=\"edit-field-de-civilite-assure-1\"]//div[1]"), 10, 2, flux, index, source);
        InformationsPersonne.infoNom(driver, By.id("edit-field-de-assure-prenom-0-value"), 10, 2, flux, index, source);
        InformationsPersonne.infoPrenom(driver, By.id("edit-field-de-assure-1-nom-usage-0-value"), 10, 2, flux, index, source);
        InformationsPersonne.infoDateNaissance(driver, By.id("edit-field-de-assure-date-naissance-0-day"), 10, 2, flux, index, source);
        InformationsPersonne.infoNom(driver, By.id("edit-field-de-assure-nom-naissance-0-value"), 10, 2, flux, index, source);
        InformationsPersonne.infoVille(driver, By.id("edit-field-de-assure-1-lieu-naissance-0-value"), 10, 2, flux, index, source);

        if(!flux.getInfoAssureComplets().get(index).getFumeur()) {
            WebElement radioFumeur = waitForElementExplicit(driver, By.xpath("//div[@id=\"edit-field-de-assure-fumeur\"]//div[2]"), 60);
            radioFumeur.click();
        } else {
            WebElement radioFumeur = waitForElementExplicit(driver, By.xpath("//div[@id=\"edit-field-de-assure-fumeur\"]//div[1]"), 60);
            radioFumeur.click();
        }
        choixStatutProfession(flux, index);

        InformationsPersonne.infoStatutProfession(driver, By.xpath("//select[@id=\"edit-field-de-assure-statut\"]"), 10, 2, flux, index, source);
        WebElement inputProfession = waitForElementExplicit(driver, By.id("edit-field-de-assure-profession-0-value"), 60);
        inputProfession.sendKeys(flux.getPersonnes().get(index).getProfession());

        selectKm(flux, index);

        if (!flux.getInfoAssureComplets().get(index).getTravailManuel()) {
            WebElement radioManutention = waitForElementExplicit(driver, By.xpath("//div[@id=\"edit-field-assure-travaux-manuels\"]//div[2]"), 60);
            radioManutention.click();
        } else if (flux.getInfoAssureComplets().get(index).getTravailManuel()) {
            WebElement radioManutention = waitForElementExplicit(driver, By.xpath("//div[@id=\"edit-field-assure-travaux-manuels\"]//div[1]"), 60);
            radioManutention.click();
        }

        if (flux.getInfoAssureComplets().get(index).getTravailHauteur()) {
            if ((flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("15 à 20m") || flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("Plus de 20m"))) {
                WebElement radioHauteur = waitForElementExplicit(driver, By.xpath("//div[@id=\"edit-field-assure-hauteur\"]//div[1]"), 60);
                radioHauteur.click();
            } else {
                WebElement radioHauteur = waitForElementExplicit(driver, By.xpath("//div[@id=\"edit-field-assure-hauteur\"]//div[2]"), 60);
                radioHauteur.click();
            }
        }
        if (!flux.getInfoAssureComplets().get(index).getTravailHauteur()) {
            WebElement radioHauteur = waitForElementExplicit(driver, By.xpath("//div[@id=\"edit-field-assure-hauteur\"]//div[2]"), 60);
            radioHauteur.click();
        }

        if (stringToDouble(flux.getPrets().get(index).getMontantPret()) > 200000) {
            WebElement radioCapitaux = waitForElementExplicit(driver, By.xpath("//div[@id=\"edit-field-de-totaux-capitaux\"]//div[2]"), 60);
            radioCapitaux.click();
        } else {
            WebElement radioCapitaux = waitForElementExplicit(driver, By.xpath("//div[@id=\"edit-field-de-totaux-capitaux\"]//div[1]"), 60);
            radioCapitaux.click();
            waitThread(2);
            WebElement radioTempsPlein = waitForElementExplicit(driver, By.xpath("//div[@id=\"edit-field-de-temps-plein\"]//div[2]"), 60);
            radioTempsPlein.click();
        }
        waitThread(2);
//        InformationsPersonne.infoPays(driver, By.xpath("//select[@id=\"edit-field-de-adresse-a1-pays\"]"), 10, 2, flux, index, source);
        InformationsPersonne.infoAdresse(driver, By.id("edit-field-de-adresse-a1-adresse-0-value"), 10, 2, flux, index, source);
        InformationsPersonne.infoCodePostal(driver, By.id("edit-field-de-adresse-a1-cp-0-value"), 10, 2, flux, index, source);
        InformationsPersonne.infoEmail(driver, By.id("edit-field-de-assure-courriel-0-value"), 10, 2, flux, index, source);
        InformationsPersonne.infoTelephone(driver, By.id("edit-field-de-assure-portable-0-value"), 10, 2, flux, index, source);
        // verification à faire
        if (flux.getPersonnes().size() == 2) {
            index = 1;
            if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur")) {
                waitThread(2);
                WebElement radioCivilite = waitForElement(driver,By.xpath("//div[@id=\"edit-field-de-civilite-assure-2\"]//div[1]"), 60, 1);
                actions.moveToElement(radioCivilite).click().perform();
            } else if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame")){
                waitThread(2);
                WebElement radioCivilite = LibSelenium.waitForElement(driver,By.xpath("//div[@id=\"edit-field-de-civilite-assure-2\"]//div[1]"), 60, 1);
                actions.moveToElement(radioCivilite).click().perform();
            }
            InformationsPersonne.infoNom(driver, By.id("edit-field-de-assure-2-prenom-0-value"), 10, 2, flux, index, source);
            InformationsPersonne.infoPrenom(driver, By.id("edit-field-de-assure-2-nom-usage-0-value"), 10, 2, flux, index, source);
            InformationsPersonne.infoDateNaissance(driver, By.id("edit-field-de-assure-2-date-naissance-0-day"), 10, 2, flux, index, source);
            InformationsPersonne.infoNom(driver, By.id("edit-field-de-assure-2-nom-naissance-0-value"), 10, 2, flux, index, source);
            InformationsPersonne.infoVille(driver, By.id("edit-field-de-assure-2-lieu-naissance-0-value"), 10, 2, flux, index, source);

            if (!flux.getInfoAssureComplets().get(index).getFumeur()) {
                WebElement radioFumeur = waitForElementExplicit(driver, By.xpath("//div[@id=\"edit-field-de-assure-2-fumeur\"]//div[2]"), 60);
                radioFumeur.click();
            } else {
                WebElement radioFumeur = waitForElementExplicit(driver, By.xpath("//div[@id=\"edit-field-de-assure-2-fumeur\"]//div[1]"), 60);
                radioFumeur.click();
            }

            choixStatutProfession(flux, index);

            WebElement inputProfession1 = waitForElementExplicit(driver, By.xpath("//input[@id=\"edit-field-de-assure-2-profession-0-value\"]"), 60);
            inputProfession1.sendKeys(flux.getPersonnes().get(index).getProfession());
            clickKeyEnter(actions);
            waitThread(2);

            selectKm(flux, index);

            if (!flux.getInfoAssureComplets().get(index).getTravailManuel()) {
                WebElement radioManutention = waitForElementExplicit(driver, By.xpath("//div[@id=\"edit-field-assure-2-travaux-manuels\"]//div[2]"), 60);
                radioManutention.click();
            } else if (flux.getInfoAssureComplets().get(index).getTravailManuel()) {
                WebElement radioManutention = waitForElementExplicit(driver, By.xpath("//div[@id=\"edit-field-assure-2-travaux-manuels\"]//div[1]"), 60);
                radioManutention.click();
            }

            if (flux.getInfoAssureComplets().get(index).getTravailHauteur()) {
                if ((flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("15 à 20m") || flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("Plus de 20m"))) {
                    WebElement radioHauteur = waitForElementExplicit(driver, By.xpath("//div[@id=\"edit-field-assure-2-hauteur\"]//div[1]"), 60);
                    radioHauteur.click();
                } else {
                    WebElement radioHauteur = waitForElementExplicit(driver, By.xpath("//div[@id=\"edit-field-assure-2-hauteur\"]//div[2]"), 60);
                    radioHauteur.click();
                }
            }
            if (!flux.getInfoAssureComplets().get(index).getTravailHauteur()) {
                WebElement radioHauteur = waitForElementExplicit(driver, By.xpath("//div[@id=\"edit-field-assure-2-hauteur\"]//div[2]"), 60);
                radioHauteur.click();
            }

            if(stringToDouble(flux.getPrets().get(0).getMontantPret()) > 200000) {
                WebElement radioCapitaux = waitForElementExplicit(driver, By.xpath("//div[@id=\"edit-field-de-totaux-capitaux-2\"]//div[2]"), 60);
                radioCapitaux.click();
            } else {
                WebElement radioCapitaux = waitForElementExplicit(driver, By.xpath("//div[@id=\"edit-field-de-totaux-capitaux-2\"]//div[2]"), 60);
                radioCapitaux.click();
            }

            waitThread(2);
//            InformationsPersonne.infoPays(driver, By.xpath("//select[@id=\"edit-field-de-adresse-a1-pays\"]"), 10, 2, flux, index, source);
            InformationsPersonne.infoAdresse(driver, By.xpath("//input[@id=\"edit-field-de-adresse-a2-adresse-0-value\"]"), 10, 2, flux, index, source);
            InformationsPersonne.infoCodePostal(driver, By.xpath("//input[@id=\"edit-field-de-adresse-a2-cp-0-value\"]"), 10, 2, flux, index, source);
            InformationsPersonne.infoEmail(driver, By.xpath("//input[@id=\"edit-field-de-assure-2-courriel-0-value\"]"), 10, 2, flux, index, source);
            InformationsPersonne.infoTelephone(driver, By.xpath("//input[@id=\"edit-field-de-assure-2-portable-0-value\"]"), 10, 2, flux, index, source);
        }

        InformationsPersonne.infoAdresse(driver, By.id("edit-field-de-adresse-future-adresse-0-value"), 10, 2, flux, index, source);
        InformationsPersonne.infoCodePostal(driver, By.id("edit-field-de-adresse-future-cp-0-value"), 10, 2, flux, index, source);
        WebElement selectVille = waitForElementExplicit(driver, By.xpath("//div[@id=\"field_de_adresse_future_ville_wrapper\"]//select"), 60);
        selectVille.click();
    }

    private void remplirInformationsPretEtGaranties(FluxData flux, String source) {
        int index = 0;
        choixPeriodicite();
        choixCodeTarif();
        InformationsPret.infoMontant(driver, By.xpath("//input[@id='edit-field-de-pret-1-0-inline-entity-form-field-pret-montant-0-value']"), 10, 2, flux, index);
        choixTypeTaux(flux, index);
        InformationsPret.infoTaux(driver, By.xpath("//input[@id='edit-field-de-pret-1-0-inline-entity-form-field-pret-taux-0-value']"), 10, 2, flux, index);
        InformationsPret.infoDateEffet(driver, By.xpath("//input[@id='edit-field-de-pret-1-0-inline-entity-form-field-pret-date-acceptation-0']"), 10, 2, flux, index);
        clickKeyEnter(actions);
        choixTypeDiffere(flux, index);
        InformationsPret.infoDuree(driver, By.xpath("//input[@id='edit-field-de-pret-1-0-inline-entity-form-field-pret-duree-0-value']"), 10, 2, flux, index);
        WebElement inputQuotiteDureeDiffere = waitForElementExplicit(driver, By.xpath("//input[@id=\"edit-field-de-pret-1-0-inline-entity-form-field-pret-differe-0-value\"]"), 60);
        if (!flux.getPrets().get(index).getDiffere().isEmpty() && (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Partiel") || flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Total"))) {
            inputQuotiteDureeDiffere.sendKeys(flux.getPrets().get(index).getDureeDiffere());
        } else {
            inputQuotiteDureeDiffere.sendKeys("");
        }
        choixCouverturePret(flux, index);

//        if (flux.getPrets().size() == 2 ) {
//            index = 1;
//            InformationsPret.infoMontant(driver, By.xpath("//input[@id='edit-field-de-pret-2-0-inline-entity-form-field-pret-montant-0-value']"), 10, 2, flux, index);
//            choixTypeTaux(flux, index);
//            InformationsPret.infoTaux(driver, By.xpath("//input[@id='edit-field-de-pret-2-0-inline-entity-form-field-pret-taux-0-value']"), 10, 2, flux, index);
//            InformationsPret.infoDateEffet(driver, By.xpath("//input[@id='edit-field-de-pret-2-0-inline-entity-form-field-pret-date-acceptation-0']"), 10, 2, flux, index);
//            clickEnter(actions);
//            InformationsPret.infoDuree(driver, By.xpath("//input[@id='edit-field-de-pret-2-0-inline-entity-form-field-pret-duree-0-value']"), 10, 2, flux, index);
//            choixTypeDiffere(flux, index);
//            InformationsPret.infoDiffere(driver, By.xpath("//input[@id='edit-field-de-pret-2-0-inline-entity-form-field-pret-differe-0-value']"), 10, 2, flux, index);
//            WebElement inputQuotiteDureeDiffere1 = waitForElementExplicit(driver, By.xpath("//input[@id=\"edit-field-de-pret-2-0-inline-entity-form-field-pret-differe-0-value\"]"), 60);
//            inputQuotiteDureeDiffere1.sendKeys("1");
////            choixCouverturePret(flux, index);
//        }

        WebElement buttonCalculer = waitForElementExplicit(driver, By.xpath("//*[@name=\"calculer\"]"), 60);
        buttonCalculer.click();

        if (flux.getPersonnes().size() == 2) {
            waitThread(15);
        } else {
            waitThread(10);
        }

        WebElement button = waitForElement1(driver, By.xpath("//div[@id='result-assure-1']//span[contains(normalize-space(text()), 'LA GARANTIE EMPRUNTEUR CRD')]"), 60, 1);
        button.click();
//        checkButtonGarantie(1);
        if (flux.getPersonnes().size() == 2) {
            checkButtonGarantie(2);
        }
//        WebElement buttonCalculer1 = waitForElementExplicit(driver, By.xpath("//button[contains(@id,\"edit-calculer\")]//span[text()=\"Calculer\"]"), 60);
//        buttonCalculer1.click();
    }

    private void checkButtonGarantie(int assure) {
        WebElement element = null;
        if( assure == 1) {
            waitThread(3);
            element = (WebElement) js.executeScript("return document.evaluate(\"//div[@id='result-assure-1']//span[contains(normalize-space(text()), 'LA GARANTIE EMPRUNTEUR CRD')]\", document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;");
            if (element != null) {
                element.click();
            }
            else if (element == null) {
                element = (WebElement) driver.findElement(By.xpath("//div[@id='result-assure-1']//span[contains(normalize-space(text()), 'LA GARANTIE EMPRUNTEUR CRD')]\n"));
                if (element != null) {
                    element.click();
                } else {
                    element = (WebElement) driver.findElement(By.xpath("//div[@id='result-assure-1']//div[contains(@class, 'form-item') and contains(@class, 'js-form-item') and contains(@class, 'form-type-checkbox')][1]\\n"));
                    element.click();
                }
            }

        }
        if( assure == 2) {
            waitThread(3);
            element = (WebElement) js.executeScript("return document.evaluate(\"//div[@id='result-assure-2']//span[contains(normalize-space(text()), 'LA GARANTIE EMPRUNTEUR CRD')]\", document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null).singleNodeValue;");
            if (element != null) {
                element.click();
            }
            else if (element == null) {
                element = (WebElement) driver.findElement(By.xpath("//div[@id='result-assure-2']//span[contains(normalize-space(text()), 'LA GARANTIE EMPRUNTEUR CRD')]\n"));
                if (element != null) {
                    element.click();
                } else {
                    element = (WebElement) driver.findElement(By.xpath("//div[@id='result-assure-2']//div[contains(@class, 'form-item') and contains(@class, 'js-form-item') and contains(@class, 'form-type-checkbox')][1]\\n"));
                    element.click();
                }
            }

        }

    }

    private void choixCouverturePret(FluxData flux, int index) {
        if (index == 0 ) {
            choixGarantieCouverturePret(flux, index);
            WebElement inputQuotiteDC = waitForElementExplicit(driver, By.xpath("//input[@id=\"edit-field-de-pret-1-0-inline-entity-form-field-garantie-a1-quotite-1-0-value\"]"), 60);
            inputQuotiteDC.sendKeys("50");
            choixFranchiseITT(flux, index);
            WebElement inputQuotiteItt = waitForElementExplicit(driver, By.xpath("//input[@id=\"edit-field-de-pret-1-0-inline-entity-form-field-garantie-a1-quotite-nv-1-0-value\"]"), 60);
            inputQuotiteItt.sendKeys("50");
            choixPrevoyanceCredit(flux, index);
            WebElement inputPrevoyanceCredit = waitForElementExplicit(driver, By.xpath("//input[@id=\"edit-field-de-pret-1-0-inline-entity-form-field-garantie-a1-quotite-2-0-value\"]"), 60);
            inputPrevoyanceCredit.sendKeys("50");
            choixPrevCFranchiseITT(flux, index);
            WebElement inputPrevCFranchiseItt = waitForElementExplicit(driver, By.xpath("//input[@id=\"edit-field-de-pret-1-0-inline-entity-form-field-garantie-a1-quotite-nv-2-0-value\"]"), 60);
            inputPrevCFranchiseItt.sendKeys("50");
        }
        if (flux.getPersonnes().size() == 2) {
            choixGarantieCouverturePret(flux, 1);
            WebElement inputQuotiteDC1 = waitForElementExplicit(driver, By.xpath("//input[@id=\"edit-field-de-pret-1-0-inline-entity-form-field-garantie-a2-quotite-1-0-value\"]"), 60);
            inputQuotiteDC1.sendKeys("50");
            choixFranchiseITT(flux, 1);
            WebElement inputQuotiteItt1 = waitForElementExplicit(driver, By.xpath("//input[@id=\"edit-field-de-pret-1-0-inline-entity-form-field-garantie-a2-quotite-nv-1-0-value\"]"), 60);
            inputQuotiteItt1.sendKeys("50");
            choixPrevoyanceCredit(flux, 1);
            WebElement inputPrevoyanceCredit1 = waitForElementExplicit(driver, By.xpath("//input[@id=\"edit-field-de-pret-1-0-inline-entity-form-field-garantie-a2-quotite-2-0-value\"]"), 60);
            inputPrevoyanceCredit1.sendKeys("50");
            choixPrevCFranchiseITT(flux, 1);
            WebElement inputPrevCFranchiseItt1 = waitForElementExplicit(driver, By.xpath("//input[@id=\"edit-field-de-pret-1-0-inline-entity-form-field-garantie-a2-quotite-nv-2-0-value\"]"), 60);
            inputPrevCFranchiseItt1.sendKeys("50");
        }
        if (flux.getPrets().size() == 2) {
            index = 1;
            InformationsPret.infoMontant(driver, By.xpath("//input[@id='edit-field-de-pret-2-0-inline-entity-form-field-pret-montant-0-value']"), 10, 2, flux, index);
            choixTypeTaux(flux, index);
            InformationsPret.infoTaux(driver, By.xpath("//input[@id='edit-field-de-pret-2-0-inline-entity-form-field-pret-taux-0-value']"), 10, 2, flux, index);
            InformationsPret.infoDateEffet(driver, By.xpath("//input[@id='edit-field-de-pret-2-0-inline-entity-form-field-pret-date-acceptation-0']"), 10, 2, flux, index);
            clickEnter(actions);
            InformationsPret.infoDuree(driver, By.xpath("//input[@id='edit-field-de-pret-2-0-inline-entity-form-field-pret-duree-0-value']"), 10, 2, flux, index);
            choixTypeDiffere(flux, index);
            InformationsPret.infoDiffere(driver, By.xpath("//input[@id='edit-field-de-pret-2-0-inline-entity-form-field-pret-differe-0-value']"), 10, 2, flux, index);
            WebElement inputQuotiteDureeDiffere1 = waitForElementExplicit(driver, By.xpath("//input[@id=\"edit-field-de-pret-2-0-inline-entity-form-field-pret-differe-0-value\"]"), 60);
            if (!flux.getPrets().get(index).getDiffere().isEmpty() && (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Partiel") || flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Total"))) {
                inputQuotiteDureeDiffere1.sendKeys(flux.getPrets().get(index).getDureeDiffere());
            } else {
                inputQuotiteDureeDiffere1.sendKeys("");
            }
            choixGarantieCouverturePret1(flux, 1);
            WebElement inputQuotitePret2 = waitForElementExplicit(driver, By.xpath("//input[@id=\"edit-field-de-pret-2-0-inline-entity-form-field-garantie-a1-quotite-1-0-value\"]"), 60);
            inputQuotitePret2.sendKeys("50");
            choixFranchiseITT1(flux, 1);
            WebElement inputQuotiteIttPret2 = waitForElementExplicit(driver, By.xpath("//input[@id=\"edit-field-de-pret-2-0-inline-entity-form-field-garantie-a1-quotite-nv-1-0-value\"]"), 60);
            inputQuotiteIttPret2.sendKeys("50");
            choixPrevoyanceCredit1(flux, 1);
            WebElement inputPrevoyanceCreditPret2 = waitForElementExplicit(driver, By.xpath("//input[@id=\"edit-field-de-pret-2-0-inline-entity-form-field-garantie-a1-quotite-2-0-value\"]"), 60);
            inputPrevoyanceCreditPret2.sendKeys("50");
            choixPrevCFranchiseITT1(flux, 1);
            WebElement inputPrevCFranchiseIttPret2 = waitForElementExplicit(driver, By.xpath("//input[@id=\"edit-field-de-pret-2-0-inline-entity-form-field-garantie-a1-quotite-nv-2-0-value\"]"), 60);
            inputPrevCFranchiseIttPret2.sendKeys("50");
        }

    }

    private void choixPrevCFranchiseITT(FluxData flux, int index) {
        WebElement dropdownPrevCFranchiseItt = null;
        if (index == 0) {
            dropdownPrevCFranchiseItt = waitForElementExplicit(driver, By.xpath("//select[@id=\"edit-field-de-pret-1-0-inline-entity-form-field-garantie-a1-franchiseitt-1\"]"), 60);
        } if (index == 1) {
            dropdownPrevCFranchiseItt = waitForElementExplicit(driver, By.xpath("//select[@id=\"edit-field-de-pret-1-0-inline-entity-form-field-garantie-a2-franchiseitt-2\"]"), 60);
        }
        if (dropdownPrevCFranchiseItt != null) {
            Select selectPrevCFranchiseItt = new Select(dropdownPrevCFranchiseItt);
            List<WebElement> optionsPrevCFranchiseItt = selectPrevCFranchiseItt.getOptions();
            for (WebElement webElement: optionsPrevCFranchiseItt){
                if (containsAllElements(webElement.getText(), flux.getInfoAssureComplets().get(0).getGarantie().toString())){
                    selectPrevCFranchiseItt.selectByValue("180");
                    break;
                }
            }
        }
    }

    private void choixPrevCFranchiseITT1(FluxData flux, int index) {
        WebElement dropdownPrevCFranchiseItt = null;
        if (index == 0) {
            dropdownPrevCFranchiseItt = waitForElementExplicit(driver, By.xpath("//select[@id=\"edit-field-de-pret-2-0-inline-entity-form-field-garantie-a1-franchiseitt-1\"]"), 60);
        } if (index == 1) {
            dropdownPrevCFranchiseItt = waitForElementExplicit(driver, By.xpath("//select[@id=\"edit-field-de-pret-2-0-inline-entity-form-field-garantie-a1-franchiseitt-2\"]"), 60);
        }
        if (dropdownPrevCFranchiseItt != null) {
            Select selectPrevCFranchiseItt = new Select(dropdownPrevCFranchiseItt);
            List<WebElement> optionsPrevCFranchiseItt = selectPrevCFranchiseItt.getOptions();
            for (WebElement webElement: optionsPrevCFranchiseItt){
                if (containsAllElements(webElement.getText(), flux.getInfoAssureComplets().get(0).getGarantie().toString())){
                    selectPrevCFranchiseItt.selectByValue("180");
                    break;
                }
            }
        }
    }

    private void choixPrevoyanceCredit(FluxData flux, int index) {
        WebElement dropdownPrevoyanceCredit = null;
        if (index == 0) {
            dropdownPrevoyanceCredit = waitForElementExplicit(driver, By.xpath("//select[@name=\"field_de_pret_1[0][inline_entity_form][field_garantie_a1_type_2]\"]"), 60);
        } if (index == 1) {
             dropdownPrevoyanceCredit = waitForElementExplicit(driver, By.xpath("//select[@id=\"edit-field-de-pret-1-0-inline-entity-form-field-garantie-a2-type-2\"]"), 60);
        }
        if (dropdownPrevoyanceCredit != null) {
            Select selectPrevoyanceCredit = new Select(dropdownPrevoyanceCredit);
            List<WebElement> optionsPrevoyanceCredit = selectPrevoyanceCredit.getOptions();
            for (WebElement webElement: optionsPrevoyanceCredit){
                if (containsAllElements(webElement.getText(), flux.getInfoAssureComplets().get(0).getGarantie().toString())){
                    selectPrevoyanceCredit.selectByValue("ipp");
                    break;
                }
            }
        }
    }

    private void choixPrevoyanceCredit1(FluxData flux, int index) {
        WebElement dropdownPrevoyanceCredit = null;
        if (index == 0) {
            dropdownPrevoyanceCredit = waitForElementExplicit(driver, By.xpath("//select[@name=\"edit-field-de-pret-2-0-inline-entity-form-field-garantie-a1-type-2\"]"), 60);
        } if (index == 1) {
            dropdownPrevoyanceCredit = waitForElementExplicit(driver, By.xpath("//select[@id=\"edit-field-de-pret-2-0-inline-entity-form-field-garantie-a1-type-2\"]"), 60);
        }
        if (dropdownPrevoyanceCredit != null) {
            Select selectPrevoyanceCredit = new Select(dropdownPrevoyanceCredit);
            List<WebElement> optionsPrevoyanceCredit = selectPrevoyanceCredit.getOptions();
            for (WebElement webElement: optionsPrevoyanceCredit){
                if (containsAllElements(webElement.getText(), flux.getInfoAssureComplets().get(0).getGarantie().toString())){
                    selectPrevoyanceCredit.selectByValue("ipp");
                    break;
                }
            }
        }
    }

    private void choixFranchiseITT(FluxData flux, int index) {
        WebElement dropdownFranchiseItt = null;
        if (index == 0) {
            dropdownFranchiseItt = waitForElementExplicit(driver, By.xpath("//select[@id=\"edit-field-de-pret-1-0-inline-entity-form-field-garantie-a1-franchiseitt-1\"]"), 60);
        } if (index == 1) {
            dropdownFranchiseItt = waitForElementExplicit(driver, By.xpath("//select[@id=\"edit-field-de-pret-1-0-inline-entity-form-field-garantie-a2-franchiseitt-1\"]"), 60);
        }
        if (dropdownFranchiseItt != null) {
            Select selectFranchiseItt = new Select(dropdownFranchiseItt);
            List<WebElement> optionsFranchiseItt = selectFranchiseItt.getOptions();
            for (WebElement webElement: optionsFranchiseItt){
                if (containsAllElements(webElement.getText(), flux.getInfoAssureComplets().get(0).getGarantie().toString())){
                    selectFranchiseItt.selectByValue("180");
                    break;
                }
            }
        }
    }

    private void choixFranchiseITT1(FluxData flux, int index) {
        WebElement dropdownFranchiseItt = null;
        if (index == 0) {
            dropdownFranchiseItt = waitForElementExplicit(driver, By.xpath("//select[@id=\"edit-field-de-pret-2-0-inline-entity-form-field-garantie-a1-franchiseitt-1\"]"), 60);
        } if (index == 1) {
            dropdownFranchiseItt = waitForElementExplicit(driver, By.xpath("//select[@id=\"edit-field-de-pret-2-0-inline-entity-form-field-garantie-a1-franchiseitt-1\"]"), 60);
        }
        if (dropdownFranchiseItt != null) {
            Select selectFranchiseItt = new Select(dropdownFranchiseItt);
            List<WebElement> optionsFranchiseItt = selectFranchiseItt.getOptions();
            for (WebElement webElement: optionsFranchiseItt){
                if (containsAllElements(webElement.getText(), flux.getInfoAssureComplets().get(0).getGarantie().toString())){
                    selectFranchiseItt.selectByValue("180");
                    break;
                }
            }
        }
    }

    private void choixGarantieCouverturePret(FluxData flux, int index) {
        WebElement dropdownCouverturePret = null;
        if (index == 0) {
            dropdownCouverturePret = waitForElementExplicit(driver, By.xpath("//select[@id=\"edit-field-de-pret-1-0-inline-entity-form-field-garantie-a1-type-1\"]"), 60);
        }
        if (index == 1) {
            dropdownCouverturePret = waitForElementExplicit(driver, By.xpath("//select[@id=\"edit-field-de-pret-1-0-inline-entity-form-field-garantie-a2-type-1\"]"), 60);
        }
        if (dropdownCouverturePret != null) {
            Select selectCouverturePret = new Select(dropdownCouverturePret);
            List<WebElement> optionsCouverturePret = selectCouverturePret.getOptions();
            for (WebElement webElement : optionsCouverturePret) {
                if (containsAllElements(webElement.getText(), flux.getInfoAssureComplets().get(0).getGarantie().toString())) {
                    selectCouverturePret.selectByValue("ipp");
                    break;
                }
//                else if (containsAllElements(webElement.getText(), flux.getInfoAssureComplets().get(0).getGarantie().toString())) {
//                    selectCouverturePret.selectByValue("itt");
//                    break;
//                }
            }
        }
    }

    private void choixGarantieCouverturePret1(FluxData flux, int index) {
        WebElement dropdownCouverturePret = null;
        if (index == 0) {
            dropdownCouverturePret = waitForElementExplicit(driver, By.xpath("//select[@id=\"edit-field-de-pret-2-0-inline-entity-form-field-garantie-a1-type-1\"]"), 60);
        }
        if (index == 1) {
            dropdownCouverturePret = waitForElementExplicit(driver, By.xpath("//select[@id=\"edit-field-de-pret-2-0-inline-entity-form-field-garantie-a1-type-1\"]"), 60);
        }
        if (dropdownCouverturePret != null) {
            Select selectCouverturePret = new Select(dropdownCouverturePret);
            List<WebElement> optionsCouverturePret = selectCouverturePret.getOptions();
            for (WebElement webElement : optionsCouverturePret) {
                if (containsAllElements(webElement.getText(), flux.getInfoAssureComplets().get(0).getGarantie().toString())) {
                    selectCouverturePret.selectByValue("ipp");
                    break;
                }
//                else if (containsAllElements(webElement.getText(), flux.getInfoAssureComplets().get(0).getGarantie().toString())) {
//                    selectCouverturePret.selectByValue("itt");
//                    break;
//                }
            }
        }
    }

    private void choixTypeTaux(FluxData flux, int index) {
        WebElement dropdownTypeTaux = null;
        if (index == 0) {
            dropdownTypeTaux = waitForElementExplicit(driver, By.xpath("//select[@id=\"edit-field-de-pret-1-0-inline-entity-form-field-pret-type-taux\"]"), 60);
        } if (index == 1) {
            dropdownTypeTaux = waitForElementExplicit(driver, By.xpath("//select[@id=\"edit-field-de-pret-2-0-inline-entity-form-field-pret-type-taux\"]"), 60);
        }
        if (dropdownTypeTaux != null) {
            Select selectTypeTaux = new Select(dropdownTypeTaux);
            List<WebElement> optionsTypeTaux = selectTypeTaux.getOptions();
            for (WebElement webElement: optionsTypeTaux) {
                if (webElement.getText().contains("Fixe") && flux.getPrets().get(index).getTypeTaux().equalsIgnoreCase("Fixe")) {
                    selectTypeTaux.selectByValue("fixe");
                    break;
                } else if (webElement.getText().contains("Variable") && flux.getPrets().get(index).getTypeTaux().equalsIgnoreCase("Variable")) {
                    selectTypeTaux.selectByValue("variable");
                    break;
                }
            }
        }
    }

    private void choixTypeDiffere(FluxData flux, int index) {
        WebElement dropdownDiffere = null;
        if (index == 0) {
            dropdownDiffere = waitForElementExplicit(driver, By.xpath("//select[@id=\"edit-field-de-pret-1-0-inline-entity-form-field-pret-type-differe\"]"), 60);
        } if (index == 1) {
            dropdownDiffere = waitForElementExplicit(driver, By.xpath("//select[@id=\"edit-field-de-pret-2-0-inline-entity-form-field-pret-type-differe\"]"), 60);
        }
        if (dropdownDiffere != null) {
            Select selectDiffere = new Select(dropdownDiffere);
            List<WebElement> optionsDiffere = selectDiffere.getOptions();
            for (WebElement webElement: optionsDiffere) {
                if (webElement.getText().contains("Partiel") && flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Partiel")) {
                    selectDiffere.selectByValue("c");
                    break;
                } else if (webElement.getText().contains("Total") && flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Total")) {
                    selectDiffere.selectByValue("ci");
                    break;
                }
            }
        }
    }

    private void choixCodeTarif() {
        WebElement dropdownTarif = waitForElementExplicit(driver, By.xpath("//select[@id=\"edit-field-de-tarif-value\"]"), 60);
        Select selectTarif = new Select(dropdownTarif);
        List<WebElement> optionsTarif = selectTarif.getOptions();
        for (WebElement webElement: optionsTarif){
            if (webElement.getText().contains("T1")){
                selectTarif.selectByValue("14273992");
                break;
            }
        }
    }

    private void choixPeriodicite() {
        WebElement dropdownPeriodicite = waitForElementExplicit(driver, By.xpath("//select[@id=\"edit-field-de-periodicite\"]"), 60);
        Select selectPeriodicite = new Select(dropdownPeriodicite);
        List<WebElement> optionsPeriodicite = selectPeriodicite.getOptions();
        for (WebElement webElement: optionsPeriodicite){
            if (webElement.getText().contains("Mensuelle")){
                selectPeriodicite.selectByValue("mois");
                break;
            }
        }
    }

    private void choixObjetPret(FluxData flux) {
        WebElement dropdownNatureProjet = waitForElementExplicit(driver, By.id("edit-field-de-nature-projet"), 60);
        Select selectNatureProjet = new Select(dropdownNatureProjet);
        List<WebElement> optionsNatureProjet = selectNatureProjet.getOptions();
        for (WebElement webElement: optionsNatureProjet){
            if (webElement.getText().contains("Achat résidence principale ou Prêt Professionnel") &&
                    (flux.getPrets().get(0).getObjet().equalsIgnoreCase("Résidence principale")) || flux.getPrets().get(0).getObjet().contains("Prêt professionnel")) {
                selectNatureProjet.selectByValue("achat");
                break;
            } else if (webElement.getText().contains("Résidence secondaire") && flux.getPrets().get(0).getObjet().equalsIgnoreCase("Résidence secondaire")){
                selectNatureProjet.selectByValue("residence_secondaire");
                break;
            } else if (webElement.getText().contains("Achat investissement locatif") && flux.getPrets().get(0).getObjet().equalsIgnoreCase("Investissement locatif")){
                selectNatureProjet.selectByValue("achat_invest_locatif");
                break;
            } else if (webElement.getText().contains("Consommation, travaux, professionnel (souscrit seul)") && flux.getPrets().get(0).getObjet().equalsIgnoreCase("Travaux")){
                selectNatureProjet.selectByValue("travaux");
                break;
            }
        }
    }

    private void choixStatutProfession(FluxData flux, int index) {
        WebElement dropdownStatutProfession = null;
        if (index == 0 ) {
            dropdownStatutProfession = waitForElementExplicit(driver, By.xpath("//select[@id=\"edit-field-de-assure-statut\"]"), 60);
        }
        if (index == 1 ) {
            dropdownStatutProfession = waitForElementExplicit(driver, By.xpath("//select[@id=\"edit-field-de-assure-2-statut\"]"), 60);
        }
        if (dropdownStatutProfession != null) {
            Select selectStatutProfession = new Select(dropdownStatutProfession);
            List<WebElement> optionsStatutProfession = selectStatutProfession.getOptions();
            for (WebElement webElement : optionsStatutProfession) {
                if (webElement.getText().contains("Salarié Cadre / Fonctionnaire Catégorie A") &&
                        (flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Salarié cadre") || (flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Fonctionnaire classe a")))) {
                    selectStatutProfession.selectByValue("salarie_c");
                    break;
                } else if (webElement.getText().contains("Salarié Non Cadre / Fonctionnaire Catégorie B ou C") &&
                        (flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Salarié non cadre : employé") || (flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Fonctionnaire hors classe a")))) {
                    selectStatutProfession.selectByValue("salarie_nc");
                    break;
                } else if (webElement.getText().contains("Chef d'entreprise") && flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Chef d\\'entreprise")) {
                    selectStatutProfession.selectByValue("chef_d_entreprise");
                    break;
                } else if (webElement.getText().contains("Profession Libérale hors paramédical") &&
                          (flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Profession libérale")) || flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Profession libérale médicale") ) {
                    selectStatutProfession.selectByValue("profession_liberale");
                    break;
                } else if (webElement.getText().contains("Profession Paramédicale") && flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Profession libérale paramédicale")) {
                    selectStatutProfession.selectByValue("profession_paramedicale");
                    break;
                } else if (webElement.getText().contains("Artisan / Commerçant") &&
                        (flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Artisan") || (flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Commerçant")))) {
                    selectStatutProfession.selectByValue("artisan_commercant");
                    break;
                } else if (webElement.getText().contains("Agriculteur") && flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Agriculteur")) {
                    selectStatutProfession.selectByValue("agriculteur");
                    break;
                }
            }
        }

    }

    private void selectKm(FluxData flux, int index) {
        WebElement dropdownNbKm = null;
        if (index == 0) {
            dropdownNbKm = waitForElementExplicit(driver, By.xpath("//select[@id=\"edit-field-de-assure-kms-annuels\"]"), 60);
        } if (index == 1) {
            dropdownNbKm = waitForElementExplicit(driver, By.xpath("//select[@id=\"edit-field-de-assure-2-kms-annuels\"]"), 60);
        }
        if (dropdownNbKm != null) {
            Select selectNbKm = new Select(dropdownNbKm);
            List<WebElement> optionsNbKm = selectNbKm.getOptions();
            for (WebElement webElement : optionsNbKm) {
                if (webElement.getText().contains("moins de 20 000 kms") && (!flux.getInfoAssureComplets().get(index).getDeplacementPro20000())) {
                    selectNbKm.selectByValue("moins_20");
                    break;
                } else if (webElement.getText().contains("plus de 20 000 kms") && flux.getInfoAssureComplets().get(index).getDeplacementPro20000()) {
                    selectNbKm.selectByValue("plus_20");
                    break;
                }
            }
        }
    }

    @Override
    protected void pageName(String namePage) {}
}

