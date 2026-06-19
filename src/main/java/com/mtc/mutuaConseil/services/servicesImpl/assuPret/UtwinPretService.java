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
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UtwinPretService extends BaseAutomationService implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(UtwinPretService.class);
    public static WebDriver driver;
    private static Actions actions;
    private JavascriptExecutor js = null;
    private final TypeAssuranceService typeAssuranceService;

    public UtwinPretService(TypeAssuranceService typeAssuranceService){
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        log.info("Debut de traitement -- Utwin");
        Tarif tarifUtwinPret = TarifUtils.createDefaultTarif(c, typeAssuranceService, 1L);
        // Déterminer le navigateur à partir du compte si possible (sinon Chromedriver Selenium par défaut)
        BrowserType browserType = BrowserType.SELENIUM_CHROME;
        try {
            if (c != null) {
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
            }

            // Initialise le navigateur via la factory (headless = false pour imiter ton ancien comportement)
            init(browserType, false);

            driver = getWebDriver();
            if (driver == null) {
                log.error("Impossible d'obtenir le WebDriver depuis le BrowserAdapter");
            }
            // Initialiser le JavascriptExecutor depuis le BrowserAdapter
            js = getJavascriptExecutor();
            if (js == null) {
                log.error("Impossible d'obtenir le JavascriptExecutor depuis le BrowserAdapter");
            }

            // Navigation initiale
            humanNavigate(c.getUrlFournisseur());

            element.typeByXpath("//input[@id='username']", c.getUsername());

            element.typeByXpath("//input[@id='password']", c.getPassword());

            element.clickByXpath("//button[@id='kc-login']");

            WaitUtils.sleepMs(2000);

            element.clickByXpath("//button[@class='btnPrincipal btIconesBtFaireUnDevis']");
            WaitUtils.sleepMs(1000);
            element.clickByXpath("//button[.//b[text()='Devis emprunteur']]");
            // Changement de page
            switchPage();
            element.clickByXpath("//span[text()='Personne Physique']");

            remplirInformationsPersonne(flux);

            scrollDown(0,300);

            remplirInformationsPret(flux);

            //Suivant
            WaitUtils.sleepMs(2000);
            element.clickJSExecutorByXpath("//button[starts-with(normalize-space(.), 'Suivant')]\n");

            choixGaranties(flux);
            WaitUtils.sleepMs(1000);
            scrollDown(0,650);

            //Tarifer
            element.clickByActions("//button[@class=\"btn navButton-orange navButtonText mr-md-3 px-4 text-uppercase font-weight-bold\" and contains(text(), \"Tarifer\")]\n");

            //Cout
            WaitUtils.sleepMs(5000);
            WebElement spanCout = waitForElement(driver, By.xpath("//div[@class='col-3 bloc-offre'][1]//div[@class=\"tarifsEtTaux\"]//span[@class=\"big text-nowrap\"]"), 30, 2);
            String cout = spanCout.getText();
            log.info("Cout {} : ", cout);
            tarifUtwinPret.setMontant(cout);
            String screenshotBytes = captureScreenshot(tarifUtwinPret.getNom(), false, tarifUtwinPret);
            if (screenshotBytes != null) {
                tarifUtwinPret.setCaptureImg(screenshotBytes);
            }
            tarifUtwinPret.setExecution(true);
        } catch (Exception e) {
            log.error("An error occurred : ", e);
            tarifUtwinPret.setErreur(e.getMessage());
            String screenshotBytesErreur = captureScreenshot(tarifUtwinPret.getNom(), true, tarifUtwinPret);
            tarifUtwinPret.setCaptureImgErreur(screenshotBytesErreur);
            tarifUtwinPret.setEtape("");
        } finally {
            cleanup();
        }
      return tarifUtwinPret;
    }

    private void remplirInformationsPersonne(FluxData flux) {
        int index = 0;
        //Civilite
        if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur")) {
            element.clickByXpath("//div[@id=\"assures0\"]//app-form-radio-button[@formcontrolname=\"civilite\"]//span[contains(text(),'Monsieur')]");
        } else if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame")){
            element.clickByXpath("//div[@id=\"assures0\"]//app-form-radio-button[@formcontrolname=\"civilite\"]//span[contains(text(),'Madame')]");
        }
        //Nom
        element.typeByXpath("//div[@id='assures0']//input[@placeholder='ex : DUPONT']", flux.getPersonnes().get(index).getNom());
        //Prenom
        element.typeByXpath("//div[@id='assures0']//input[@placeholder='ex : Jean-Michel']", flux.getPersonnes().get(index).getPrenom());
        //Date naissance
        element.typeByXpath("//div[@id='assures0']//input[@placeholder='jj/mm/aaaa']", flux.getPersonnes().get(index).getDateNaissance());
        // Choix statut profession
        choixStatutProfession(flux, index);

        if (flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Salarié non cadre : employé")){
            choixProfessionMedicaleParamedicale(flux, index);
        }

        WaitUtils.sleepMs(1000);
        //Profession spécifique
        element.clickByActions("//div[@id='assures0']//app-form-dropdownlist[@formcontrolname=\"metierARisque\"]//div//ng-select");
        element.clickByActions("//span[contains(text(),\"Non, je n'exerce aucune profession de cette liste\")]");

        //Exacte Profession
        element.typeByXpath("//input[@placeholder='Profession exacte']", flux.getPersonnes().get(index).getProfession());
        //Code postal
        element.typeByXpath("//input[@formcontrolname=\"codePostal\"]", flux.getPersonnes().get(index).getCodePostal());

        if (flux.getInfoAssureComplets().get(index).getFumeur()){
            element.clickByXpath("//*[@id=\"assures0\"]/fieldset/div/app-form-container/div/div/div/div[2]/div/div/div/div[2]/div/div[1]/div[2]/switch-button/div/div");
        }
        if (flux.getInfoAssureComplets().get(index).getInstrumentPrecis()){
            element.clickByXpath("//div[@id='assures0']//switch-button[@formcontrolname=\"utiliseInstrumentPrecision\"]");
        }
        if (flux.getInfoAssureComplets().get(index).getTravailHauteur()){
            if(flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("3 à 10m")
               || flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("10 à 12m")
               || flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("12 à 15m")
               || flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("15 à 20m")
               || flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("Plus de 20m")
            ) {
                element.clickJSExecutorByXpath("//*[@id=\"assures0\"]/fieldset/div/app-form-container/div/div/div/div[2]/div/div/div/div[2]/div/div[3]/div[2]/switch-button/div/div");
            }
        }
        if (flux.getInfoAssureComplets().get(index).getTravailManuelManuLourde() || flux.getInfoAssureComplets().get(index).getTravailManuel()){
            element.clickJSExecutorByXpath("//*[@id=\"assures0\"]/fieldset/div/app-form-container/div/div/div/div[2]/div/div/div/div[2]/div/div[4]/div[2]/switch-button/div/div");
        }
        if (flux.getInfoAssureComplets().get(index).getDeplacementPro20000()){
            element.clickJSExecutorByXpath("//*[@id=\"assures0\"]/fieldset/div/app-form-container/div/div/div/div[2]/div/div/div/div[2]/div/div[5]/div[2]/switch-button/div/div");
        }
        if (flux.getInfoAssureComplets().get(index).getDeplacementEtranger60()){
            element.clickJSExecutorByXpath("//div[@id='assures0']//switch-button[@formcontrolname=\"deplacementEtranger\"]");;
        }
        if (flux.getInfoAssureComplets().get(index).getDeplacementPaysRisque()){
            element.clickJSExecutorByXpath("//div[@id='assures0']//switch-button[@formcontrolname=\"deplacementPaysARisque\"]");
        }

        if (flux.getPersonnes().size() == 2) {
            index = 1;
            element.clickByXpath("//span[normalize-space()='Ajouter un(e) assuré(e)']");
            //Civilite
            if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur")) {
                element.clickByXpath("//div[@id=\"assures1\"]//app-form-radio-button[@formcontrolname=\"civilite\"]//span[contains(text(),'Monsieur')]");
            } else if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame")){
                element.clickByXpath("//div[@id=\"assures1\"]//app-form-radio-button[@formcontrolname=\"civilite\"]//span[contains(text(),'Madame')]");
            }
            //Nom
            element.typeByXpath("//div[@id='assures1']//input[@placeholder='ex : DUPONT']", flux.getPersonnes().get(index).getNom());
            //Prenom
            element.typeByXpath("//div[@id='assures1']//input[@placeholder='ex : Jean-Michel']", flux.getPersonnes().get(index).getPrenom());
            //Date naissance
            element.typeByXpath("//div[@id='assures1']//input[@placeholder='jj/mm/aaaa']", flux.getPersonnes().get(index).getDateNaissance());
            //Profession
            choixStatutProfession(flux, index);

            if (flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Salarié non cadre : employé")){
                choixProfessionMedicaleParamedicale(flux, index);
            }

            //Profession spécifique
            WaitUtils.sleepMs(2000);
            element.clickByActions("//div[@id='assures1']//app-form-dropdownlist[@formcontrolname=\"metierARisque\"]//div//ng-select");
            element.clickByActions("//span[contains(text(),\"Non, je n'exerce aucune profession de cette liste\")]");

            //Exacte Profession
            element.typeByXpath("//div[@id='assures1']//input[@placeholder='Profession exacte']", flux.getPersonnes().get(index).getProfession());

            //Code postal
            element.typeByXpath("//div[@id='assures1']//input[@formcontrolname=\"codePostal\"]", flux.getPersonnes().get(index).getCodePostal());

            if (flux.getInfoAssureComplets().get(index).getFumeur()){
                element.clickJSExecutorByXpath("//*[@id=\"assures1\"]/fieldset/div/app-form-container/div/div/div/div[2]/div/div/div/div[2]/div/div[1]/div[2]/switch-button/div/div");
            }
            if (flux.getInfoAssureComplets().get(index).getInstrumentPrecis()){
                element.clickJSExecutorByXpath("//div[@id='assures1']//switch-button[@formcontrolname=\"utiliseInstrumentPrecision\"]");
            }
            if (flux.getInfoAssureComplets().get(index).getTravailHauteur()) {
                if(flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("3 à 10m")
                        || flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("10 à 12m")
                        || flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("12 à 15m")
                        || flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("15 à 20m")
                        || flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("Plus de 20m")
                ) {
                    element.clickJSExecutorByXpath("//*[@id=\"assures1\"]/fieldset/div/app-form-container/div/div/div/div[2]/div/div/div/div[2]/div/div[3]/div[2]/switch-button/div/div");
                }
            }
            if (flux.getInfoAssureComplets().get(index).getTravailManuelManuLourde() || flux.getInfoAssureComplets().get(index).getTravailManuel()){
                element.clickJSExecutorByXpath("//*[@id=\"assures1\"]/fieldset/div/app-form-container/div/div/div/div[2]/div/div/div/div[2]/div/div[4]/div[2]/switch-button/div/div");
            }
            if (flux.getInfoAssureComplets().get(index).getDeplacementPro20000()){
                element.clickJSExecutorByXpath("//*[@id=\"assures1\"]/fieldset/div/app-form-container/div/div/div/div[2]/div/div/div/div[2]/div/div[5]/div[2]/switch-button/div/div");
            }
            if (flux.getInfoAssureComplets().get(index).getDeplacementEtranger60()){
                element.clickJSExecutorByXpath("//div[@id='assures1']//switch-button[@formcontrolname=\"deplacementEtranger\"]");
            }
            if (flux.getInfoAssureComplets().get(index).getDeplacementPaysRisque()){
                element.clickJSExecutorByXpath("//div[@id='assures1']//switch-button[@formcontrolname=\"deplacementPaysARisque\"]");
            }
        }
    }

    private void remplirInformationsPret(FluxData flux) {
        int index = 0;
        //Type projet
        choixObjetPret(flux, index);

        WaitUtils.sleepMs(2000);
        element.clickJSExecutorByXpath("//app-form-radio-button[@formcontrolname=\"typeAffaireId\"]//div//label[1]");

        //Date prévisionnelle
        scrollDown(0,300);

        element.typeByXpath("//input[@formcontrolname='datePrevisionDeblocage']", flux.getPrets().get(index).getDateEffet());

        //Localisation
        element.clickByActions("//app-form-dropdownlist[@formcontrolname=\"paysId\"]//div//ng-select");

        element.clickByActions("//span[@class=\"ng-select-single-dropdown-option\" and contains(text(), \"France métropolitaine (continentale & Corse)\")]\n");

        scrollDown(0,300);

        //Type de pret
        choixTypePret(flux, index);

        //Taux
        element.typeByXpath("//input[@formcontrolname='taux']", flux.getPrets().get(index).getTaux());
        //Montant
        element.typeByXpath("//input[@formcontrolname='montantInitial']", flux.getPrets().get(index).getMontantPret());
        //Durée
        element.typeByXpath("//input[@formcontrolname='dureeInitiale']", flux.getPrets().get(index).getDuree());

        if (flux.getPrets().get(index).getDureeDiffere() != null) {
            element.typeByXpath("//input[@formcontrolname='dontDiffereInitial']", flux.getPrets().get(index).getDureeDiffere());
        }

        ChoixTypeTaux(flux, index);

        scrollDown(0,300);

        if (flux.getPrets().size() == 2) {
            index = 1;
            element.clickJSExecutorByXpath("//button//span[contains(text(), \"Ajouter un prêt\")]");

            //Type de pret
            choixTypePret(flux, index);

            //Taux
            element.typeByXpath("//div[@id=\"pret1\"]//input[@formcontrolname='taux']", flux.getPrets().get(index).getTaux());
            //Montant
            element.typeByXpath("//div[@id=\"pret1\"]//input[@formcontrolname='montantInitial']", flux.getPrets().get(index).getMontantPret());
            //Durée
            element.typeByXpath("//div[@id=\"pret1\"]//input[@formcontrolname='dureeInitiale']", flux.getPrets().get(index).getDuree());
            if (flux.getPrets().get(index).getDureeDiffere() != null) {
                element.typeByXpath("//div[@id=\"pret1\"]//app-form-input[@label=\"Dont différé\"]//input[@formcontrolname='dontDiffereInitial']", flux.getPrets().get(index).getDureeDiffere());
            }
            ChoixTypeTaux(flux, index);
        }
        //Banque
        choixBanque(flux, index);
    }

    private void ChoixTypeTaux(FluxData flux, int index) {
        if (index == 0) {
            if (flux.getPrets().get(index).getTypeTaux().equalsIgnoreCase("Fixe")){
                element.clickJSExecutorByXpath("//div[@id=\"pret0\"]//app-form-radio-button[@formcontrolname=\"typeTaux\"]//span[contains(text(), \"Fixe\")]");
            }
            if (flux.getPrets().get(index).getTypeTaux().equalsIgnoreCase("Variable")){
                element.clickJSExecutorByXpath("//div[@id=\"pret0\"]//app-form-radio-button[@formcontrolname=\"typeTaux\"]//span[contains(text(), \"Variable\")]");
            }
        }
        if (index == 1) {
            if (flux.getPrets().get(index).getTypeTaux().equalsIgnoreCase("Fixe")){
                element.clickJSExecutorByXpath("//div[@id=\"pret1\"]//app-form-radio-button[@formcontrolname=\"typeTaux\"]//span[contains(text(), \"Fixe\")]");
            }
            if (flux.getPrets().get(index).getTypeTaux().equalsIgnoreCase("Variable")){
                element.clickJSExecutorByXpath("//div[@id=\"pret1\"]//app-form-radio-button[@formcontrolname=\"typeTaux\"]//span[contains(text(), \"Variable\")]");
            }
        }
    }

    private void choixStatutProfession( FluxData flux, int index) {
        if (index == 0) {
            element.clickByXpath("//div[@id='assures0']//app-form-dropdownlist[@formcontrolname=\"statutProfessionnel\"]//div//ng-select");
        }
        if (index == 1) {
            element.clickByXpath("//div[@id='assures1']//app-form-dropdownlist[@formcontrolname=\"statutProfessionnel\"]//div//ng-select");
        }
        WaitUtils.sleepMs(1000);
        List<WebElement> options = driver.findElements(By.className("ng-option"));
        js = (JavascriptExecutor) driver;
        for (WebElement option : options) {
            if (option.getText().contains("Salarié Cadre/ Assimilé-cadre / Ingénieur") && flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Salarié cadre")) {
                js.executeScript("arguments[0].scrollIntoView(true);", option);
                option.click();
                scrollDown(0,-450);
                break;
            }
            if (option.getText().contains("Professions agricoles") && flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Agriculteur")) {
                js.executeScript("arguments[0].scrollIntoView(true);", option);
                option.click();
                scrollDown(0,-450);
                break;
            }
            if (option.getText().contains("Commerçant") && flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Commerçant")) {
                js.executeScript("arguments[0].scrollIntoView(true);", option);
                option.click();
                scrollDown(0,-450);
                break;
            }
            if (option.getText().contains("Salarié non cadre (hors employé de bureau & personnel navigant") && flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Salarié non cadre : employé")) {
                js.executeScript("arguments[0].scrollIntoView(true);", option);
                option.click();
                scrollDown(0,-450);
                break;
            }
            if (option.getText().contains("Artisan") && flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Artisan")) {
                js.executeScript("arguments[0].scrollIntoView(true);", option);
                option.click();
                scrollDown(0,-450);
                break;
            }
            if (option.getText().contains("Profession libérale médicale ou paramédicale / Médecin (ou Interne) généraliste/ spécialiste") && flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Profession libérale médicale")) {
                js.executeScript("arguments[0].scrollIntoView(true);", option);
                option.click();
                break;
            }
            if (option.getText().contains("Profession libérale (hors médical/ paramédical)") && flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Profession libérale")) {
                js.executeScript("arguments[0].scrollIntoView(true);", option);
                option.click();
                break;
            }
            if (option.getText().contains("Dirigeant de société/ Gérant/ Chef d'entreprise (de moins de 10 salariés) artisan/commerçant") && flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Chef d\\'entreprise")) {
                js.executeScript("arguments[0].scrollIntoView(true);", option);
                option.click();
                break;
            }
            if (option.getText().contains("Fonctionnaire") && flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Fonctionnaire classe a")) {
                js.executeScript("arguments[0].scrollIntoView(true);", option);
                option.click();
                break;
            }
        }
    }

    private void choixProfessionMedicaleParamedicale(FluxData flux, int index) {
        if (index == 0) {
            element.clickByActions("//div[@id='assures0']//app-form-dropdownlist[@formcontrolname=\"professionMedicale\"]//div//ng-select");
        }
        if (index == 1) {
            element.clickByActions("//div[@id='assures1']//app-form-dropdownlist[@formcontrolname=\"professionMedicale\"]//div//ng-select");
        }
        WaitUtils.sleepMs(1000);
        List<WebElement> options = driver.findElements(By.className("ng-option"));
        js = (JavascriptExecutor) driver;
        for (WebElement option : options) {
            if ( (option.getText().contains("Chirurgiens") || option.getText().contains("chirurgiens-dentistes")) && (flux.getPersonnes().get(index).getProfession().contains("Chirurgiens")
               || flux.getPersonnes().get(index).getProfession().contains("chirurgiens-dentistes"))) {
                js.executeScript("arguments[0].scrollIntoView(true);", option);
                option.click();
                scrollDown(0, -300);
                break;
            } else if ((option.getText().contains("Médecins") || option.getText().contains("médecins spécialistes") || option.getText().contains("interne") ||
                       option.getText().contains("vétérinaires") || option.getText().contains("pharmacien")) && (flux.getPersonnes().get(index).getProfession().contains("Médecins")
                      || flux.getPersonnes().get(index).getProfession().contains("médecins spécialistes") || flux.getPersonnes().get(index).getProfession().contains("interne")
                      || flux.getPersonnes().get(index).getProfession().contains("vétérinaires") || flux.getPersonnes().get(index).getProfession().contains("pharmacien"))) {
                js.executeScript("arguments[0].scrollIntoView(true);", option);
                option.click();
                scrollDown(0, -300);
                break;
            }
            if (option.getText().contains("Ostéopathes") && flux.getPersonnes().get(index).getProfession().contains("Ostéopathes")) {
                js.executeScript("arguments[0].scrollIntoView(true);", option);
                option.click();
                scrollDown(0, -300);
                break;
            } else if (option.getText().contains("sages-femmes") && (flux.getPersonnes().get(index).getProfession().contains("sages-femmes")
                || flux.getPersonnes().get(index).getProfession().contains("kiné"))) {
                js.executeScript("arguments[0].scrollIntoView(true);", option);
                option.click();
                scrollDown(0, -300);
                break;
            } else if (option.getText().contains("autres") && flux.getPersonnes().get(index).getProfession().contains("autres")) {
                js.executeScript("arguments[0].scrollIntoView(true);", option);
                option.click();
                scrollDown(0, -300);
                break;
            } else {
                js.executeScript("arguments[0].scrollIntoView(true);", option);
                option.click();
                scrollDown(0, -450);
                break;
            }
        }
    }

    private void choixObjetPret(FluxData flux, int index) {
        element.clickByActions("//app-form-dropdownlist[@formcontrolname=\"typeProjetId\"]//div//ng-select");
        WaitUtils.sleepMs(1000);
        if (flux.getPrets().get(index).getObjet().equalsIgnoreCase("Résidence principale")) {
            element.clickByActions("//span[@class=\"ng-select-single-dropdown-option\" and contains(text(), \"Résidence principale\")]\n");
        }
        if (flux.getPrets().get(index).getObjet().equalsIgnoreCase("Résidence secondaire")) {
            element.clickByActions("//span[@class=\"ng-select-single-dropdown-option\" and contains(text(), \"Acquisition résidence secondaire\")]\n");
        }
        if (flux.getPrets().get(index).getObjet().equalsIgnoreCase("Investissement professionnel")) {
            element.clickByActions("//span[@class=\"ng-select-single-dropdown-option\" and contains(text(), \"Investissements locatifs à usage d’habitation ou mixte (hors SCPI)\")]\n");
        }
        if (flux.getPrets().get(index).getObjet().equalsIgnoreCase("Prêt à objet professionnel")) {
            element.clickByActions("//span[@class=\"ng-select-single-dropdown-option\" and contains(text(), \"Prêt professionnel\")]\n");
        }
    }

    private void choixTypePret(FluxData flux, int index) {
        if (index == 0) {
            element.clickByActions("//app-form-dropdownlist[@formcontrolname=\"typePretId\"]//div//ng-select");
        }
        if (index == 1) {
            element.clickByActions("//div[@id=\"pret1\"]//app-form-dropdownlist[@formcontrolname=\"typePretId\"]//div//ng-select");
        }
        WaitUtils.sleepMs(1000);
        if (flux.getPrets().get(index).getType().equalsIgnoreCase("Amortissable")) {
            element.clickByActions("//span[@class=\"ng-select-single-dropdown-option\" and contains(text(), \"Amortissable\")]\n");
        }
        if (flux.getPrets().get(index).getType().equalsIgnoreCase("Prêt relais")) {
            element.clickByActions("//span[@class=\"ng-select-single-dropdown-option\" and contains(text(), \"Prêt relais\")]\n");
        }
        if (flux.getPrets().get(index).getType().equalsIgnoreCase("Prêt à paliers")) {
            element.clickByActions("//span[@class=\"ng-select-single-dropdown-option\" and contains(text(), \"Prêt à palier\")]\n");
        }
        if (flux.getPrets().get(index).getType().equalsIgnoreCase("Crédit bail")) {
            element.clickByActions("//span[@class=\"ng-select-single-dropdown-option\" and contains(text(), \"Crédit-bail\")]\n");
        }
    }

    private void choixBanque(FluxData flux, int index){
        element.clickByActions("//app-form-dropdownlist[@formcontrolname=\"banqueId\"]//div//ng-select");
        List<WebElement> options = driver.findElements(By.className("ng-option"));
        js = (JavascriptExecutor) driver;
        for (WebElement option : options) {
            if (flux.getPrets().get(index).getBanque().equalsIgnoreCase(option.getText())) {
                js.executeScript("arguments[0].scrollIntoView(true);", option);
                option.click();
                break;
            }

        }

    }

    private void choixGaranties(FluxData flux){
        WaitUtils.sleepMs(1000);
        if (flux.getInfoAssureComplets().get(0).getGarantie().equalsIgnoreCase("IPT, ITT")) {
            element.typeByXpath("//*[@id='tab-0-panel']/div/div/div/div/div[2]/div/table/tbody/tr[3]/td[5]/input", "0");
            WaitUtils.sleepMs(1000);
            element.clickByXpath("//*[@id=\"tab-0-panel\"]/div/div/div/div/div[2]/div/table/tbody/tr[11]/td[5]/div/div/switch-button");
        }
        if (flux.getPrets().size() == 2) {
            WaitUtils.sleepMs(1000);
            element.clickByXpath("//a[@id=\"tab-1\"]");
            if (flux.getInfoAssureComplets().get(0).getGarantie().equalsIgnoreCase("IPT, ITT")) {
                element.typeByXpath("//*[@id='tab-1-panel']/div/div/div/div/div[2]/div/table/tbody/tr[3]/td[5]/input", "0");
                WaitUtils.sleepMs(1000);
                element.clickByXpath("//*[@id=\"tab-1-panel\"]/div/div/div/div/div[2]/div/table/tbody/tr[11]/td[5]/div/div/switch-button");
            }
        }

    }

}
