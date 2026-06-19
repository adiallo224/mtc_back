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
public class HennerMutuelIndivService extends PageElementInteraction implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(HennerMutuelIndivService.class);
    public static WebDriver driver;
    private final String source = "HennerMutuelIndivService";
    private static Actions actions;
    private final TypeAssuranceService typeAssuranceService;

    public HennerMutuelIndivService(TypeAssuranceService typeAssuranceService){
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        Tarif tarifHennerMutuelIndiv = TarifUtils.createDefaultTarif(c, typeAssuranceService, 2L);;
        tarifHennerMutuelIndiv.setNom(c.getNomFournisseur());
        driver = new EdgeDriver();
        driver.manage().window().maximize();
        actions = new Actions(driver);
        try {
            connexion(c);
            remplirCreationDevis();
            remplirChoixDevis();
            remplirContrat(flux);
            remplirSante();
            remplirDevisSante(flux);
            suivant();
            waitThread(5);
            WebElement sectionPrix = waitForElement(driver, By.xpath("/html/body/app-root/app-auth/div/div/div/div/app-calculator/div/main/app-indiv/app-indiv-pricing/div/div[5]/div[3]/div[2]/div/div[1]/div[2]"), 10, 1);
            String cout = sectionPrix.getText();
            log.info("cout {}", cout);
            tarifHennerMutuelIndiv.setMontant(cout);
            String screenshotBytes = captureScreenshot(driver, tarifHennerMutuelIndiv.getNom(), false, tarifHennerMutuelIndiv);
            if (screenshotBytes != null) {
                tarifHennerMutuelIndiv.setCaptureImg(screenshotBytes);
            }
            tarifHennerMutuelIndiv.setExecution(true);
        } catch (Exception e) {
            log.error("An error occurred", e);
            tarifHennerMutuelIndiv.setErreur(e.getMessage());
            String screenshotBytesErreur = captureScreenshot(driver, tarifHennerMutuelIndiv.getNom(), true, tarifHennerMutuelIndiv);
            tarifHennerMutuelIndiv.setCaptureImgErreur(screenshotBytesErreur);
            tarifHennerMutuelIndiv.setEtape("");
        } finally {
            driver.quit();
        }
        return tarifHennerMutuelIndiv;
    }

    public void connexion(Compte c) {
        driver.get(c.getUrlFournisseur());
        waitThread(1);
        WebElement buttonCookies = waitForElement(driver, By.xpath("/html/body/app-root/henner-cookie-banner/div/ul/li[3]/button"), 10, 2);
        buttonCookies.click();
        WebElement inputLogin = waitForElement(driver, By.id("mat-input-0"), 10, 2);
        inputLogin.sendKeys(c.getUsername());
        WebElement inputPassword = waitForElement(driver, By.id("mat-input-1"), 10, 2);
        inputPassword.sendKeys(c.getPassword());
        waitThread(1);
        WebElement buttonLogin = waitForElement1(driver, By.xpath("//span[normalize-space()='Se connecter']"), 20, 2);
        buttonLogin.click();
        waitThread(1);
    }

    private void remplirCreationDevis() {
        WebElement buttonDevis = waitForElement(driver, By.xpath("//a[@class='mat-focus-indicator button-tarifer__link mat-raised-button mat-button-base mat-primary ng-star-inserted']"), 10, 2);
        buttonDevis.click();
    }

    private void remplirChoixDevis() {
        WebElement buttonChoixDevis = waitForElement(driver, By.xpath("//span[normalize-space()='PARTICULIER']"), 10, 1);
        buttonChoixDevis.click();
    }

    private void remplirContrat(FluxData flux) {
        waitThread(1);
        WebElement inputDateNaissance = waitForElement(driver, By.xpath("//input[@data-placeholder='Date de naissance']"), 10, 1);
        inputDateNaissance.sendKeys(flux.getPersonnes().get(0).getDateNaissance());
        waitThread(1);
        WebElement inputCodePostal = waitForElement(driver, By.xpath("//input[@data-placeholder='Code postal']"), 10, 1);
        inputCodePostal.sendKeys(flux.getPersonnes().get(0).getCodePostal());
        waitThread(1);
        WebElement inputNom = waitForElement(driver, By.xpath("//input[@data-placeholder='Nom (facultatif)']"), 10, 1);
        inputNom.sendKeys(flux.getPersonnes().get(0).getNom());
        waitThread(1);
        WebElement inputPrenom = waitForElement(driver, By.xpath("//input[@data-placeholder='Prénom (facultatif)']"), 10, 1);
        inputPrenom.sendKeys(flux.getPersonnes().get(0).getPrenom());
        waitThread(1);
        WebElement buttonValider = waitForElement(driver, By.xpath("//span[normalize-space()='VALIDER']"), 10, 1);
        buttonValider.click();
    }

    private void remplirSante() {
        waitThread(1);
        WebElement buttonChoixDevis = waitForElement(driver, By.xpath("//span[normalize-space()='Santé']"), 10, 1);
        buttonChoixDevis.click();
    }

    private void remplirDevisSante(FluxData flux) {
        waitThread(1);
        WebElement inputDateEffet = waitForElement(driver, By.xpath("//input[@data-placeholder=\"Date d'effet\"]"), 10, 1);
        inputDateEffet.sendKeys(dateEffet(1));
        choixRegime(flux, "//span[normalize-space()='Régime']");
        if (flux.getPersonnes().size() >= 2) {
            remplirConjoint(flux);
            scrollDown(driver, 0, 100);
        }
        if (flux.getEnfants().get(0).getNom() != null && !flux.getEnfants().get(0).getNom().isEmpty()) {
            remplirEnfant(flux, "/html/body/app-root/app-auth/div/div/div/div/app-calculator/div/main/app-indiv/app-indiv-recap/app-indiv-client-info/div/div/div[2]/div/div/div/app-indiv-form/div/form/div[1]/div/div[3]/div/div[1]/div[2]/div[1]/mat-form-field/div/div[1]/div[1]/input", "/html/body/app-root/app-auth/div/div/div/div/app-calculator/div/main/app-indiv/app-indiv-recap/app-indiv-client-info/div/div/div[2]/div/div/div/app-indiv-form/div/form/div[1]/div/div[3]/div/div[1]/div[2]/div[2]/mat-form-field/div/div[1]/div/mat-select/div/div[1]/span", 0);
            scrollDown(driver, 0, 100);
        }
        if (flux.getEnfants().size() >= 2) {
            remplirEnfant(flux, "/html/body/app-root/app-auth/div/div/div/div/app-calculator/div/main/app-indiv/app-indiv-recap/app-indiv-client-info/div/div/div[2]/div/div/div/app-indiv-form/div/form/div[1]/div/div[3]/div/div[2]/div[2]/div[1]/mat-form-field/div/div[1]/div[1]/input", "/html/body/app-root/app-auth/div/div/div/div/app-calculator/div/main/app-indiv/app-indiv-recap/app-indiv-client-info/div/div/div[2]/div/div/div/app-indiv-form/div/form/div[1]/div/div[3]/div/div[2]/div[2]/div[2]/mat-form-field/div/div[1]/div/mat-select/div/div[1]/span", 1);
            scrollDown(driver, 0, 250);
        }
    }

    private void remplirConjoint(FluxData flux) {
        ajoutConjoint();
        waitThread(1);
        WebElement inputDateNaissanceConjoint = waitForElement(driver, By.xpath("/html/body/app-root/app-auth/div/div/div/div/app-calculator/div/main/app-indiv/app-indiv-recap/app-indiv-client-info/div/div/div[2]/div/div/div/app-indiv-form/div/form/div[1]/div/div[2]/div/div/div[2]/div[1]/mat-form-field/div/div[1]/div[1]/input"), 10, 1);
        inputDateNaissanceConjoint.sendKeys(flux.getPersonnes().get(1).getDateNaissance());
        choixRegime(flux, "/html/body/app-root/app-auth/div/div/div/div/app-calculator/div/main/app-indiv/app-indiv-recap/app-indiv-client-info/div/div/div[2]/div/div/div/app-indiv-form/div/form/div[1]/div/div[2]/div/div/div[2]/div[2]/mat-form-field/div/div[1]/div/mat-select/div/div[1]/span");
    }

    private void remplirEnfant(FluxData flux, String xpathDateNaissance, String xpathRegime, int index) {
        ajoutEnfant();
        waitThread(1);
        WebElement inputDateNaissanceEnfant = waitForElement(driver, By.xpath(xpathDateNaissance), 10, 1);
        inputDateNaissanceEnfant.sendKeys(flux.getEnfants().get(index).getDateNaissance());
        choixRegimeEnfant(flux, xpathRegime);
    }

    private void choixRegime(FluxData flux, String xpath) {
        waitThread(1);
        WebElement selectRegime = waitForElement(driver, By.xpath(xpath), 10, 1);
        selectRegime.click();
        WebElement selectOption = waitForElement(driver, By.xpath("//div[@role=\"listbox\"]"), 10, 1);
        waitThread(1);
        List<WebElement> items = selectOption.findElements(By.xpath(".//mat-option[@role='option']"));
        for (WebElement element : items) {
            if (element.getText().equalsIgnoreCase("Régime Général")) {
                element.click();
                break;
            }
        }
    }

    private void choixRegimeEnfant(FluxData flux, String xpath) {
        waitThread(1);
        WebElement selectRegime = waitForElement(driver, By.xpath(xpath), 10, 1);
        selectRegime.click();
        WebElement selectOption = waitForElement(driver, By.xpath("//div[@role=\"listbox\"]"), 10, 1);
        waitThread(1);
        List<WebElement> items = selectOption.findElements(By.xpath(".//mat-option[@role='option']"));
        for (WebElement element : items) {
            if (element.getText().equalsIgnoreCase("Régime Général")) {
                element.click();
                break;
            }
        }
    }

    private void ajoutConjoint() {
        waitThread(1);
        WebElement divAjoutConjoint = waitForElement(driver, By.xpath("//div[normalize-space()='Ajouter un conjoint']"), 10, 1);
        divAjoutConjoint.click();
    }

    private void ajoutEnfant() {
        waitThread(1);
        WebElement divAjoutEnfant = waitForElement(driver, By.xpath("//div[normalize-space()='Ajouter un enfant']"), 10, 1);
        divAjoutEnfant.click();
    }

    private void suivant() {
        waitThread(1);
        WebElement buttonSuivant = waitForElement(driver, By.xpath("//span[normalize-space()='TARIFER']"), 10, 1);
        buttonSuivant.click();
    }

    @Override
    protected void pageName(String namePage) {}
}
