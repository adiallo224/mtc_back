package com.mtc.mutuaConseil.services.servicesImpl.assuPret;

import com.mtc.mutuaConseil.models.Compte;
import com.mtc.mutuaConseil.models.FluxData;
import com.mtc.mutuaConseil.models.Tarif;
import com.mtc.mutuaConseil.services.LaunchedService;
import com.mtc.mutuaConseil.services.servicesImpl.TypeAssuranceService;
import com.mtc.mutuaConseil.utils.InformationsPersonne;
import com.mtc.mutuaConseil.utils.InformationsPret;
import com.mtc.mutuaConseil.utils.PageElementInteraction;
import com.mtc.mutuaConseil.utils.TarifUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
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
public class SWLService extends PageElementInteraction implements LaunchedService {

    private Logger log = LoggerFactory.getLogger(SWLService.class);
    public static WebDriver driver;
    private Actions actions;
    private final String source = "SwlifePret";
    private JavascriptExecutor js = null;
    private final TypeAssuranceService typeAssuranceService;

    public SWLService(TypeAssuranceService typeAssuranceService){
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        log.info("Debut de traitement *-- Swiss Life pret service --*");
        Tarif tarif = TarifUtils.createDefaultTarif(c, typeAssuranceService, 1L);
        driver = new EdgeDriver();
        driver.manage().window().maximize();
        actions = new Actions(driver);
        js = (JavascriptExecutor) driver;
        driver.get(c.getUrlFournisseur());
        waitThread(1);
        try {
            WebElement buttonSeconnecter = waitForElement(driver, By.xpath("//a[@class='button-connection']"), 25, 3);
            actions.moveToElement(buttonSeconnecter).click().perform();
            WebElement inputlogin = waitForElement(driver, By.xpath("//input[@id='userNameInput']"), 25, 3);
            inputlogin.sendKeys(c.getUsername());
            WebElement inputPassword = waitForElement(driver, By.xpath("//input[@id='passwordInput']"), 15, 1);
            inputPassword.sendKeys(c.getPassword());
            WebElement buttonLogin = waitForElement(driver, By.xpath("//span[@id='submitButton']"), 15, 1);
            actions.moveToElement(buttonLogin).click().perform();
            waitThread(2);
            clicButton(driver, By.xpath("//div[9]//div[1]//div[1]//div[1]//div[1]"), actions);
            clicButton(driver, By.xpath("//span[contains(text(),'SwissLife Assurance des Emprunteurs')]"), actions);

            // Switch sur la nouvelle page
            switchPage(driver);

            clicButton(driver, By.xpath("//span[normalize-space()='Nouveau devis']"), actions);

            remplirCaracteristiqueDevis(flux);
            remplirInformationsPret(flux);
            remplirInformationsPersonne(flux, source);
            remplirGaranties(flux);

            WebElement totalElement = null;
            if (flux.getPersonnes().size() == 1) {
                waitThread(5);
                totalElement = recupererElementTable(driver, By.xpath("//div[@id=\"resultatTable2\"]//table"), "td", 0, 2);
            } else if (flux.getPersonnes().size() == 2) {
                waitThread(5);
                totalElement = recupererElementTable(driver, By.xpath("//div[@id=\"resultatTable2\"]//table"), "td", 0, 3);
            }
            String cout = null;
            if (totalElement != null) {
                cout = totalElement.getText();
                log.info("Cout : {} ", cout);
                tarif.setMontant(cout);
                String screenshotBytes = captureScreenshot(driver, tarif.getNom(), false, tarif);
                if (screenshotBytes != null) {
                    tarif.setCaptureImg(screenshotBytes);
                }
                tarif.setExecution(true);
            }
            log.info("Fin de traitement -- Swiss Life");
        } catch (Exception e) {
            log.error("An error occurred", e);
            tarif.setErreur(e.getMessage());
            String screenshotBytesErreur = captureScreenshot(driver, tarif.getNom(), true, tarif);
            tarif.setCaptureImgErreur(screenshotBytesErreur);
            tarif.setEtape("");
        } finally {
            driver.quit();
        }
      return tarif;
    }

    private void remplirCaracteristiqueDevis(FluxData flux) {
        clicButton(driver, By.xpath("//span[normalize-space()='Nouvelle assurance emprunteur']"), actions);
        waitThread(2);
        selectByClickTwoElement(driver, By.id("commission_label"), By.id("commission_10"));
        if (flux.getPrets().size() == 2) {
            WebElement dropdownPret = waitForElement1(driver, By.id("nbPretsDevis_label"), 30, 1); // Remplacez avec l'élément qui déclenche le dropdown
            dropdownPret.click();
            waitThread(1);
            WebElement liElement = waitForElement1(driver, By.xpath("//li[text()='2']"), 30, 1);
            liElement.click();
        }
        if (flux.getPersonnes().size() == 2) {
            WebElement dropdownAssure = waitForElement1(driver, By.id("nbAssuresDevis_label"), 30, 1); // Remplacez avec l'élément qui déclenche le dropdown
            dropdownAssure.click();
            waitThread(1);
            WebElement liElement = waitForElement1(driver, By.id("nbAssuresDevis_1"), 30, 1);
            liElement.click();
        }
//            WebElement inputConseiller = waitForElement(driver, By.id("autocompleteAdvisorName_input"), 25, 3);
//            inputConseiller.sendKeys("OLIVIER");
//            WebElement tdAutoCompleteColumn = waitForElement(driver, By.xpath("//td[@class=\"autoCompleteColumn\"]"), 25, 3);
//            tdAutoCompleteColumn.click();
        nextPage(driver, By.xpath("//a[@class=\"nextBtn\"]"), 10, 1);
    }

    private void remplirInformationsPret(FluxData flux) {
        int index = 0;
        if (!flux.getPrets().get(index).getType().equalsIgnoreCase("Amortissable")) {
            choixTypePret(flux, index);
        }
        elementNeutre(driver);
        InformationsPret.infoMontant(driver, By.id("mtEmpruntPretDevis2"), 10, 2, flux, index);
        elementNeutre(driver);
        InformationsPret.infoDuree(driver, By.id("dureePret2"), 10, 2, flux, index);
        waitThread(2);
        InformationsPret.infoTaux(driver, By.id("tauxPretDevis2"), 10, 2, flux, index);
        waitThread(4);
        elementNeutre(driver);
        if (!flux.getPrets().get(index).getTypeTaux().equalsIgnoreCase("Fixe")) {
            choixTypeTaux(flux, index);
        }
        elementNeutre(driver);
        if (!flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Pas de différé")) {
            choixDiffere(flux, index);
        }
        if (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Total") || flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Partiel")) {
            WebElement inputDureeDiffere= waitForElement1(driver, By.id("nbMoisDifferePretDevis2"), 10, 1);
            inputDureeDiffere.sendKeys(flux.getPrets().get(index).getDureeDiffere());
            elementNeutre(driver);
        }
        elementNeutre(driver);
        InformationsPret.infoDateEffet(driver, By.id("dateEffetPretDevis2_input"), 10, 2, flux, index);
        elementNeutre(driver);
        choixObjetFinancement(flux, index);
        elementNeutre(driver);
        if (flux.getPrets().size() == 2) {
            waitThread(2);
            nextPage(driver, By.xpath("//a[@class=\"nextBtn\"]"), 10, 1);
            index = 1;
            waitThread(2);
            if (!flux.getPrets().get(index).getType().equalsIgnoreCase("Amortissable")) {
                choixTypePret(flux, index);
            }
            elementNeutre(driver);
            InformationsPret.infoMontant(driver, By.id("mtEmpruntPretDevis2"), 10, 2, flux, index);
            elementNeutre(driver);
            InformationsPret.infoDuree(driver, By.id("dureePret2"), 10, 2, flux, index);
            elementNeutre(driver);
            InformationsPret.infoTaux(driver, By.id("tauxPretDevis2"), 10, 2, flux, index);
            elementNeutre(driver);
            if (!flux.getPrets().get(index).getTypeTaux().equalsIgnoreCase("Fixe")) {
                choixTypeTaux(flux, index);
            }
            elementNeutre(driver);
            if (!flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Pas de différé")) {
                choixDiffere(flux, index);
            }
            if (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Total") || flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Partiel")) {
                WebElement inputDureeDiffere= waitForElement1(driver, By.id("nbMoisDifferePretDevis2"), 10, 1);
                inputDureeDiffere.sendKeys(flux.getPrets().get(index).getDureeDiffere());
                elementNeutre(driver);
            }
            elementNeutre(driver);
            InformationsPret.infoDateEffet(driver, By.id("dateEffetPretDevis2_input"), 10, 2, flux, index);
            elementNeutre(driver);
            choixObjetFinancement(flux, index);
            elementNeutre(driver);
            InformationsPret.infoTaux(driver, By.id("tauxPretDevis2"), 10, 2, flux, index);
            elementNeutre(driver);
        }
        waitThread(1);
        nextPage(driver, By.xpath("//a[@class=\"nextBtn\"]"), 10, 1);
    }

    private void remplirInformationsPersonne(FluxData flux, String source) {
        int index = 0;
        waitThread(2);
        if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur")) {
            waitThread(1);
            selectByClickTwoElement(driver, By.id("civility2_label"), By.id("civility2_1"));
        } else if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame")){
            waitThread(1);
            selectByClickTwoElement(driver, By.id("civility2_label"), By.id("civility2_2"));
        }
        InformationsPersonne.infoNom(driver, By.id("lastname2"), 10, 2, flux, index, source);
        elementNeutre(driver);
        InformationsPersonne.infoPrenom(driver, By.id("firstname2"), 10, 2, flux, index, source);
        elementNeutre(driver);
        InformationsPersonne.infoDateNaissance(driver, By.id("dateOfBirth2_input"), 10, 2, flux, index, source);
        elementNeutre(driver);
        waitThread(2);
        choixProfession(flux, index);
        waitThread(2);
        selectByClickTwoElement(driver, By.id("categProfSpec_label"), By.id("categProfSpec_3"));
        waitThread(2);
        selectByClickTwoElement(driver, By.id("occupationAtRiskLabel"), By.xpath("//div[@id=\"occupationAtRiskLabel_panel\"]//div[@class=\"ui-selectcheckboxmenu-items-wrapper\"]//li//label[text()=\"N'exerce aucune de ces professions\"]"));
        WebElement inputProfession = waitForElement(driver, By.id("professionAssure2"), 10, 1);
        inputProfession.sendKeys(flux.getPersonnes().get(index).getProfession());
        waitThread(2);
        if(flux.getInfoAssureComplets().get(index).getFumeur()) {
            WebElement buttonFumeur = driver.findElement(By.xpath("//label[@fieldid='LIBDEVfumeur']"));
            buttonFumeur.click();
        }
        if(flux.getInfoAssureComplets().get(index).getTravailManuel()) {
            WebElement buttonTravailManuel = driver.findElement(By.xpath("//label[@fieldid='LIBDEVprofManuelle']"));
            buttonTravailManuel.click();
        }
        if(flux.getInfoAssureComplets().get(index).getTravailHauteur() || flux.getInfoAssureComplets().get(index).getTravailManuelManuLourde()) {
            WebElement buttonTravailManuelLourdeHauteur = driver.findElement(By.xpath("//label[@fieldid='LIBDEVmetierExpose']"));
            buttonTravailManuelLourdeHauteur.click();
        }
        if(flux.getInfoAssureComplets().get(index).getDeplacementPro20000()) {
            WebElement buttonDeplacement = driver.findElement(By.xpath("//label[@fieldid='LIBDEVmetierExpose']"));
            buttonDeplacement.click();
        }
        if(flux.getInfoAssureComplets().get(index).getDeplacementPaysRisque() || flux.getInfoAssureComplets().get(index).getDeplacementEtranger60()) {
            WebElement buttonDeplacementEtranger = driver.findElement(By.xpath("//label[@fieldid='LIBDEVtravelRiskedCountry']"));
            buttonDeplacementEtranger.click();
        }
        nextPage(driver, By.xpath("//a[@class=\"nextBtn\"]"), 10, 1);
        // verification à faire
        if (flux.getPersonnes().size() == 2){
            index = 1;
            if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur")) {
                selectByClickTwoElement(driver, By.id("civility2_label"), By.id("civility2_1"));
            } else if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame")){
                waitThread(1);
                selectByClickTwoElement(driver, By.id("civility2_label"), By.id("civility2_2"));
            }
            InformationsPersonne.infoNom(driver, By.id("lastname2"), 10, 2, flux, index, source);
            elementNeutre(driver);
            InformationsPersonne.infoPrenom(driver, By.id("firstname2"), 10, 2, flux, index, source);
            elementNeutre(driver);
            InformationsPersonne.infoDateNaissance(driver, By.id("dateOfBirth2_input"), 10, 2, flux, index, source);
            elementNeutre(driver);
            waitThread(2);
            choixProfession(flux, index);
            waitThread(2);
            selectByClickTwoElement(driver, By.id("categProfSpec_label"), By.id("categProfSpec_3"));
            waitThread(2);
            selectByClickTwoElement(driver, By.id("occupationAtRiskLabel"), By.xpath("//div[@id=\"occupationAtRiskLabel_panel\"]//div[@class=\"ui-selectcheckboxmenu-items-wrapper\"]//li//label[text()=\"N'exerce aucune de ces professions\"]"));
            WebElement inputProfession1 = waitForElement(driver, By.id("professionAssure2"), 10, 1);
            inputProfession1.sendKeys(flux.getPersonnes().get(index).getProfession());
            waitThread(2);
            elementNeutre(driver);
            if(flux.getInfoAssureComplets().get(index).getFumeur()) {
                WebElement buttonFumeur = driver.findElement(By.xpath("//label[@fieldid='LIBDEVfumeur']"));
                buttonFumeur.click();
            }
            if(flux.getInfoAssureComplets().get(index).getTravailManuel()) {
                WebElement buttonTravailManuel = driver.findElement(By.xpath("//label[@fieldid='LIBDEVprofManuelle']"));
                buttonTravailManuel.click();
            }
            if(flux.getInfoAssureComplets().get(index).getTravailHauteur() || flux.getInfoAssureComplets().get(index).getTravailManuelManuLourde()) {
                WebElement buttonTravailManuelLourdeHauteur = driver.findElement(By.xpath("//label[@fieldid='LIBDEVmetierExpose']"));
                buttonTravailManuelLourdeHauteur.click();
            }
            if(flux.getInfoAssureComplets().get(index).getDeplacementPro20000()) {
                WebElement buttonDeplacement = driver.findElement(By.xpath("//label[@fieldid='LIBDEVmetierExpose']"));
                buttonDeplacement.click();
            }
            if(flux.getInfoAssureComplets().get(index).getDeplacementPaysRisque() || flux.getInfoAssureComplets().get(index).getDeplacementEtranger60()) {
                WebElement buttonDeplacementEtranger = driver.findElement(By.xpath("//label[@fieldid='LIBDEVtravelRiskedCountry']"));
                buttonDeplacementEtranger.click();
            }


            nextPage(driver, By.xpath("//a[@class=\"nextBtn\"]"), 10, 1);
        }
    }

    private void remplirGaranties(FluxData flux) {
         if (flux.getInfoAssureComplets().get(0).getGarantie().equalsIgnoreCase("IPT, ITT, IPP")) {
             WebElement buttonGarantie = waitForElement(driver, By.xpath("//div[@id=\"formulesGarantiesPanel\"]//div[1]//div[2]"), 10, 1);
             buttonGarantie.click();
         }else if (flux.getInfoAssureComplets().get(0).getGarantie().equalsIgnoreCase("IPT, ITT")) {
             WebElement buttonGarantie = waitForElement(driver, By.xpath("//div[@id=\"formulesGarantiesPanel\"]//div[1]//div[1]"), 10, 1);
             buttonGarantie.click();
         }
         waitThread(2);
         nextPage(driver, By.xpath("//a[@class=\"nextBtn\"]"), 10, 1);
         waitThread(2);
//         WebElement quotiteDeces = waitForElement(driver, By.id("garantieQuotiteVal"), 10, 1);
//         quotiteDeces.sendKeys(flux.getInfoAssureComplets().get(0).getQuotiteDeces());
         nextPage(driver, By.xpath("//a[@class=\"nextBtn\"]"), 10, 1);

         if (flux.getPersonnes().size() == 2) {
             if (flux.getInfoAssureComplets().get(1).getGarantie() != null) {
                 if (flux.getInfoAssureComplets().get(0).getGarantie().equalsIgnoreCase("IPT, ITT, IPP")) {
                     WebElement buttonGarantie = waitForElement(driver, By.xpath("//div[@id=\"formulesGarantiesPanel\"]//div[1]//div[2]"), 10, 1);
                     buttonGarantie.click();
                 } else if (flux.getInfoAssureComplets().get(1).getGarantie().equalsIgnoreCase("IPT, ITT")) {
                     WebElement buttonGarantie = waitForElement(driver, By.xpath("//div[@id=\"formulesGarantiesPanel\"]//div[1]//div[1]"), 10, 1);
                     buttonGarantie.click();
                 }
             }
             waitThread(2);
             nextPage(driver, By.xpath("//a[@class=\"nextBtn\"]"), 10, 1);
             waitThread(2);
             nextPage(driver, By.xpath("//a[@class=\"nextBtn\"]"), 10, 1);
         }

        if (flux.getPrets().size() == 2) {
            if (flux.getInfoAssureComplets().size() == 1 && flux.getInfoAssureComplets().get(0).getGarantie() != null) {
                if (flux.getInfoAssureComplets().get(0).getGarantie().equalsIgnoreCase("IPT, ITT, IPP")) {
                    WebElement buttonGarantie = waitForElement(driver, By.xpath("//div[@id=\"formulesGarantiesPanel\"]//div[1]//div[2]"), 10, 1);
                    buttonGarantie.click();
                } else if (flux.getInfoAssureComplets().get(0).getGarantie().equalsIgnoreCase("IPT, ITT")) {
                    WebElement buttonGarantie = waitForElement(driver, By.xpath("//div[@id=\"formulesGarantiesPanel\"]//div[1]//div[1]"), 10, 1);
                    buttonGarantie.click();
                }
            } else if (flux.getInfoAssureComplets().size() == 2 && flux.getInfoAssureComplets().get(1).getGarantie() != null) {
                if (flux.getInfoAssureComplets().get(1).getGarantie().equalsIgnoreCase("IPT, ITT, IPP")) {
                    WebElement buttonGarantie = waitForElement(driver, By.xpath("//div[@id=\"formulesGarantiesPanel\"]//div[1]//div[2]"), 10, 1);
                    buttonGarantie.click();
                } else if (flux.getInfoAssureComplets().get(1).getGarantie().equalsIgnoreCase("IPT, ITT")) {
                    WebElement buttonGarantie = waitForElement(driver, By.xpath("//div[@id=\"formulesGarantiesPanel\"]//div[1]//div[1]"), 10, 1);
                    buttonGarantie.click();
                }
            }
            waitThread(2);
            nextPage(driver, By.xpath("//a[@class=\"nextBtn\"]"), 10, 1);
            waitThread(2);
//            WebElement quotiteDeces1 = waitForElement(driver, By.id("garantieQuotiteVal"), 10, 1);
//            quotiteDeces1.sendKeys(flux.getInfoAssureComplets().get(0).getQuotiteDeces());
            nextPage(driver, By.xpath("//a[@class=\"nextBtn\"]"), 10, 1);
        }
    }

    private void choixObjetFinancement(FluxData flux, int index) {
        if (flux.getPrets().get(index).getObjet().equalsIgnoreCase("Résidence principale")) {
            selectByClickTwoElement(driver, By.id("labelobjetWizardDevis_label"), By.id("labelobjetWizardDevis_1"));
        } else if (flux.getPrets().get(index).getObjet().equalsIgnoreCase("Résidence secondaire")) {
            selectByClickTwoElement(driver, By.id("labelobjetWizardDevis_label"), By.id("labelobjetWizardDevis_2"));
        } else if (flux.getPrets().get(index).getObjet().equalsIgnoreCase("Investissement locatif")) {
            selectByClickTwoElement(driver, By.id("labelobjetWizardDevis_label"), By.id("labelobjetWizardDevis_3"));
        } else if (flux.getPrets().get(index).getObjet().equalsIgnoreCase("Prêt professionnel")) {
            selectByClickTwoElement(driver, By.id("labelobjetWizardDevis_label"), By.id("labelobjetWizardDevis_4"));
        }
        elementNeutre(driver);
    }

    private void choixDiffere(FluxData flux, int index) {
        if (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Pas de différé")) {
            selectByClickTwoElement(driver, By.id("differeType_label"), By.xpath("//li[@id='differeType_1']"));
        } else if (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Partiel")) {
            selectByClickTwoElement(driver, By.id("differeType_label"), By.xpath("//li[@id='differeType_2']"));
        } else if (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Total")) {
            selectByClickTwoElement(driver, By.id("differeType_label"), By.xpath("//li[@id='differeType_3']"));
        }
        elementNeutre(driver);
    }

    private void choixTypeTaux(FluxData flux, int index) {
        if (flux.getPrets().get(index).getTypeTaux().equalsIgnoreCase("Variable")) {
            selectByClickTwoElement(driver, By.id("typetaux_label"), By.xpath("//li[@id='typetaux_2']"));
        } else if (flux.getPrets().get(index).getTypeTaux().equalsIgnoreCase("Fixe")) {
            selectByClickTwoElement(driver, By.id("typetaux_label"), By.xpath("//li[@id='typetaux_1']"));
        }
        elementNeutre(driver);
    }

    public void choixTypePret(FluxData flux, int index) {
        if (flux.getPrets().get(index).getType().equalsIgnoreCase("Amortissable")) {
            selectByClickTwoElement(driver, By.id("typePretDevis2_label"), By.xpath("//li[@data-label='Prêt amortissable']"));
        } else  if (flux.getPrets().get(index).getType().equalsIgnoreCase("Prêt à taux zéro")) {
            selectByClickTwoElement(driver, By.id("typePretDevis2_label"), By.xpath("//li[@data-label='Prêt à taux zéro']"));
        } else  if (flux.getPrets().get(index).getType().equalsIgnoreCase("Prêt in fine")) {
            selectByClickTwoElement(driver, By.id("typePretDevis2_label"), By.xpath("//li[@data-label='Prêt in fine']"));
        } else  if (flux.getPrets().get(index).getType().equalsIgnoreCase("Prêt relais")) {
            selectByClickTwoElement(driver, By.id("typePretDevis2_label"), By.xpath("//li[@data-label='Prêt relais']"));
        } else  if (flux.getPrets().get(index).getType().equalsIgnoreCase("Crédit bail")) {
            selectByClickTwoElement(driver, By.id("typePretDevis2_label"), By.xpath("//li[@data-label='Crédit Bail et LOA']"));
        }
        elementNeutre(driver);
    }

    private void choixProfession(FluxData flux, int index) {
        if (flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Salarié non cadre : employé")) {
            selectByClickTwoElement(driver, By.id("categProfession"), By.id("categProfession_11"));
        } else if (flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Salarié cadre")) {
            selectByClickTwoElement(driver, By.id("categProfession"), By.id("categProfession_10"));
        } else if (flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Profession libérale")) {
            selectByClickTwoElement(driver, By.id("categProfession"), By.id("categProfession_9"));
        } else if (flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Agriculteur")) {
            selectByClickTwoElement(driver, By.id("categProfession"), By.id("categProfession_1"));
        } else if (flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Artisan") ||
                   flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Commerçant") ||
                   flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Ouvrier")) {
            selectByClickTwoElement(driver, By.id("categProfession"), By.id("categProfession_2"));
        } else if (flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Chef d\\'entreprise")) {
            selectByClickTwoElement(driver, By.id("categProfession"), By.id("categProfession_3"));
        } else if (flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Fonctionnaire classe a")) {
            selectByClickTwoElement(driver, By.id("categProfession"), By.id("categProfession_5"));
        } else if (flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Fonctionnaire hors classe a")) {
            selectByClickTwoElement(driver, By.id("categProfession"), By.id("categProfession_6"));
        }
        elementNeutre(driver);
    }

    private void choixObjetFinancement1(FluxData flux, int index) {
        WebElement selectElementLabel = null;
        WebElement selectElement = null;
        if (index == 0) {
            selectElementLabel = waitForElement1(driver, By.id("labelobjetWizardDevis_label"), 30, 1);
            selectElement = waitForElement1(driver, By.id("labelobjetWizardDevis_input"), 30, 1);
        }
        if (index == 1) {
            selectElementLabel = waitForElement1(driver, By.id("labelobjetWizardDevis_label"), 30, 1);
            selectElement = waitForElement1(driver, By.id("labelobjetWizardDevis_input"), 30, 1);
        }
        if (selectElement != null && selectElementLabel != null) {
            waitThread(1);
            Select select = new Select(selectElement);
            List<WebElement> options = select.getOptions();
            selectElementLabel.click();
            for (WebElement opt : options) {
                WebElement choix = null;
                if (opt.getText().equalsIgnoreCase("Résidence principale") && flux.getPrets().get(index).getObjet().equalsIgnoreCase("Résidence principale")){
                    choix = waitForElement1(driver, By.xpath("//li[@id='labelobjetWizardDevis_1']"), 20, 1);
                    choix.click();
                    clickEnter(actions);
                    break;
                } else if (opt.getText().equalsIgnoreCase("Résidence secondaire") && flux.getPrets().get(index).getObjet().equalsIgnoreCase("Résidence secondaire")){
                    choix = waitForElement1(driver, By.xpath("//li[@id='labelobjetWizardDevis_2']"), 20, 1);
                    choix.click();
                    clickEnter(actions);
                    break;
                } else if (opt.getText().equalsIgnoreCase("Investissement locatif") && flux.getPrets().get(index).getObjet().equalsIgnoreCase("Investissement locatif")){
                    choix = waitForElement1(driver, By.xpath("//li[@id='labelobjetWizardDevis_3']"), 20, 1);
                    choix.click();
                    clickEnter(actions);
                    break;
                } else if (opt.getText().equalsIgnoreCase("Prêt professionnel") && flux.getPrets().get(index).getObjet().equalsIgnoreCase("Prêt professionnel")){
                    choix = waitForElement1(driver, By.xpath("//li[@id='labelobjetWizardDevis_4']"), 20, 1);
                    choix.click();
                    clickEnter(actions);
                    break;
                }
            }
        }
    }

    private void choixDiffere1(FluxData flux, int index) {
        WebElement dropDownDiffereLabel = null;
        WebElement dropDownDiffere = null;
        if (index == 0) {
            dropDownDiffereLabel = waitForElement1(driver, By.id("differeType_label"), 30, 1);
            dropDownDiffere = waitForElement1(driver, By.id("differeType_input"), 30, 1);
        }
        if (index == 1) {
            dropDownDiffereLabel = waitForElement1(driver, By.id("differeType_label"), 30, 1);
            dropDownDiffere = waitForElement1(driver, By.id("differeType_input"), 30, 1);
        }
        if (dropDownDiffere != null && dropDownDiffereLabel != null) {
            waitThread(1);
            dropDownDiffereLabel.click();
            Select select = new Select(dropDownDiffere);
            List<WebElement> options = select.getOptions();
            for (WebElement opt : options) {
                WebElement choix = null;
                if (opt.getText().equalsIgnoreCase("Différé partiel") && flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Partiel")){
                    choix.click();
                    clickEnter(actions);
                    break;
                } else if (opt.getText().equalsIgnoreCase("Différé total") && flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Total")){
                    choix = waitForElement1(driver, By.id("differeType_3"), 20, 1);
                    choix.click();
                    clickEnter(actions);
                    break;
                } else if (opt.getText().equalsIgnoreCase("Pas de différé") && flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Pas de différé")){
                    choix = waitForElement1(driver, By.id("differeType_1"), 20, 1);
                    choix.click();
                    clickEnter(actions);
                    break;
                }
            }
        }
    }

    private void choixTypeTaux1(FluxData flux, int index) {
        WebElement dropDownTauxLabel = null;
        WebElement dropDownTaux = null;
        if (index == 0) {
            dropDownTauxLabel = waitForElement1(driver, By.id("typetaux_label"), 30, 1);
            dropDownTaux = waitForElement1(driver, By.id("typetaux_input"), 30, 1);
        }
        if (index == 1) {
            dropDownTauxLabel = waitForElement1(driver, By.id("typetaux_label"), 30, 1);
            dropDownTaux = waitForElement1(driver, By.id("typetaux_input"), 30, 1);
        }
        if (dropDownTaux != null && dropDownTauxLabel != null) {
            waitThread(1);
            dropDownTauxLabel.click();
            Select select = new Select(dropDownTaux);
            List<WebElement> options = select.getOptions();
            for (WebElement opt : options) {
                WebElement choix = null;
                if (opt.getText().equalsIgnoreCase("Variable") && flux.getPrets().get(index).getTypeTaux().equalsIgnoreCase("Variable")){
                    choix = waitForElement1(driver, By.xpath("//option[@value='VARIABLE']"), 20, 1);
                    choix.click();
                    clickEnter(actions);
                    break;
                }
            }
        }
    }

    public void choixTypePret1(FluxData flux, int index) {
        WebElement dropdown = null;
        WebElement dropdownLabel = null;
        if (index == 0) {
            dropdownLabel = waitForElement1(driver, By.id("typePretDevis2_label"), 30, 1);
            dropdown = waitForElement1(driver, By.id("typePretDevis2_input"), 30, 1);
        }
        if (index == 1) {
            dropdownLabel = waitForElement1(driver, By.id("typePretDevis2_label"), 30, 1);
            dropdown = waitForElement1(driver, By.id("typePretDevis2_input"), 30, 1);
        }
        if (dropdown != null && dropdownLabel != null) {
            waitThread(1);
            dropdownLabel.click();
            Select select = new Select(dropdown);
            List<WebElement> options = select.getOptions();
            for (WebElement opt :  options) {
                WebElement choix = null;
                if (opt.getText().equalsIgnoreCase("Prêt amortissable") && flux.getPrets().get(index).getType().equalsIgnoreCase("Amortissable")) {
                    choix = waitForElement1(driver, By.xpath("//option[@value='IMMO_AMORTISSABLE']"), 20, 1);
                    choix.click();
                    clickEnter(actions);
                    break;
                } else if (opt.getText().equalsIgnoreCase("Prêt à taux zéro") && flux.getPrets().get(index).getType().equalsIgnoreCase("Prêt à taux zéro")) {
                    choix = waitForElement1(driver, By.xpath("//option[@value='PTZ']"), 20, 1);
                    choix.click();
                    clickEnter(actions);
                    break;
                } else if (opt.getText().equalsIgnoreCase("Prêt in fine") && flux.getPrets().get(index).getType().equalsIgnoreCase("Prêt in fine")) {
                    choix = waitForElement1(driver, By.xpath("//option[@value='IMMO_IN_FINE']"), 20, 1);
                    choix.click();
                    clickEnter(actions);
                    break;
                } else if (opt.getText().equalsIgnoreCase("Prêt relais") && flux.getPrets().get(index).getType().equalsIgnoreCase("Prêt relais")) {
                    choix = waitForElement1(driver, By.xpath("//option[@value='RELAIS']"), 20, 1);
                    choix.click();
                    clickEnter(actions);
                    break;
                } else if (opt.getText().equalsIgnoreCase("Crédit Bail et LOA") && flux.getPrets().get(index).getType().equalsIgnoreCase("Crédit bail")) {
                    choix = waitForElement1(driver, By.xpath("//option[@value='CREDIT_BAIL']"), 20, 1);
                    choix.click();
                    clickEnter(actions);
                    break;
                }
            }
        }
    }

    @Override
    protected void pageName(String namePage) {}

}
