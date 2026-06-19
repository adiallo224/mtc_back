package com.mtc.mutuaConseil.services.servicesImpl.assuMutuelPro;


import com.mtc.mutuaConseil.models.Compte;
import com.mtc.mutuaConseil.models.FluxData;
import com.mtc.mutuaConseil.models.Tarif;
import com.mtc.mutuaConseil.services.LaunchedService;
import com.mtc.mutuaConseil.utils.InformationsUser;
import com.mtc.mutuaConseil.utils.PageElementInteraction;
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
public class HennerMutuelProService extends PageElementInteraction implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(HennerMutuelProService.class);
    public static WebDriver driver;
    private final String source = "HennerMutuelProService";
    private static Actions actions;


    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        Tarif tarif = new Tarif();
        tarif.setNom(c.getNomFournisseur());
        driver = new EdgeDriver();
        driver.manage().window().maximize();
        actions = new Actions(driver);
        try {
            connexion(c);
            accesTNS();
            remplirClient(flux);
            remplirChoixRisques(flux);
            waitThread(7);
            List<WebElement> sectionDiv = driver.findElements(By.xpath("(//div[contains(@class, 'indiv-pricing--content--bloc-item')]//span[@class='indiv-pricing--content--bloc-item-element-top--price-amount'])[2]"));
            String cout = sectionDiv.get(0).getText();
            log.info("cout {}", cout);
            tarif.setMontant(cout);
            String screenshotBytes = captureScreenshot(driver, tarif.getNom(), false, tarif);
            if (screenshotBytes != null) {
//                 scrollDown(driver, 0, 500);
                tarif.setCaptureImg(screenshotBytes);
            }
        } catch (Exception e) {
            log.error("An error occurred", e);
        } finally {
            driver.quit();
        }
        return tarif;
    }

    public void connexion(Compte c) {
        driver.get(c.getUrlFournisseur());
        WebElement buttonCookies = waitForElement(driver, By.xpath("//span[contains(text(), \" TOUT ACCEPTER \")]"), 10, 1);
        buttonCookies.click();
        InformationsUser.infoLogin(driver, By.id("mat-input-0"), 10, 1, c.getUsername());
        InformationsUser.infoPassword(driver, By.id("mat-input-1"), 10, 1, c.getPassword());
        waitThread(1);
        WebElement buttonLogin = waitForElement(driver, By.xpath("/html/body/app-root/app-login/div/app-login-form/div/henner-authentication/form/div/div[2]/button/span"), 10, 1);
        buttonLogin.click();
    }

    private void accesTNS() {
        waitThread(6);
        WebElement buttonCreationDevis = waitForElement(driver, By.xpath("//a[contains(@class, 'button-tarifer__link')]"), 10, 1);
        buttonCreationDevis.click();
        waitThread(1);
        WebElement buttonTNS = waitForElement(driver, By.xpath("//button[@title=\"TNS\"]"), 10, 1);
        buttonTNS.click();
        waitThread(1);
        WebElement buttonSante = waitForElement(driver, By.xpath("//span[contains(text(), \"Santé Seule\")]"), 10, 1);
        buttonSante.click();
    }

    private void remplirClient(FluxData flux) {
        WebElement inputDateNaissance = waitForElement(driver, By.xpath("//input[@placeholder='Date de naissance']"), 10, 1);
        inputDateNaissance.sendKeys(flux.getPersonnes().get(0).getDateNaissance());
        WebElement inputCodePostal = waitForElement(driver, By.xpath("//input[@placeholder='Code postal']"), 10, 1);
        inputCodePostal.sendKeys(flux.getPersonnes().get(0).getCodePostal());
        WebElement selectStatut = waitForElement(driver, By.xpath("//span[contains(text(),'Statut')]"), 10, 1);
        selectStatut.click();
        waitThread(1);
        WebElement spanStatut = waitForElement(driver, By.xpath("//span[contains(text(), \"Artisan ou commerçant soumis à l’impôt sur le bénéfice industriel et commercial (BIC)\")]"), 10, 1);
        spanStatut.click();
        WebElement inputNom = waitForElement(driver, By.xpath("//input[@placeholder='Nom (facultatif)']"), 10, 1);
        inputNom.sendKeys(flux.getPersonnes().get(0).getNom());
        WebElement inputPrenom = waitForElement(driver, By.xpath("//input[@placeholder='Prénom (facultatif)']"), 10, 1);
        inputPrenom.sendKeys(flux.getPersonnes().get(0).getPrenom());
        waitThread(1);
        WebElement buttonValider = waitForElement(driver, By.xpath("//span[contains(text(), \"VALIDER\")]"), 10, 1);
        buttonValider.click();
    }

    private void remplirChoixRisques(FluxData flux) {
        waitThread(2);
        WebElement inputDateEffet = waitForElement(driver, By.xpath("//input[@placeholder=\"Date d'effet\"]"), 10, 1);
        inputDateEffet.sendKeys(dateEffet(1));
        if (flux.getPersonnes().size() == 2) {
            WebElement buttonAjoutConjoint = waitForElement(driver, By.xpath("//div[contains(text(), \"Ajouter un conjoint\")]"), 10, 1);
            buttonAjoutConjoint.click();
            waitThread(1);
            WebElement inputDateNaissanceConjoint = waitForElement(driver, By.xpath("//div[contains(text(), \"Ajouter un conjoint\")]"), 10, 1);
            inputDateNaissanceConjoint.sendKeys(flux.getPersonnes().get(1).getDateNaissance());
            WebElement selectStatutConjoint = waitForElement(driver, By.id("mat-select-6"), 10, 1);
            selectStatutConjoint.click();
            waitThread(1);
            WebElement spanStatutConjoint = waitForElement(driver, By.xpath("//span[contains(text(), \"Régime Général\")]"), 10, 1);
            spanStatutConjoint.click();;
        }
        if (!flux.getEnfants().isEmpty() && flux.getEnfants().get(0).getNom().length() > 1) {
            WebElement buttonAjoutEnfant = waitForElement(driver, By.xpath("//div[contains(text(), \"Ajouter un enfant\")]"), 10, 1);
            buttonAjoutEnfant.click();
            waitThread(1);
            WebElement inputDateNaissanceEnfant = waitForElement(driver, By.xpath("//div[contains(text(), \"Ajouter un conjoint\")]"), 10, 1);
            inputDateNaissanceEnfant.sendKeys(flux.getEnfants().get(0).getDateNaissance());
        }
        waitThread(1);
        WebElement buttonTarifer = waitForElement(driver, By.xpath("//span[contains(text(), \"TARIFER\")]"), 10, 1);
        buttonTarifer.click();
    }

    @Override
    protected void pageName(String namePage) {}
}
