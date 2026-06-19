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

@Service
public class SmisoMutuelProService extends PageElementInteraction implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(SmisoMutuelProService.class);
    public static WebDriver driver;
    private final String source = "SmisoMutuelProService";
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
            remplirSaisieInformationsAdherant(flux);
            scrollDown(driver, 0, 250);
            waitThread(2);
            WebElement totalElement = driver.findElement(By.xpath("//td[@class='prixCotisationSansOption formule_2']/span"));
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
        InformationsUser.infoLogin(driver, By.id("login"), 10, 1, c.getUsername());
        InformationsUser.infoPassword(driver, By.id("pwd"), 10, 1, c.getPassword());
        waitThread(1);
        WebElement buttonLogin = waitForElement1(driver, By.id("authentificateSubmit"), 10, 1);
        buttonLogin.click();
    }

    private void remplirSaisieInformationsAdherant(FluxData flux) {
        // Contrat
        WebElement inputDateEffet = waitForElement(driver, By.id("DateEffetSouhaitee"), 10, 1);
        inputDateEffet.sendKeys(dateEffet(1));
        // Identité
        int index = 0;
        choixCivilite(flux, index, "//span//a[@id='CiviliteSouscripteur-button']", "CiviliteSouscripteur");
        WebElement inputNom = waitForElement(driver, By.id("NomSouscripteur"), 10, 1);
        inputNom.sendKeys(flux.getPersonnes().get(index).getNom());
        WebElement inputPrenom = waitForElement(driver, By.id("PrenomSouscripteur"), 10, 1);
        inputPrenom.sendKeys(flux.getPersonnes().get(index).getPrenom());
        WebElement inputDateNais = waitForElement(driver, By.id("DateNaissanceSouscripteur"), 10, 1);
        inputDateNais.sendKeys(flux.getPersonnes().get(index).getDateNaissance());
        choixRegime(flux, index, "//span//a[@id='RegimeSouscripteur-button']");
        // Coordonnées
        waitThread(1);
        WebElement inputAdresse = waitForElement(driver, By.id("AdresseSouscripteur"), 10, 1);
        inputAdresse.sendKeys(flux.getPersonnes().get(index).getNumeroVoie() + " " + flux.getPersonnes().get(index).getNomVoie());
        WebElement inputCodePostal = waitForElement(driver, By.id("CodePostalSouscripteur"), 10, 1);
        inputCodePostal.sendKeys(flux.getPersonnes().get(index).getCodePostal());
        WebElement inputVille = waitForElement(driver, By.id("VilleSouscripteur"), 10, 1);
        inputVille.sendKeys(flux.getPersonnes().get(index).getVille());
        // Couverture actuelle
        WebElement radioCouverture = waitForElement(driver, By.id("RiaIndicateurCouverture-false"), 10, 1);
        radioCouverture.click();

        if (flux.getPersonnes().size() == 2) {
            index = 1;
            WebElement buttonAjoutBeneficiaire = waitForElement(driver, By.xpath("ajoutGroupeBeneficiaireInfoSouscripteur"), 10, 1);
            buttonAjoutBeneficiaire.click();
            choixLienParente(flux, "conjoint", "//span//a[@id='Beneficiaire_0_LienParente-button']");
            choixRegime(flux, index, "//span//a[@id='Beneficiaire_0_Regime-button']");
            WebElement inputNomConjoint = waitForElement(driver, By.id("Beneficiaire_0_Nom"), 10, 1);
            inputNomConjoint.sendKeys(flux.getPersonnes().get(index).getNom());
            WebElement inputPrenomConjoint = waitForElement(driver, By.id("Beneficiaire_0_Prenom"), 10, 1);
            inputPrenomConjoint.sendKeys(flux.getPersonnes().get(index).getPrenom());
            WebElement inputDateNaisConjoint = waitForElement(driver, By.id("Beneficiaire_0_DateNaissance"), 10, 1);
            inputDateNaisConjoint.sendKeys(flux.getPersonnes().get(index).getDateNaissance());
        }

        if (!flux.getEnfants().get(0).getNom().isEmpty()) {
            WebElement buttonAjoutBeneficiaire = waitForElement(driver, By.xpath("ajoutGroupeBeneficiaireInfoSouscripteur"), 10, 1);
            buttonAjoutBeneficiaire.click();
            choixLienParente(flux, "enfant", "//span//a[@id='Beneficiaire_1_LienParente-button']");
            choixRegime(flux, index, "//span//a[@id='Beneficiaire_1_Regime-button']");
            WebElement inputNomConjoint = waitForElement(driver, By.id("Beneficiaire_1_Nom"), 10, 1);
            inputNomConjoint.sendKeys(flux.getEnfants().get(0).getNom());
            WebElement inputPrenomConjoint = waitForElement(driver, By.id("Beneficiaire_1_Prenom"), 10, 1);
            inputPrenomConjoint.sendKeys(flux.getEnfants().get(0).getPrenom());
            WebElement inputDateNaisConjoint = waitForElement(driver, By.id("Beneficiaire_1_DateNaissance"), 10, 1);
            inputDateNaisConjoint.sendKeys(flux.getEnfants().get(0).getDateNaissance());
            if (flux.getEnfants().size() >= 2) {
                WebElement buttonAjoutBeneficiaire1 = waitForElement(driver, By.xpath("ajoutGroupeBeneficiaireInfoSouscripteur"), 10, 1);
                buttonAjoutBeneficiaire1.click();
                choixLienParente(flux, "enfant", "//span//a[@id='Beneficiaire_2_LienParente-button']");
                choixRegime(flux, index, "//span//a[@id='Beneficiaire_2_Regime-button']");
                WebElement inputNomConjoint1 = waitForElement(driver, By.id("Beneficiaire_1_Nom"), 10, 1);
                inputNomConjoint1.sendKeys(flux.getEnfants().get(0).getNom());
                WebElement inputPrenomConjoint1 = waitForElement(driver, By.id("Beneficiaire_2_Prenom"), 10, 1);
                inputPrenomConjoint1.sendKeys(flux.getEnfants().get(0).getPrenom());
                WebElement inputDateNaisConjoint1 = waitForElement(driver, By.id("Beneficiaire_2_DateNaissance"), 10, 1);
                inputDateNaisConjoint1.sendKeys(flux.getEnfants().get(0).getDateNaissance());
            }
            if (flux.getEnfants().size() >= 3 ) {

            }
        }

        waitThread(1);
        WebElement buttonNext = waitForElement(driver, By.id("next"), 10, 1);
        buttonNext.click();
    }

    private void choixLienParente(FluxData flux, String typeBeneficiaire, String elt) {
        WebElement buttonCivilite = null;
        buttonCivilite = waitForElement(driver, By.id(elt), 10, 1);
        if (buttonCivilite != null) {
            buttonCivilite.click();
            waitThread(1);
            if (typeBeneficiaire.equalsIgnoreCase("conjoint")) {
                WebElement spanCivilite = waitForElement(driver, By.xpath("//a[contains(text(), 'Conjoint')]"), 10, 1);
                spanCivilite.click();
            }
            if (typeBeneficiaire.equalsIgnoreCase("enfant")) {
                WebElement spanCivilite = waitForElement(driver, By.xpath("//a[contains(text(), 'Enfant')]"), 10, 1);
                spanCivilite.click();
            }
        }
    }

    private void choixCivilite(FluxData flux, int index, String locatorBtnSelect, String locatorSelect) {
        WebElement dropdownCivilite = null;
        dropdownCivilite = waitForElement(driver, By.xpath(locatorBtnSelect), 10, 1);
        if (dropdownCivilite != null) {
            dropdownCivilite.click();
//            waitThread(1);
            if (index == 0) {
                if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur")) {
                    WebElement optionMonsieur = driver.findElement(By.xpath("//a[contains(text(), 'Monsieur')]"));
                    optionMonsieur.click();
                }
                if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame")) {
                    WebElement optionMadame = driver.findElement(By.xpath("//a[contains(text(), 'Madame')]"));
                    optionMadame.click();
                }
            }
            if (index == 1) {
                if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur")) {
                    WebElement optionMonsieur = driver.findElement(By.xpath("//a[contains(text(), 'Monsieur')]"));
                    optionMonsieur.click();
                }
                if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame")) {
                    WebElement optionMadame = driver.findElement(By.xpath("//a[contains(text(), 'Madame')]"));
                    optionMadame.click();
                }
            }
        }
    }

    private void choixRegime(FluxData flux, int index, String elt) {
        WebElement buttonRegime = null;
        buttonRegime = waitForElement(driver, By.xpath(elt), 10, 1);
        if (buttonRegime != null) {
            buttonRegime.click();
//            waitThread(1);
            WebElement optionTNS = driver.findElement(By.xpath("//a[contains(text(), 'TNS')]"));
            optionTNS.click();
        }
    }

    @Override
    protected void pageName(String namePage) {}
}
