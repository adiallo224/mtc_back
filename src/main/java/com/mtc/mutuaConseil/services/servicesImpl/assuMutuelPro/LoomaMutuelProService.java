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
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LoomaMutuelProService extends PageElementInteraction implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(LoomaMutuelProService.class);
    public static WebDriver driver;
    private final String source = "LoomaMutuelProService";
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
            projet();
            profil(flux);
            garantieTarifs(flux);
            nextPage(driver, By.id("etape-suivante"), 10, 1);
            WebElement sectionPrix = waitForElement(driver, By.xpath("//table//tr//td[@class='niveaus niveau2 structures structure32']"), 25, 1);
            waitThread(1);
            String cout = sectionPrix.getText();
            log.info("cout {}", cout);
            tarif.setMontant(cout);
            waitThread(1);
            scrollDown(driver, 0, -400);
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

    private void connexion(Compte c) {
        driver.get(c.getUrlFournisseur());
//        WebElement buttonSeConnecter = waitForElement(driver, By.xpath("//button//span[normalize-space()='Se connecter']"), 10, 2);
//        buttonSeConnecter.click();
//        WebElement spanEspaceCourtier = waitForElement(driver, By.xpath("//span[normalize-space()='Espace courtiers']"), 10, 2);
//        spanEspaceCourtier.click();
        InformationsUser.infoLogin(driver, By.id("identifiant"), 10, 2, c.getUsername());
        InformationsUser.infoPassword(driver, By.id("password"), 10, 2, c.getPassword());
        waitThread(2);
        WebElement buttonLogin = waitForElement1(driver, By.xpath("//button[normalize-space()='Se connecter']"), 20, 2);
        buttonLogin.click();
    }

    private void projet() {
        waitThread(1);
        WebElement buttonProjet = waitForElement(driver, By.xpath("//span[normalize-space()='Projets']"), 10, 2);
        buttonProjet.click();
        waitThread(1);
        WebElement linkNouveauProjet = waitForElement(driver, By.xpath("//a[normalize-space()='Nouveau projet']"), 10, 2);
        linkNouveauProjet.click();
        waitThread(1);
        WebElement imgProjet = waitForElement(driver, By.xpath("//img[@alt='TNS']"), 10, 2);
        imgProjet.click();
    }

    private void profil(FluxData flux) {
        waitThread(1);
        WebElement inputNom = waitForElement(driver, By.id("nom"), 10, 2);
        inputNom.sendKeys(flux.getPersonnes().get(0).getNom());
        WebElement inputPrenom = waitForElement(driver, By.id("prenom"), 10, 2);
        inputPrenom.sendKeys(flux.getPersonnes().get(0).getPrenom());
        WebElement inputDateNaissance = waitForElement(driver, By.id("dns"), 10, 2);
        inputDateNaissance.sendKeys(flux.getPersonnes().get(0).getDateNaissance());
        waitThread(1);
        WebElement buttonValider = waitForElement(driver, By.id("profil-client"), 10, 2);
        buttonValider.click();
    }

    private void garantieTarifs(FluxData flux) {
        waitThread(1);
        // Données projet
        choixRisque(By.id("risque"));
        WebElement inputDateEffet = waitForElement(driver, By.id("date_effet"), 10, 2);
        inputDateEffet.sendKeys(dateEffet(1));
        elementNeutre(driver);
        // Données personnelles TNS
        choixSituation(flux, By.id("situation_id"));
        if (!flux.getEnfants().get(0).getNom().isEmpty()) {
            WebElement inputNombreEnfants = waitForElement(driver, By.xpath("//div[@class='block bt plus']"), 10, 2);
            for (int i = 0; i < flux.getEnfants().size(); i++ ) {
                 inputNombreEnfants.click();
            }
        }
        WebElement inputAdresse = waitForElement(driver, By.id("ad1"), 10, 2);
        inputAdresse.clear();
        inputAdresse.sendKeys(flux.getPersonnes().get(0).getNumeroVoie() + " " + flux.getPersonnes().get(0).getNomVoie());
        WebElement inputVille = waitForElement(driver, By.id("ville_nom"), 10, 2);
        waitThread(1);
        inputVille.clear();
        waitThread(1);
        inputVille.sendKeys(flux.getPersonnes().get(0).getVille());
        waitThread(1);
        WebElement ulListVille = waitForElement(driver, By.id("ui-id-2"), 10, 2);
        List<WebElement> suggestionsListVille = ulListVille.findElements(By.className("ui-menu-item-wrapper"));
        for (WebElement suggestion : suggestionsListVille) {
            if (suggestion.getText().contains(flux.getPersonnes().get(0).getCodePostal()) && suggestion.getText().contains(flux.getPersonnes().get(0).getVille().toUpperCase())) {
                suggestion.click();
                break;
            }
        }
        // Données professionnelles TNS
        choixStatut(flux, By.id("statut_id"));
        WebElement inputSiret = waitForElement(driver, By.id("soc_siret"), 10, 2);
        inputSiret.clear();
        inputSiret.sendKeys(flux.getEntreprise().getSiret());
        WebElement inputCodeNAF = waitForElement(driver, By.id("soc_naf_code"), 10, 2);
        inputCodeNAF.clear();
        inputCodeNAF.sendKeys(flux.getEntreprise().getCodeAPE());
        waitThread(1);
        WebElement ulListCodeNaf = waitForElement(driver, By.id("ui-id-3"), 10, 2);
        List<WebElement> suggestionsListCodeNaf = ulListCodeNaf.findElements(By.className("ui-menu-item-wrapper"));
        for (WebElement suggestion : suggestionsListCodeNaf) {
//            if (suggestion.getText().contains(flux.getEntreprise().getCodeAPE())) {
                suggestion.click();
                break;
//            }
        }
    }

    private void choixRisque(By locator) {
        waitThread(1);
        WebElement dropdownRisque = waitForElement(driver, locator, 10, 1);
        if (dropdownRisque != null) {
            Select selectRisque = new Select(dropdownRisque);
            selectRisque.selectByValue("2");
        }
    }

    private void choixSituation(FluxData flux, By locator) {
        waitThread(1);
        WebElement dropdownSituation = waitForElement(driver, locator, 10, 1);
        if (dropdownSituation != null) {
            Select selectSituation = new Select(dropdownSituation);
            if (flux.getPersonnes().size() == 1 ) {
                selectSituation.selectByValue("2");
            }
            if (flux.getPersonnes().size() == 2 ) {
                selectSituation.selectByValue("1");
            }
        }
    }

    private void choixStatut(FluxData flux, By locator) {
        waitThread(1);
        WebElement dropdownStatut = waitForElement(driver, locator, 10, 1);
        if (dropdownStatut != null) {
            Select selectStatut = new Select(dropdownStatut);
            if (flux.getPersonnes().get(0).getProfessionSpecifique().equalsIgnoreCase("Artisan")) {
                selectStatut.selectByValue("5");
            }
            if (flux.getPersonnes().get(0).getProfessionSpecifique().equalsIgnoreCase("Commercant")) {
                selectStatut.selectByValue("6");
            }
            if (flux.getPersonnes().get(0).getProfessionSpecifique().equalsIgnoreCase("Agriculteur")) {
                selectStatut.selectByValue("9");
            }
            if (flux.getPersonnes().get(0).getProfessionSpecifique().equalsIgnoreCase("Conjoint collaborateur")) {
                selectStatut.selectByValue("10");
            }
            if (flux.getPersonnes().get(0).getProfessionSpecifique().equalsIgnoreCase("Mandataire non rémunéré")) {
                selectStatut.selectByValue("7");
            }
            if (flux.getPersonnes().get(0).getProfessionSpecifique().equalsIgnoreCase("Profession libérale")) {
                selectStatut.selectByValue("3");
            }
            if (flux.getPersonnes().get(0).getProfessionSpecifique().equalsIgnoreCase("Travailleur indépendant")) {
                selectStatut.selectByValue("2");
            }
            if (flux.getPersonnes().get(0).getProfessionSpecifique().equalsIgnoreCase("Chef d'entreprise")) {
                selectStatut.selectByValue("1");
            }
            if (flux.getPersonnes().get(0).getProfessionSpecifique().equalsIgnoreCase("Auto Entrepreneur")) {
                selectStatut.selectByValue("4");
            }
        }
    }

    @Override
    protected void pageName(String namePage) {}
}
