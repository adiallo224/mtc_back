package com.mtc.mutuaConseil.services.servicesImpl.assuMutuelPro;


import com.mtc.mutuaConseil.models.Compte;
import com.mtc.mutuaConseil.models.FluxData;
import com.mtc.mutuaConseil.models.Tarif;
import com.mtc.mutuaConseil.services.LaunchedService;
import com.mtc.mutuaConseil.services.servicesImpl.TypeAssuranceService;
import com.mtc.mutuaConseil.utils.PageElementInteraction;
import com.mtc.mutuaConseil.utils.TarifUtils;
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
public class RepamMutuelProService extends PageElementInteraction implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(RepamMutuelProService.class);
    public static WebDriver driver;
    private final String source = "RepamMutuelProService";
    private static Actions actions;
    private JavascriptExecutor js = null;
    private final TypeAssuranceService typeAssuranceService;

    public RepamMutuelProService(TypeAssuranceService typeAssuranceService){
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        Tarif repamMutuelPro = TarifUtils.createDefaultTarif(c, typeAssuranceService, 3L);
        driver = new EdgeDriver();
        driver.manage().window().maximize();
        actions = new Actions(driver);
        js = (JavascriptExecutor) driver;
        try {
            connexion(c);
            remplirClient(flux);
            waitThread(1);
            WebElement sectionPrix = waitForElement(driver, By.xpath("/html/body/div/div/div[3]/form/div/div[2]/div/div[2]/div[1]/div/div[3]"), 25, 1);
            String cout = getPrix(sectionPrix.getText()) + " €";
            log.info("cout {}", cout);
            repamMutuelPro.setMontant(cout);
            String screenshotBytes = captureScreenshot(driver, repamMutuelPro.getNom(), false, repamMutuelPro);
            if (screenshotBytes != null) {
                repamMutuelPro.setCaptureImg(screenshotBytes);
            }
            repamMutuelPro.setExecution(true);
        } catch (Exception e) {
            log.error("An error occurred", e);
            repamMutuelPro.setErreur(e.getMessage());
            String screenshotBytesErreur = captureScreenshot(driver, repamMutuelPro.getNom(), true, repamMutuelPro);
            repamMutuelPro.setCaptureImgErreur(screenshotBytesErreur);
            repamMutuelPro.setEtape("");
        } finally {
            driver.quit();
        }
        return repamMutuelPro;
    }

    public void connexion(Compte c) {
        driver.get(c.getUrlFournisseur());
        WebElement inputlogin = waitForElement(driver, By.xpath("//input[@placeholder=\"Identifiant\"]"), 10, 1);
        inputlogin.sendKeys(c.getUsername());
        WebElement inputPassword = waitForElement(driver, By.xpath("//input[@placeholder=\"Mot de passe\"]"), 10, 1);
        inputPassword.sendKeys(c.getPassword());
        clicButtonByXpath(driver, "//form[@name=\"loginForm\"]//button", actions);
        waitThread(2);
        WebElement buttonSouscription = waitForElement(driver, By.xpath("//*[@id=\"wrapper\"]/div[1]/div[2]/div/button"), 10, 1);
        buttonSouscription.click();
        waitThread(2);
        WebElement liSanteProfessionnelle = waitForElement(driver, By.xpath("//*[@id=\"wrapper\"]/div[1]/div[2]/div/ul/li[2]"), 10, 1);
        liSanteProfessionnelle.click();
        //Debut debranchement vers la nouvelle page
        waitThread(2);
        switchPage(driver);
        WebElement buttonProposition = waitForElement(driver, By.xpath("/html/body/div/div/div[2]/form/div[1]/button"), 10, 1);
        buttonProposition.click();
        waitThread(1);
        WebElement divSelectionOffre = waitForElement(driver, By.xpath("/html/body/div/div/div[2]/div/div[2]/div[2]/div/div[4]/button"), 10, 1);
        divSelectionOffre.click();
        waitThread(1);
        WebElement buttonRecueilRepam = waitForElement(driver, By.xpath("/html/body/div/div/div[2]/div/div[1]/div/div[4]/button[1]/div"), 10, 1);
        buttonRecueilRepam.click();
    }

    private void remplirClient(FluxData flux) {
        waitThread(1);
        WebElement inputDateEffet = waitForElement(driver, By.xpath("//input[@placeholder='JJ/MM/AAAA']"), 10, 1);
        inputDateEffet.clear();
        inputDateEffet.sendKeys(dateEffet(1));
        waitThread(1);
        WebElement buttonContinuer = waitForElement(driver, By.xpath("/html/body/div/div/div[3]/form/div[1]/div[3]/button/div"), 10, 1);
        buttonContinuer.click();

        WebElement radioAdherent = waitForElement(driver, By.id("customer.isMember-1"), 10, 1);
        radioAdherent.click();
        WebElement inputNom = waitForElement(driver, By.name("customer.lastName"), 10, 1);
        inputNom.sendKeys(flux.getPersonnes().getFirst().getNom());
        WebElement inputPrenom = waitForElement(driver, By.name("customer.firstName"), 10, 1);
        inputPrenom.sendKeys(flux.getPersonnes().getFirst().getPrenom());
        WebElement inputDateNaissance = waitForElement(driver, By.xpath("//input[@placeholder='JJ/MM/AAAA']"), 10, 1);
        inputDateNaissance.sendKeys(flux.getPersonnes().getFirst().getDateNaissance());
        WebElement inputCodePostal = waitForElement(driver, By.name("customer.address.postCode"), 10, 1);
        inputCodePostal.sendKeys(flux.getPersonnes().getFirst().getCodePostal());

        choixProfession(flux);
        waitThread(2);
        WebElement buttonContinuerEnregistrer = waitForElement(driver, By.xpath("//div[normalize-space()='Enregistrer et continuer']"), 10, 1);
        buttonContinuerEnregistrer.click();
    }

    private void choixProfession(FluxData flux) {
        WebElement inputProfession = waitForElement(driver, By.xpath("/html/body/div/div/div[3]/form/div[2]/div[1]/div[4]/div[2]/div/div[1]"), 10, 1);
        waitThread(2);
        if (inputProfession != null) {
            if (inputProfession.isEnabled()) {
                System.out.println("ici");
                waitThread(8);
                inputProfession.click();
            } else {
                waitThread(2);
                inputProfession.click();
            }
        }

        List<WebElement> options = driver.findElements(By.cssSelector("div[class*='option']"));
        for (WebElement option : options) {
            if (flux.getPersonnes().getFirst().getProfessionSpecifique().contains("Artisan") && option.getText().equalsIgnoreCase("Artisan")) {
                option.click();
                break;
            }
            if (flux.getPersonnes().getFirst().getProfessionSpecifique().contains("entreprise") && option.getText().equalsIgnoreCase("Chef d’entreprise")) {
                option.click();
                break;
            }
            if (flux.getPersonnes().getFirst().getProfessionSpecifique().contains("Agriculteur") && option.getText().equalsIgnoreCase("Exploitant Agricole")) {
                option.click();
                break;
            }
            if (flux.getPersonnes().getFirst().getProfessionSpecifique().contains("Salarié cadre") && option.getText().equalsIgnoreCase("Cadre")) {
                option.click();
                break;
            }
            if (flux.getPersonnes().getFirst().getProfessionSpecifique().contains("Commerçant") && option.getText().equalsIgnoreCase("Commerçant")) {
                option.click();
                break;
            }
            if (flux.getPersonnes().getFirst().getProfessionSpecifique().contains("Salarié non cadre : employé") && option.getText().equalsIgnoreCase("Cadre et employé de la fonction publique")) {
                option.click();
                break;
            }
            if (flux.getPersonnes().getFirst().getProfession().contains("Ouvrier") && option.getText().equalsIgnoreCase("Ouvrier")) {
                option.click();
                break;
            }
        }
    }

    @Override
    protected void pageName(String namePage) {}
}
