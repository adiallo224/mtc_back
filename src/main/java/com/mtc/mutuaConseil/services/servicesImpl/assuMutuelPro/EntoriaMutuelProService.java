package com.mtc.mutuaConseil.services.servicesImpl.assuMutuelPro;

import com.mtc.mutuaConseil.models.Compte;
import com.mtc.mutuaConseil.models.FluxData;
import com.mtc.mutuaConseil.models.Tarif;
import com.mtc.mutuaConseil.services.LaunchedService;
import com.mtc.mutuaConseil.utils.InformationsUser;
import com.mtc.mutuaConseil.utils.PageElementInteraction;
import com.mtc.mutuaConseil.utils.Shared;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.interactions.Actions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EntoriaMutuelProService extends PageElementInteraction implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(EntoriaMutuelProService.class);
    public static WebDriver driver;
    private final String source = "EntoriaMutuelProService";
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
            remplirContrat(flux);
            waitThread(2);
            WebElement sectionPrix = waitForElement(driver, By.xpath("//section[@class='pc-results__recommendations'][1]"), 25, 1);
            waitThread(2);
            WebElement divTotal = sectionPrix.findElement(By.xpath("//div[@class='pc-offer-mobile__infos']//div[@class='pc-offer-price']"));
            waitThread(2);
            WebElement totalElement = divTotal.findElement(By.xpath("//span[@class='pc-price']//strong"));
            waitThread(1);
            String cout = totalElement.getText();
            log.info("cout {}", cout);
            tarif.setMontant(cout);
            String screenshotBytes = captureScreenshot(driver, tarif.getNom(), false, tarif);
            if (screenshotBytes != null) {
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
        waitThread(2);
        WebElement inputCodeCourtier = waitForElement(driver, By.id("mat-input-0"), 10, 2);
        inputCodeCourtier.sendKeys("98836");
        InformationsUser.infoLogin(driver, By.id("mat-input-1"), 10, 2, c.getUsername());
        InformationsUser.infoPassword(driver, By.id("password"), 10, 2, c.getPassword());
        WebElement buttonLogin = waitForElement1(driver, By.xpath("//span[contains(text(), \"Se connecter\")]"), 20, 2);
        buttonLogin.click();
    }

    private void remplirContrat(FluxData flux) {
        waitThread(7);
        WebElement labelOffre = waitForElement(driver, By.xpath("//label[contains(text(), \"Offres\")]"), 10, 1);
        actions.moveToElement(labelOffre).click().perform();
        waitThread(1);
        WebElement divTarifer = waitForElement(driver, By.xpath("//div[contains(text(), \"Tarifer\")]"), 10, 1);
        actions.moveToElement(divTarifer).click().perform();
        waitThread(1);
        WebElement inputDateNaissance = waitForElement(driver, By.id("mat-input-0"), 10, 1);
        inputDateNaissance.sendKeys(flux.getPersonnes().get(0).getDateNaissance());
        choixProfession(flux, By.xpath("//div[@role=\"listbox\"]"));
        choixStatut(flux, By.xpath("//div[@role=\"listbox\"]"));
        choixRegime(flux, By.xpath("//div[@role=\"listbox\"]"));
        choixDepartement(flux, By.xpath("//div[@role=\"listbox\"]"));
//        WebElement radioAssureEntoria = waitForElement(driver, By.id("mat-radio-3"), 10, 1);
//        radioAssureEntoria.click();
        WebElement buttonTarifExpress = waitForElement1(driver, By.xpath("//span[contains(text(), \"suivant\")]"), 20, 2);
        buttonTarifExpress.click();
    }

    private void choixProfession(FluxData flux, By locator) {
        WebElement inputChoixProfession = waitForElement(driver, By.id("mat-input-2"), 10, 1);
        inputChoixProfession.click();
        scrollAndSelect1(driver, locator, flux.getPersonnes().get(0).getProfession(), ".//span[@class='mat-option-text']");
    }

    private void choixStatut(FluxData flux, By locator) {
        WebElement inputMetierExerce = waitForElement(driver, By.id("mat-input-3"), 10, 1);
        inputMetierExerce.click();
        scrollAndSelectStatut(driver, locator, flux.getPersonnes().get(0).getProfession(), ".//span[@class='mat-option-text']");
    }

    private void choixRegime(FluxData flux, By locator) {
        WebElement inputChoixRegime = waitForElement(driver, By.id("mat-input-4"), 10, 1);
        inputChoixRegime.click();
        scrollAndSelectRegime(driver, locator, flux.getPersonnes().get(0).getRegime(), ".//span[@class='mat-option-text']");
    }

    private void choixDepartement(FluxData flux, By locator) {
        WebElement inputChoixDepartement = waitForElement(driver, By.id("mat-input-1"), 10, 1);
        inputChoixDepartement.click();
        scrollAndSelect1(driver, locator, Shared.getDepartementFromCodePostal(flux.getPersonnes().get(0).getCodePostal()), ".//span[@class='mat-option-text']");
    }

    public void scrollAndSelectStatut(WebDriver driver, By locator, String itemToSelect, String itemsLocator) {
        WebElement listbox = waitForElement(driver, locator, 15, 1);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        List<WebElement> items = listbox.findElements(By.xpath(itemsLocator));
        for (WebElement item : items) {
            if (item.getText().equalsIgnoreCase( "Gérant majoritaire" )) {
                item.click();
                break;
            }
        }
    }

    public void scrollAndSelectRegime(WebDriver driver, By locator, String itemToSelect, String itemsLocator) {
        WebElement listbox = waitForElement(driver, locator, 15, 1);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        List<WebElement> items = listbox.findElements(By.xpath(itemsLocator));
        for (WebElement item : items) {
            if (item.getText().equalsIgnoreCase( "Régime général" )) {
                item.click();
                break;
            }
        }
    }

    public void scrollAndSelectProfession(WebDriver driver, By locator, String itemToSelect, String itemsLocator) {
        WebElement listbox = waitForElement(driver, locator, 15, 1);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        List<WebElement> items = listbox.findElements(By.xpath(itemsLocator));
        for (WebElement item : items) {
            if (item.getText().equalsIgnoreCase(itemToSelect) || item.getText().contains(itemToSelect)) {
                waitThread(5);
                item.click();
                break;
            }
        }
    }

    @Override
    protected void pageName(String namePage) {}
}
