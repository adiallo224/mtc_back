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
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AMI3FMutuelIndivService extends PageElementInteraction implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(AMI3FMutuelIndivService.class);
    public static WebDriver driver;
    private final String source = "AMI3FMutuelIndivService";
    private static Actions actions;
    private final TypeAssuranceService typeAssuranceService;

    public AMI3FMutuelIndivService(TypeAssuranceService typeAssuranceService){
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        Tarif tarifAMI3FMutuelIndiv = TarifUtils.createDefaultTarif(c, typeAssuranceService, 2L);;
        tarifAMI3FMutuelIndiv.setNom(c.getNomFournisseur());
        driver = new EdgeDriver();
        driver.manage().window().maximize();
        actions = new Actions(driver);
        try {
            connexion(c);
            choixTarification();
            remplirComplementaireSante(flux);
            waitThread(5);
            WebElement sectionPrix = waitForElement(driver, By.xpath("/html/body/div/div/div[3]/div[2]/form/div/div[1]/div[2]/div[5]/div[10]/div[4]/table/tbody/tr/td[2]/div[1]"), 10, 1);
            String cout = sectionPrix.getText();
            log.info("cout {}", cout);
            tarifAMI3FMutuelIndiv.setMontant(cout);
            String screenshotBytes = captureScreenshot(driver, tarifAMI3FMutuelIndiv.getNom(), false, tarifAMI3FMutuelIndiv);
            if (screenshotBytes != null) {
                tarifAMI3FMutuelIndiv.setCaptureImg(screenshotBytes);
            }
            tarifAMI3FMutuelIndiv.setExecution(true);
        } catch (Exception e) {
            log.error("An error occurred", e);
            tarifAMI3FMutuelIndiv.setErreur(e.getMessage());
            String screenshotBytesErreur = captureScreenshot(driver, tarifAMI3FMutuelIndiv.getNom(), true, tarifAMI3FMutuelIndiv);
            tarifAMI3FMutuelIndiv.setCaptureImgErreur(screenshotBytesErreur);
            tarifAMI3FMutuelIndiv.setEtape("");
        } finally {
            driver.quit();
        }
        return tarifAMI3FMutuelIndiv;
    }

    public void connexion(Compte c) {
        driver.get(c.getUrlFournisseur());
        WebElement inputLogin = waitForElement(driver, By.name("login"), 10, 1);
        inputLogin.sendKeys(c.getUsername());
        WebElement inputPassword = waitForElement(driver, By.name("password"), 10, 1);
        inputPassword.sendKeys(c.getPassword());
        waitThread(1);
        WebElement buttonLogin = waitForElement1(driver, By.xpath("//button[@type='submit']"), 10, 1);
        buttonLogin.click();
        waitThread(1);
    }

    private void choixTarification() {
        waitThread(2);
        WebElement linkParticulier = waitForElement(driver, By.xpath("//*[@id=\"navmenu-tarif\"]"), 10, 1);
        linkParticulier.click();
        WebElement linkComplementaire = waitForElement(driver, By.xpath("//span[normalize-space()='Complémentaire Santé']"), 10, 1);
        linkComplementaire.click();
        scrollDown(driver, 0, 450);
        waitThread(2);
        WebElement linkFaireDevis = waitForElement(driver, By.xpath("//span[normalize-space()='ACCÉDER A LA TARIFICATION']"), 10, 1);
        linkFaireDevis.click();
    }

    private void remplirComplementaireSante(FluxData flux) {
        WebElement inputNom = waitForElement(driver, By.id("nom_assure_1"), 10, 1);
        inputNom.sendKeys(flux.getPersonnes().get(0).getNom());
        WebElement inputPrenom = waitForElement(driver, By.id("prenom_assure_1"), 10, 1);
        inputPrenom.sendKeys(flux.getPersonnes().get(0).getPrenom());
        WebElement inputNomNaissance = waitForElement(driver, By.id("nom_naiss_assure_1"), 10, 1);
        inputNomNaissance.sendKeys(flux.getPersonnes().get(0).getNom());
        WebElement inputDateNaissance = waitForElement(driver, By.id("dt_naiss_assure_1"), 10, 1);
        inputDateNaissance.sendKeys(flux.getPersonnes().get(0).getDateNaissance());
        choixPays(flux, 0);
        WebElement inputCodePostal = waitForElement(driver, By.id("cp_naiss_assure_1"), 10, 1);
        inputCodePostal.sendKeys(flux.getPersonnes().get(0).getCodePostal());
        choixVille(flux, 0);
        choixSexe(flux, 0);
        choixSituationFamilliale(flux, 0);
        choixRegime(flux, 0);
        WebElement inputProfession = waitForElement(driver, By.id("profession_assure_1"), 10, 1);
        inputProfession.sendKeys(flux.getPersonnes().get(0).getProfession());
        WebElement inputFonctionPublic = waitForElement(driver, By.id("div_radio_is_ppe_assure_1_false"), 10, 1);
        inputFonctionPublic.click();
        WebElement inputPPE = waitForElement(driver, By.id("radio_ppe_famille_assure_1_false"), 10, 1);
        inputPPE.click();
        // Partie conjoint
        if (flux.getPersonnes().size() >= 2) {
            WebElement buttonAjouter = waitForElement(driver, By.id("btn-add-conjoint"), 10, 1);
            buttonAjouter.click();
            WebElement inputNom2 = waitForElement(driver, By.id("nom_assure_2"), 10, 1);
            inputNom2.sendKeys(flux.getPersonnes().get(1).getNom());
            WebElement inputPrenom2 = waitForElement(driver, By.id("prenom_assure_2"), 10, 1);
            inputPrenom2.sendKeys(flux.getPersonnes().get(1).getPrenom());
            WebElement inputNomNaissance2 = waitForElement(driver, By.id("nom_naiss_assure_2"), 10, 1);
            inputNomNaissance2.sendKeys(flux.getPersonnes().get(1).getNom());
            WebElement inputDateNaissance2 = waitForElement(driver, By.id("dt_naiss_assure_2"), 10, 1);
            inputDateNaissance2.sendKeys(flux.getPersonnes().get(1).getDateNaissance());
            choixPays(flux, 1);
            WebElement inputCodePostal2 = waitForElement(driver, By.id("cp_naiss_assure_2"), 10, 1);
            inputCodePostal2.sendKeys(flux.getPersonnes().get(1).getCodePostal());
            choixVille(flux, 1);
            choixSexe(flux, 1);
            choixRegime(flux, 1);
            WebElement inputProfession2 = waitForElement(driver, By.id("profession_assure_2"), 10, 1);
            inputProfession2.sendKeys(flux.getPersonnes().get(1).getProfession());
            WebElement inputFonctionPublic2 = waitForElement(driver, By.id("div_radio_is_ppe_assure_2_false"), 10, 1);
            inputFonctionPublic2.click();
            WebElement inputPPE2 = waitForElement(driver, By.id("radio_ppe_famille_assure_2_false"), 10, 1);
            inputPPE2.click();
            // Partie enfant
            if (flux.getEnfants().get(0).getNom() != null) {
                waitThread(1);
                WebElement buttonAjouterEnfant = waitForElement(driver, By.id("btn-add-enfant"), 10, 1);
                buttonAjouterEnfant.click();
                waitThread(1);
                choixSexeEnfant(flux, 0);
                WebElement inputNomEnfant = waitForElement(driver, By.id("nom_enfant_1"), 10, 1);
                inputNomEnfant.sendKeys(flux.getEnfants().get(0).getNom());
                WebElement inputPrenomEnfant = waitForElement(driver, By.id("prenom_enfant_1"), 10, 1);
                inputPrenomEnfant.sendKeys(flux.getEnfants().get(0).getPrenom());
                WebElement inputDateNaissanceEnfant = waitForElement(driver, By.id("dt_naiss_enfant_1"), 10, 1);
                inputDateNaissanceEnfant.sendKeys(flux.getEnfants().get(0).getDateNaissance());
                choixPaysEnfant(flux, 0);
                WebElement inputCodePostalEnfant = waitForElement(driver, By.id("cp_naiss_enfant_1"), 10, 1);
                inputCodePostalEnfant.sendKeys(flux.getPersonnes().get(0).getCodePostal());
                choixVilleEnfants(flux, 0);
                choixRegimeEnfant(flux, 0);
            }
            if (flux.getEnfants().size() == 2) {
                WebElement buttonAjouterEnfant2 = waitForElement(driver, By.id("btn-add-enfant"), 10, 1);
                buttonAjouterEnfant2.click();
                choixSexeEnfant(flux, 1);
                WebElement inputNomEnfant2 = waitForElement(driver, By.id("nom_enfant_2"), 10, 1);
                inputNomEnfant2.sendKeys(flux.getPersonnes().get(1).getNom());
                WebElement inputPrenomEnfant2 = waitForElement(driver, By.id("prenom_enfant_2"), 10, 1);
                inputPrenomEnfant2.sendKeys(flux.getPersonnes().get(1).getPrenom());
                WebElement inputDateNaissanceEnfant2 = waitForElement(driver, By.id("dt_naiss_enfant_2"), 10, 1);
                inputDateNaissanceEnfant2.sendKeys(flux.getPersonnes().get(1).getDateNaissance());
                choixPaysEnfant(flux, 1);
                WebElement inputCodePostalEnfant = waitForElement(driver, By.id("cp_naiss_enfant_2"), 10, 1);
                inputCodePostalEnfant.sendKeys(flux.getPersonnes().get(0).getCodePostal());
                choixVilleEnfants(flux, 1);
                choixRegimeEnfant(flux, 1);
            }
            suivant();
        }
        if (flux.getPersonnes().size() == 1) {
            suivant();
        }
        adresseRisques(flux, 0);
    }

    private void adresseRisques(FluxData flux, int index) {
        waitThread(2);
        WebElement inputAdresse = waitForElement(driver, By.id("r_adresse_1"), 10, 1);
        inputAdresse.sendKeys(flux.getPersonnes().get(index).getNumeroVoie() + " " + flux.getPersonnes().get(index).getNomVoie());
        WebElement inputCodePostal1 = waitForElement(driver, By.id("r_code_postal"), 10, 1);
        inputCodePostal1.sendKeys(flux.getPersonnes().get(index).getCodePostal());
        choixVille1(flux, index);
        suivant();
    }

    private void suivant() {
        WebElement buttonSuivant = waitForElement(driver, By.xpath("//input[@src=\"img/suivant.jpg\"]"), 10, 1);
        buttonSuivant.click();
    }

    private void choixRegime(FluxData flux, int index) {
        WebElement dropdown = null;
        if (index == 0) {
            dropdown = waitForElement(driver, By.id("regime_assure_1"), 10, 1);
        } if (index == 1) {
              dropdown = waitForElement(driver, By.id("regime_assure_2"), 10, 1);
        }
        if (dropdown != null) {
            Select select = new Select(dropdown);
            List<WebElement> options = select.getOptions();
            for (WebElement webElement : options) {
                if (webElement.getText().contains("Régime général")) {
                    webElement.click();
                    break;
                }
            }
        }
    }

    private void choixRegimeEnfant(FluxData flux, int index) {
        WebElement dropdown = null;
        if (index == 0) {
            dropdown = waitForElement(driver, By.id("regime_enfant_1"), 10, 1);
        } if (index == 1) {
            dropdown = waitForElement(driver, By.id("regime_enfant_2"), 10, 1);
        }
        if (dropdown != null) {
            Select select = new Select(dropdown);
            List<WebElement> options = select.getOptions();
            for (WebElement webElement : options) {
                if (webElement.getText().contains("de l'adhérent principal")) {
                    webElement.click();
                    break;
                }
            }
        }
    }

    private void choixPays(FluxData flux, int index) {
        WebElement dropdown = null;
        if (index == 0) {
            dropdown = waitForElement(driver, By.id("pays_naiss_assure_1"), 10, 1);
        } if (index == 1) {
            dropdown = waitForElement(driver, By.id("pays_naiss_assure_2"), 10, 1);
        }
        if (dropdown != null) {
            Select select = new Select(dropdown);
            List<WebElement> options = select.getOptions();
            for (WebElement webElement : options) {
                if (webElement.getText().contains(flux.getPersonnes().get(index).getPays())) {
                    webElement.click();
                    break;
                }
            }
        }
    }

    private void choixPaysEnfant(FluxData flux, int index) {
        WebElement dropdown = null;
        if (index == 0) {
            dropdown = waitForElement(driver, By.id("pays_naiss_enfant_1"), 10, 1);
        } if (index == 1) {
            dropdown = waitForElement(driver, By.id("pays_naiss_assure_2"), 10, 1);
        }
        if (dropdown != null) {
            Select select = new Select(dropdown);
            List<WebElement> options = select.getOptions();
            for (WebElement webElement : options) {
                if (webElement.getText().contains(flux.getPersonnes().get(index).getPays())) {
                    webElement.click();
                    break;
                }
            }
        }
    }

    private void choixVille(FluxData flux, int index) {
        WebElement dropdown = null;
        if (index == 0) {
            dropdown = waitForElement(driver, By.id("ville_naiss_assure_1"), 10, 1);
        } if (index == 1) {
            dropdown = waitForElement(driver, By.id("ville_naiss_assure_2"), 10, 1);
        }
        if (dropdown != null) {
            Select select = new Select(dropdown);
            List<WebElement> options = select.getOptions();
            for (WebElement webElement : options) {
                if (webElement.getText().equalsIgnoreCase(flux.getPersonnes().get(index).getVille())) {
                    webElement.click();
                    break;
                }
            }
        }
    }

    private void choixVilleEnfants(FluxData flux, int index) {
        WebElement dropdown = null;
        if (index == 0) {
            dropdown = waitForElement(driver, By.id("ville_naiss_enfant_1"), 10, 1);
        } if (index == 1) {
            dropdown = waitForElement(driver, By.id("ville_naiss_enfant_2"), 10, 1);
        }
        if (dropdown != null) {
            Select select = new Select(dropdown);
            List<WebElement> options = select.getOptions();
            for (WebElement webElement : options) {
                if (webElement.getText().equalsIgnoreCase(flux.getPersonnes().get(index).getVille())) {
                    webElement.click();
                    break;
                }
            }
        }
    }

    private void choixVille1(FluxData flux, int index) {
        WebElement dropdown = null;
        if (index == 0) {
            dropdown = waitForElement(driver, By.id("r_ville"), 10, 1);
        } if (index == 1) {
            dropdown = waitForElement(driver, By.id("r_ville"), 10, 1);
        }
        if (dropdown != null) {
            Select select = new Select(dropdown);
            List<WebElement> options = select.getOptions();
            for (WebElement webElement : options) {
                if (webElement.getText().equalsIgnoreCase(flux.getPersonnes().get(index).getVille())) {
                    webElement.click();
                    break;
                }
            }
        }
    }

    private void choixSexe(FluxData flux, int index) {
        WebElement dropdown = null;
        if (index == 0) {
            dropdown = waitForElement(driver, By.id("sexe_assure_1"), 10, 1);
        } else if (index == 1){
            dropdown = waitForElement(driver, By.id("sexe_assure_2"), 10, 1);
        }
        if (dropdown != null) {
            Select select = new Select(dropdown);
            List<WebElement> options = select.getOptions();
            for (WebElement webElement : options) {
                if (webElement.getText().equalsIgnoreCase("Homme") && flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur")) {
                    webElement.click();
                    break;
                }
                if (webElement.getText().equalsIgnoreCase("Femme") && flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame")) {
                    webElement.click();
                    break;
                }
            }
        }
    }

    private void choixSexeEnfant(FluxData flux, int index) {
        WebElement dropdown = null;
        if (index == 0) {
            dropdown = waitForElement(driver, By.id("sexe_enfant_1"), 10, 1);
        } else if (index == 1){
            dropdown = waitForElement(driver, By.id("sexe_enfant_2"), 10, 1);
        }
        if (dropdown != null) {
            Select select = new Select(dropdown);
            List<WebElement> options = select.getOptions();
            for (WebElement webElement : options) {
                if (webElement.getText().equalsIgnoreCase("Masculin") && flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur")) {
                    webElement.click();
                    break;
                }
                if (webElement.getText().equalsIgnoreCase("Féminin") && flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame")) {
                    webElement.click();
                    break;
                }
            }
        }
    }

    private void choixSituationFamilliale(FluxData flux, int index) {
        WebElement dropdown = null;
        if (index == 0) {
            dropdown = waitForElement(driver, By.id("i_sitfam"), 10, 1);
        } if (index == 1) {
            dropdown = waitForElement(driver, By.id("i_sitfam"), 10, 1);
        }
        if (dropdown != null) {
            Select select = new Select(dropdown);
            List<WebElement> options = select.getOptions();
            for (WebElement webElement : options) {
                if (flux.getPersonnes().size() == 1 && webElement.getText().equalsIgnoreCase("Célibataire")) {
                    webElement.click();
                    break;
                }
                if (flux.getPersonnes().size() >= 2 && webElement.getText().contains("Marié")) {
                    webElement.click();
                    break;
                }
            }
        }
    }

    @Override
    protected void pageName(String namePage) {}
}
