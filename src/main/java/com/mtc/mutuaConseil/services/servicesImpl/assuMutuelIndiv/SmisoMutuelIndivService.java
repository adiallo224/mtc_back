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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SmisoMutuelIndivService extends PageElementInteraction implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(SmisoMutuelIndivService.class);
    public static WebDriver driver;
    private final String source = "SmisoMutuelIndivService";
    private static Actions actions;
    private final TypeAssuranceService typeAssuranceService;

    public SmisoMutuelIndivService(TypeAssuranceService typeAssuranceService){
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        Tarif tarifSmisoMutuelIndiv = TarifUtils.createDefaultTarif(c, typeAssuranceService, 2L);;
        tarifSmisoMutuelIndiv.setNom(c.getNomFournisseur());
        driver = new EdgeDriver();
        driver.manage().window().maximize();
        actions = new Actions(driver);
        try {
            connexion(c);
            remplirContrat(flux);
            remplirIdentite(flux);
            remplirCoordonnees(flux);
            remplirCouverture();
            if (flux.getPersonnes().size() >= 2 || flux.getEnfants().get(0).getNom() != null && !flux.getEnfants().get(0).getNom().isEmpty()) {
                remplirBenficiaires(flux);
            }
            scrollDown(driver, 0, 300);
            suivant();
            waitThread(7);
            WebElement sectionPrix = waitForElement(driver, By.xpath("//*[@id='formInfoChoixCotisation']/div/table/tbody/tr[2]/td[4]"), 10, 1);
            String cout = sectionPrix.getText();
            log.info("cout {}", cout);
            tarifSmisoMutuelIndiv.setMontant(cout);
            String screenshotBytes = captureScreenshot(driver, tarifSmisoMutuelIndiv.getNom(), false, tarifSmisoMutuelIndiv);
            if (screenshotBytes != null) {
                tarifSmisoMutuelIndiv.setCaptureImg(screenshotBytes);
            }
            tarifSmisoMutuelIndiv.setExecution(true);
        } catch (Exception e) {
            log.error("An error occurred", e);
            tarifSmisoMutuelIndiv.setErreur(e.getMessage());
            String screenshotBytesErreur = captureScreenshot(driver, tarifSmisoMutuelIndiv.getNom(), true, tarifSmisoMutuelIndiv);
            tarifSmisoMutuelIndiv.setCaptureImgErreur(screenshotBytesErreur);
            tarifSmisoMutuelIndiv.setEtape("");
        } finally {
            driver.quit();
        }
        return tarifSmisoMutuelIndiv;
    }

    public void connexion(Compte c) {
        driver.get(c.getUrlFournisseur());
        WebElement buttonCookies = waitForElement(driver, By.xpath("//*[@id=\"bandeauAcceptationCookies\"]/div/div[2]/a[3]"), 10, 2);
        buttonCookies.click();
        WebElement inputLogin = waitForElement(driver, By.id("login"), 10, 2);
        inputLogin.sendKeys(c.getUsername());
        WebElement inputPassword = waitForElement(driver, By.id("pwd"), 10, 2);
        inputPassword.sendKeys(c.getPassword());
        waitThread(1);
        WebElement buttonLogin = waitForElement1(driver, By.id("authentificateSubmit"), 20, 2);
        buttonLogin.click();
        waitThread(1);
    }

    private void remplirContrat(FluxData flux) {
        WebElement inputDateEffet = waitForElement(driver, By.id("DateEffetSouhaitee"), 10, 1);
        inputDateEffet.sendKeys(dateEffet(1));
    }

    private void remplirIdentite(FluxData flux) {
        waitThread(1);
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
        waitThread(1);
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

    private void remplirBenficiaires(FluxData flux) {
        waitThread(1);
        if (flux.getPersonnes().size() >= 2) {
            scrollDown(driver, 0, 200);
            ajoutSouscripteur();
            waitThread(1);
            choixLienParente(flux, "//*[@id='Beneficiaire_0_LienParente-button']", "//*[contains(@id,'Beneficiaire_0_LienParente-menu')]//li", "conjoint");
            choixLienCiviliteBeneficiaire(flux, "//*[@id='Beneficiaire_0_Civilite-button']", "//*[contains(@id,'Beneficiaire_0_Civilite-menu')]//li", "conjoint", 0);
            WebElement inputNomConjoint = waitForElement(driver, By.id("Beneficiaire_0_Nom"), 10, 1);
            inputNomConjoint.sendKeys(flux.getPersonnes().get(1).getNom());
            WebElement inputPrenomConjoint = waitForElement(driver, By.id("Beneficiaire_0_Prenom"), 10, 1);
            inputPrenomConjoint.sendKeys(flux.getPersonnes().get(1).getPrenom());
            WebElement inputDateNaissanceSouscripteurConjoint = waitForElement(driver, By.id("Beneficiaire_0_DateNaissance"), 10, 1);
            inputDateNaissanceSouscripteurConjoint.sendKeys(flux.getPersonnes().get(1).getDateNaissance());
            choixRegimeBeneficiaires(flux, "//*[@id='Beneficiaire_0_Regime-button']", "//*[contains(@id,'Beneficiaire_0_Regime-menu')]//li");
        }
        if (flux.getEnfants().get(0).getNom() != null && !flux.getEnfants().get(0).getNom().isEmpty()) {
            ajoutSouscripteur();
            waitThread(1);
            choixLienParente(flux, "//*[@id='Beneficiaire_1_LienParente-button']", "//*[contains(@id,'Beneficiaire_1_LienParente-menu')]//li", "enfant");
            choixLienCiviliteBeneficiaire(flux, "//*[@id='Beneficiaire_1_Civilite-button']", "//*[contains(@id,'Beneficiaire_1_Civilite-menu')]//li", "enfant", 0);
            WebElement inputNomEnfant = waitForElement(driver, By.id("Beneficiaire_1_Nom"), 10, 1);
            inputNomEnfant.sendKeys(flux.getEnfants().get(0).getNom());
            WebElement inputPrenomEnfant = waitForElement(driver, By.id("Beneficiaire_1_Prenom"), 10, 1);
            inputPrenomEnfant.sendKeys(flux.getEnfants().get(0).getPrenom());
            WebElement inputDateNaissanceSouscripteurEnfant = waitForElement(driver, By.id("Beneficiaire_1_DateNaissance"), 10, 1);
            inputDateNaissanceSouscripteurEnfant.sendKeys(flux.getEnfants().get(0).getDateNaissance());
            choixRegimeBeneficiaires(flux, "//*[@id='Beneficiaire_1_Regime-button']", "//*[contains(@id,'Beneficiaire_1_Regime-menu')]//li");
        }
        if (flux.getEnfants().size() == 2) {
            ajoutSouscripteur();
            waitThread(1);
            choixLienParente(flux, "//*[@id='Beneficiaire_2_LienParente-button']", "//*[contains(@id,'Beneficiaire_2_LienParente-menu')]//li", "enfant");
            choixLienCiviliteBeneficiaire(flux, "//*[@id='Beneficiaire_2_Civilite-button']", "//*[contains(@id,'Beneficiaire_2_Civilite-menu')]//li", "enfant", 1);
            WebElement inputNomEnfant2 = waitForElement(driver, By.id("Beneficiaire_2_Nom"), 10, 1);
            inputNomEnfant2.sendKeys(flux.getEnfants().get(1).getNom());
            WebElement inputPrenomEnfant2 = waitForElement(driver, By.id("Beneficiaire_2_Prenom"), 10, 1);
            inputPrenomEnfant2.sendKeys(flux.getEnfants().get(1).getPrenom());
            WebElement inputDateNaissanceSouscripteurEnfant2 = waitForElement(driver, By.id("Beneficiaire_2_DateNaissance"), 10, 1);
            inputDateNaissanceSouscripteurEnfant2.sendKeys(flux.getEnfants().get(1).getDateNaissance());
            choixRegimeBeneficiaires(flux, "//*[@id='Beneficiaire_2_Regime-button']", "//*[contains(@id,'Beneficiaire_2_Regime-menu')]//li");
        }

    }

    private void suivant() {
        WebElement buttonSuivant = waitForElement(driver, By.id("next"), 10, 1);
        buttonSuivant.click();
    }

    private void ajoutSouscripteur() {
        WebElement buttonSuivant = waitForElement(driver, By.id("ajoutGroupeBeneficiaireInfoSouscripteur"), 10, 1);
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
        WebElement dropdownCivilite = null;
        if (index == 0) {
            dropdownCivilite = waitForElement(driver, By.xpath("//*[@id='CiviliteSouscripteur-button']"), 10, 1);
            dropdownCivilite.click();
        }
        if (dropdownCivilite != null) {
            List<WebElement> options = driver.findElements(By.xpath("//*[contains(@id,'CiviliteSouscripteur-menu')]//li"));
            if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M"))
                options.get(1).click();
            if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme"))
                options.get(2).click();
        }
    }

    private void choixLienParente(FluxData flux, String pathDropdown, String listLi, String lien) {
        waitThread(1);
        WebElement dropdownLienParente = waitForElement(driver, By.xpath(pathDropdown), 10, 1);
        dropdownLienParente.click();
        List<WebElement> options = driver.findElements(By.xpath(listLi));
        if (lien.equalsIgnoreCase("conjoint"))
            options.get(1).click();
        if (lien.equalsIgnoreCase("enfant"))
            options.get(2).click();

    }

    private void choixLienCiviliteBeneficiaire(FluxData flux, String pathDropdown, String listLi, String lien, int indexEnfant) {
        waitThread(1);
        WebElement dropdownCiviliteBeneficiaire = waitForElement(driver, By.xpath(pathDropdown), 10, 1);
        dropdownCiviliteBeneficiaire.click();
        List<WebElement> options = driver.findElements(By.xpath(listLi));
        if (lien.equalsIgnoreCase("conjoint"))
            options.get(1).click();
        if (lien.equalsIgnoreCase("enfant")) {
            if (flux.getEnfants().get(indexEnfant).getCivilite().equalsIgnoreCase("Monsieur"))
                options.get(1).click();
            else
                options.get(2).click();
        }

    }

    private void choixRegimeBeneficiaires(FluxData flux, String pathDropdown, String listLi) {
        WebElement dropdownCiviliteBeneficiaire = waitForElement(driver, By.xpath(pathDropdown), 10, 1);
        dropdownCiviliteBeneficiaire.click();
        List<WebElement> options = driver.findElements(By.xpath(listLi));
        options.get(1).click();
    }

    @Override
    protected void pageName(String namePage) {}
}
