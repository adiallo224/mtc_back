package com.mtc.mutuaConseil.services.servicesImpl.assuPret;

import com.mtc.mutuaConseil.models.Compte;
import com.mtc.mutuaConseil.models.FluxData;
import com.mtc.mutuaConseil.models.Tarif;
import com.mtc.mutuaConseil.services.LaunchedService;
import com.mtc.mutuaConseil.services.servicesImpl.AuthenticatorService;
import com.mtc.mutuaConseil.services.servicesImpl.TypeAssuranceService;
import com.mtc.mutuaConseil.utils.InformationsPersonne;
import com.mtc.mutuaConseil.utils.LibSelenium;
import com.mtc.mutuaConseil.utils.PageElementInteraction;
import com.mtc.mutuaConseil.utils.TarifUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ApicilService extends PageElementInteraction implements LaunchedService {

    @Autowired
    private AuthenticatorService authenticatorService;
    private final Logger log = LoggerFactory.getLogger(ApicilService.class);
    public static WebDriver driver;
    private Actions actions;
    private String source ="ApicilPret";
    private final TypeAssuranceService typeAssuranceService;

    public ApicilService(TypeAssuranceService typeAssuranceService){
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        log.info("Debut de traitement -- Apicil");
        Tarif tarifApicilPret = TarifUtils.createDefaultTarif(c, typeAssuranceService, 1L);
        driver = new EdgeDriver();
        driver.manage().window().maximize();
        actions = new Actions(driver);
        driver.get(c.getUrlFournisseur());
        try {
            WebElement buttonCookies = waitForElement(driver, By.xpath("//div[@id=\"CybotCookiebotDialogBodyButtonsWrapper\"]//button[@id=\"CybotCookiebotDialogBodyLevelButtonLevelOptinAllowAll\"]"), 10, 1);
            buttonCookies.click();
            WebElement inputLogin = waitForElement(driver, By.xpath("//div[@id=\"sfdc_username_container\"]//input"), 10, 1);
            inputLogin.sendKeys(c.getUsername());
            WebElement inputPassword = waitForElement(driver, By.xpath("//div[@id=\"sfdc_password_container\"]//input"), 10, 1);
            inputPassword.sendKeys(c.getPassword());
            WebElement buttonLogin = waitForElement(driver, By.xpath("//div[@class=\"salesforceIdentityLoginForm2\"]//button"), 10, 1);
            buttonLogin.click();


//            String path = "src/main/resources/qRCodes/qrcode.jpg";
//            String secretKey = authenticatorService.getSecretKey(c, path);
//            String code = authenticatorService.getTOTPCode(secretKey);
//            log.info("Code : " + code);
//            WebElement inputToken = waitForElement(driver, By.xpath("//input[@class=\"token\"]"), 10, 1);
//            inputToken.sendKeys(code);
//            WebElement buttonSoumettre = waitForElement(driver, By.xpath("//button[normalize-space()='SOUMETTRE']"), 10, 1);
            waitThread(22);
//            buttonSoumettre.click();

            WebElement buttonFaireUnDevis = waitForElement(driver, By.xpath("//button[@class=\"btnPrincipal btIconesBtFaireUnDevis\"]"), 10, 1);
            buttonFaireUnDevis.click();
    //        WebElement buttonDevisEmprunteur = waitForElement(driver, By.xpath("//button[@class=\"sousButt hiddenButton\"]"), 10, 1);
    //        buttonDevisEmprunteur.click();
            WebElement buttonDevisEmprunteur = waitForElement1(driver, By.xpath("//b[normalize-space()='Devis emprunteur']"), 10, 1);
            buttonDevisEmprunteur.click();
            switchPage(driver);
            waitThread(8);
            WebElement buttonPersonnePhysique = waitForElement1(driver, By.xpath("//input[@id=\"ctl00_cph_Body_personnephysique\"]"), 60, 2);
            actions.moveToElement(buttonPersonnePhysique).click().perform();
            remplirInformationsPerso(flux, source);
            nextPage(driver, By.xpath("//input[@id=\"ctl00_btn_Suivant\"]"), 10, 1);
            waitThread(3);
            // Page projet
            remplirInformationsPret(flux, source);
            nextPage(driver, By.xpath("//input[@id=\"ctl00_btn_Suivant\"]"), 10, 1);
            waitThread(3);
            // Page choix des garanties
            WebElement total = waitForElement(driver, By.xpath("//span[@id=\"ctl00_cph_Body_tarifsurmesure\"]"), 10, 1);
            if(total == null){
                log.warn("total is not found");
                return null;
            } else {
                String cout = total.getText();
                log.info("cout : {} ", cout);
                tarifApicilPret.setMontant(cout);
                String screenshotBytes = captureScreenshot(driver, tarifApicilPret.getNom(), false, tarifApicilPret);
                if (screenshotBytes != null) {
                    tarifApicilPret.setCaptureImg(screenshotBytes);
                }
                tarifApicilPret.setExecution(true);
            }
        } catch (Exception e) {
            log.error("An error occurred: ", e);
            tarifApicilPret.setErreur(e.getMessage());
            String screenshotBytesErreur = captureScreenshot(driver, tarifApicilPret.getNom(), true, tarifApicilPret);
            tarifApicilPret.setCaptureImgErreur(screenshotBytesErreur);
            tarifApicilPret.setEtape("");
        } finally {
            driver.quit();
        }
       return tarifApicilPret;
    }

    private void remplirInformationsPerso(FluxData flux, String source) {
        int index = 0;
        if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur")) {
            WebElement radioCivilite = waitForElement(driver,By.xpath("//input[@id=\"ctl00_cph_Body_uc_emprunteur_civilite_m\"]"), 20, 1);
            actions.moveToElement(radioCivilite).click().perform();
        } else if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame")){
            WebElement radioCivilite = LibSelenium.waitForElement(driver,By.xpath("//input[@id=\"ctl00_cph_Body_uc_emprunteur_civilite_mme\"]"), 20, 1);
            actions.moveToElement(radioCivilite).click().perform();
        }
        InformationsPersonne.infoNom(driver, By.xpath("//input[@id=\"ctl00_cph_Body_uc_emprunteur_nom\"]"), 10, 1, flux, index, source);
        InformationsPersonne.infoPrenom(driver, By.xpath("//input[@id=\"ctl00_cph_Body_uc_emprunteur_prenom\"]"), 10, 1, flux, index, source);
//        waitThread(2);
        WebElement inputDateNaissance = waitForElement(driver, By.xpath("//*[@id=\"ctl00_cph_Body_uc_emprunteur_datenaissance\"]"), 10, 1);
        selectDate(driver, inputDateNaissance, flux.getPersonnes().get(index).getDateNaissance());

        clickEnter(actions);
        WebElement buttonPaysResidence = waitForElement(driver, By.xpath("//input[@id=\"ctl00_cph_Body_uc_emprunteur_expatrie_non\"]"), 10, 1);
        actions.moveToElement(buttonPaysResidence).click().perform();

        WebElement inputCodePostal = waitForElement(driver, By.xpath("//input[@id=\"ctl00_cph_Body_uc_emprunteur_codepostal\"]"), 10, 1);
        inputCodePostal.sendKeys(flux.getPersonnes().get(index).getCodePostal());

        WebElement inputVille = waitForElement(driver, By.xpath("//input[@id=\"ctl00_cph_Body_uc_emprunteur_ville\"]"), 10, 1);
        inputVille.sendKeys(flux.getPersonnes().get(index).getVille());

        WebElement inputAdresse = waitForElement(driver, By.xpath("//input[@id=\"ctl00_cph_Body_uc_emprunteur_voie\"]"), 10, 1);
        inputAdresse.sendKeys(flux.getPersonnes().get(index).getNomVoie());

        WebElement inputNumero = waitForElement(driver, By.xpath("//input[@id=\"ctl00_cph_Body_uc_emprunteur_numerovoie\"]"), 10, 1);
        inputNumero.sendKeys(flux.getPersonnes().get(index).getNumeroVoie());

        scrollDown(driver, 0, 450);

        WebElement inputEmail = waitForElement(driver, By.xpath("//input[@id=\"ctl00_cph_Body_uc_emprunteur_email\"]"), 10, 1);
        inputEmail.sendKeys(flux.getPersonnes().get(index).getEmail());

        WebElement inputTelephone = waitForElement(driver, By.xpath("//input[@id=\"ctl00_cph_Body_uc_emprunteur_telephone\"]"), 10, 1);
        inputTelephone.sendKeys(flux.getPersonnes().get(index).getTelephone());

        selectProfessionStatut(flux, index);
        waitThread(1);
        WebElement selectProfessionExerce = waitForElement(driver, By.xpath("//select[@id=\"ctl00_cph_Body_uc_emprunteur_situationexercice\"]"), 10, 1);
        Select selectedExerce = new Select(selectProfessionExerce);
        selectedExerce.selectByVisibleText("Non, je n'exerce aucune profession de cette liste");

        WebElement inputProfession = waitForElement(driver, By.xpath("//input[@id=\"ctl00_cph_Body_uc_emprunteur_tb_ProfessionExacte\"]"), 10, 1);
        inputProfession.sendKeys(flux.getPersonnes().get(index).getProfession());

        scrollDown(driver, 0, 450);

        WebElement buttonKilometre = waitForElement(driver, By.xpath("//input[@id=\"ctl00_cph_Body_uc_emprunteur_deplacementproinf20000\"]"), 10, 1);
        actions.moveToElement(buttonKilometre).click().perform();

        WebElement buttonPrecision = waitForElement(driver, By.xpath("//input[@id=\"ctl00_cph_Body_uc_emprunteur_risquephysique_non\"]"), 10, 1);
        actions.moveToElement(buttonPrecision).click().perform();

        WebElement buttonCharge = waitForElement(driver, By.xpath("//input[@id=\"ctl00_cph_Body_uc_emprunteur_portcharge_non\"]"), 10, 1);
        actions.moveToElement(buttonCharge).click().perform();

        WebElement buttonHauteur = waitForElement(driver, By.xpath("//input[@id=\"ctl00_cph_Body_uc_emprunteur_travailleenhauteur_non\"]"), 10, 1);
        actions.moveToElement(buttonHauteur).click().perform();

        WebElement buttonVoyage = waitForElement(driver, By.xpath("//input[@id=\"ctl00_cph_Body_uc_emprunteur_voyageetranger_non\"]"), 10, 1);
        actions.moveToElement(buttonVoyage).click().perform();

        WebElement buttonDeplacementPaysRisque = waitForElement(driver, By.xpath("//input[@id=\"ctl00_cph_Body_uc_emprunteur_VoyagePaysRisque_non\"]"), 10, 1);
        actions.moveToElement(buttonDeplacementPaysRisque).click().perform();

        scrollDown(driver, 0, 350);

        WebElement buttonSport = waitForElement(driver, By.xpath("//input[@id=\"ctl00_cph_Body_uc_emprunteur_pratiquesport_non\"]"), 10, 1);
        actions.moveToElement(buttonSport).click().perform();

        WebElement buttonFumeur = waitForElement(driver, By.xpath("//input[@id=\"ctl00_cph_Body_uc_emprunteur_fumeur_non\"]"), 10, 1);
        actions.moveToElement(buttonFumeur).click().perform();

        if (flux.getPersonnes().size() == 2){
            index = 1;
            WebElement buttonAjouterPersonne = waitForElement(driver, By.xpath("//div[@id='btncoemprunteur']"), 10, 1);
            actions.moveToElement(buttonAjouterPersonne).click().perform();
            if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur")) {
                WebElement radioCivilite = waitForElement(driver,By.xpath("//div[@id='div_Civilite']//label[@class='btn btn-primary'][normalize-space()='M.']"), 20, 1);
                actions.moveToElement(radioCivilite).click().perform();
            } else if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame")){
                WebElement radioCivilite = LibSelenium.waitForElement(driver,By.xpath("//div[@id='co-empr']//div[@id='div_Civilite']//label[@class='btn btn-primary'][normalize-space()='Mme']"), 20, 1);
                actions.moveToElement(radioCivilite).click().perform();
            }

            WebElement inputNomCo = waitForElement(driver, By.xpath("//input[@id='ctl00_cph_Body_uc_coemprunteur_nom']"), 10, 1);
            inputNomCo.sendKeys(flux.getPersonnes().get(index).getNom());

            WebElement inputPrenomCo = waitForElement(driver, By.xpath("//input[@id='ctl00_cph_Body_uc_coemprunteur_prenom']"), 10, 1);
            inputPrenomCo.sendKeys(flux.getPersonnes().get(index).getPrenom());

            WebElement inputDateNaissanceCo = waitForElement(driver, By.xpath("//input[@id='ctl00_cph_Body_uc_coemprunteur_datenaissance']"), 10, 1);
            selectDate(driver, inputDateNaissanceCo, flux.getPersonnes().get(index).getDateNaissance());
            clickEnter(actions);
            WebElement buttonPaysResidenceCo = waitForElement(driver, By.xpath("//div[@id='co-empr']//div[@id='div_PaysResidence']//div[@class='form-group']//label[1]"), 10, 1);
            actions.moveToElement(buttonPaysResidenceCo).click().perform();

//            WebElement inputCodePostalCo = waitForElement(driver, By.xpath("//input[@id='ctl00_cph_Body_uc_coemprunteur_codepostal']"), 10, 1);
//            inputCodePostalCo.sendKeys(flux.getPersonnes().get(index).getCodePostal());
//
//            WebElement inputVilleCo = waitForElement(driver, By.xpath("//input[@id='ctl00_cph_Body_uc_coemprunteur_ville']"), 10, 1);
//            inputVilleCo.sendKeys(flux.getPersonnes().get(index).getVille());
//
//            WebElement inputAdresseCo = waitForElement(driver, By.xpath("//input[@id='ctl00_cph_Body_uc_coemprunteur_voie']"), 10, 1);
//            inputAdresseCo.sendKeys(flux.getPersonnes().get(index).getAdresse());
//
//            WebElement inputNumeroCo = waitForElement(driver, By.xpath("//input[@id='ctl00_cph_Body_uc_coemprunteur_numerovoie']"), 10, 1);
//            inputNumeroCo.sendKeys("12");

            scrollDown(driver, 0, 450);

            WebElement inputEmailCo = waitForElement(driver, By.xpath("//input[@id='ctl00_cph_Body_uc_coemprunteur_email']"), 10, 1);
            inputEmailCo.sendKeys(flux.getPersonnes().get(index).getEmail());

            WebElement inputTelephoneCo = waitForElement(driver, By.xpath("//input[@id='ctl00_cph_Body_uc_coemprunteur_telephone']"), 10, 1);
            inputTelephoneCo.sendKeys(flux.getPersonnes().get(index).getTelephone());

            selectProfessionStatut(flux, index);
            waitThread(1);

            WebElement selectProfessionExerceCo = waitForElement(driver, By.xpath("//select[@id='ctl00_cph_Body_uc_coemprunteur_situationexercice']"), 10, 1);
            Select selectedExerceCo = new Select(selectProfessionExerceCo);
            selectedExerceCo.selectByVisibleText("Non, je n'exerce aucune profession de cette liste");

            WebElement inputProfessionCo = waitForElement(driver, By.xpath("//input[@id='ctl00_cph_Body_uc_coemprunteur_tb_ProfessionExacte']"), 10, 1);
            inputProfessionCo.sendKeys(flux.getPersonnes().get(index).getProfession());

            scrollDown(driver, 0, 450);

            WebElement buttonKilometreCo = waitForElement(driver, By.xpath("//*[@id=\"ctl00_cph_Body_uc_coemprunteur_deplacementproinf20000\"]"), 10, 1);
            actions.moveToElement(buttonKilometreCo).click().perform();

            WebElement buttonPrecisionCo = waitForElement(driver, By.xpath("//*[@id=\"ctl00_cph_Body_uc_coemprunteur_lb_risquephysique_non\"]"), 10, 1);
            actions.moveToElement(buttonPrecisionCo).click().perform();

            WebElement buttonChargeCo = waitForElement(driver, By.xpath("//*[@id=\"ctl00_cph_Body_uc_coemprunteur_portcharge_non\"]"), 10, 1);
            actions.moveToElement(buttonChargeCo).click().perform();

            WebElement buttonHauteurCo = waitForElement(driver, By.xpath("//*[@id=\"ctl00_cph_Body_uc_coemprunteur_travailleenhauteur_non\"]"), 10, 1);
            actions.moveToElement(buttonHauteurCo).click().perform();

            WebElement buttonVoyageCo = waitForElement(driver, By.xpath("//*[@id=\"ctl00_cph_Body_uc_coemprunteur_voyageetranger_non\"]"), 10, 1);
            actions.moveToElement(buttonVoyageCo).click().perform();

            WebElement buttonDeplacementPaysRisqueCo = waitForElement(driver, By.xpath("//*[@id=\"ctl00_cph_Body_uc_coemprunteur_VoyagePaysRisque_non\"]"), 10, 1);
            actions.moveToElement(buttonDeplacementPaysRisqueCo).click().perform();

            scrollDown(driver, 0, 350);

            WebElement buttonSportCo = waitForElement(driver, By.xpath("//*[@id=\"ctl00_cph_Body_uc_coemprunteur_pratiquesport_non\"]"), 10, 1);
            actions.moveToElement(buttonSportCo).click().perform();

            WebElement buttonFumeurCo = waitForElement(driver, By.xpath("//*[@id=\"ctl00_cph_Body_uc_coemprunteur_fumeur_non\"]"), 10, 1);
            actions.moveToElement(buttonFumeurCo).click().perform();
        }
    }

    private void remplirInformationsPret(FluxData flux, String source) {
        int index = 0;
        typeProjet(flux, index);

        WebElement buttonNouveauPret = waitForElement(driver, By.xpath("//input[@id=\"ctl00_cph_Body_nouveauPret\"]"), 10, 1);
        actions.moveToElement(buttonNouveauPret).click().perform();
        clickEnter(actions);
        WebElement selectLocalisationBien = waitForElement(driver, By.xpath("//select[@id=\"ctl00_cph_Body_localisationBien\"]"), 10, 1);
        Select selectedLBien = new Select(selectLocalisationBien);
        selectedLBien.selectByValue("METRO");

        WebElement inputDateFonds = waitForElement(driver, By.xpath("//input[@id=\"ctl00_cph_Body_DateEffet\"]"), 10, 1);
        selectDate(driver, inputDateFonds, flux.getPrets().get(0).getDateEffet());

        choixBanque(flux, index);

        nombrePrets(flux);

        typePret(flux, index);

        WebElement inputMontant = waitForElement(driver, By.xpath("//input[@id=\"ctl00_cph_Body_pret1_montantinitial\"]"), 10, 1);
        inputMontant.sendKeys(flux.getPrets().get(index).getMontantPret());

        WebElement inputTaux = waitForElement(driver, By.xpath("//input[@id=\"ctl00_cph_Body_pret1_taux\"]"), 10, 1);
        inputTaux.sendKeys(flux.getPrets().get(index).getTaux());

        WebElement buttonPretFixe = waitForElement(driver, By.xpath("//input[@id=\"ctl00_cph_Body_pret1_typetaux_fixe\"]"), 10, 1);
        actions.moveToElement(buttonPretFixe).click().perform();

        WebElement inputDuree = waitForElement(driver, By.xpath("//input[@id=\"ctl00_cph_Body_pret1_dureeinitiale\"]"), 10, 1);
        inputDuree.sendKeys(flux.getPrets().get(index).getDuree());

        if(flux.getPrets().get(index).getDureeDiffere() != null) {
            WebElement inputDiffere = waitForElement(driver, By.xpath("//input[@id=\"ctl00_cph_Body_pret1_differeinitial\"]"), 10, 1);
            inputDiffere.sendKeys(flux.getPrets().get(index).getDureeDiffere());
        }

        if (flux.getPrets().size() == 2) {
            index = 1;

            typePret(flux, index);

            WebElement inputMontant1 = waitForElement(driver, By.xpath("//input[@id=\"ctl00_cph_Body_pret2_montantinitial\"]"), 10, 1);
            inputMontant1.sendKeys(flux.getPrets().get(index).getMontantPret());

            WebElement inputTaux1 = waitForElement(driver, By.xpath("//input[@id=\"ctl00_cph_Body_pret2_taux\"]"), 10, 1);
            inputTaux1.sendKeys(flux.getPrets().get(index).getTaux());

            WebElement buttonPretFixe1 = waitForElement(driver, By.xpath("//input[@id=\"ctl00_cph_Body_pret2_typetaux_fixe\"]"), 10, 1);
            actions.moveToElement(buttonPretFixe1).click().perform();

            WebElement inputDuree1 = waitForElement(driver, By.xpath("//input[@id=\"ctl00_cph_Body_pret2_dureeinitiale\"]"), 10, 1);
            inputDuree1.sendKeys(flux.getPrets().get(index).getDuree());
            if(flux.getPrets().get(index).getDureeDiffere() != null) {
                WebElement inputDiffere1 = waitForElement(driver, By.xpath("//input[@id=\"ctl00_cph_Body_pret2_differeinitial\"]"), 10, 1);
                inputDiffere1.sendKeys(flux.getPrets().get(index).getDureeDiffere());
            }
        }
    }

    private void selectProfessionStatut(FluxData flux, int index) {
        WebElement selectProfession = null;
        if (index == 0) {
            selectProfession = waitForElement(driver, By.xpath("//select[@id=\"ctl00_cph_Body_uc_emprunteur_profession_1\"]"), 10, 1);
        } if (index == 1 ) {
            selectProfession = waitForElement(driver, By.xpath("//select[@id='ctl00_cph_Body_uc_coemprunteur_profession_1']"), 10, 1);
        }
        Select selected = new Select(selectProfession);
        if (flux.getPersonnes().get(index).getProfessionSpecifique().contains("Salarié cadre"))
            selected.selectByVisibleText("Salarié Cadre/ Assimilé-cadre / Ingénieur (hors personnel navigant)");
        if (flux.getPersonnes().get(index).getProfessionSpecifique().contains("Commerçant"))
            selected.selectByVisibleText("Commerçant");
        if (flux.getPersonnes().get(index).getProfessionSpecifique().contains("Artisan"))
            selected.selectByVisibleText("Artisan");
        if (flux.getPersonnes().get(index).getProfessionSpecifique().contains("Agric"))
            selected.selectByVisibleText("Professions agricoles");
        if (flux.getPersonnes().get(index).getProfessionSpecifique().contains("Ouvrier"))
            selected.selectByVisibleText("Ouvriers / Professions du Transport");
        if (flux.getPersonnes().get(index).getProfessionSpecifique().contains("Salarié non cadre"))
            selected.selectByVisibleText("Salarié non cadre (hors employé de bureau & personnel navigant)");
        if (flux.getPersonnes().get(index).getProfessionSpecifique().contains("Fonctionnaire"))
            selected.selectByVisibleText("Fonctionnaire");
        if (flux.getPersonnes().get(index).getProfessionSpecifique().contains("Profession libérale médicale"))
            selected.selectByVisibleText("Profession libérale médicale ou paramédicale / Médecin (ou Interne) généraliste/ spécialiste");
        if (flux.getPersonnes().get(index).getProfessionSpecifique().contains("Profession libérale paramédicale"))
            selected.selectByVisibleText("Profession intermédiaire de la santé et du travail social");
        if (flux.getPersonnes().get(index).getProfessionSpecifique().contains("Profession libérale"))
            selected.selectByVisibleText("Profession libérale (hors médical/ paramédical)");
    }

    private void typePret(FluxData flux, int index) {
        if (index == 0) {
            WebElement selectTypePret = waitForElement(driver, By.xpath("//select[@id=\"ctl00_cph_Body_pret1_typepret\"]"), 10, 1);
            Select selectedTypePret = new Select(selectTypePret);
            selectedTypePret.selectByVisibleText(flux.getPrets().get(index).getType());
        } else if (index == 1) {
            WebElement selectTypePret = waitForElement(driver, By.xpath("//select[@id=\"ctl00_cph_Body_pret2_typepret\"]"), 10, 1);
            Select selectedTypePret = new Select(selectTypePret);
            selectedTypePret.selectByVisibleText(flux.getPrets().get(index).getType());
        }
    }

    private void nombrePrets(FluxData flux) {
        if(flux.getPrets().size() == 1) {
            WebElement buttonNombrePret = waitForElement(driver, By.xpath("//input[@id=\"ctl00_cph_Body_nombrepret1\"]"), 10, 1);
            actions.moveToElement(buttonNombrePret).click().perform();
        } if(flux.getPrets().size() == 2) {
            WebElement buttonNbPrets = waitForElement(driver, By.xpath("//input[@id=\"ctl00_cph_Body_nombrepret2\"]"), 10, 1);
            actions.moveToElement(buttonNbPrets).click().perform();
        } if(flux.getPrets().size() == 3) {
            WebElement buttonNbPrets = waitForElement(driver, By.xpath("//input[@id=\"ctl00_cph_Body_nombrepret3\"]"), 10, 1);
            actions.moveToElement(buttonNbPrets).click().perform();
        } if(flux.getPrets().size() == 4) {
            WebElement buttonNbPrets = waitForElement(driver, By.xpath("//input[@id=\"ctl00_cph_Body_nombrepret4\"]"), 10, 1);
            actions.moveToElement(buttonNbPrets).click().perform();
        } if(flux.getPrets().size() == 5) {
            WebElement buttonNbPrets = waitForElement(driver, By.xpath("//input[@id=\"ctl00_cph_Body_nombrepret5\"]"), 10, 1);
            actions.moveToElement(buttonNbPrets).click().perform();
        }
    }

    private void typeProjet(FluxData flux, int index) {
        WebElement selectTypeProjet = waitForElement(driver, By.xpath("//select[@id=\"ctl00_cph_Body_typeprojet\"]"), 10, 1);
        Select selectedTProjet = new Select(selectTypeProjet);
        if (flux.getPrets().get(0).getObjet().contains("Résidence principale"))
            selectedTProjet.selectByVisibleText(flux.getPrets().get(0).getObjet());
        if (flux.getPrets().get(0).getObjet().contains("Résidence secondaire"))
            selectedTProjet.selectByVisibleText(flux.getPrets().get(0).getObjet());
        if (flux.getPrets().get(0).getObjet().contains("Investissement locatif"))
            selectedTProjet.selectByVisibleText(flux.getPrets().get(0).getObjet());
        if (flux.getPrets().get(0).getObjet().contains("Prêt professionnel"))
            selectedTProjet.selectByVisibleText(flux.getPrets().get(0).getObjet());
        if (flux.getPrets().get(0).getObjet().contains("Autre"))
            selectedTProjet.selectByVisibleText(flux.getPrets().get(0).getObjet());
        if (flux.getPrets().get(0).getObjet().contains("Crédit"))
            selectedTProjet.selectByVisibleText(flux.getPrets().get(0).getObjet());
        if (flux.getPrets().get(0).getObjet().contains("Investissement locatif"))
            selectedTProjet.selectByVisibleText(flux.getPrets().get(0).getObjet());
    }

    private void choixBanque(FluxData flux, int index) {
        WebElement selectBanque = waitForElement(driver, By.xpath("//select[@id=\"ctl00_cph_Body_banque\"]"), 10, 1);
        Select selectedBanque = new Select(selectBanque);
        List<WebElement> options = selectedBanque.getOptions();
        for (WebElement webElement: options) {
            if (flux.getPrets().get(index).getBanque().equalsIgnoreCase(webElement.getText())) {
                selectedBanque.selectByVisibleText("BNP Paribas");
                break;
            } if (flux.getPrets().get(index).getBanque().equalsIgnoreCase(webElement.getText())) {
                selectedBanque.selectByVisibleText("Axa banque");
                break;
            } if (flux.getPrets().get(index).getBanque().equalsIgnoreCase(webElement.getText())) {
                selectedBanque.selectByVisibleText("BOURSOBANK");
                break;
            } if (flux.getPrets().get(index).getBanque().equalsIgnoreCase(webElement.getText())) {
                selectedBanque.selectByVisibleText("BANQUE PALATINE");
                break;
            } if (flux.getPrets().get(index).getBanque().equalsIgnoreCase(webElement.getText())) {
                selectedBanque.selectByVisibleText("Banque populaire Auvergne Rhône Alpes");
                break;
            } if (flux.getPrets().get(index).getBanque().equalsIgnoreCase(webElement.getText())) {
                selectedBanque.selectByVisibleText("Caisse d'épargne Auvergne et du Limousin");
                break;
            } if (flux.getPrets().get(index).getBanque().equalsIgnoreCase(webElement.getText())) {
                selectedBanque.selectByVisibleText("CIC");
                break;
            } if (flux.getPrets().get(index).getBanque().equalsIgnoreCase(webElement.getText())) {
                selectedBanque.selectByVisibleText("Axa banque");
                break;
            } if (flux.getPrets().get(index).getBanque().equalsIgnoreCase(webElement.getText())) {
                selectedBanque.selectByVisibleText("Société Générale");
                break;
            } if (flux.getPrets().get(index).getBanque().equalsIgnoreCase(webElement.getText())) {
                selectedBanque.selectByVisibleText("La Banque Postale");
                break;
            } if (flux.getPrets().get(index).getBanque().equalsIgnoreCase(webElement.getText())) {
                selectedBanque.selectByVisibleText("Crédit mutuel");
                break;
            } if (flux.getPrets().get(index).getBanque().equalsIgnoreCase(webElement.getText())) {
                selectedBanque.selectByVisibleText("Crédit agricole");
                break;
            } if (flux.getPrets().get(index).getBanque().equalsIgnoreCase(webElement.getText())) {
                selectedBanque.selectByVisibleText("Caisse d\\'épargne");
                break;
            } if (flux.getPrets().get(index).getBanque().equalsIgnoreCase(webElement.getText())) {
                selectedBanque.selectByVisibleText("Banque populaire");
                break;
            } if (flux.getPrets().get(index).getBanque().equalsIgnoreCase(webElement.getText())) {
                selectedBanque.selectByVisibleText("Banque inconnue");
                break;
            }
        }
    }

    @Override
    protected void pageName(String namePage) {}
}
