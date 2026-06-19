package com.mtc.mutuaConseil.services.servicesImpl.assuMutuelIndiv;

import com.mtc.mutuaConseil.models.Compte;
import com.mtc.mutuaConseil.models.FluxData;
import com.mtc.mutuaConseil.models.Tarif;
import com.mtc.mutuaConseil.services.LaunchedService;
import com.mtc.mutuaConseil.services.servicesImpl.TypeAssuranceService;
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
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ECAMutuelIndivService extends PageElementInteraction implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(ECAMutuelIndivService.class);
    public static WebDriver driver;
    private final String source = "ECAMutuelIndivService";
    private static Actions actions;
    private final TypeAssuranceService typeAssuranceService;

    public ECAMutuelIndivService(TypeAssuranceService typeAssuranceService){
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        Tarif tarifECAMutuelIndivService = TarifUtils.createDefaultTarif(c, typeAssuranceService, 2L);;
        tarifECAMutuelIndivService.setNom(c.getNomFournisseur());
        driver = new EdgeDriver();
        driver.manage().window().maximize();
        actions = new Actions(driver);
        try {
            connexion(c);
            choixComplementaire();
            remplirComplementaireSante(flux);
            scrollDown(driver, 0, 350);
            suivant();
            waitThread(5);
            WebElement sectionPrix = waitForElement(driver, By.xpath("//*[@id=\"tarif_sante_OPTION_BUDGET_150_B\"]"), 10, 1);
            String cout = sectionPrix.getText();
            log.info("cout {}", cout);
            tarifECAMutuelIndivService.setMontant(cout);
            String screenshotBytes = captureScreenshot(driver, tarifECAMutuelIndivService.getNom(), false, tarifECAMutuelIndivService);
            if (screenshotBytes != null) {
                tarifECAMutuelIndivService.setCaptureImg(screenshotBytes);
            }
            tarifECAMutuelIndivService.setExecution(true);
        } catch (Exception e) {
            log.error("An error occurred", e);
            tarifECAMutuelIndivService.setErreur(e.getMessage());
            String screenshotBytesErreur = captureScreenshot(driver, tarifECAMutuelIndivService.getNom(), true, tarifECAMutuelIndivService);
            tarifECAMutuelIndivService.setCaptureImgErreur(screenshotBytesErreur);
            tarifECAMutuelIndivService.setEtape("");
        } finally {
            driver.quit();
        }
        return tarifECAMutuelIndivService;
    }

    public void connexion(Compte c) {
        driver.get(c.getUrlFournisseur());
        WebElement inputLogin = waitForElement(driver, By.id("login-name"), 10, 2);
        inputLogin.sendKeys(c.getUsername());
        WebElement inputPassword = waitForElement(driver, By.id("login-password"), 10, 2);
        inputPassword.sendKeys(c.getPassword());
        waitThread(1);
        WebElement buttonLogin = waitForElement1(driver, By.xpath("//*[@id=\"main-container\"]/div[1]/div/div[1]/div/form/div[2]/button"), 10, 1);
        buttonLogin.click();
        waitThread(1);
    }

    private void choixComplementaire() {
        waitThread(2);
        WebElement linkParticulier = waitForElement(driver, By.xpath("//*[@id=\"main-container\"]/div/div/div/div[2]/div[2]/div[2]/a[1]"), 10, 1);
        linkParticulier.click();
        WebElement linkComplementaire = waitForElement(driver, By.xpath("//*[@id=\"panelsStayOpen-collapseOne\"]/div/div/div[1]/a"), 10, 1);
        linkComplementaire.click();
        scrollDown(driver, 0, 350);
        waitThread(2);
        WebElement linkFaireDevis = waitForElement(driver, By.xpath("//*[@id=\"main-container\"]/div/div/div/div[4]/div[2]/div/div[2]/div/a"), 10, 1);
        linkFaireDevis.click();
    }

    private void remplirComplementaireSante(FluxData flux) {
        WebElement inputDateEffet = waitForElement(driver, By.id("date_effet_sante"), 10, 1);
        waitThread(1);
        inputDateEffet.click();
        inputDateEffet.sendKeys(dateEffet(1));

        choixRegime(flux, 0);

        WebElement inputDateNaissance = waitForElement(driver, By.id("dn_assure"), 10, 1);
        waitThread(1);
        inputDateNaissance.click();
        inputDateNaissance.sendKeys(flux.getPersonnes().get(0).getDateNaissance());

        if (flux.getPersonnes().size() == 2) {
            WebElement inputChoixConjoint = waitForElement(driver, By.id("has_conjoint_sante-0"), 10, 1);
            inputChoixConjoint.click();
            waitThread(1);
            WebElement inputDateNaissanceConjoint = waitForElement(driver, By.id("dn_conjoint"), 10, 1);
            inputDateNaissanceConjoint.click();
            waitThread(1);
            inputDateNaissanceConjoint.sendKeys(flux.getPersonnes().get(1).getDateNaissance());
        }
        WebElement inputCodePostal = waitForElement(driver, By.id("code_postal"), 10, 1);
        waitThread(1);
        inputCodePostal.click();
        inputCodePostal.sendKeys(flux.getPersonnes().get(0).getCodePostal());

        choixNbEnfants(flux);

        remplirEnfant(flux);
        waitThread(4);
        scrollDown(driver, 0, 400);

        WebElement choixBudgetMensuel = waitForElement(driver, By.id("budget_entre_50_100"), 10, 1);
        choixBudgetMensuel.click();

        waitThread(2);

        WebElement choixTutelle = waitForElement(driver, By.id("tutelle_ou_curatelle_non"), 10, 1);
        choixTutelle.click();

        scrollDown(driver, 0, 250);
        WebElement choixCouvertureActuel  = waitForElement(driver, By.id("couverture_sante_non"), 10, 1);
        choixCouvertureActuel.click();

        WebElement choixCSS_CMU = waitForElement(driver, By.id("beneficiaire_css_non"), 10, 1);
        choixCSS_CMU.click();
        scrollDown(driver, 0, 150);

        WebElement choixSoinsCourant = waitForElement(driver, By.id("soins_generaux_faible"), 10, 1);
        choixSoinsCourant.click();
        scrollDown(driver, 0, 300);

        WebElement choixHospitalisation = waitForElement(driver, By.id("hospitalisation_faible"), 10, 1);
        choixHospitalisation.click();

        scrollDown(driver, 0, 300);

        waitThread(2);

        WebElement choixOptique = waitForElement(driver, By.id("optique_faible"), 10, 1);
        choixOptique.click();
        scrollDown(driver, 0, 150);

        WebElement choixDentaire = waitForElement(driver, By.id("dentaire_faible"), 10, 1);
        choixDentaire.click();
        scrollDown(driver, 0, 250);

        WebElement choixAppareilAuditif = waitForElement(driver, By.id("appareil_auditif_faible"), 10, 1);
        choixAppareilAuditif.click();
        scrollDown(driver, 0, 150);

        waitThread(2);

        WebElement choixMedecinesDouces = waitForElement(driver, By.id("medecines_douces_non"), 10, 1);
        choixMedecinesDouces.click();
        scrollDown(driver, 0, 250);
    }

    private void remplirEnfant(FluxData flux) {
        if (flux.getEnfants().get(0).getNom() != null && !flux.getEnfants().get(0).getNom().isEmpty()) {
            WebElement inputDateNaissanceEnfant1 = waitForElement(driver, By.id("dn_enfant_sante_1"), 10, 1);
            waitThread(1);
            inputDateNaissanceEnfant1.click();
            inputDateNaissanceEnfant1.sendKeys(flux.getEnfants().get(0).getDateNaissance());
            scrollDown(driver, 0, 100);
        }
        if (flux.getEnfants().size() >= 2) {
            WebElement inputDateNaissanceEnfant2 = waitForElement(driver, By.id("dn_enfant_sante_2"), 10, 1);
            waitThread(1);
            inputDateNaissanceEnfant2.click();
            inputDateNaissanceEnfant2.sendKeys(flux.getEnfants().get(1).getDateNaissance());
            scrollDown(driver, 0, 400);
        }
    }

    private void choixNbEnfants(FluxData flux) {
        WebElement dropdownNbEnfants = waitForElement(driver, By.id("nbr_enfants_sante"), 10, 1);
        Select selectNbEnfants = new Select(dropdownNbEnfants);
        List<WebElement> optionsNbEnfants = selectNbEnfants.getOptions();
        for (WebElement webElement: optionsNbEnfants){
            if (webElement.getText().equalsIgnoreCase("0") && flux.getEnfants().get(0).getNom().equals("")) {
                selectNbEnfants.selectByVisibleText("0");
                break;
            }
            if (webElement.getText().equalsIgnoreCase("1") && (!flux.getEnfants().get(0).getNom().equals("") && flux.getEnfants().size() == 1)) {
                selectNbEnfants.selectByVisibleText("1");
                break;
            }
            if (webElement.getText().equalsIgnoreCase("2") && flux.getEnfants().size() == 2) {
                selectNbEnfants.selectByVisibleText("2");
                break;
            }
            if (webElement.getText().equalsIgnoreCase("3") && flux.getEnfants().size() == 3) {
                selectNbEnfants.selectByVisibleText("3");
                break;
            }
            if (webElement.getText().equalsIgnoreCase("4") && flux.getEnfants().size() == 4) {
                selectNbEnfants.selectByVisibleText("4");
                break;
            }
            if (webElement.getText().equalsIgnoreCase("5") && flux.getEnfants().size() == 5) {
                selectNbEnfants.selectByVisibleText("5");
                break;
            }
            if (webElement.getText().equalsIgnoreCase("6") && flux.getEnfants().size() == 6) {
                selectNbEnfants.selectByVisibleText("6");
                break;
            }
            if (webElement.getText().equalsIgnoreCase("7") && flux.getEnfants().size() == 7) {
                selectNbEnfants.selectByVisibleText("7");
                break;
            }
            if (webElement.getText().equalsIgnoreCase("8") && flux.getEnfants().size() == 8) {
                selectNbEnfants.selectByVisibleText("8");
                break;
            }
        }
    }

    private void suivant() {
        WebElement buttonSuivant = waitForElement(driver, By.xpath("//*[@id=\"calculer_tarif\"]"), 10, 1);
        buttonSuivant.click();
    }

    private void choixRegime(FluxData flux, int index) {
        WebElement dropdown = waitForElement(driver, By.id("regime_social_sante"), 10, 1);
        Select select = new Select(dropdown);
        List<WebElement> options = select.getOptions();
        for (WebElement webElement: options){
            if (webElement.getText().equalsIgnoreCase("Régime Général")) {
                select.selectByVisibleText("Régime Général");
                break;
            }
        }
    }

    @Override
    protected void pageName(String namePage) {}

}
