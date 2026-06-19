package com.mtc.mutuaConseil.services.servicesImpl.assuMutuelPro;

import com.mtc.mutuaConseil.models.Compte;
import com.mtc.mutuaConseil.models.FluxData;
import com.mtc.mutuaConseil.models.Tarif;
import com.mtc.mutuaConseil.services.LaunchedService;
import com.mtc.mutuaConseil.utils.InformationsPersonne;
import com.mtc.mutuaConseil.utils.InformationsUser;
import com.mtc.mutuaConseil.utils.PageElementInteraction;
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
public class FFAMutuelProService extends PageElementInteraction implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(FFAMutuelProService.class);
    public static WebDriver driver;
    private final String source = "FFAMutuelProService";
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
            remplirBesoinsClient(flux, source);
            remplirAdherents(flux);
            remplirCoordonnees(flux, 0);
            recherche();
            scrollDown(driver, 0, 500);
            waitThread(7);
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
        WebElement buttonCookies = waitForElement(driver, By.xpath("//div//button[@aria-label=\"Non merci\"]"), 10, 2);
        buttonCookies.click();
        InformationsUser.infoLogin(driver, By.id("username"), 10, 2, c.getUsername());
        InformationsUser.infoPassword(driver, By.id("password"), 10, 2, c.getPassword());
        waitThread(2);
        WebElement buttonLogin = waitForElement1(driver, By.id("kc-form-buttons"), 20, 2);
        buttonLogin.click();
    }

    private void remplirContrat(FluxData flux) {
        waitThread(2);
        if (flux.getPersonnes().size() == 1) {
            WebElement buttonAdherentSeul = waitForElement(driver, By.xpath("//div[@class='pc-radio-button']//label[normalize-space(text())='Adhérent seul']"), 10, 2);
            buttonAdherentSeul.click();
        }
        if (flux.getPersonnes().size() == 2) {
            WebElement buttonCouple = waitForElement(driver, By.xpath("//div[@class='pc-radio-button']//label[normalize-space(text())='Couple']"), 10, 2);
            buttonCouple.click();
        }
        if (flux.getPersonnes().size() == 1 && (!flux.getEnfants().isEmpty()) && !flux.getEnfants().get(0).getCivilite().isEmpty()) {
            WebElement buttonAdherentEnfant = waitForElement(driver, By.xpath("//div[@class='pc-radio-button']//label[normalize-space(text())='Adhérent et enfant(s)']"), 10, 2);
            buttonAdherentEnfant.click();
        }
        if (flux.getPersonnes().size() == 2 && !flux.getEnfants().isEmpty() && flux.getEnfants().get(0).getCivilite() != null) {
            WebElement buttonCoupleEnfant = waitForElement(driver, By.xpath("//div[@class='pc-radio-button']//label[normalize-space(text())='Couple et enfant(s)']"), 10, 2);
            buttonCoupleEnfant.click();
        }
        // Remplacement d’un contrat santé souscrit chez un autre assureur

//        WebElement inputDateStart = waitForElement(driver, By.id("startDate"), 10, 2);
//        inputDateStart.sendKeys("");

        WebElement buttonContratSanteSouscrit = waitForElement(driver, By.xpath("//div[@class='pc-radio-button']//label[normalize-space(text())='Non']"), 10, 2);
        buttonContratSanteSouscrit.click();
    }

    private void remplirBesoinsClient(FluxData flux, String source) {
    }

    private void remplirAdherents(FluxData flux) {
        waitThread(2);
        WebElement inputDateNaissance = waitForElement(driver, By.id("birthdate"), 10, 2);
        inputDateNaissance.sendKeys(flux.getPersonnes().get(0).getDateNaissance());
        choixCategorieSocioPro(flux, 0);
//        choixRegime(flux, 0);
    }

    private void remplirCoordonnees(FluxData flux, int index) {
        waitThread(2);
        if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur")) {
            WebElement radioCivilite = waitForElement(driver, By.xpath("//div[@class='pc-radio-button']//label[normalize-space(text())='Monsieur']"), 15, 1);
            radioCivilite.click();
        } else if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame")) {
            WebElement radioCivilite = waitForElement(driver, By.xpath("//div[@class='pc-radio-button']//label[normalize-space(text())='Madame']"), 15, 1);
            radioCivilite.click();
        }
        InformationsPersonne.infoPrenom(driver, By.id("insured_firstname"), 10, 2, flux, index, source);
        InformationsPersonne.infoNom(driver, By.id("inusred_lastname"), 10, 2, flux, index, source);
        InformationsPersonne.infoCodePostal(driver, By.id("postalCode"), 10, 2, flux, index, source);
        InformationsPersonne.infoTelephone(driver, By.id("phone"), 10, 2, flux, index, source);
        InformationsPersonne.infoEmail(driver, By.id("email"), 10, 2, flux, index, source);
    }

    private void choixRegime(FluxData flux, int index) {
        waitThread(2);
        WebElement dropdownRegime = null;
        if (index == 0) {
            dropdownRegime = waitForElement(driver, By.id("insured_regime"), 10, 1);
        } if (index == 1) {
            dropdownRegime = waitForElement(driver, By.id("insured_regime"), 10, 1);
        }
        if (dropdownRegime != null) {
            Select selectRegime = new Select(dropdownRegime);
            if (flux.getPersonnes().get(index).getRegime().equalsIgnoreCase("Salarié"))
                selectRegime.selectByVisibleText("Sécurité Sociale");
            if (flux.getPersonnes().get(index).getRegime().equalsIgnoreCase("IPT, ITT"))
                selectRegime.selectByVisibleText("Sécurité sociale des indépendants");
            if (flux.getPersonnes().get(index).getRegime().equalsIgnoreCase("IPT"))
                selectRegime.selectByVisibleText("Alsace Moselle");
            if (flux.getPersonnes().get(index).getRegime().equalsIgnoreCase("Aucune"))
                selectRegime.selectByVisibleText("Régime Agricole - MSA");
        }
    }

    private void choixCategorieSocioPro(FluxData flux, int index) {
        WebElement dropdownCategorieSocioPro = null;
        if (index == 0) {
            dropdownCategorieSocioPro = waitForElement(driver, By.id("insured_category_select"), 15, 1);
        } if (index == 1) {
            dropdownCategorieSocioPro = waitForElement(driver, By.id("insured_category_select"), 15, 1);
        }
        if (dropdownCategorieSocioPro != null) {
            Select selectCategorieSocioPro = new Select(dropdownCategorieSocioPro);
            List<WebElement> options = selectCategorieSocioPro.getOptions();
            for (WebElement opt : options) {
                if (opt.getText().contains("Agriculteurs exploitants") && flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Agriculteur")) {
                    selectCategorieSocioPro.selectByVisibleText("Agriculteurs exploitants");
                } else if (opt.getText().contains("Artisans") && flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Artisan")) {
                    selectCategorieSocioPro.selectByVisibleText("Artisans");
                } else if (opt.getText().contains("Cadres") && flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Salarié cadre")) {
                    selectCategorieSocioPro.selectByVisibleText("Cadres");
                } else if (opt.getText().contains("Cadres et employés de la fonction publique") && flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Salarié non cadre : employé")) {
                    selectCategorieSocioPro.selectByVisibleText("Cadres et employés de la fonction publique");
                }
//                else if (opt.getText().contains("Chefs d'entreprise") && flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Rachat crédit")) {
//                    selectCategorieSocioPro.selectByVisibleText("Rachat crédit");
//                }
                else if (opt.getText().contains("Commerçants et assimilés") && flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Commerçant")) {
                    selectCategorieSocioPro.selectByVisibleText("Commerçants et assimilés");
                }
//                else if (opt.getText().contains("Employés, agents de maitrise") && flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Rachat crédit")) {
//                    selectCategorieSocioPro.selectByVisibleText("Rachat crédit");
//                }
                else if (opt.getText().contains("Ouvriers") && flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Ouvrier")) {
                    selectCategorieSocioPro.selectByVisibleText("Ouvriers");
                } else if (opt.getText().contains("Professions libérales et assimilés") && flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Profession libérale")) {
                    selectCategorieSocioPro.selectByVisibleText("Professions libérales et assimilés");
                }
            }
        }
    }

    private void recherche() {
        WebElement radioCivilite = waitForElement(driver, By.xpath("//button[normalize-space(text())='Découvrir les offres']"), 15, 1);
        radioCivilite.click();
    }

    @Override
    protected void pageName(String namePage) {}
}
