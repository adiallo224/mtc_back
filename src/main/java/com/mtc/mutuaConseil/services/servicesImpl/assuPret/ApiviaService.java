package com.mtc.mutuaConseil.services.servicesImpl.assuPret;

import com.mtc.mutuaConseil.models.Compte;
import com.mtc.mutuaConseil.models.FluxData;
import com.mtc.mutuaConseil.models.Tarif;
import com.mtc.mutuaConseil.services.LaunchedService;
import com.mtc.mutuaConseil.services.servicesImpl.TypeAssuranceService;
import com.mtc.mutuaConseil.utils.PageElementInteraction;
import com.mtc.mutuaConseil.utils.TarifUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class ApiviaService extends PageElementInteraction implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(ApiviaService.class);
    private static WebDriver driver;
    private static WebDriverWait wait;
    private Actions actions;
    JavascriptExecutor js = null;
    private final TypeAssuranceService typeAssuranceService;

    public ApiviaService(TypeAssuranceService typeAssuranceService){
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        log.info("Debut de traitement -- Apivia");
        Tarif tarifApivia = TarifUtils.createDefaultTarif(c, typeAssuranceService, 1L);
        EdgeOptions options = new EdgeOptions();
        options.addArguments("start-maximized");
        options.addArguments("disable-infobars");
        options.addArguments("--disable-extensions");
        driver = new EdgeDriver(options);
        driver.manage().window().maximize();
        wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        actions = new Actions(driver);
        js = (JavascriptExecutor) driver;
        driver.get(c.getUrlFournisseur());
        try {
//            WebElement inputlogin = waitForElement1(driver, By.xpath("/html/body/div[1]/form/div[2]/div[3]/button"), 20,2);
//            inputlogin.sendKeys(c.getUsername());
//            WebElement inputPassword = waitForElement1(driver, By.xpath("/html/body/div[1]/form/div[2]/div[3]/button"), 20,2);
//            inputPassword.sendKeys(c.getPassword());
//            WebElement buttonConnexion = waitForElement1(driver, By.xpath("/html/body/div[1]/form/div[2]/div[3]/button"), 20,2);
//            buttonConnexion.click();
            
            informationsGenerales(flux);

            waitThread(2);
            next(driver, 1);
//            WebElement buttonEtapeSuivant = waitForElement1(driver, By.xpath("/html/body/div[1]/form/div[2]/div[3]/button"), 20,2);
//            buttonEtapeSuivant.click();

            informationsPersonnes(flux);
            waitThread(2);
            next(driver, 2);
//            WebElement buttonEtapeSuivant1 = waitForElement(driver, By.xpath("/html/body/div[1]/form/div[3]/div[3]/button"), 20, 2);
//            buttonEtapeSuivant1.click();

            informationsPret(flux);
            waitThread(2);
            next(driver, 3);
//            WebElement buttonEtapeSuivant2 = waitForElement(driver, By.xpath("/html/body/div[1]/form/div[4]/div[3]/button"), 20, 2);
//            buttonEtapeSuivant2.click();

            informationsComplementaires(flux);
            waitThread(30);
            WebElement buttonEtapeSuivant3 = waitForElement(driver, By.xpath("//button[@id=\"demande_form_comparer\"]"), 20, 2);
            buttonEtapeSuivant3.click();

            String cout = "";
            List<WebElement> montantElements = driver.findElements(By.cssSelector("div.panel-body span.h3.infoTarifMens"));
            if (!montantElements.isEmpty()) {
                WebElement premierMontantElement = montantElements.get(0);
                cout = premierMontantElement.getText();
                log.info("Montant récupéré : " + cout);
                tarifApivia.setMontant(cout);
                String screenshotBytes = captureScreenshot(driver, tarifApivia.getNom(), false, tarifApivia);
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
            String screenshotBytesErreur = captureScreenshot(driver, tarifApivia.getNom(), true, tarifApivia);
            tarifApivia.setCaptureImgErreur(screenshotBytesErreur);
            tarifApivia.setEtape("");
        } finally {
            driver.quit();
        }
        return tarifApivia;
    }

    public void next(WebDriver driver, int page) {
        WebElement buttonNext = null;
        if (page == 1) {
            buttonNext = waitForElement1(driver, By.xpath("//button[contains(text(),'Etape suivante : Vous')]"), 20,2);
        }
        if (page == 2) {
            buttonNext = waitForElement(driver, By.xpath("//button[contains(text(),'Etape suivante : Les prêts à assurer')]"), 20,2);
        }
        if (page == 3) {
            buttonNext = waitForElement(driver, By.xpath("//button[contains(text(),'Etape suivante : Comment vous joindre')]"), 20, 2);
        }
        if (buttonNext != null)
            buttonNext.click();
    }

    private void informationsGenerales(FluxData flux) {
        WebElement radioButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("/html/body/div[1]/form/div[2]/div[2]/div[2]/div/div/label[1]")));
        radioButton.click();
        choixObjetPret(flux);
        choixBanque(flux);
    }

    private void informationsPersonnes(FluxData flux) {
        int index = 0;
        waitThread(2);
        if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur")) {
            WebElement radioButtonEmprunteur = waitForElement1(driver, By.xpath("//div[@id=\"demande_form_personnes_0_titre\"]//label[ contains(text(), 'M.')]"), 60, 1);
            actions.moveToElement(radioButtonEmprunteur).click().perform();
        } else if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame")){
            WebElement radioButtonEmprunteur = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//div[@id=\"demande_form_personnes_0_titre\"]//label[ contains(text(), 'Mme')]")));
            actions.moveToElement(radioButtonEmprunteur).click().perform();
        }

        WebElement inputPrenom = waitForElement1(driver, By.id("demande_form_personnes_0_prenom"), 20, 2);
        inputPrenom.sendKeys(flux.getPersonnes().get(index).getPrenom());
        WebElement inputNom = waitForElement1(driver, By.id("demande_form_personnes_0_nom"),20, 2);
        inputNom.sendKeys(flux.getPersonnes().get(index).getNom());
        WebElement selectDate = waitForElement1(driver, By.xpath("//input[@id='demande_form_personnes_0_datenaissance']"), 20, 2);
        selectDate.sendKeys(flux.getPersonnes().get(index).getDateNaissance());
        waitThread(2);
        WebElement inputLieuNais = waitForElement(driver, By.id("demande_form_personnes_0_villenaissance"), 20, 2);
        inputLieuNais.sendKeys(flux.getPersonnes().get(index).getVille());
        clickKeyEnter(actions);
        choixFumeur(flux, index);
        choixProfession(flux, index);
        choixManutention(flux, index);
        choixDeplacementPro(flux, index);
        choixTravailHauteur(flux, index);
        choixTravailRisque(flux, index);
        if (flux.getPersonnes().size() == 2) {
            index = 1;
            WebElement buttonAjouterPersonne = waitForElement(driver, By.xpath("//button[contains(@class, 'btn-ajoutPersonne')]"), 20, 2);
            actions.moveToElement(buttonAjouterPersonne).click().perform();
            waitThread(1);
            if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur")) {
                WebElement radioButtonEmprunteur = waitForElement1(driver, By.xpath("//div[@id=\"demande_form_personnes_1_titre\"]//label[ contains(text(), 'M.')]"), 60, 1);
                actions.moveToElement(radioButtonEmprunteur).click().perform();
            } else if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame")){
                WebElement radioButtonEmprunteur = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//div[@id=\"demande_form_personnes_1_titre\"]//label[ contains(text(), 'Mme')]")));
                actions.moveToElement(radioButtonEmprunteur).click().perform();
            }
            WebElement inputPrenom1 = waitForElement1(driver, By.id("demande_form_personnes_1_prenom"), 20, 2);
            inputPrenom1.sendKeys(flux.getPersonnes().get(index).getPrenom());
            WebElement inputNom1 = waitForElement1(driver, By.id("demande_form_personnes_1_nom"),20, 2);
            inputNom1.sendKeys(flux.getPersonnes().get(index).getNom());
            WebElement selectDate1 = waitForElement1(driver, By.xpath("//input[@id='demande_form_personnes_1_datenaissance']"), 20, 2);
            selectDate1.sendKeys(flux.getPersonnes().get(index).getDateNaissance());
            waitThread(2);
            WebElement inputLieuNais1 = waitForElement1(driver, By.id("demande_form_personnes_1_villenaissance"), 20, 2);
            inputLieuNais1.sendKeys(flux.getPersonnes().get(index).getVille());
            clickKeyEnter(actions);
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
        WebElement inputMontant = waitForElement1(driver, By.id("demande_form_prets_0_montant"), 20, 2);
        inputMontant.sendKeys(flux.getPrets().get(index).getMontantPret());
        WebElement inputTaux = waitForElement1(driver, By.id("demande_form_prets_0_taux"), 20, 2);
        inputTaux.sendKeys(flux.getPrets().get(index).getTaux());
        WebElement inputDuree = waitForElement1(driver, By.id("demande_form_prets_0_duree"), 20, 2);
        inputDuree.sendKeys(flux.getPrets().get(index).getDuree());
        if (flux.getPrets().get(index).getType().equalsIgnoreCase("Amortissable")) {
            choixTypePret(flux, index);
        }
        if (flux.getPrets().size() == 2) {
            index = 1;
            waitThread(2);
            WebElement buttonAjouterPret = waitForElement1(driver, By.xpath("//button[contains(@class, 'btn-ajoutPret')]"), 20, 2);
            actions.moveToElement(buttonAjouterPret).click().perform();
            WebElement inputMontant1 = waitForElement1(driver, By.id("demande_form_prets_1_montant"), 20, 2);
            inputMontant1.sendKeys(flux.getPrets().get(index).getMontantPret());
            WebElement inputTaux1 = waitForElement1(driver, By.id("demande_form_prets_1_taux"), 20, 2);
            inputTaux1.sendKeys(flux.getPrets().get(index).getTaux());
            WebElement inputDuree1 = waitForElement1(driver, By.id("demande_form_prets_1_duree"), 20, 2);
            inputDuree1.sendKeys(flux.getPrets().get(index).getDuree());
            if (flux.getPrets().get(index).getType().equalsIgnoreCase("Amortissable")) {
                choixTypePret(flux, index);
            }
        }
        WebElement inputFraisCourtage = waitForElement(driver, By.id("demande_form_fraiscourtage"), 20, 2);
        inputFraisCourtage.sendKeys("15");
    }

    private void informationsComplementaires(FluxData flux) {
        int index = 0;
        WebElement inputAdresse = waitForElement(driver, By.id("demande_form_personnes_0_adresse1"), 20, 1);
        inputAdresse.sendKeys(flux.getPersonnes().get(index).getNumeroVoie() + " " + flux.getPersonnes().get(index).getNomVoie());
//        WebElement inputComplement = waitForElement(driver, By.id("demande_form_personnes_0_adresse2"), 20, 1);
//        inputComplement.sendKeys("Bravo");
        WebElement inputCodePostal = waitForElement(driver, By.id("demande_form_personnes_0_cp"), 20, 1);
        inputCodePostal.sendKeys(flux.getPersonnes().get(index).getCodePostal());
        WebElement inputVille = waitForElement(driver, By.id("demande_form_personnes_0_ville"), 20, 1);
        inputVille.sendKeys(flux.getPersonnes().get(index).getVille());
        WebElement inputEmail = waitForElement(driver, By.id("demande_form_personnes_0_email"), 20, 1);
        inputEmail.sendKeys(flux.getPersonnes().get(index).getEmail());
        WebElement inputTelPortable = waitForElement(driver, By.id("demande_form_personnes_0_tel1"), 20, 1);
        inputTelPortable.sendKeys(flux.getPersonnes().get(index).getTelephone());
        if (flux.getPersonnes().size() == 2) {
            index = 1;
            WebElement inputAdresse1 = waitForElement(driver, By.id("demande_form_personnes_1_adresse1"), 20, 1);
            inputAdresse1.sendKeys(flux.getPersonnes().get(index).getNumeroVoie() + " " + flux.getPersonnes().get(index).getNomVoie());
            WebElement inputComplement1 = waitForElement(driver, By.id("demande_form_personnes_1_adresse2"), 20, 1);
            inputComplement1.sendKeys("");
            WebElement inputCodePostal1 = waitForElement(driver, By.id("demande_form_personnes_1_cp"), 20, 1);
            inputCodePostal1.sendKeys(flux.getPersonnes().get(index).getCodePostal());
            WebElement inputVille1 = waitForElement(driver, By.id("demande_form_personnes_1_ville"), 20, 1);
            inputVille1.sendKeys(flux.getPersonnes().get(index).getVille());
            WebElement inputEmail1 = waitForElement(driver, By.id("demande_form_personnes_1_email"), 20, 1);
            inputEmail1.sendKeys(flux.getPersonnes().get(index).getEmail());
            WebElement inputTelPortable1 = waitForElement(driver, By.id("demande_form_personnes_1_tel1"), 20, 1);
            inputTelPortable1.sendKeys(flux.getPersonnes().get(index).getTelephone());
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
        WebElement checkChoixAccepte = waitForElement(driver, By.id("consentementPersonne0"),15, 1);
        waitThread(1);
        actions.moveToElement(checkChoixAccepte).click().perform();
        if (flux.getPersonnes().size() == 2) {
            WebElement checkChoixAccepte1 = waitForElement(driver, By.id("consentementPersonne1"), 15, 1);
            waitThread(1);
            actions.moveToElement(checkChoixAccepte1).click().perform();
        }
        WebElement checkChoixEtudeAccepte = waitForElement(driver, By.xpath("//div[@id='demande_form_consentementcontact']//label[@for='demande_form_consentementcontact_1']"),15, 1);
        waitThread(1);
        actions.moveToElement(checkChoixEtudeAccepte).click().perform();
    }

    private void choixBanque(FluxData flux) {
        WebElement dropdownElementBanque = waitForElement1(driver, By.id("demande_form_preteur"), 20, 2);
        Select selectBanque = new Select(dropdownElementBanque);
        List<WebElement> options = selectBanque.getOptions();
        for (WebElement opt : options){
            if (opt.getText().equalsIgnoreCase("BNP Paribas") && flux.getPrets().get(0).getBanque().equalsIgnoreCase("BNP Paribas")) {
                selectBanque.selectByVisibleText("BNP Paribas");
                break;
            } else if (opt.getText().equalsIgnoreCase("Banque Populaire Auvergne Rhône Alpes") && flux.getPrets().get(0).getBanque().equalsIgnoreCase("Banque populaire") ) {
                selectBanque.selectByVisibleText("Banque Populaire Auvergne Rhône Alpes");
                break;
            } else if (opt.getText().equalsIgnoreCase("Axa Banque") && flux.getPrets().get(0).getBanque().equalsIgnoreCase("Axa banque")) {
                selectBanque.selectByVisibleText("Axa Banque");
                break;
            } else if (opt.getText().equalsIgnoreCase("Caisse d'Epargne Rhône Alpes") && (flux.getPrets().get(0).getBanque().equalsIgnoreCase("Caisse d\\'épargne"))) {
                selectBanque.selectByVisibleText("Caisse d'Epargne Rhône Alpes");
                break;
            } else if (opt.getText().equalsIgnoreCase("Crédit Agricole Sud Rhône Alpes") && flux.getPrets().get(0).getBanque().equalsIgnoreCase("Crédit agricole") ) {
                selectBanque.selectByVisibleText("Crédit Agricole Sud Rhône Alpes");
                break;
            } else if (opt.getText().equalsIgnoreCase("CIC") && flux.getPrets().get(0).getBanque().equalsIgnoreCase("CIC")) {
                selectBanque.selectByVisibleText("CIC");
                break;
            } else if (opt.getText().equalsIgnoreCase("Crédit Mutuel Savoie Mont-Blanc") && (flux.getPrets().get(0).getBanque().equalsIgnoreCase("Crédit mutuel"))) {
                selectBanque.selectByVisibleText("Crédit Mutuel Savoie Mont-Blanc");
                break;
            } else if (opt.getText().equalsIgnoreCase("LCL") && (flux.getPrets().get(0).getBanque().equalsIgnoreCase("LCL"))) {
                selectBanque.selectByVisibleText("LCL");
                break;
            } else if (opt.getText().equalsIgnoreCase("SG Societe Generale") && (flux.getPrets().get(0).getBanque().equalsIgnoreCase("Société Générale"))) {
                selectBanque.selectByVisibleText("SG Societe Generale");
                break;
            } else if (opt.getText().equalsIgnoreCase("BoursoBank") && (flux.getPrets().get(0).getBanque().equalsIgnoreCase("BoursoBank"))) {
                selectBanque.selectByVisibleText("BoursoBank");
                break;
            } else if (opt.getText().equalsIgnoreCase("La Banque Postale") && (flux.getPrets().get(0).getBanque().equalsIgnoreCase("La Banque Postale"))) {
                selectBanque.selectByVisibleText("La Banque Postale");
                break;
            }
        }
    }

    private void choixObjetPret(FluxData flux) {
        WebElement dropdownElement = waitForElement1(driver, By.id("demande_form_objetprojet"), 30, 2);
        Select dropdownObjet = new Select(dropdownElement);
        List<WebElement> options = dropdownObjet.getOptions();
        for (WebElement opt : options){
            if (opt.getText().equalsIgnoreCase("Résidence principale primo accédant") && flux.getPrets().get(0).getObjet().equalsIgnoreCase("Résidence principale")) {
                dropdownObjet.selectByVisibleText("Résidence principale primo accédant");
                break;
            }
            else if (opt.getText().equalsIgnoreCase("Résidence secondaire ou autre bien immobilier") &&
               (flux.getPrets().get(0).getObjet().equalsIgnoreCase("Résidence secondaire") || flux.getPrets().get(0).getObjet().equalsIgnoreCase("Autre immobilier"))) {
                dropdownObjet.selectByVisibleText("Résidence secondaire ou autre bien immobilier");
                break;
            }
            else if (opt.getText().equalsIgnoreCase("Investissement locatif") && flux.getPrets().get(0).getObjet().equalsIgnoreCase("Investissement locatif")) {
                dropdownObjet.selectByVisibleText("Investissement locatif");
                break;
            }
            else if (opt.getText().equalsIgnoreCase("Prêts professionnels") && (flux.getPrets().get(0).getObjet().equalsIgnoreCase("Prêt à objet professionnel"))) {
                dropdownObjet.selectByVisibleText("Prêts professionnels");
                break;
            }
        }
    }

    private void choixFumeur(FluxData flux, int index) {
        WebElement dropdownElementFumeur = null;
        if (index == 0) {
            dropdownElementFumeur = waitForElement(driver, By.xpath("//select[@id=\"demande_form_personnes_0_fumeur\"]"), 20, 2);
        } else if (index == 1) {
            dropdownElementFumeur = waitForElement1(driver, By.id("demande_form_personnes_1_fumeur"), 20, 2);
        }
        if (dropdownElementFumeur != null) {
            Select dropdownFumeur = new Select(dropdownElementFumeur);
            List<WebElement> options = dropdownFumeur.getOptions();
            for (WebElement opt : options) {
                if (!flux.getInfoAssureComplets().get(index).getFumeur() && opt.getText().equalsIgnoreCase("Non fumeur")) {
                    dropdownFumeur.selectByVisibleText("Non fumeur");
                    break;
                } else if (flux.getInfoAssureComplets().get(index).getFumeur() && opt.getText().equalsIgnoreCase("Fumeur")) {
                    dropdownFumeur.selectByVisibleText("Fumeur");
                    break;
                }
            }
        }
    }

    private void choixProfession(FluxData flux, int index) {
        WebElement dropdownElementProfession = null;
        if (index == 0) {
            dropdownElementProfession = waitForElement1(driver, By.xpath("//select[@id=\"demande_form_personnes_0_situationpro\"]"), 20, 2);
        } else if (index == 1) {
            dropdownElementProfession = waitForElement1(driver, By.xpath("//select[@id=\"demande_form_personnes_1_situationpro\"]"), 20, 2);
        }
        if (dropdownElementProfession != null) {
            Select dropdownProfession = new Select(dropdownElementProfession);
            List<WebElement> options = dropdownProfession.getOptions();
            for (WebElement opt : options) {
                if (opt.getText().equalsIgnoreCase("Commerçant") && flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Commerçant")) {
                    dropdownProfession.selectByVisibleText("Commerçant");
                    break;
                } else if (opt.getText().equalsIgnoreCase("Artisan") && flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Artisan")) {
                    dropdownProfession.selectByVisibleText("Artisan");
                    break;
                } else if (opt.getText().equalsIgnoreCase("Profession agricole") && flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Agriculteur")) {
                    dropdownProfession.selectByVisibleText("Profession agricole");
                    break;
                } else if (opt.getText().equalsIgnoreCase("Profession libérale (hors paramédical)") && flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Profession libérale")) {
                    dropdownProfession.selectByVisibleText("Profession libérale (hors paramédical)");
                    break;
                } else if (opt.getText().equalsIgnoreCase("Fonctionnaire catégorie A") && flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Fonctionnaire classe a")) {
                    dropdownProfession.selectByVisibleText("Fonctionnaire catégorie A");
                    break;
                } else if (opt.getText().equalsIgnoreCase("Ouvrier") && flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Ouvrier")) {
                    dropdownProfession.selectByVisibleText("Ouvrier");
                    break;
                } else if (opt.getText().equalsIgnoreCase("Cadre / Chef d'entreprise (hors BTP)") &&
                          (flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Salarié cadre") || flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Chef d\\'entreprise"))) {
                    dropdownProfession.selectByVisibleText("Cadre / Chef d'entreprise (hors BTP)");
                    break;
                } else if (opt.getText().equalsIgnoreCase("Employé (hors paramédical)") &&
                        (flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Salarié non cadre : employé") || flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Chef d\\'entreprise"))) {
                    dropdownProfession.selectByVisibleText("Employé (hors paramédical)");
                    break;
                }
            }
        }
    }

    private void choixManutention(FluxData flux, int index) {
        WebElement dropdownManutention = null;
        if (index == 0) {
            dropdownManutention = waitForElement(driver, By.xpath("//select[@id=\"demande_form_personnes_0_manutention\"]"), 20, 2);
        } else if (index == 1) {
            dropdownManutention = waitForElement(driver, By.xpath("//select[@id=\"demande_form_personnes_1_manutention\"]"), 20, 2);
        }
        if (dropdownManutention != null) {
            Select selectManutention = new Select(dropdownManutention);
            List<WebElement> options = selectManutention.getOptions();
            for (WebElement opt : options) {
                if (flux.getInfoAssureComplets().get(index).getTravailManuel()) {
                    selectManutention.selectByVisibleText("Légère");
                    break;
                } else if (flux.getInfoAssureComplets().get(index).getTravailManuelManuLourde()) {
                    selectManutention.selectByVisibleText("Lourde ou avec outillage");
                    break;
                } else {
                    selectManutention.selectByVisibleText("Pas de manutention");
                    break;
                }
            }
        }
    }

    private void choixDeplacementPro(FluxData flux, int index) {
        WebElement dropdownElementDeplacementPro = null;
        if (index == 0) {
            dropdownElementDeplacementPro = waitForElement(driver, By.xpath("//select[@id=\"demande_form_personnes_0_deplacements\"]"), 20, 2);
        } else if (index == 1) {
            dropdownElementDeplacementPro = waitForElement(driver, By.xpath("//select[@id=\"demande_form_personnes_1_deplacements\"]"), 20, 2);
        }
        if (dropdownElementDeplacementPro != null) {
            Select dropdownDeplacementPro = new Select(dropdownElementDeplacementPro);
            List<WebElement> options = dropdownDeplacementPro.getOptions();
            for (WebElement opt : options) {
                if (!flux.getInfoAssureComplets().get(index).getDeplacementPro20000() && opt.getText().equalsIgnoreCase("De 15 000 Km à 30 000 Km")) {
                    dropdownDeplacementPro.selectByVisibleText("De 15 000 Km à 30 000 Km");
                    break;
                } else if (flux.getInfoAssureComplets().get(index).getDeplacementPro20000() && opt.getText().equalsIgnoreCase("Plus de 30 000 Km")) {
                    dropdownDeplacementPro.selectByVisibleText("Plus de 30 000 Km");
                    break;
                }
            }
        }
    }

    private void choixTravailHauteur(FluxData flux, int index) {
        WebElement dropdownElementTravailHauteur = null;
        if (index == 0) {
            dropdownElementTravailHauteur = waitForElement(driver, By.xpath("//select[@id=\"demande_form_personnes_0_hauteur\"]"), 20, 2);
        } else if (index == 1) {
            dropdownElementTravailHauteur = waitForElement(driver, By.xpath("//select[@id=\"demande_form_personnes_1_hauteur\"]"), 20, 2);
        }
        if (dropdownElementTravailHauteur != null) {
            Select dropdownTravailHauteur = new Select(dropdownElementTravailHauteur);
            List<WebElement> options = dropdownTravailHauteur.getOptions();
            for (WebElement opt : options) {
                if (!flux.getInfoAssureComplets().get(index).getTravailHauteur() && opt.getText().equalsIgnoreCase("Non")) {
                    dropdownTravailHauteur.selectByVisibleText("Non");
                    break;
                } else if (flux.getInfoAssureComplets().get(index).getTravailHauteur() && opt.getText().equalsIgnoreCase("De 3 à 20 mètres")) {
                    dropdownTravailHauteur.selectByVisibleText("De 3 à 20 mètres");
                    break;
                }
            }
        }
    }

    private void choixTravailRisque(FluxData flux, int index) {
        WebElement dropdownElementProfRisque = null;
        if (index == 0) {
            dropdownElementProfRisque = waitForElement(driver, By.xpath("//select[@id=\"demande_form_personnes_0_profrisque\"]"), 20, 2);
        } else if (index == 1) {
            dropdownElementProfRisque = waitForElement(driver, By.xpath("//select[@id=\"demande_form_personnes_1_profrisque\"]"), 20, 2);
        }
        if (dropdownElementProfRisque != null) {
            Select dropdownProfRisque = new Select(dropdownElementProfRisque);
            List<WebElement> options = dropdownProfRisque.getOptions();
            for (WebElement opt : options) {
                if ((!flux.getInfoAssureComplets().get(index).getMetierExpose() || !flux.getInfoAssureComplets().get(index).getProduitDanger()) && opt.getText().equalsIgnoreCase("Non")) {
                    dropdownProfRisque.selectByVisibleText("Non");
                    break;
                } else if ((!flux.getInfoAssureComplets().get(index).getMetierExpose() || !flux.getInfoAssureComplets().get(index).getProduitDanger()) && opt.getText().equalsIgnoreCase("Oui")) {
                    dropdownProfRisque.selectByVisibleText("Oui");
                    break;
                }
            }
        }
    }

    private void choixTypePret(FluxData flux, int index) {
        WebElement dropdownTypePret = null;
        if (index == 0) {
            dropdownTypePret = waitForElement(driver, By.xpath("//select[@id=\"demande_form_prets_0_type\"]"), 20, 2);
        } else if (index == 1) {
            dropdownTypePret = waitForElement(driver, By.xpath("//select[@id=\"demande_form_prets_1_type\"]"), 20, 2);
        }
        if (dropdownTypePret != null) {
            Select selectTypePret = new Select(dropdownTypePret);
            List<WebElement> options = selectTypePret.getOptions();
            for (WebElement opt : options) {
                if (flux.getPrets().get(index).getType().equalsIgnoreCase("Amortissable")  && opt.getText().equalsIgnoreCase("Prêt amortissable classique")) {
                    selectTypePret.selectByVisibleText("Prêt amortissable classique");
                    break;
                } else if (flux.getPrets().get(index).getType().equalsIgnoreCase("Prêt à paliers") && opt.getText().equalsIgnoreCase("Prêt à paliers")) {
                    selectTypePret.selectByVisibleText("Prêt à paliers");
                    break;
                }
            }
        }
    }

    @Override
    protected void pageName(String namePage) {}
}
