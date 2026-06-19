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
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class DigitalInsurePretService extends PageElementInteraction implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(DigitalInsurePretService.class);
    public static WebDriver driver;
    private Actions actions;
    private final String source = "DigitalInsurePret";
    private JavascriptExecutor js = null;
    private final TypeAssuranceService typeAssuranceService;

    public DigitalInsurePretService(TypeAssuranceService typeAssuranceService){
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        log.info("Debut de traitement -- DigitalInsure");
        Tarif tarifDigitalInsurePret = TarifUtils.createDefaultTarif(c, typeAssuranceService, 1L);
        driver = new EdgeDriver();
        driver.manage().window().maximize();
        actions = new Actions(driver);
        js = (JavascriptExecutor) driver;
        driver.get(c.getUrlFournisseur());
        waitThread(1);
        try {
            inputByName(driver, "username", c.getUsername());
            inputByName(driver, "password", c.getPassword());
            clicButtonByXpath(driver, "//button[text()='Se connecter']", actions);
            waitThread(3);

            remplirInformationsPersonne(flux, source);

            scrollDown(driver, 0, 300);

            waitThread(2);
            remplirInformationsPret(flux, source);

            //Valider
            clicButtonByXpath(driver, "//div[@class=\"wrap-submit-form\"]//button[text()='Valider le formulaire']", actions);

            waitThread(15);
            WebElement tableElement = waitForElement(driver, By.cssSelector(".table.table-empty-corner"), 25, 3);

            List<WebElement> rows = tableElement.findElements(By.tagName("tr"));
            WebElement firstCell = waitForElement(driver, By.xpath("//td[3]//div"), 25, 3);
            String cout = firstCell.getText();
            log.info("Cout {}", cout);
            tarifDigitalInsurePret.setMontant(cout);
            String screenshotBytes = captureScreenshot(driver, tarifDigitalInsurePret.getNom(), false, tarifDigitalInsurePret);
            if (screenshotBytes != null) {
//                tarif.setCaptureImg(screenshotBytes);
            }
            tarifDigitalInsurePret.setExecution(true);
            log.info("Fin de traitement -- DigitalInsure");
        } catch (Exception e) {
            log.error("An error occurred: ", e);
            tarifDigitalInsurePret.setErreur(e.getMessage());
            String screenshotBytesErreur = captureScreenshot(driver, tarifDigitalInsurePret.getNom(), true, tarifDigitalInsurePret);
            tarifDigitalInsurePret.setCaptureImgErreur(screenshotBytesErreur);
            tarifDigitalInsurePret.setEtape("");
        } finally {
            driver.quit();
        }
      return tarifDigitalInsurePret;
    }

    private void remplirInformationsPersonne(FluxData flux, String source) {
        int index = 0;
        //Civilite
        if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur")) {
            WebElement selectCivilite = waitForElement(driver, By.xpath(".//select[contains(@id, 'insured_insdGender')]"), 25, 3);
            selectCivilite.findElement(By.xpath(".//option[text()='Monsieur']")).click();
        } else if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame")) {
            WebElement selectCivilite = waitForElement(driver, By.xpath(".//select[contains(@id, 'insured_insdGender')]"), 25, 3);
            selectCivilite.findElement(By.xpath(".//option[text()='Madame']")).click();
        }
        WebElement selectCivilite = waitForElement(driver, By.xpath(".//select[contains(@id, 'insured_insdGender')]"), 25, 3);
        selectCivilite.findElement(By.xpath(".//option[text()='Monsieur']")).click();
        //prenom
        inputByLocator(driver, By.xpath(".//input[contains(@id, 'insured_lastname')]"), flux.getPersonnes().get(index).getPrenom());
        //nom
        inputByLocator(driver, By.xpath(".//input[contains(@id, 'insured_firstname')]"), flux.getPersonnes().get(index).getNom());
        //Date naissance
        inputByLocator(driver, By.xpath(".//input[contains(@id, 'insured_insdDateOfBirthDt')]"), flux.getPersonnes().get(index).getDateNaissance());
        //Ville naissance
        inputByLocator(driver, By.xpath(".//input[contains(@id, 'insured_cityOfBirth')]"), flux.getPersonnes().get(index).getVille());
        //Email
        inputByLocator(driver, By.xpath(".//input[contains(@id, 'insured_email')]"), flux.getPersonnes().get(index).getEmail());
        //Telephone
        inputByLocator(driver, By.xpath(".//input[contains(@id, 'insured_mobilePhoneNumber')]"), flux.getPersonnes().get(index).getTelephone());
        //adresse
        inputByLocator(driver, By.xpath(".//input[contains(@id, 'addressline1')]"), flux.getPersonnes().get(index).getNumeroVoie() + " " + flux.getPersonnes().get(index).getNomVoie());
        inputByLocator(driver, By.xpath(".//input[contains(@id, 'addresscity')]"), flux.getPersonnes().get(index).getVille());
        inputByLocator(driver, By.xpath(".//input[contains(@id, 'addresszipcode')]"), flux.getPersonnes().get(index).getCodePostal());
        inputByLocator(driver, By.xpath(".//input[contains(@id, 'addresscountry')]"), flux.getPersonnes().get(index).getPays());
        waitThread(2);
        clickEnter(actions);
        scrollDown(driver, 0, 100);
        if (flux.getPersonnes().get(index).getNationalite().equalsIgnoreCase("Française")) {
            inputByLocator(driver, By.xpath("//input[contains(@id, 'label.nationality')]"), "France");
        }
        waitThread(1);
        clickEnter(actions);
        // Habitudes de vie
        //Profession
        scrollDown(driver, 0, 500);
        choixCategorieProfession(flux, index);
        //Profession exacte
        inputByLocator(driver, By.xpath(".//input[contains(@id, 'insured_exactJob')]"), flux.getPersonnes().get(index).getProfession());
        //Regime social
        waitThread(3);
        choixRegime(flux, index);
        //Déplacements professionnels
        choixDeplacementProfessionnelle(flux, index);
        //Travail hauteur
        choixTravailHauteur(flux, index);

        if (flux.getInfoAssureComplets().get(index).getFumeur()) {
            WebElement radioFumeur = driver.findElement(By.xpath(".//input[contains(@id, 'insured_insdSmoker_0')]"));
            actions.moveToElement(radioFumeur).click().perform();
        }
        if (flux.getInfoAssureComplets().get(index).getFumeurSansNico()) {
            WebElement radioFumeurSansNico = driver.findElement(By.xpath(".//input[contains(@id, 'insured_insdESmoker_0')]"));
            actions.moveToElement(radioFumeurSansNico).click().perform();
        }
        if (flux.getInfoAssureComplets().get(index).getFumeurElecNico()) {
            WebElement radioFumeurElecNico = driver.findElement(By.xpath(".//input[contains(@id, 'insured_esmokerNoNicotine_0')]"));
            actions.moveToElement(radioFumeurElecNico).click().perform();
        }
        if (flux.getInfoAssureComplets().get(index).getTravailManuel()) {
            WebElement radioTravailManuel = driver.findElement(By.xpath(".//input[contains(@id, 'insured_insdManualWork_0')]"));
            actions.moveToElement(radioTravailManuel).click().perform();
        }
        if (flux.getInfoAssureComplets().get(index).getProduitDanger()) {
            WebElement radioProduitDanger = driver.findElement(By.xpath(".//input[contains(@id, 'insured_dangerousProduct_0')]"));
            actions.moveToElement(radioProduitDanger).click().perform();
        }
        if (flux.getInfoAssureComplets().get(index).getTravailManuelManuLourde()) {
            WebElement radioTravailManuelManuLourde = driver.findElement(By.xpath(".//input[contains(@id, 'insured_insdManualWorkRisk_0')]"));
            actions.moveToElement(radioTravailManuelManuLourde).click().perform();
        }
        if (flux.getInfoAssureComplets().get(index).getMetierExpose()) {
            WebElement radioMetierExpose = driver.findElement(By.xpath(".//input[contains(@id, 'insured_workRisk_0')]"));
            actions.moveToElement(radioMetierExpose).click().perform();
        }
        if (flux.getInfoAssureComplets().get(index).getSportRisque()) {
            WebElement radioSportRisque = driver.findElement(By.xpath(".//input[contains(@id, 'insured_insdSportsActivities_0')]"));
            actions.moveToElement(radioSportRisque).click().perform();
        }

        if (flux.getPersonnes().size() == 2) {
            index = 1;
            WebElement buttonAjouterAssure = driver.findElement(By.xpath("//button[@class=\"btn\" and text()=\" Ajouter un assuré \"]"));
            actions.moveToElement(buttonAjouterAssure).click().perform();
            if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur")) {
                WebElement selectCivilite1 = waitForElement(driver, By.xpath("//select[contains(@id, 'insured_insdGender_1')]"), 25, 3);
                selectCivilite1.findElement(By.xpath(".//option[text()='Monsieur']")).click();
            } else if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame")) {
                WebElement selectCivilite1 = waitForElement(driver, By.xpath("//select[contains(@id, 'insured_insdGender_1')]"), 25, 3);
                selectCivilite1.findElement(By.xpath(".//option[text()='Madame']")).click();
            }
            //Nom
            inputByLocator(driver, By.xpath("//input[contains(@id, 'insured_lastname_1')]"), flux.getPersonnes().get(index).getPrenom());
            //Prenom
            inputByLocator(driver, By.xpath("//input[contains(@id, 'insured_firstname_1')]"), flux.getPersonnes().get(index).getNom());
            //Date naissance
            inputByLocator(driver, By.xpath("//input[contains(@id, 'insured_insdDateOfBirthDt_1')]"), flux.getPersonnes().get(index).getDateNaissance());
            //Ville naissance
            inputByLocator(driver, By.xpath("//input[contains(@id, 'insured_cityOfBirth_1')]"), flux.getPersonnes().get(index).getVille());
            //Email
            inputByLocator(driver, By.xpath("//input[contains(@id, 'insured_email_1')]"), flux.getPersonnes().get(index).getEmail());
            //Telephone
            inputByLocator(driver, By.xpath("//input[contains(@id, 'insured_mobilePhoneNumber_1')]"), flux.getPersonnes().get(index).getTelephone());
            //adresse
            inputByLocator(driver, By.xpath("//input[contains(@id, 'insured_address1') and contains(@id, 'addressline1')]"), flux.getPersonnes().get(index).getNumeroVoie() + " " + flux.getPersonnes().get(index).getNomVoie());
            inputByLocator(driver, By.xpath("//input[contains(@id, 'insured_address1') and contains(@id, 'addresscity')]"), flux.getPersonnes().get(index).getVille());
            inputByLocator(driver, By.xpath("//input[contains(@id, 'insured_address1') and contains(@id, 'addresszipcode')]"), flux.getPersonnes().get(index).getCodePostal());
            inputByLocator(driver, By.xpath("//input[contains(@id, 'insured_address1') and contains(@id, 'addresscountry')]"), flux.getPersonnes().get(index).getPays());
            waitThread(2);
            clickEnter(actions);
            if (flux.getPersonnes().get(index).getNationalite().equalsIgnoreCase("Française")) {
                inputByLocator(driver, By.xpath("/html[1]/body[1]/form[1]/main[1]/form[1]/div[2]/form[1]/form[1]/form[1]/form[1]/form[1]/div[1]/div[3]/div[1]/div[2]/div[2]/form[1]/div[2]/div[4]/div[1]/div[1]/div[1]/div[1]/div[1]/input[1]"), "France");
            }
            waitThread(1);
            clickEnter(actions);
            // Habitudes de vie
            //Profession
            scrollDown(driver, 0, 500);
            choixCategorieProfession(flux, index);
            //Profession exacte
            inputByLocator(driver, By.xpath("//input[contains(@id, 'insured_exactJob_1')]"), flux.getPersonnes().get(index).getProfession());
            //Regime social
            waitThread(2);
            choixRegime(flux, index);
            //Déplacements professionnels
            choixDeplacementProfessionnelle(flux, index);
            //Travail hauteur
            choixTravailHauteur(flux, index);

            if (flux.getInfoAssureComplets().get(index).getFumeur()) {
                WebElement radioFumeur = driver.findElement(By.xpath(".//input[contains(@id, 'insured_insdSmoker_1')]"));
                actions.moveToElement(radioFumeur).click().perform();
            }
            if (flux.getInfoAssureComplets().get(index).getFumeurSansNico()) {
                WebElement radioFumeurSansNico = driver.findElement(By.xpath(".//input[contains(@id, 'insured_insdESmoker_1')]"));
                actions.moveToElement(radioFumeurSansNico).click().perform();
            }
            if (flux.getInfoAssureComplets().get(index).getFumeurElecNico()) {
                WebElement radioFumeurElecNico = driver.findElement(By.xpath(".//input[contains(@id, 'insured_esmokerNoNicotine_1')]"));
                actions.moveToElement(radioFumeurElecNico).click().perform();
            }
            if (flux.getInfoAssureComplets().get(index).getTravailManuel()) {
                WebElement radioTravailManuel = driver.findElement(By.xpath(".//input[contains(@id, 'insured_insdManualWork_1')]"));
                actions.moveToElement(radioTravailManuel).click().perform();
            }
            if (flux.getInfoAssureComplets().get(index).getProduitDanger()) {
                WebElement radioProduitDanger = driver.findElement(By.xpath(".//input[contains(@id, 'insured_dangerousProduct_1')]"));
                actions.moveToElement(radioProduitDanger).click().perform();
            }
            if (flux.getInfoAssureComplets().get(index).getTravailManuelManuLourde()) {
                WebElement radioTravailManuelManuLourde = driver.findElement(By.xpath(".//input[contains(@id, 'insured_insdManualWorkRisk_1')]"));
                actions.moveToElement(radioTravailManuelManuLourde).click().perform();
            }
            if (flux.getInfoAssureComplets().get(index).getMetierExpose()) {
                WebElement radioMetierExpose = driver.findElement(By.xpath(".//input[contains(@id, 'insured_workRisk_1')]"));
                actions.moveToElement(radioMetierExpose).click().perform();
            }
            if (flux.getInfoAssureComplets().get(index).getSportRisque()) {
                WebElement radioSportRisque = driver.findElement(By.xpath(".//input[contains(@id, 'insured_insdSportsActivities_1')]"));
                actions.moveToElement(radioSportRisque).click().perform();
            }
        }
    }

    private void remplirInformationsPret(FluxData flux, String source) {
        int index = 0;
        //Type de pret
        choixTypePret(flux, index);
        //Montant
        inputByLocator(driver, By.xpath(".//input[contains(@id, 'loan_loanAmount_0')]"), flux.getPrets().get(index).getMontantPret());
        //Duree
        inputByLocator(driver, By.xpath(".//input[contains(@id, 'loan_loanDuration_0')]"), flux.getPrets().get(index).getDuree());
        //Taux
        inputByLocator(driver, By.xpath(".//input[contains(@id, 'loan_loanRate_0')]"), flux.getPrets().get(index).getTaux());
        //Date effet
        inputByLocator(driver, By.xpath(".//input[contains(@id, 'loan_effectiveDate_0')]"), flux.getPrets().get(index).getDateEffet());
        //Type de différé
        choixTypeDiffere(flux, index);

        if (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Partiel")) {
            WebElement inputDureeDifferePartiel = waitForElement(driver, By.xpath(".//input[contains(@id, 'loan_loanDeferredDuration2')]"), 10, 1);
            inputDureeDifferePartiel.sendKeys(flux.getPrets().get(index).getDureeDiffere());
        }
        if (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Total")) {
            WebElement inputDureeDiffereTotal = waitForElement(driver, By.xpath(".//input[contains(@id, 'loan_loanDeferredDuration2')]"), 10, 1);
            inputDureeDiffereTotal.sendKeys(flux.getPrets().get(index).getDureeDiffere());
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
        scrollDown(driver, 0, 800);
        waitThread(1);
        if (flux.getPrets().size() == 2) {
            waitThread(3);
            WebElement buttonAddPret = waitForElement(driver, By.xpath("//button[contains(@class, 'btn') and contains(text(), 'Ajouter un prêt')]"), 30, 1);
            buttonAddPret.click();
            index = 1;
            choixTypePret(flux, index);
            //Montant
            inputByLocator(driver, By.xpath(".//input[contains(@id, 'loan_loanAmount_1')]"), flux.getPrets().get(index).getMontantPret());
            //Duree
            inputByLocator(driver, By.xpath(".//input[contains(@id, 'loan_loanDuration_1')]"), flux.getPrets().get(index).getDuree());
            //Taux
            inputByLocator(driver, By.xpath(".//input[contains(@id, 'loan_loanRate_1')]"), flux.getPrets().get(index).getTaux());
            //Date effet
            inputByLocator(driver, By.xpath(".//input[contains(@id, 'loan_effectiveDate_1')]"), flux.getPrets().get(index).getDateEffet());
            //Type de différé
            choixTypeDiffere(flux, index);

            if (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Partiel")) {
                WebElement inputDureeDifferePartiel = waitForElement(driver, By.xpath(".//input[contains(@id, 'loan_loanDeferredDuration2_1')]"), 10, 1);
                inputDureeDifferePartiel.sendKeys(flux.getPrets().get(index).getDureeDiffere());
            }
            if (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Total")) {
                WebElement inputDureeDiffereTotal = waitForElement(driver, By.xpath(".//input[contains(@id, 'loan_loanDeferredDuration2_1')]"), 10, 1);
                inputDureeDiffereTotal.sendKeys(flux.getPrets().get(index).getDureeDiffere());
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
        WebElement dropdownDepPro = null;
        if (index == 0){
            dropdownDepPro = waitForElement1(driver, By.xpath(".//select[contains(@id, 'insured_insdAnnualMilage')]"), 10, 1);
        }
        if (index == 1) {
            dropdownDepPro = waitForElement1(driver, By.xpath("//select[contains(@id, 'insured_insdAnnualMilage_1')]"), 10, 1);
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
            waitThread(1);
            dropdownTravailHauteur = waitForElement1(driver, By.xpath("//select[contains(@id, 'insured_insdWorkAtHeight')]"), 10, 1);
        }
        if (index == 1) {
            waitThread(1);
            dropdownTravailHauteur = waitForElement1(driver, By.xpath("//select[contains(@id, 'insured_insdWorkAtHeight_1')]"), 10, 1);
        }
        if (dropdownTravailHauteur != null) {
            if (flux.getInfoAssureComplets().get(index).getTravailHauteur()) {
                Select selectTravailHauteur = new Select(dropdownTravailHauteur);
                if (flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("0 à 3m"))
                    selectTravailHauteur.selectByVisibleText("0 - 3");
                else if (flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("3 à 10m"))
                    selectTravailHauteur.selectByVisibleText("3 - 10");
                else if (flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("10 à 12m"))
                    selectTravailHauteur.selectByVisibleText("10 - 12");
                else if (flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("12 à 15m"))
                    selectTravailHauteur.selectByVisibleText("12 - 15");
                else if (flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("15 à 20m"))
                    selectTravailHauteur.selectByVisibleText("15 - 20");
                else if (flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("Plus de 20m"))
                    selectTravailHauteur.selectByVisibleText("Plus de 20m");
            }
        }
    }

    private void choixRegime(FluxData flux, int index) {
        if (flux.getPersonnes().get(index).getRegime() != null) {
            WebElement dropdownRegimeSocial = null;
            waitThread(2);
            if (index == 0) {
                waitThread(1);
                dropdownRegimeSocial = waitForElement(driver, By.xpath(".//select[contains(@id, 'insured_socialRegime')]"), 10, 1);
            }
            if (index == 1) {
                waitThread(1);
                dropdownRegimeSocial = waitForElement(driver, By.xpath("//select[contains(@id, 'insured_socialRegime_1')]"), 10, 1);
            }
            if (dropdownRegimeSocial != null) {
                Select selectRegimeSocial = new Select(dropdownRegimeSocial);
                waitThread(2);
                selectRegimeSocial.selectByVisibleText(flux.getPersonnes().get(index).getRegime());
            }
        }
    }

    private void choixCategorieProfession(FluxData flux, int index) {
        WebElement dropdownProfession = null;
        if (index == 0){
            waitThread(1);
            dropdownProfession = waitForElement1(driver, By.xpath(".//select[contains(@id, 'insured_professionalCategory')]"), 10, 1);
        }
        if (index == 1) {
            waitThread(1);
            dropdownProfession = waitForElement1(driver, By.xpath("//select[contains(@id, 'insured_professionalCategory_1')]"), 10, 1);
        }
        if (dropdownProfession != null) {
            Select selectProfession = new Select(dropdownProfession);
            waitThread(2);
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
            waitThread(1);
            selectTypePret.selectByVisibleText(flux.getPrets().get(index).getType());
        }
    }

    private void choixObjetFinancement(FluxData flux, int index) {
        WebElement dropdownObjetFinancement = null;
        if (index == 0){
            dropdownObjetFinancement = waitForElement1(driver, By.xpath(".//select[contains(@id, 'loan_loanPurposeOfFinancing_0')]"), 30, 1);
        }
        if (index == 1) {
            dropdownObjetFinancement = waitForElement1(driver, By.xpath(".//select[contains(@id, 'loan_loanPurposeOfFinancing_1')]"), 30, 1);
        }
        if (dropdownObjetFinancement != null) {
            Select selectObjetFinancement = new Select(dropdownObjetFinancement);
            waitThread(1);
            selectObjetFinancement.selectByVisibleText(flux.getPrets().get(index).getObjet());
        }
    }

    private void choixBanque(FluxData flux, int index) {
        WebElement dropdownBanque = null;
        if (index == 0){
            dropdownBanque = waitForElement1(driver, By.xpath(".//select[contains(@id, 'loan_loan.bankInformation_bankCode_0')]"), 30, 1);
        }
        if (index == 1) {
            dropdownBanque = waitForElement1(driver, By.xpath(".//select[contains(@id, 'loan_loan.bankInformation_bankCode_1')]"), 30, 1);
        }
        if (dropdownBanque != null) {
            Select selectBanque = new Select(dropdownBanque);
            List<WebElement> options = selectBanque.getOptions();
            for (WebElement option : options) {
                if ((option.getText().contains("AXA") || option.getText().equalsIgnoreCase("AXA")) && flux.getPrets().get(index).getBanque().equalsIgnoreCase("Axa banque")) {
                    js.executeScript("arguments[0].scrollIntoView(true);", option);
                    option.click();
                    scrollDown(driver, 0,-450);
                    break;
                }
                if ((option.getText().contains("BNP Paribas") || option.getText().equalsIgnoreCase("BNP Paribas")) && flux.getPrets().get(index).getBanque().equalsIgnoreCase("BNP Paribas")) {
                    js.executeScript("arguments[0].scrollIntoView(true);", option);
                    option.click();
                    scrollDown(driver, 0,-450);
                    break;
                }
                if ((option.getText().contains("Caisse d\\'épargne") || option.getText().equalsIgnoreCase("Caisse d\\'épargne")) && flux.getPrets().get(index).getBanque().equalsIgnoreCase("Caisse d\\'épargne")) {
                    js.executeScript("arguments[0].scrollIntoView(true);", option);
                    option.click();
                    scrollDown(driver, 0,-450);
                    break;
                }
                if ((option.getText().contains("Caisse agricole") || option.getText().equalsIgnoreCase("Caisse agricole")) && flux.getPrets().get(index).getBanque().equalsIgnoreCase("Caisse agricole")) {
                    js.executeScript("arguments[0].scrollIntoView(true);", option);
                    option.click();
                    scrollDown(driver, 0,-450);
                    break;
                }
                if ((option.getText().contains("Crédit Mutuel") || option.getText().equalsIgnoreCase("Crédit Mutuel")) && flux.getPrets().get(index).getBanque().equalsIgnoreCase("Crédit Mutuel")) {
                    js.executeScript("arguments[0].scrollIntoView(true);", option);
                    option.click();
                    scrollDown(driver, 0,-450);
                    break;
                }
                if ((option.getText().contains("LCL") || option.getText().equalsIgnoreCase("LCL")) && flux.getPrets().get(index).getBanque().equalsIgnoreCase("LCL")) {
                    js.executeScript("arguments[0].scrollIntoView(true);", option);
                    option.click();
                    scrollDown(driver, 0,-450);
                    break;
                }
                if ((option.getText().contains("CIC") || option.getText().equalsIgnoreCase("CIC")) && flux.getPrets().get(index).getBanque().equalsIgnoreCase("CIC")) {
                    js.executeScript("arguments[0].scrollIntoView(true);", option);
                    option.click();
                    scrollDown(driver, 0,-450);
                    break;
                }
                if ((option.getText().contains("La Banque Postale") || option.getText().equalsIgnoreCase("La Banque Postale")) && flux.getPrets().get(index).getBanque().equalsIgnoreCase("La Banque Postale")) {
                    js.executeScript("arguments[0].scrollIntoView(true);", option);
                    option.click();
                    scrollDown(driver, 0,-450);
                    break;
                }
                if ((option.getText().contains("Société Générale") || option.getText().equalsIgnoreCase("Société Générale")) && flux.getPrets().get(index).getBanque().equalsIgnoreCase("Société Générale")) {
                    js.executeScript("arguments[0].scrollIntoView(true);", option);
                    option.click();
                    scrollDown(driver, 0,-450);
                    break;
                }
            }
        }
    }

    private void choixTypeDiffere(FluxData flux, int index) {
        WebElement dropdownTypeDiffere = null;
        if (index == 0){
            waitThread(1);
            dropdownTypeDiffere = driver.findElement(By.xpath(".//select[contains(@id, 'loan_loanDeferredType2_0')]"));
        }
        if (index == 1) {
            waitThread(1);
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
            WebElement couvertureIPP = waitForElement1(driver, By.xpath("//td[@cov-code='IPP']"), 10, 1);
            couvertureIPP.click();
        }
//        WebElement couvertureOption = waitForElement(driver, By.xpath("//input[contains(@name,'DOS_PSY')]"), 10, 1);
//        couvertureOption.click();
    }

    @Override
    protected void pageName(String namePage) {}
}
