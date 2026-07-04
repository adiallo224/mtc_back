package com.mtc.mutuaConseil.services.servicesImpl.assuMutuelIndiv;

import com.mtc.mutuaConseil.models.Compte;
import com.mtc.mutuaConseil.models.FluxData;
import com.mtc.mutuaConseil.models.Tarif;
import com.mtc.mutuaConseil.services.LaunchedService;
import com.mtc.mutuaConseil.services.servicesImpl.TypeAssuranceService;
import com.mtc.mutuaConseil.utils.InformationsPersonne;
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
public class ApicilMIService extends PageElementInteraction implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(ApicilMIService.class);
    public static WebDriver driver;
    private final String source = "ApicilMutuelIndivService";
    private static Actions actions;
    private final TypeAssuranceService typeAssuranceService;

    public ApicilMIService(TypeAssuranceService typeAssuranceService){
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        Tarif tarifApicilMutuelIndiv = TarifUtils.createDefaultTarif(c, typeAssuranceService, 2L);;
        tarifApicilMutuelIndiv.setNom(c.getNomFournisseur());
        driver = new EdgeDriver();
        driver.manage().window().maximize();
        actions = new Actions(driver);
        try {
            connexion(c);
            remplirContrat(flux);
            remplirIdentite(flux);
            remplirCoordonnees(flux);
            remplirCouverture();
            scrollDown(driver, 0, 300);
            suivant();
            waitThread(7);
            WebElement sectionPrix = waitForElement(driver, By.xpath("//*[@id='formInfoChoixCotisation']/div/table/tbody/tr[2]/td[4]"), 10, 1);
            String cout = sectionPrix.getText();
            log.info("cout {}", cout);
            tarifApicilMutuelIndiv.setMontant(cout);
            String screenshotBytes = captureScreenshot(driver, tarifApicilMutuelIndiv.getNom(), false, tarifApicilMutuelIndiv);
            if (screenshotBytes != null) {
                tarifApicilMutuelIndiv.setCaptureImg(screenshotBytes);
            }
            tarifApicilMutuelIndiv.setExecution(true);
        } catch (Exception e) {
            log.error("An error occurred", e);
            tarifApicilMutuelIndiv.setErreur(e.getMessage());
            String screenshotBytesErreur = captureScreenshot(driver, tarifApicilMutuelIndiv.getNom(), true, tarifApicilMutuelIndiv);
            tarifApicilMutuelIndiv.setCaptureImgErreur(screenshotBytesErreur);
            tarifApicilMutuelIndiv.setEtape("");
        } finally {
            driver.quit();
        }
        return tarifApicilMutuelIndiv;
    }

    public void connexion(Compte c) {
        driver.get(c.getUrlFournisseur());
        WebElement buttonCookies = waitForElement(driver, By.xpath("//div[@id=\"CybotCookiebotDialogBodyButtonsWrapper\"]//button[@id=\"CybotCookiebotDialogBodyLevelButtonLevelOptinAllowAll\"]"), 10, 1);
        buttonCookies.click();
        WebElement inputLogin = waitForElement(driver, By.xpath("//div[@id=\"sfdc_username_container\"]//input"), 10, 1);
        inputLogin.sendKeys(c.getUsername());
        WebElement inputPassword = waitForElement(driver, By.xpath("//div[@id=\"sfdc_password_container\"]//input"), 10, 1);
        inputPassword.sendKeys(c.getPassword());
        WebElement buttonLogin = waitForElement(driver, By.xpath("//div[@class=\"salesforceIdentityLoginForm2\"]//button"), 10, 1);
        buttonLogin.click();
    }

    private void remplirContrat(FluxData flux) {
        WebElement inputDateEffet = waitForElement(driver, By.id("DateEffetSouhaitee"), 10, 1);
        inputDateEffet.sendKeys(dateEffet(1));
    }

    private void remplirIdentite(FluxData flux) {
        choixCivilite(flux, 0);
        WebElement inputNom = waitForElement(driver, By.id("NomSouscripteur"), 10, 1);
        inputNom.sendKeys(flux.getPersonnes().get(0).getNom());
        WebElement inputPrenom = waitForElement(driver, By.id("PrenomSouscripteur"), 10, 1);
        inputPrenom.sendKeys(flux.getPersonnes().get(0).getPrenom());
        WebElement inputDateNaissanceSouscripteur = waitForElement(driver, By.id("DateNaissanceSouscripteur"), 10, 1);
        inputDateNaissanceSouscripteur.sendKeys(flux.getPersonnes().get(0).getDateNaissance());
        choixRegime(flux, 0);
    }

    private void remplirCoordonnees(FluxData flux) {
        WebElement inputAdresse = waitForElement(driver, By.id("AdresseSouscripteur"), 10, 1);
        inputAdresse.sendKeys(flux.getPersonnes().get(0).getNumeroVoie() + " " + flux.getPersonnes().get(0).getNomVoie());
        WebElement inputCodePostal = waitForElement(driver, By.id("CodePostalSouscripteur"), 10, 1);
        inputCodePostal.sendKeys(flux.getPersonnes().get(0).getCodePostal());
        WebElement inputVille = waitForElement(driver, By.id("VilleSouscripteur"), 10, 1);
        inputVille.sendKeys(flux.getPersonnes().get(0).getVille());
        WebElement inputEmail = waitForElement(driver, By.id("EmailSouscripteur"), 10, 1);
        inputEmail.sendKeys(flux.getPersonnes().get(0).getEmail());
        WebElement inputPortable = waitForElement(driver, By.id("PortableSouscripteur"), 10, 1);
        inputPortable.sendKeys(flux.getPersonnes().get(0).getTelephone());
    }

    private void remplirCouverture() {
        WebElement inputCouvertureActuelle = waitForElement(driver, By.id("RiaIndicateurCouverture-false"), 10, 1);
        inputCouvertureActuelle.click();
    }

    private void suivant() {
        WebElement buttonSuivant = waitForElement(driver, By.id("next"), 10, 1);
        buttonSuivant.click();
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
        waitThread(1);
        WebElement dropdownRegime = null;
        if (index == 0) {
            dropdownRegime = waitForElement(driver, By.xpath("//*[@id=\"RegimeSouscripteur-button\"]"), 10, 1);
            dropdownRegime.click();
        }
        if (dropdownRegime != null) {
            List<WebElement> options = driver.findElements(By.xpath("//*[contains(@id,'RegimeSouscripteur-menu')]//li"));
            options.get(1).click();
        }
    }

    private void choixCivilite(FluxData flux, int index) {
        waitThread(1);
        WebElement dropdownRegime = null;
        if (index == 0) {
            dropdownRegime = waitForElement(driver, By.xpath("//*[@id='CiviliteSouscripteur-button']"), 10, 1);
            dropdownRegime.click();
        }
        if (dropdownRegime != null) {
            List<WebElement> options = driver.findElements(By.xpath("//*[contains(@id,'CiviliteSouscripteur-menu')]//li"));
            if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M"))
                options.get(1).click();
            if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme"))
                options.get(2).click();
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

    @Override
    protected void pageName(String namePage) {}

}
