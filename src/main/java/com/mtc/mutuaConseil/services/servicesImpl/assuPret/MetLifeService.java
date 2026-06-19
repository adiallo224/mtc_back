package com.mtc.mutuaConseil.services.servicesImpl.assuPret;

import com.mtc.mutuaConseil.models.Compte;
import com.mtc.mutuaConseil.models.FluxData;
import com.mtc.mutuaConseil.models.Tarif;
import com.mtc.mutuaConseil.services.LaunchedService;
import com.mtc.mutuaConseil.services.servicesImpl.TypeAssuranceService;
import com.mtc.mutuaConseil.utils.InformationsPersonne;
import com.mtc.mutuaConseil.utils.InformationsUser;
import com.mtc.mutuaConseil.utils.PageElementInteraction;
import com.mtc.mutuaConseil.utils.TarifUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.interactions.Actions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MetLifeService extends PageElementInteraction implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(MetLifeService.class);
    private static WebDriver driver;
    private static Actions actions;
    private final String source = "MetLifePret";
    private JavascriptExecutor js = null;
    private final TypeAssuranceService typeAssuranceService;

    public MetLifeService(TypeAssuranceService typeAssuranceService){
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        log.info("Debut de traitement -- Metlife");
        Tarif tarifMetLifePret = TarifUtils.createDefaultTarif(c, typeAssuranceService, 1L);
        driver = new EdgeDriver();
        driver.manage().window().maximize();
        actions = new Actions(driver);
        js = (JavascriptExecutor) driver;
        try {
            waitThread(2);
            driver.get(c.getUrlFournisseur());
            InformationsUser.infoLogin(driver, By.id("login"), 10, 2, c.getUsername());
            InformationsUser.infoPassword(driver, By.id("password"), 10, 2, c.getPassword());
            WebElement buttonLogin = waitForElement(driver, By.id("bt_entrer"), 10, 2);
            actions.moveToElement(buttonLogin).click().perform();

            waitThread(2);
            WebElement liDossiers = waitForElement(driver, By.xpath("//*[@id=\"BT_102\"]"), 10, 2);
            actions.moveToElement(liDossiers).click().perform();

            waitThread(2);
            WebElement formDossiersEmprunteurs = waitForElement(driver, By.xpath("//form[@name=\"cbpCredit\"]//a"), 10, 2);
            actions.moveToElement(formDossiersEmprunteurs).click().perform();

            switchPage(driver);

            waitThread(3);
//            WebElement buttonNouvelleDemande = waitForElement(driver, By.xpath("//div[@class=\"header-actions\"]//adh-button[@class=\"action-dossier-creation\"]//button"), 10, 2);
//            actions.moveToElement(buttonNouvelleDemande).click().perform();
//            WebElement buttonNouvelleDemande = waitForElement(driver, By.xpath("//div[@class=\"header-actions\"]//adh-button[@class=\"action-dossier-creation\"]//button//span[contains(text(), ' Nouvelle demande ')]"), 10, 2);
            WebElement buttonNouvelleDemande = waitForElement(driver, By.xpath("//button[@class='btn btn-secondary text-medium text-uppercase text-wrap' and @type='button' and @name='undefined']"), 10, 2);
            js.executeScript("arguments[0].click();", buttonNouvelleDemande);

            waitThread(1);
            WebElement buttonOffre = waitForElement(driver, By.xpath("//app-selection-offre[1]/app-card-cta/adh-button"), 10, 2);
            actions.moveToElement(buttonOffre).click().perform();

            waitThread(2);
            coordonneesPersonne(flux, source);

           //Page suivante
            waitThread(1);   //button[contains(@class, 'btn-secondary') and contains(text(), 'Suivant')]
            nextPage(driver, By.xpath("//div[@class=\"footer-container\"]//button//span[text()=' Suivant ']"), 15, 1);
            waitThread(2);
            informationsPersonne(flux, source);

            nextPage(driver, By.xpath("//div[@class=\"footer-container\"]//button//span[text()=' Suivant ']"), 15, 1);

            waitThread(2);
            informationsPrets(flux, source);

            nextPage(driver, By.xpath("//div[@class=\"footer-container\"]//button//span[text()=' Suivant ']"), 15, 1);

            waitThread(2);
            informationsPreteur(flux, source);

            //Page cout
            waitThread(5);
//            WebElement divElement = driver.findElement(By.xpath("//div[@data-testid=\"tarification-produit-tarif-total-assurance\"]"));
//            WebElement divElement = driver.findElement(By.xpath("//div[@class='montant-simulation__total ng-star-inserted']"));
            WebElement divElement = driver.findElement(By.xpath("//div[@data-testid='tarification-produit-tarif-total-assurance']"));
            String cout = getCotisation(divElement.getText());
            tarifMetLifePret.setMontant(cout + " €");
            String screenshotBytes = captureScreenshot(driver, tarifMetLifePret.getNom(), false, tarifMetLifePret);
            if (screenshotBytes != null) {
                tarifMetLifePret.setCaptureImg(screenshotBytes);
            }
            tarifMetLifePret.setExecution(true);
            log.info("Fin de traitement -- DigitalInsure");
        } catch (Exception e) {
            log.error("An error occurred : ", e);
            tarifMetLifePret.setErreur(e.getMessage());
            String screenshotBytesErreur = captureScreenshot(driver, tarifMetLifePret.getNom(), true, tarifMetLifePret);
            tarifMetLifePret.setCaptureImgErreur(screenshotBytesErreur);
            tarifMetLifePret.setEtape("");
        } finally {
            driver.quit();
        }
        return tarifMetLifePret;
    }

    private static void coordonneesPersonne(FluxData flux, String source) {
        int index = 0;
        if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur")) {
            WebElement radioCivilite = driver.findElement(By.xpath("//adh-input-radio-button[@data-testid=\"form-assure-0-control-civilite-option-Monsieur\"]"));
            radioCivilite.click();
        } else if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame")){
            WebElement radioCivilite = driver.findElement(By.xpath("//adh-input-radio-button[@data-testid=\"form-assure-0-control-civilite-option-Madame\"]"));
            radioCivilite.click();
        }
        InformationsPersonne.infoNom(driver, By.xpath("//*[@id=\"adh-input-assure-nom-0\"]"), 10, 2, flux, index, source);
        InformationsPersonne.infoPrenom(driver, By.xpath("//*[@id=\"adh-input-assure-prenom-0\"]"), 10, 2, flux, index, source);
        InformationsPersonne.infoDateNaissance(driver, By.xpath("//*[@id=\"adh-input-assure-dateNaissance-0\"]"), 10, 2, flux, index, source);
        InformationsPersonne.infoEmail(driver, By.xpath("//*[@id=\"adh-input-assure-email-0\"]"), 10, 2, flux, index, source);
        InformationsPersonne.infoTelephone(driver, By.xpath("//*[@id=\"adh-input-assure-telephone-0\"]"), 10, 2, flux, index, source);
        if (flux.getPersonnes().size() == 2) {
            index = 1;
            WebElement labelAjoutAssure = driver.findElement(By.xpath("//div[text()=\"Ajouter un assuré\"]"));
            actions.moveToElement(labelAjoutAssure).click().perform();
            //Civilite
            if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur")) {
                WebElement radioCivilite = driver.findElement(By.xpath("//adh-input-radio-button[@data-testid=\"form-assure-1-control-civilite-option-Monsieur\"]"));
                radioCivilite.click();
            } else if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame")){
                WebElement radioCivilite = driver.findElement(By.xpath("//adh-input-radio-button[@data-testid=\"form-assure-1-control-civilite-option-Madame\"]"));
                radioCivilite.click();
            }
            InformationsPersonne.infoNom(driver, By.xpath("//*[@id=\"adh-input-assure-nom-1\"]"), 10, 2, flux, index, source);
            InformationsPersonne.infoPrenom(driver, By.xpath("//*[@id=\"adh-input-assure-prenom-1\"]"), 10, 2, flux, index, source);
            InformationsPersonne.infoDateNaissance(driver, By.xpath("//*[@id=\"adh-input-assure-dateNaissance-1\"]"), 10, 2, flux, index, source);
            InformationsPersonne.infoEmail(driver, By.xpath("//*[@id=\"adh-input-assure-email-1\"]"), 10, 2, flux, index, source);
            InformationsPersonne.infoTelephone(driver, By.xpath("//*[@id=\"adh-input-assure-telephone-1\"]"), 10, 2, flux, index, source);
        }
    }

    private void informationsPersonne(FluxData flux, String source) {
        int index = 0;
        waitThread(5);
        WebElement dropDownPays = waitForElement(driver, By.xpath("//*[@id=\"adh-input-residenceFiscale-paysResidenceFiscale-0\"]"), 10, 2);
        actions.moveToElement(dropDownPays).click().perform();

        WebElement optionPays = waitForElement(driver, By.xpath("//div[@id=\"adh-input-residenceFiscale-paysResidenceFiscale-0-autocomplete-panel\"]//adh-option[1]"), 10, 2);
        actions.moveToElement(optionPays).click().perform();

        choixStatutProfession(flux, index);

        WebElement dropDownPro = waitForElement(driver, By.xpath("//adh-select[@id=\"profession-professionARisque-0\"]"), 10, 2);
        actions.moveToElement(dropDownPro).click().perform();

        WebElement optionPro = waitForElement(driver, By.xpath("//div[@id=\"adh-input-profession-professionARisque-0-panel\"]//adh-option[1]"), 10, 2);
        actions.moveToElement(optionPro).click().perform();

        WebElement inputPro = waitForElement(driver, By.xpath("//*[@id=\"adh-input-profession-libelle-0\"]"), 10, 2);
        inputPro.sendKeys(flux.getPersonnes().get(index).getProfession());

        if (flux.getInfoAssureComplets().get(index).getTravailManuel()) {
            WebElement radioTravailManuelle = waitForElement(driver, By.xpath("//adh-input-radio-button[@data-testid='informationsPersonnelles-0-group-profession-control-manuelle-option-true']"), 10, 2);
            actions.moveToElement(radioTravailManuelle).click().perform();
        }

        if (flux.getInfoAssureComplets().get(index).getTravailHauteur()) {
            WebElement radioTravailHauteur = waitForElement(driver, By.xpath("//adh-input-radio-button[@data-testid='informationsPersonnelles-0-group-profession-control-travauxEnHauteur-option-true']"), 10, 2);
            actions.moveToElement(radioTravailHauteur).click().perform();
        }

        WebElement buttonSportRisque = waitForElement(driver, By.xpath("//*[@id=\"donneesAssurance-sportsARisque-0\"]/div/span"), 10, 2);
        buttonSportRisque.click();

        WebElement buttonNonPratique = waitForElement(driver, By.xpath("//*[@id=\"portal-content-selection-sports-a-risque\"]/app-sports-a-risque-modal/div/adh-button[2]/button"), 10, 2);
        buttonNonPratique.click();

        scrollDown(driver, 0,300);

        if (!flux.getInfoAssureComplets().get(index).getFumeur()) {
//            WebElement radioFumeur = waitForElement(driver, By.xpath("//adh-input-radio[@id=\"donneesAssurance-fumeur-0\"]//input[@id=\"adh-radio-1404\"]"), 10, 2);
            waitThread(2);
            WebElement radioFumeur = waitForElement(driver, By.xpath("//adh-input-radio[@id=\"donneesAssurance-fumeur-0\"]//adh-input-radio-button[2]"), 10, 2);
            radioFumeur.click();
        }
        if (flux.getInfoAssureComplets().get(index).getFumeur()) {
            waitThread(2);
            WebElement radioFumeur = waitForElement(driver, By.xpath("//adh-input-radio[@id=\"donneesAssurance-fumeur-0\"]//input[@id=\"adh-radio-1403\"]"), 10, 2);
            radioFumeur.click();
        }

        if (flux.getPersonnes().size() == 2) {
            index = 1;
            scrollDown(driver, 0,-300);
            WebElement dropDownPays1 = waitForElement(driver, By.xpath("//adh-autocomplete//button[@id=\"adh-input-residenceFiscale-paysResidenceFiscale-1\"]"), 10, 2);
            actions.moveToElement(dropDownPays1).click().perform();

            WebElement optionPays1 = waitForElement(driver, By.xpath("//div[@id=\"adh-input-residenceFiscale-paysResidenceFiscale-1-autocomplete-panel\"]//adh-option//span[contains(text(), ' FRANCE ')]"), 10, 2);
            actions.moveToElement(optionPays1).click().perform();

            choixStatutProfession(flux, index);

            WebElement dropDownPro1 = waitForElement(driver, By.xpath("//adh-select[@id=\"profession-professionARisque-1\"]"), 10, 2);
            actions.moveToElement(dropDownPro1).click().perform();

            WebElement optionPro1 = waitForElement(driver, By.xpath("//div[@id=\"adh-input-profession-professionARisque-1-panel\"]//adh-option[1]"), 10, 2);
            actions.moveToElement(optionPro1).click().perform();

            WebElement inputPro1 = waitForElement(driver, By.xpath("//*[@id=\"adh-input-profession-libelle-1\"]"), 10, 2);
            inputPro1.sendKeys(flux.getPersonnes().get(index).getProfession());

            if (flux.getInfoAssureComplets().get(index).getTravailManuel()) {
                WebElement radioTravailManuelle1 = waitForElement(driver, By.xpath("//adh-input-radio-button[@data-testid='informationsPersonnelles-1-group-profession-control-manuelle-option-true']"), 10, 2);
                actions.moveToElement(radioTravailManuelle1).click().perform();
            }

            if (flux.getInfoAssureComplets().get(index).getTravailHauteur()) {
                WebElement radioTravailHauteur1 = waitForElement(driver, By.xpath("//adh-input-radio-button[@data-testid='informationsPersonnelles-1-group-profession-control-travauxEnHauteur-option-true']"), 10, 2);
                actions.moveToElement(radioTravailHauteur1).click().perform();
            }

            WebElement buttonSportRisque1 = waitForElement(driver, By.xpath("//*[@id=\"donneesAssurance-sportsARisque-1\"]/div/span"), 10, 2);
            buttonSportRisque1.click();

            WebElement buttonNonPratique1 = waitForElement(driver, By.xpath("//*[@id=\"portal-content-selection-sports-a-risque\"]/app-sports-a-risque-modal/div/adh-button[2]/button"), 10, 2);
            buttonNonPratique1.click();

            scrollDown(driver, 0,300);

            if (!flux.getInfoAssureComplets().get(index).getFumeur()) {
                waitThread(2);
                WebElement radioFumeur1 = waitForElement(driver, By.xpath("//adh-input-radio[@id=\"donneesAssurance-fumeur-1\"]//adh-input-radio-button[2]"), 10, 2);
                radioFumeur1.click();
            }
            if (flux.getInfoAssureComplets().get(index).getFumeur()) {
                waitThread(2);
                WebElement radioFumeur1 = waitForElement(driver, By.xpath("//adh-input-radio[@id=\"donneesAssurance-fumeur-1\"]//input[@id=\"adh-radio-1424\"]"), 10, 2);
                radioFumeur1.click();
            }
        }
    }

    private void informationsPrets(FluxData flux, String source) {
        int index = 0;
        WebElement inputDateEffetGaranties = waitForElement(driver, By.id("adh-input-dateEffetGaranties"), 10, 2);
        inputDateEffetGaranties.clear();
        String jourMois = getCaracteres(flux.getPrets().get(index).getDateEffet(), 5, "droite");
        inputDateEffetGaranties.sendKeys(jourMois);

        choixObjetFinancement(flux, index);

        choixTypePret(flux, index);

        waitThread(1);
        WebElement inputMontant = waitForElement(driver, By.id("adh-input-pret-montant-0"), 10, 2);
        inputMontant.sendKeys(flux.getPrets().get(index).getMontantPret());

        WebElement inputTaux = waitForElement(driver, By.id("adh-input-pret-taux-0"), 10, 2);
        inputTaux.sendKeys(flux.getPrets().get(index).getTaux());

        choixTypeTaux(flux, index);

        scrollDown(driver, 0, 400);
        waitThread(2);

        choixEcheances(flux, index);

        waitThread(1);
        if (!flux.getPrets().get(index).getDiffere().isEmpty() && (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Partiel") || flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Total"))) {
            WebElement inputDuree = waitForElement(driver, By.id("adh-input-pret-duree-0"), 10, 1);
            inputDuree.sendKeys(flux.getPrets().get(index).getDureeAmort());
            WebElement inputDiffere = waitForElement(driver, By.id("adh-input-pret-differe-0"), 10, 1);
            inputDiffere.sendKeys(flux.getPrets().get(index).getDureeDiffere());
            if (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Total")){
                WebElement buttonTotal = waitForElement(driver, By.xpath("//adh-input-radio-button[@data-testid='form-pret-0-control-idNatureDiffere-option-2']"), 10, 1);
                actions.moveToElement(buttonTotal).click().perform();
            } else if (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Partiel")) {
                WebElement buttonTotal = waitForElement(driver, By.xpath("//adh-input-radio-button[@data-testid='form-pret-0-control-idNatureDiffere-option-1']"), 10, 1);
                actions.moveToElement(buttonTotal).click().perform();
            }
        }
        else {
            WebElement inputDuree = waitForElement(driver, By.id("adh-input-pret-duree-0"), 10, 2);
            inputDuree.sendKeys(flux.getPrets().get(index).getDuree());
            WebElement inputDiffere = waitForElement(driver, By.id("adh-input-pret-differe-0"), 10, 2);
            if (flux.getPrets().get(index).getDureeDiffere() == null){
                inputDiffere.sendKeys("0");
            } else {
                inputDiffere.sendKeys(flux.getPrets().get(index).getDureeDiffere());
            }
        }

//        WebElement buttonPeriodicite = waitForElement(driver, By.xpath("//app-dynamic-dropdown//adh-reactive-dropdown//button[@data-testid=\"pret-0-idPeriodiciteEcheancePret\"]"), 10, 2);
//        buttonPeriodicite.click();
//
//        WebElement inputPeriodicite = waitForElement(driver, By.xpath("//app-dynamic-dropdown//adh-reactive-dropdown//input[@data-testid=\"pret-0-idPeriodiciteEcheancePret-input\"]"), 10, 2);
//        inputPeriodicite.sendKeys("Mensuel");
//        clickKeyEnter(actions);

        if (flux.getPrets().size() == 2) {
            index = 1;
            WebElement labelAjoutPret = driver.findElement(By.xpath("//div[text()=\"Ajouter un prêt\"]"));
            actions.moveToElement(labelAjoutPret).click().perform();
            waitThread(1);

            choixTypePret(flux, index);

            waitThread(1);
            WebElement inputMontant1 = waitForElement(driver, By.id("adh-input-pret-montant-1"), 10, 2);
            inputMontant1.sendKeys(flux.getPrets().get(index).getMontantPret());

            WebElement inputTaux1 = waitForElement(driver, By.id("adh-input-pret-taux-1"), 10, 2);
            inputTaux1.sendKeys(flux.getPrets().get(index).getTaux());

            choixTypeTaux(flux, index);

            scrollDown(driver, 0, 400);
            waitThread(2);

            choixEcheances(flux, index);

            waitThread(1);
            if (!flux.getPrets().get(index).getDiffere().isEmpty() && (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Partiel") || flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Total"))) {
                WebElement inputDuree = waitForElement(driver, By.id("adh-input-pret-duree-1"), 10, 1);
                inputDuree.sendKeys(flux.getPrets().get(index).getDureeAmort());
                WebElement inputDiffere = waitForElement(driver, By.id("adh-input-pret-differe-1"), 10, 1);
                inputDiffere.sendKeys(flux.getPrets().get(index).getDureeDiffere());
                if (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Total")){
                    WebElement buttonTotal = waitForElement(driver, By.xpath("//adh-input-radio-button[@data-testid='form-pret-1-control-idNatureDiffere-option-2']"), 10, 1);
                    actions.moveToElement(buttonTotal).click().perform();
                } else if (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Partiel")) {
                    WebElement buttonTotal = waitForElement(driver, By.xpath("//adh-input-radio-button[@data-testid='form-pret-1-control-idNatureDiffere-option-1']"), 10, 1);
                    actions.moveToElement(buttonTotal).click().perform();
                }
            } else {
                WebElement inputDuree1 = waitForElement(driver, By.id("adh-input-pret-duree-1"), 10, 2);
                inputDuree1.sendKeys(flux.getPrets().get(index).getDuree());

                WebElement inputDiffere1 = waitForElement(driver, By.id("adh-input-pret-differe-1"), 10, 2);
                if (flux.getPrets().get(index).getDureeDiffere() == null){
                    inputDiffere1.sendKeys("0");
                } else {
                  inputDiffere1.sendKeys(flux.getPrets().get(index).getDureeDiffere());
                }
            }

//            WebElement buttonPretMois1 = waitForElement(driver, By.id("//adh-button[@data-testid=\"pret-1-duree-0\"]//button[1]"), 10, 2);
//            actions.moveToElement(buttonPretMois1).click().perform();
//
//            WebElement buttonDiffereMois1 = waitForElement(driver, By.xpath("//adh-button[@data-testid=\"pret-1-differe-0\"]//button[1]"), 10, 2);
//            actions.moveToElement(buttonDiffereMois1).click().perform();
//
//            WebElement buttonPeriodicite1 = waitForElement(driver, By.xpath("//app-dynamic-dropdown//adh-reactive-dropdown//button[@data-testid=\"pret-1-idPeriodiciteEcheancePret\"]"), 10, 2);
//            buttonPeriodicite1.click();
//
//            WebElement inputPeriodicite1 = waitForElement(driver, By.xpath("//app-dynamic-dropdown//adh-reactive-dropdown//input[@data-testid=\"pret-1-idPeriodiciteEcheancePret-input\"]"), 10, 2);
//            inputPeriodicite1.sendKeys("Mensuel");
//            clickKeyEnter(actions);
        }
    }

    private void choixTypeTaux(FluxData flux, int index) {
        WebElement buttonFixe = null;
        if (index == 0) {
            if (flux.getPrets().get(index).getTypeTaux().equalsIgnoreCase("Fixe")) {
                buttonFixe = waitForElement(driver, By.xpath("//adh-input-radio-button[@data-testid=\"form-pret-0-control-idTypeTaux-option-10\"]"), 10, 2);
                actions.moveToElement(buttonFixe).click().perform();
            }
            if (flux.getPrets().get(index).getTypeTaux().equalsIgnoreCase("Variable")) {
                WebElement buttonVariable = waitForElement(driver, By.xpath("//adh-input-radio-button[@data-testid=\"form-pret-0-control-idTypeTaux-option-11\"]"), 10, 2);
                actions.moveToElement(buttonVariable).click().perform();
            }
        } else if (index == 1) {
            if (flux.getPrets().get(index).getTypeTaux().equalsIgnoreCase("Fixe")) {
                buttonFixe = waitForElement(driver, By.xpath("//adh-input-radio-button[@data-testid=\"form-pret-1-control-idTypeTaux-option-10\"]"), 10, 2);
                actions.moveToElement(buttonFixe).click().perform();
            }
            if (flux.getPrets().get(index).getTypeTaux().equalsIgnoreCase("Variable")) {
                WebElement buttonVariable = waitForElement(driver, By.xpath("//adh-input-radio-button[@data-testid=\"form-pret-1-control-idTypeTaux-option-10\"]"), 10, 2);
                actions.moveToElement(buttonVariable).click().perform();
            }
        }


    }

    private void choixEcheances(FluxData flux, int index) {
        WebElement inputEcheances = null;
        if (index == 0) {
            inputEcheances = waitForElement(driver, By.id("adh-input-pret-idTypeEcheance-0"), 10, 2);
            inputEcheances.click();
            if (flux.getPrets().get(index).getTypeTaux().equalsIgnoreCase("Fixe")) {
                WebElement constantes = waitForElement(driver, By.xpath("//adh-option[@data-testid=\"form-pret-0-control-idTypeEcheance-option-100\"]"), 15, 1);
                constantes.click();
//            inputEcheances.sendKeys("Constantes");
            } else if (flux.getPrets().get(index).getTypeTaux().equalsIgnoreCase("Variable")) {
                WebElement variables = waitForElement(driver, By.xpath("//adh-option[@data-testid=\"form-pret-0-control-idTypeEcheance-option-102\"]"), 15, 1);
                variables.click();
//            inputEcheances.sendKeys("Variables");
            } else if (flux.getPrets().get(index).getType().equalsIgnoreCase("Amortissable")) {
                WebElement amortissements = waitForElement(driver, By.xpath("//adh-option[@data-testid=\"form-pret-0-control-idTypeEcheance-option-103\"]"), 15, 1);
                amortissements.click();
                inputEcheances.sendKeys("Amortissement");
            }
        } else if (index == 1) {
            inputEcheances = waitForElement(driver, By.id("adh-input-pret-idTypeEcheance-1"), 10, 2);
            inputEcheances.click();
            if (flux.getPrets().get(index).getTypeTaux().equalsIgnoreCase("Fixe")) {
                WebElement constantes = waitForElement(driver, By.xpath("//adh-option[@data-testid=\"form-pret-1-control-idTypeEcheance-option-100\"]"), 15, 1);
                constantes.click();
//            inputEcheances.sendKeys("Constantes");
            } else if (flux.getPrets().get(index).getTypeTaux().equalsIgnoreCase("Variable")) {
                WebElement variables = waitForElement(driver, By.xpath("//adh-option[@data-testid=\"form-pret-1-control-idTypeEcheance-option-102\"]"), 15, 1);
                variables.click();
//            inputEcheances.sendKeys("Variables");
            } else if (flux.getPrets().get(index).getType().equalsIgnoreCase("Amortissable")) {
                WebElement amortissements = waitForElement(driver, By.xpath("//adh-option[@data-testid=\"form-pret-1-control-idTypeEcheance-option-103\"]"), 15, 1);
                amortissements.click();
                inputEcheances.sendKeys("Amortissement");
            }
        }

    }

    private void informationsPreteur(FluxData flux, String source) {
        WebElement linkSG = null;
        if (flux.getPrets().get(0).getBanque().equalsIgnoreCase("Banque populaire")) {
            linkSG = waitForElement(driver, By.xpath("//div[@data-testid=\"select-organisme-preteur\"]//div//img[@alt=\"BANQUE POPULAIRE\"]"), 10, 2);
            actions.moveToElement(linkSG).click().perform();
        } else if (flux.getPrets().get(0).getBanque().equalsIgnoreCase("Caisse d'épargne")) {
            linkSG = waitForElement(driver, By.xpath("//div[@data-testid=\"select-organisme-preteur\"]//div//img[@alt=\"CAISSE D'EPARGNE\"]"), 10, 2);
            actions.moveToElement(linkSG).click().perform();
        } else if (flux.getPrets().get(0).getBanque().equalsIgnoreCase("Société Générale")) {
            linkSG = waitForElement(driver, By.xpath("//div[@data-testid=\"select-organisme-preteur\"]//div//img[@alt=\"GROUPE SOCIETE GENERALE\"]"), 10, 2);
            actions.moveToElement(linkSG).click().perform();
        } else if (flux.getPrets().get(0).getBanque().equalsIgnoreCase("BNP Paribas")) {
            linkSG = waitForElement(driver, By.xpath("//div[@data-testid=\"select-organisme-preteur\"]//div//img[@alt=\"GROUPE BNP PARIBAS\"]"), 10, 2);
            actions.moveToElement(linkSG).click().perform();
        } else if (flux.getPrets().get(0).getBanque().equalsIgnoreCase("Crédit agricole")) {
            linkSG = waitForElement(driver, By.xpath("//div[@data-testid=\"select-organisme-preteur\"]//div//img[@alt=\"CREDIT AGRICOLE\"]"), 10, 2);
            actions.moveToElement(linkSG).click().perform();
        } else if (flux.getPrets().get(0).getBanque().equalsIgnoreCase("Crédit mutuel")) {
            linkSG = waitForElement(driver, By.xpath("//div[@data-testid=\"select-organisme-preteur\"]//div//img[@alt=\"CREDIT MUTUEL\"]"), 10, 2);
            actions.moveToElement(linkSG).click().perform();
        } else if (flux.getPrets().get(0).getBanque().equalsIgnoreCase("La Banque Postale")) {
            linkSG = waitForElement(driver, By.xpath("//div[@data-testid=\"select-organisme-preteur\"]//div//img[@alt=\"LA BANQUE POSTALE\"]"), 10, 2);
            actions.moveToElement(linkSG).click().perform();
        } else if (flux.getPrets().get(0).getBanque().equalsIgnoreCase("CIC")) {
            linkSG = waitForElement(driver, By.xpath("//div[@data-testid='select-organisme-preteur']//div//img[@alt='CREDIT INDUSTRIEL ET COMMERCIAL']"), 10, 2);
            actions.moveToElement(linkSG).click().perform();
        } else if (flux.getPrets().get(0).getBanque().equalsIgnoreCase("LCL")) {
            linkSG = waitForElement(driver, By.xpath("//div[@data-testid=\"select-organisme-preteur\"]//div//img[@alt=\"LCL - CREDIT LYONNAIS\"]"), 10, 2);
            actions.moveToElement(linkSG).click().perform();
        } else {
            WebElement inputSearchBanque = waitForElement(driver, By.id("filter-box-input"), 10, 2);
            inputSearchBanque.sendKeys(flux.getPrets().get(0).getBanque());
            WebElement optionChoixAgence = waitForElement(driver, By.xpath("//app-filter-box-enseigne//div//ul//li[1]"), 10, 2);
            optionChoixAgence.click();
        }

        WebElement inputchoixAgence = waitForElement(driver, By.id("filter-box-input"), 10, 2);
        inputchoixAgence.sendKeys(flux.getPersonnes().get(0).getCodePostal());
        waitThread(2);
        WebElement optionChoixAgence = waitForElement(driver, By.xpath("//div[@class=\"filter-box-agence\"]//ul//li[1]"), 10, 2);
        optionChoixAgence.click();

        nextPage(driver, By.xpath("//div[@class=\"footer-container\"]//button//span[text()=' Suivant ']"), 30, 1);
    }

    private void choixTypePret(FluxData flux, int index) {
        WebElement buttonTypePret = null;
        WebElement objetTypePretChoix = null;
        if (index == 0) {
            buttonTypePret = waitForElement(driver, By.id("adh-input-pret-idNaturePret-0"), 10, 2);
            if (buttonTypePret != null) {
                buttonTypePret.click();
                if (flux.getPrets().get(index).getType().equalsIgnoreCase("Amortissable")) {
                    objetTypePretChoix = waitForElement(driver, By.xpath("//adh-option[@data-testid=\"form-pret-0-control-idNaturePret-option-51\"]"), 10, 2);
                    objetTypePretChoix.click();
                } else if (flux.getPrets().get(index).getType().equalsIgnoreCase("Prêt relais")) {
                    objetTypePretChoix = waitForElement(driver, By.xpath("//adh-option[@data-testid=\"form-pret-0-control-idNaturePret-option-5\"]"), 10, 2);
                    objetTypePretChoix.click();
                } else if (flux.getPrets().get(index).getType().equalsIgnoreCase("Crédit bail")) {
                    objetTypePretChoix = waitForElement(driver, By.xpath("//adh-option[@data-testid=\"form-pret-0-control-idNaturePret-option-10\"]"), 10, 2);
                    objetTypePretChoix.click();
                } else if (flux.getPrets().get(index).getType().equalsIgnoreCase("Prêt à taux zéro")) {
                    objetTypePretChoix = waitForElement(driver, By.xpath("//adh-option[@data-testid=\"form-pret-0-control-idNaturePret-option-38\"]"), 10, 2);
                    objetTypePretChoix.click();
                }
            }
        }
        if (index == 1) {
            buttonTypePret = waitForElement(driver, By.id("adh-input-pret-idNaturePret-1"), 10, 2);
            if (buttonTypePret != null) {
                buttonTypePret.click();
                if (flux.getPrets().get(index).getType().equalsIgnoreCase("Amortissable")) {
                    objetTypePretChoix = waitForElement(driver, By.xpath("//adh-option[@data-testid=\"form-pret-1-control-idNaturePret-option-51\"]"), 10, 2);
                    objetTypePretChoix.click();
                } else if (flux.getPrets().get(index).getType().equalsIgnoreCase("Prêt relais")) {
                    objetTypePretChoix = waitForElement(driver, By.xpath("//adh-option[@data-testid=\"form-pret-1-control-idNaturePret-option-5\"]"), 10, 2);
                    objetTypePretChoix.click();
                } else if (flux.getPrets().get(index).getType().equalsIgnoreCase("Crédit bail")) {
                    objetTypePretChoix = waitForElement(driver, By.xpath("//adh-option[@data-testid=\"form-pret-1-control-idNaturePret-option-10\"]"), 10, 2);
                    objetTypePretChoix.click();
                } else if (flux.getPrets().get(index).getType().equalsIgnoreCase("Prêt à taux zéro")) {
                    objetTypePretChoix = waitForElement(driver, By.xpath("//adh-option[@data-testid=\"form-pret-1-control-idNaturePret-option-38\"]"), 10, 2);
                    objetTypePretChoix.click();
                }
            }
        }

    }

    private void choixObjetFinancement(FluxData flux, int index) {
        WebElement dropdownObjetFinancement = waitForElement(driver, By.id("idObjetFinancement"), 10, 2);
        dropdownObjetFinancement.click();
        WebElement objetFinancement = null;
        if (flux.getPrets().get(index).getObjet().equalsIgnoreCase("Résidence principale")) {
            objetFinancement = waitForElement(driver, By.xpath("//adh-option[@data-testid=\"form-infoDossier-control-idObjetFinancement-option-8\"]"), 10, 2);
            objetFinancement.click();
        } else if (flux.getPrets().get(index).getObjet().equalsIgnoreCase("Résidence secondaire")) {
            objetFinancement = waitForElement(driver, By.xpath("//adh-option[@data-testid=\"form-infoDossier-control-idObjetFinancement-option-9\"]"), 10, 2);
            objetFinancement.click();
            elementNeutre(driver);
        } else if (flux.getPrets().get(index).getObjet().equalsIgnoreCase("Investissement locatif")) {
            objetFinancement = waitForElement(driver, By.xpath("//adh-option[@data-testid=\"form-infoDossier-control-idObjetFinancement-option-3\"]"), 10, 2);
            objetFinancement.click();
            elementNeutre(driver);
        }
    }

    private void choixStatutProfession(FluxData flux, int index) {
        WebElement dropDownSituationPro = null;
        if (index == 0) {
            dropDownSituationPro = waitForElement(driver, By.xpath("//*[@id=\"adh-input-profession-idStatutProfessionnel-0\"]"), 10, 2);
            actions.moveToElement(dropDownSituationPro).click().perform();
            WebElement optionSituationPro = null;
            if (flux.getPersonnes().get(index).getProfessionSpecifique().contains("paramédicale")) {
                optionSituationPro = waitForElement(driver, By.xpath("//div[@id=\"adh-input-profession-idStatutProfessionnel-0-panel\"]//adh-option[3]"), 10, 2);
                actions.moveToElement(optionSituationPro).click().perform();
            }
            if (flux.getPersonnes().get(index).getProfessionSpecifique().contains("médicale")) {
                optionSituationPro = waitForElement(driver, By.xpath("//div[@id=\"adh-input-profession-idStatutProfessionnel-0-panel\"]//adh-option[4]"), 10, 2);
                actions.moveToElement(optionSituationPro).click().perform();
            } else {
                optionSituationPro = waitForElement(driver, By.xpath("//div[@id=\"adh-input-profession-idStatutProfessionnel-0-panel\"]//adh-option[1]"), 10, 2);
                actions.moveToElement(optionSituationPro).click().perform();
            }
        }
        if (index == 1) {
            dropDownSituationPro = waitForElement(driver, By.xpath("//adh-select[@id=\"profession-idStatutProfessionnel-1\"]//button[@id=\"adh-input-profession-idStatutProfessionnel-1\"]"), 10, 2);
            actions.moveToElement(dropDownSituationPro).click().perform();
            WebElement optionSituationPro = null;
            if (flux.getPersonnes().get(index).getProfessionSpecifique().contains("paramédicale")) {
                optionSituationPro = waitForElement(driver, By.xpath("//div[@id=\"adh-input-profession-idStatutProfessionnel-1-panel\"]//adh-option[3]"), 10, 2);
                actions.moveToElement(optionSituationPro).click().perform();
            }
            if (flux.getPersonnes().get(index).getProfessionSpecifique().contains("médicale")) {
                optionSituationPro = waitForElement(driver, By.xpath("//div[@id=\"adh-input-profession-idStatutProfessionnel-1-panel\"]//adh-option[4]"), 10, 2);
                actions.moveToElement(optionSituationPro).click().perform();
            } else {
                optionSituationPro = waitForElement(driver, By.xpath("//div[@id=\"adh-input-profession-idStatutProfessionnel-1-panel\"]//adh-option[1]"), 10, 2);
                actions.moveToElement(optionSituationPro).click().perform();
            }
        }
    }

    @Override
    protected void pageName(String namePage) {}
}
