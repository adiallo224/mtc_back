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
public class ApiviaMutuelIndivService extends PageElementInteraction implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(AlptisMutuelIndivService.class);
    public static WebDriver driver;
    private final String source = "AlptisMutuelIndivService";
    private static Actions actions; private final TypeAssuranceService typeAssuranceService;

    public ApiviaMutuelIndivService(TypeAssuranceService typeAssuranceService){
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        Tarif tarifApiviaMutuelIndiv = TarifUtils.createDefaultTarif(c, typeAssuranceService, 2L);;
        tarifApiviaMutuelIndiv.setNom(c.getNomFournisseur());
        driver = new EdgeDriver();
        driver.manage().window().maximize();
        actions = new Actions(driver);
        try {
            connexion(c);
            remplirTarificateur();
            remplirChoixDevis();
            remplirContrat(flux);
            calculer();
            waitThread(5);
            scrollDown(driver, 0, 800);
            waitThread(1);
            WebElement sectionPrix = waitForElement(driver, By.xpath("/html/body/center/table/tbody/tr/td[2]/div/div[2]/form/div[4]/div[2]/div/table/tbody/tr[8]/td[4]"), 10, 1);
            String cout = sectionPrix.getText().substring(0, 8);
            log.info("cout {}", cout);
            tarifApiviaMutuelIndiv.setMontant(cout);
            String screenshotBytes = captureScreenshot(driver, tarifApiviaMutuelIndiv.getNom(), false, tarifApiviaMutuelIndiv);
            if (screenshotBytes != null) {
                tarifApiviaMutuelIndiv.setCaptureImg(screenshotBytes);
            }
            tarifApiviaMutuelIndiv.setExecution(true);
        } catch (Exception e) {
            log.error("An error occurred", e);
            tarifApiviaMutuelIndiv.setErreur(e.getMessage());
            String screenshotBytesErreur = captureScreenshot(driver, tarifApiviaMutuelIndiv.getNom(), true, tarifApiviaMutuelIndiv);
            tarifApiviaMutuelIndiv.setCaptureImgErreur(screenshotBytesErreur);
            tarifApiviaMutuelIndiv.setEtape("");
        } finally {
            driver.quit();
        }
        return tarifApiviaMutuelIndiv;
    }

    public void connexion(Compte c) {
        driver.get(c.getUrlFournisseur());
        waitThread(2);
        inputById(driver, "identifiant", c.getUsername());
        inputById(driver, "mot_passe", c.getPassword());
        WebElement buttonAccesIntranet = waitForElement(driver, By.xpath("//*[@id=\"form_extranet\"]/div/div/div[5]/button"), 10, 2);
        buttonAccesIntranet.click();
        waitThread(5);
    }

    private void remplirTarificateur() {
        WebElement buttonDevis = waitForElement(driver, By.xpath("//a[normalize-space()='TARIFICATEUR']"), 10, 2);
        buttonDevis.click();
        waitThread(2);
    }

    private void remplirChoixDevis() {
        WebElement buttonChoixDevis = waitForElement(driver, By.xpath("//a[normalize-space()='SANTE INDIVIDUELLE']"), 10, 1);
        actions.moveToElement(buttonChoixDevis).click().perform();
        switchPage(driver);
        waitThread(3);
    }

    private void remplirContrat(FluxData flux) {
        waitThread(2);
        WebElement linkNouveauTarif = waitForElement(driver, By.xpath("//a[normalize-space()='Nouveau Tarif']"), 10, 1);
        linkNouveauTarif.click();
        waitThread(3);
        WebElement inputCodePostal = waitForElement(driver, By.id("assures_PRINCIPAL_adresse_codePostal"), 10, 1);
        inputCodePostal.sendKeys(flux.getPersonnes().get(0).getCodePostal());
        waitThread(1);
        WebElement inputDateEffet = waitForElement(driver, By.id("dateEffet"), 10, 1);
        inputDateEffet.sendKeys(dateEffet(1));
        clickEnter(actions);
        waitThread(1);
        choixAnneeNaissance(flux, 0);
        waitThread(1);
        choixRegime(flux, 0);
        waitThread(1);
        remplirConjoint(flux);
        remplirEnfants(flux);
        scrollDown(driver, 0, 300);
        WebElement buttonRecueil = waitForElement(driver, By.xpath("//div//label[normalize-space()='Vous disposez de votre propre recueil des besoins']"), 10, 1);
        buttonRecueil.click();
        WebElement buttonOffres = waitForElement(driver, By.xpath("//div//label[normalize-space()='Vitamin3']"), 10, 1);
        buttonOffres.click();
    }

    private void remplirConjoint(FluxData flux) {
        waitThread(1);
        if (flux.getPersonnes().size() == 2) {
            choixAnneeNaissance(flux, 1);
            choixRegime(flux, 1);
        }
    }

    private void remplirEnfants(FluxData flux) {
        if (flux.getEnfants().get(0).getNom() != null && !flux.getEnfants().get(0).getNom().isEmpty()) {
            waitThread(1);
            ajoutBeneficiaire();
            choixAnneeNaissanceEnfant(flux, 0);
            choixRegimeEnfants(flux, 0);
        }
        if (flux.getEnfants().size() >= 2) {
            waitThread(1);
            ajoutBeneficiaire();
            choixAnneeNaissanceEnfant(flux, 1);
            choixRegimeEnfants(flux, 1);
        }
    }

    private void ajoutBeneficiaire() {
        WebElement buttonAjoutBeneficiaire = waitForElement(driver, By.id("add-child"), 10, 1);
        buttonAjoutBeneficiaire.click();
    }

    private void choixAnneeNaissance(FluxData flux, int index) {
        waitThread(1);
        WebElement selectOption = null;
        String annee = null;
        if (index == 0) {
            annee = extractYear(flux.getPersonnes().get(index).getDateNaissance());
            selectOption = waitForElement(driver, By.id("assures_PRINCIPAL_dateDeNaissance"), 10, 1);
        } if (index == 1) {
              selectOption = waitForElement(driver, By.id("assures_CONJOINT_dateDeNaissance"), 10, 1);
              annee = extractYear(flux.getPersonnes().get(index).getDateNaissance());
        }
        List<WebElement> items = selectOption.findElements(By.tagName("option"));
        for (WebElement element : items) {
            if (element.getText().equalsIgnoreCase(annee)) {
                element.click();
                break;
            }
        }
    }

    private void choixAnneeNaissanceEnfant(FluxData flux, int index) {
        waitThread(1);
        WebElement selectOption = null;
        String annee = null;
        if (index == 0) {
            annee = extractYear(flux.getEnfants().get(index).getDateNaissance());
            selectOption = waitForElement(driver, By.xpath("//*[@id=\"children-container\"]/div/div/div/div[1]/div/div/div/select"), 10, 1);
        } if (index == 1) {
            selectOption = waitForElement(driver, By.xpath("//*[@id=\"children-container\"]/div[2]/div/div/div[1]/div/div/div/select"), 10, 1);
            annee = extractYear(flux.getEnfants().get(index).getDateNaissance());
        }
        List<WebElement> items = selectOption.findElements(By.tagName("option"));
        for (WebElement element : items) {
            if (element.getText().equalsIgnoreCase(annee)) {
                element.click();
                break;
            }
        }
    }

    private void choixRegime(FluxData flux, int index) {
        waitThread(1);
        WebElement selectOption = null;
        if (index == 0) {
            selectOption = waitForElement(driver, By.id("assures_PRINCIPAL_regimeSocial"), 10, 1);
        } if (index == 1) {
              selectOption = waitForElement(driver, By.id("assures_CONJOINT_regimeSocial"), 10, 1);
        }
        List<WebElement> items = selectOption.findElements(By.tagName("option"));
        for (WebElement element : items) {
            if (element.getText().equalsIgnoreCase("Assuré social")) {
                element.click();
                break;
            }
        }
    }

    private void choixRegimeEnfants(FluxData flux, int index) {
        waitThread(1);
        WebElement selectOption = null;
        if (index == 0) {
            selectOption = waitForElement(driver, By.xpath("//*[@id=\"children-container\"]/div/div/div/div[3]/div/div/select"), 10, 1);
        } if (index == 1) {
            selectOption = waitForElement(driver, By.xpath("//*[@id=\"children-container\"]/div[2]/div/div/div[3]/div/div/select"), 10, 1);
        }
        List<WebElement> items = selectOption.findElements(By.tagName("option"));
        for (WebElement element : items) {
            if (element.getText().equalsIgnoreCase("Assuré social")) {
                element.click();
                break;
            }
        }
    }

    private void calculer() {
        waitThread(1);
        WebElement buttonSuivant = waitForElement(driver, By.xpath("//button[normalize-space()='Calculer']"), 10, 1);
        buttonSuivant.click();
    }

    @Override
    protected void pageName(String namePage) {}
}
