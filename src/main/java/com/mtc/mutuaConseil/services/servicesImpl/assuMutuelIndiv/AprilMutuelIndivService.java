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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AprilMutuelIndivService extends PageElementInteraction implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(AprilMutuelIndivService.class);
    public static WebDriver driver;
    private final String source = "AprilMutuelIndivService";
    private static Actions actions;
    private final TypeAssuranceService typeAssuranceService;

    public AprilMutuelIndivService(TypeAssuranceService typeAssuranceService){
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        Tarif tarifAprilMutuelIndiv = TarifUtils.createDefaultTarif(c, typeAssuranceService, 2L);;
        tarifAprilMutuelIndiv.setNom(c.getNomFournisseur());
        driver = new EdgeDriver();
        driver.manage().window().maximize();
        actions = new Actions(driver);
        try {
            connexion(c);
            remplirContrat(flux);
            suivant();
            waitThread(8);
            WebElement sectionPrix = waitForElement(driver, By.xpath("//span[@id='market-place_health_comparator_health-rate_1_rate_total-price']"), 10, 1);
            String cout = sectionPrix.getText();
            log.info("cout {}", cout);
            chatClose();
            tarifAprilMutuelIndiv.setMontant(cout);
            String screenshotBytes = captureScreenshot(driver, tarifAprilMutuelIndiv.getNom(), false, tarifAprilMutuelIndiv);
            if (screenshotBytes != null) {
                tarifAprilMutuelIndiv.setCaptureImg(screenshotBytes);
            }
            tarifAprilMutuelIndiv.setExecution(true);
        } catch (Exception e) {
            log.error("An error occurred", e);
            tarifAprilMutuelIndiv.setErreur(e.getMessage());
            String screenshotBytesErreur = captureScreenshot(driver, tarifAprilMutuelIndiv.getNom(), true, tarifAprilMutuelIndiv);
            tarifAprilMutuelIndiv.setCaptureImgErreur(screenshotBytesErreur);
            tarifAprilMutuelIndiv.setEtape("");
        } finally {
            driver.quit();
        }
        return tarifAprilMutuelIndiv;
    }

    public void connexion(Compte c) {
        driver.get(c.getUrlFournisseur());
        waitThread(3);
        inputByName(driver, "username", c.getUsername());
        inputByName(driver, "password", c.getPassword());
        clicButtonByXpath(driver, "//*[@id=\"login-button\"]", actions);
        waitThread(5);
    }

    private void remplirContrat(FluxData flux) {
        waitThread(2);
        WebElement inputAdherentContratIndiv = waitForElement(driver, By.xpath("//*[@id=\"radio-input_market-place_health_client-needs_effect-date-form_individual-policy_aon-choice_1_label\"]"), 10, 1);
        inputAdherentContratIndiv.click();
        waitThread(1);
        if (flux.getPersonnes().get(0).getCivilite().equalsIgnoreCase("Monsieur")) {
            WebElement inputCivilite = waitForElement(driver, By.xpath("//label[@id='radio-input_market-place_health_client-needs_main-insured-form_civilities_aon-choice_0_label']"), 10, 1);
            inputCivilite.click();
        }
        if (flux.getPersonnes().get(0).getCivilite().equalsIgnoreCase("Madame")) {
            WebElement inputCivilite = waitForElement(driver, By.xpath("//label[@id='radio-input_market-place_health_client-needs_main-insured-form_civilities_aon-choice_1_label']"), 10, 1);
            inputCivilite.click();
        }
        WebElement inputDateEffet = waitForElement(driver, By.xpath("//input[@id='market-place_health_client-needs_effect-date-form_effective-date_date-picker_input']"), 10, 1);
        inputDateEffet.sendKeys(dateEffet(1));
        waitThread(1);

        WebElement inputPrenom = waitForElement(driver, By.xpath("//input[@id='market-place_health_client-needs_main-insured-form_first-name_input-text_input']"), 10, 1);
        inputPrenom.sendKeys(flux.getPersonnes().get(0).getPrenom());
        waitThread(1);

        WebElement inputNom = waitForElement(driver, By.xpath("//input[@id='market-place_health_client-needs_main-insured-form_last-name_input-text_input']"), 10, 1);
        inputNom.sendKeys(flux.getPersonnes().get(0).getNom());
        waitThread(1);

        WebElement inputDateNaissance = waitForElement(driver, By.xpath("//input[@id='market-place_health_client-needs_main-insured-form_birth-date_date-picker_input']"), 10, 1);
        inputDateNaissance.sendKeys(flux.getPersonnes().get(0).getDateNaissance());
        waitThread(1);

        WebElement inputCodePostal = waitForElement(driver, By.xpath("//input[@id='market-place_health_client-needs_main-insured-form_countries_input-zipcode_input']"), 10, 1);
        inputCodePostal.sendKeys(flux.getPersonnes().get(0).getCodePostal());
        waitThread(1);

        choixStatut(flux);

        remplirAssureConjoint(flux);

        remplirAssureEnfant(flux);
    }

    private void remplirAssureConjoint(FluxData flux) {
        waitThread(2);
        if (flux.getPersonnes().size() == 2) {
            ajouterAssure();
            WebElement buttonConjoint = waitForElement(driver, By.id("radio-input_market-place_health_client-needs_beneficiary_0_beneficiary-form_relationships_aon-choice_0_label"), 10, 1);
            buttonConjoint.click();
            WebElement inputDateNaissanceConjoint = waitForElement(driver, By.xpath("//input[@id='market-place_health_client-needs_beneficiary_0_beneficiary-form_birth-date_date-picker_input']"), 10, 1);
            inputDateNaissanceConjoint.sendKeys(flux.getPersonnes().get(1).getDateNaissance());
            waitThread(2);
        }
    }

    private void remplirAssureEnfant(FluxData flux) {
        if (flux.getEnfants().get(0).getNom() != null && !flux.getEnfants().get(0).getNom().isEmpty()) {
            ajouterAssure();
            WebElement inputDateNaissanceEnfant = waitForElement(driver, By.id("market-place_health_client-needs_beneficiary_1_beneficiary-form_birth-date_date-picker_input"), 10, 1);
            inputDateNaissanceEnfant.sendKeys(flux.getEnfants().get(0).getDateNaissance());
            waitThread(2);
        }
        if (flux.getEnfants().size() == 2) {
            ajouterAssure();
            WebElement inputDateNaissanceEnfant = waitForElement(driver, By.id("market-place_health_client-needs_beneficiary_2_beneficiary-form_birth-date_date-picker_input"), 10, 1);
            inputDateNaissanceEnfant.sendKeys(flux.getEnfants().get(1).getDateNaissance());
            waitThread(2);
        }

    }

    private void ajouterAssure() {
        WebElement buttonAjouterAssure = waitForElement(driver, By.id("market-place_health_client-needs_add-insured-button"), 10, 1);
        buttonAjouterAssure.click();
    }

    private void choixStatut(FluxData flux) {
        waitThread(1);
        WebElement selectStatut = waitForElement(driver, By.id("market-place_health_client-needs_main-insured-form_prefessional-status_input-select_0_professionalStatus"), 10, 1);
        selectStatut.click();
        WebElement selectOption = waitForElement(driver, By.xpath("//div[@role=\"listbox\"]"), 10, 1);
        waitThread(1);
        List<WebElement> items = selectOption.findElements(By.xpath(".//div[@role='option']"));
        for (WebElement element : items) {
             if (element.getText().equalsIgnoreCase("Cadre") &&
                 (flux.getPersonnes().get(0).getProfessionSpecifique().equalsIgnoreCase("Chef d'entreprise") ||
                 flux.getPersonnes().get(0).getProfessionSpecifique().equalsIgnoreCase("Artisan") ||
                 flux.getPersonnes().get(0).getProfessionSpecifique().equalsIgnoreCase("Commerçant") ||
                 flux.getPersonnes().get(0).getProfessionSpecifique().contains("Salarié cadre"))
             ) {
                element.click();
                break;
             }
             if (element.getText().equalsIgnoreCase("Ouvrier") && flux.getPersonnes().get(0).getProfessionSpecifique().equalsIgnoreCase("Ouvrier")) {
                 element.click();
                 break;
             }
             if (element.getText().equalsIgnoreCase("Employé") && flux.getPersonnes().get(0).getProfessionSpecifique().contains("Salarié non cadre : employé")) {
                 element.click();
                 break;
             }
             if (element.getText().equalsIgnoreCase("Fonctionnaire") &&
                 (flux.getPersonnes().get(0).getProfessionSpecifique().equalsIgnoreCase("Fonctionnaire classe a")
                 || flux.getPersonnes().get(0).getProfessionSpecifique().equalsIgnoreCase("Fonctionnaire hors classe a"))) {
                 element.click();
                 break;
             }
        }
        scrollDown(driver, 0, 300);
    }

    private void suivant() {
        waitThread(1);
        WebElement buttonSuivant = waitForElement(driver, By.xpath("//button[@id='market-place_health_client-needs_see-offers']"), 10, 1);
        buttonSuivant.click();
    }

    private void chatClose() {
        waitThread(1);
        WebElement buttonSuivant = waitForElement(driver, By.id("chat-prompting-close"), 10, 1);
        buttonSuivant.click();
    }

    @Override
    protected void pageName(String namePage) {}
}
