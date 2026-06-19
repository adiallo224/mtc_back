package com.mtc.mutuaConseil.services.servicesImpl.assuPret;

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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class AprilService extends PageElementInteraction implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(AprilService.class);
    public static WebDriver driver;
    private Actions actions;
    private String source ="AprilPret";
    boolean pretTauxZero;
    private JavascriptExecutor js = null;
    private final TypeAssuranceService typeAssuranceService;

    public AprilService(TypeAssuranceService typeAssuranceService){
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        log.info("Début de traitement -- APRIL --");
        Tarif tarifAprilPret = TarifUtils.createDefaultTarif(c, typeAssuranceService, 1L);
        pretTauxZero = false;
        driver = new EdgeDriver();
        driver.manage().window().maximize();
        actions = new Actions(driver);
        js = (JavascriptExecutor) driver;
        driver.get(c.getUrlFournisseur());
        try {
            inputByName(driver, "username", c.getUsername());
            inputByName(driver, "password", c.getPassword());
            clicButtonByXpath(driver, "//button[@class=\"login__button\"]", actions);
            waitThread(2);
            fermetureNQA(driver, "chat-bot-close-icon");
            // informations projet
            remplirInformationsProjet(flux, source);
            //Informations personnes
            remplirInformationsPerso(flux, source);
            //Informations prets
            remplirInformationsPret(flux, source);
            waitThread(5);
            WebElement enter = waitForElement(driver, By.xpath("//div[@class=\"select-needs-survey\"]"), 15, 1);
            actions.click(enter);
            nextPage(driver, By.xpath("//button[@id='market-place_borrower_client-needs_button_go-to-next-step']"), 15,1);
            // Récupérer le premier loan-comparator-rate
            // WebElement premierLoanComparator = driver.findElement(By.xpath("//loan-comparator-rate-container//loan-comparator-rate[position() = 1]"));
            waitThread(8);
            WebElement coutComparator = waitForElement(driver, By.xpath("//loan-comparator-rate-container//loan-comparator-rate[position() = 1]//div//*[contains(@class, 'price') and contains(text(), '€') ]"), 15, 1);
            log.info("Texte du premier loan-comparator-rate : {}", coutComparator.getText());
            String cout = coutComparator.getText();
            log.info("cout {}", cout);
            tarifAprilPret.setMontant(cout);
            String screenshotBytes = captureScreenshot(driver, tarifAprilPret.getNom(), false, tarifAprilPret);
            if (screenshotBytes != null) {
                tarifAprilPret.setCaptureImg(screenshotBytes);
            }
            tarifAprilPret.setExecution(true);
            log.info("Fin de traitement -- APRIL --");
        } catch (Exception e) {
            log.error("An error occurred", e);
            tarifAprilPret.setErreur(e.getMessage());
            String screenshotBytesErreur = captureScreenshot(driver, tarifAprilPret.getNom(), true, tarifAprilPret);
            tarifAprilPret.setCaptureImgErreur(screenshotBytesErreur);
            tarifAprilPret.setEtape("");
        } finally {
            driver.quit();
        }
      return tarifAprilPret ;
    }

    private void remplirInformationsProjet(FluxData flux, String source) {
        int index = 0;
        if (flux.getPrets().get(index).getNouveauOuReprise() != null) {
            waitThread(2);
            clicButtonByXpath(driver, "//span[@id=\"radio-input_market-place_borrower_client-needs_project-nature_project-nature_project-nature_aon-choice_0_label_checkmark\"]", actions);
        } else if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame")){
            waitThread(2);
            clicButtonByXpath(driver, "//span[@id=\"radio-input_market-place_borrower_client-needs_project-nature_project-nature_project-nature_aon-choice_0_label_checkmark\"]", actions);
        }
        //Type projet
        clicButtonByXpath(driver, "//ng-select[@id=\"market-place_borrower_client-needs_project-nature_project-nature_project-type_input-select_0_projectObject\"]",actions);
        if (flux.getPrets().get(index).getObjet().equalsIgnoreCase("Résidence principale"))
            clicButtonByXpath(driver, "//span[text()='Résidence principale']",actions);
        //Souscripteur
        clicButtonByXpath(driver, "//span[@id=\"radio-input_market-place_borrower_client-needs_project-nature_project-nature_subscriber-type_aon-choice_0_label_checkmark\"]",actions);
        //Date effet
        inputByXpath(driver,"//input[@id=\"market-place_borrower_client-needs_project-nature_project-nature_effective-date_date-picker_input\"]", flux.getPrets().get(index).getDateEffet());
    }

    private void remplirInformationsPerso(FluxData flux, String source) {
        waitThread(4);
        int index = 0;
        if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur")) {
            WebElement radioCivilite = waitForElement(driver,By.xpath("//span[@id=\"radio-input_market-place_borrower_client-needs_insureds_insureds-container_insured_0_insured-person_civility_aon-choice_0_label_checkmark\"]"), 15, 1);
            actions.moveToElement(radioCivilite).click().perform();
        } else if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame")){
            WebElement radioCivilite = waitForElement(driver,By.xpath("//*[@id=\"radio-input_market-place_borrower_client-needs_insureds_insureds-container_insured_0_insured-person_civility_aon-choice_1_label_checkmark\"]"), 15, 1);
            actions.moveToElement(radioCivilite).click().perform();
        }
        inputByXpath(driver,"//input[@id=\"market-place_borrower_client-needs_insureds_insureds-container_insured_0_insured-person_first-name_input-text_input\"]", flux.getPersonnes().get(index).getPrenom());
        //nom
        inputByXpath(driver,"//input[@id=\"market-place_borrower_client-needs_insureds_insureds-container_insured_0_insured-person_surname_input-text_input\"]", flux.getPersonnes().get(index).getNom());
        //Date de naissance
        inputByXpath(driver, "//input[@id=\"market-place_borrower_client-needs_insureds_insureds-container_insured_0_insured-person_birth-date_date-picker_input\"]", flux.getPersonnes().get(index).getDateNaissance());
        //code postal
        inputByXpath(driver,"//input[@id=\"market-place_borrower_client-needs_insureds_insureds-container_insured_0_insured-person_address_input-zipcode_input\"]", flux.getPersonnes().get(index).getCodePostal());
        //Statut
        WebElement inputStatut = waitForElement(driver, By.xpath("//ng-select[@id=\"market-place_borrower_client-needs_insureds_insureds-container_insured_0_insured-person_professional-status_input-select_0_professionalStatusRefId\"]"),15, 1);
        actions.moveToElement(inputStatut).click().perform();
        categorieProfession(flux, index);

        WebElement inputProfession = waitForElement(driver, By.xpath("//*[@id=\"market-place_borrower_client-needs_insureds_insureds-container_insured_0_insured-person_profession_input-select_0_jobRefId\"]/div/div/div[2]/input"), 15, 1);
        waitThread(2);
        inputProfession.sendKeys(flux.getPersonnes().get(index).getProfession().toUpperCase());
        waitThread(4);
        List<WebElement> optionsProfession = driver.findElements(By.xpath("//ng-select[@id=\"market-place_borrower_client-needs_insureds_insureds-container_insured_0_insured-person_profession_input-select_0_jobRefId\"]//ng-dropdown-panel//div[@role=\"listbox\"]"));
        List<String> optionsList = new ArrayList<>(Arrays.asList(optionsProfession.get(0).getText().split("\n")));
        boolean foundOption = false;
        for (String option : optionsList) {
            if (option.trim().equalsIgnoreCase(flux.getPersonnes().get(index).getProfession())) {
                WebElement eleOption = driver.findElement(By.xpath("//ng-option//span[normalize-space(text())='" + option.trim() + "']"));
                actions.moveToElement(eleOption).click().perform();
                foundOption = true;
                break;
            }
        }
        if (!foundOption) {
            for (String option : optionsList) {
                 if (option.trim().toLowerCase().contains(flux.getPersonnes().get(index).getProfession().toLowerCase())) {
                     WebElement eleOption = driver.findElement(By.xpath("//ng-option//span[contains(text(),'" + option.trim() + "')]"));
                     actions.moveToElement(eleOption).click().perform();
                     break;
                 }
            }
        }
        //fumeur
        WebElement radioFumeur = null;
        WebElement radioFumeurYes = null;
        if (!flux.getInfoAssureComplets().get(index).getFumeur()) {
            radioFumeur = waitForElement(driver, By.xpath("//div[@id='market-place_borrower_client-needs_insureds_insureds-container_insured_0_insured-person_is-smoking_no_input-checkbox']//span[2]"), 15, 1);
            waitThread(1);
            actions.moveToElement(radioFumeur).click().perform();
        } else {
            radioFumeurYes = waitForElement(driver, By.xpath("//div[@id='market-place_borrower_client-needs_insureds_insureds-container_insured_0_insured-person_is-smoking_yes_input-checkbox']//span[2]"), 15, 1);
            actions.moveToElement(radioFumeurYes).click().perform();
        }
        //Démenager etranger
        if (!flux.getInfoAssureComplets().get(index).getDeplacementEtranger60()) {
            WebElement radioDemenageEtranger = waitForElement(driver, By.xpath("//div[@id='market-place_borrower_client-needs_insureds_insureds-container_insured_0_insured-person_moving-foreign-country_no_input-checkbox']//span[2]"), 15, 1);
            actions.moveToElement(radioDemenageEtranger).click().perform();
        } else {
            WebElement radioDemenageEtranger = waitForElement(driver, By.xpath("//div[@id='market-place_borrower_client-needs_insureds_insureds-container_insured_0_insured-person_moving-foreign-country_yes_input-checkbox']//span[2]"), 15, 1);
            actions.moveToElement(radioDemenageEtranger).click().perform();
        }
        //Deplacement
        if (!flux.getInfoAssureComplets().get(index).getDeplacementPro20000()) {
            WebElement radioDeplacement =  waitForElement(driver, By.xpath( "//div[@id='market-place_borrower_client-needs_insureds_insureds-container_insured_0_insured-person_long-distance-business-trip_no_input-checkbox']//span[2]"), 15, 1);
            actions.moveToElement(radioDeplacement).click().perform();
        } else {
            WebElement radioDeplacement =  waitForElement(driver, By.xpath( "//div[@id='market-place_borrower_client-needs_insureds_insureds-container_insured_0_insured-person_long-distance-business-trip_yes_input-checkbox']//span[2]"), 15, 1);
            actions.moveToElement(radioDeplacement).click().perform();
        }
        //Manumentation
        if (!flux.getInfoAssureComplets().get(index).getTravailManuel()) {
            WebElement radioManumentation = waitForElement(driver, By.xpath("//div[@id='market-place_borrower_client-needs_insureds_insureds-container_insured_0_insured-person_heavy-handling_no_input-checkbox']//span[2]"), 15, 1);
            actions.moveToElement(radioManumentation).click().perform();
        } else {
            WebElement radioManumentation = waitForElement(driver, By.xpath("//div[@id='market-place_borrower_client-needs_insureds_insureds-container_insured_0_insured-person_heavy-handling_yes_input-checkbox']//span[2]"), 15, 1);
            actions.moveToElement(radioManumentation).click().perform();
        }
        //Activite hauteur
        if (flux.getInfoAssureComplets().get(index).getTravailHauteur()) {
            if (flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("15 à 20m") || flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("Plus de 20m")) {
                WebElement radioActiviteHauteur = waitForElement(driver, By.xpath("//div[@id='market-place_borrower_client-needs_insureds_insureds-container_insured_0_insured-person_working-at-height_yes_input-checkbox']//span[2]"), 15, 1);
                actions.moveToElement(radioActiviteHauteur).click().perform();
            } else {
                WebElement radioActiviteHauteur = waitForElement(driver, By.xpath("//div[@id='market-place_borrower_client-needs_insureds_insureds-container_insured_0_insured-person_working-at-height_no_input-checkbox']//span[2]"), 15, 1);
                actions.moveToElement(radioActiviteHauteur).click().perform();
            }
        } else {
            WebElement radioActiviteHauteur = waitForElement(driver, By.xpath("//div[@id='market-place_borrower_client-needs_insureds_insureds-container_insured_0_insured-person_working-at-height_no_input-checkbox']//span[2]"), 15, 1);
            actions.moveToElement(radioActiviteHauteur).click().perform();
        }

        // Vérifier si la checkbox radioFumeur est cochée ou non
//        if (!flux.getInfoAssureComplets().get(index).getFumeur()) {
//            if (!radioFumeur.isSelected()) {
//                // Si la checkbox n'est pas cochée, on la coche
//                radioFumeur.click();
//            }
//        }
        //Credit en cours
        WebElement radioCreditEnCours = waitForElement(driver, By.xpath("//div[@id='market-place_borrower_client-needs_insureds_insureds-container_insured_0_insured-person_has-existing-loan_no_input-checkbox']//span[2]"), 15, 1);
        actions.moveToElement(radioCreditEnCours).click().perform();

        if (flux.getPersonnes().size() == 2) {
            waitThread(4);
            index = 1;
            WebElement buttonAjouterPersonne = waitForElement(driver, By.xpath("//*[@id=\"market-place_borrower_client-needs_insureds_insureds-container_div_add-insured\"]"), 10, 1);
            actions.moveToElement(buttonAjouterPersonne).click().perform();

            if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur")) {
                WebElement radioCivilite = waitForElement(driver,By.xpath("//*[@id=\"radio-input_market-place_borrower_client-needs_insureds_insureds-container_insured_1_insured-person_civility_aon-choice_0_label_checkmark\"]"), 15, 1);
                actions.moveToElement(radioCivilite).click().perform();
            } else if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame")){
                WebElement radioCivilite = waitForElement(driver,By.xpath("//*[@id=\"radio-input_market-place_borrower_client-needs_insureds_insureds-container_insured_1_insured-person_civility_aon-choice_1_label_checkmark\"]"), 5, 1);
                actions.moveToElement(radioCivilite).click().perform();
            }
            inputByXpath(driver,"//*[@id=\"market-place_borrower_client-needs_insureds_insureds-container_insured_1_insured-person_first-name_input-text_input\"]", flux.getPersonnes().get(index).getPrenom());
            //nom
            inputByXpath(driver,"//*[@id=\"market-place_borrower_client-needs_insureds_insureds-container_insured_1_insured-person_surname_input-text_input\"]", flux.getPersonnes().get(index).getNom());
            //Date de naissance
            inputByXpath(driver, "//*[@id=\"market-place_borrower_client-needs_insureds_insureds-container_insured_1_insured-person_birth-date_date-picker_input\"]", flux.getPersonnes().get(index).getDateNaissance());
            //code postal
            inputByXpath(driver,"//*[@id=\"market-place_borrower_client-needs_insureds_insureds-container_insured_1_insured-person_address_input-zipcode_input\"]", flux.getPersonnes().get(index).getCodePostal());
            //Statut
            WebElement inputStatut1 = waitForElement(driver, By.xpath("//*[@id=\"market-place_borrower_client-needs_insureds_insureds-container_insured_1_insured-person_professional-status_input-select_1_professionalStatusRefId\"]"),15, 1);
            actions.moveToElement(inputStatut1).click().perform();
            categorieProfession(flux, index);

            WebElement inputProfession1 = waitForElement(driver, By.xpath("//*[@id=\"market-place_borrower_client-needs_insureds_insureds-container_insured_1_insured-person_profession_input-select_1_jobRefId\"]/div/div/div[2]/input"), 15, 1);
            waitThread(2);
            inputProfession1.sendKeys(flux.getPersonnes().get(index).getProfession().toUpperCase());
            waitThread(2);
            List<WebElement> optionsProfession1 = driver.findElements(By.xpath("//ng-select[@id=\"market-place_borrower_client-needs_insureds_insureds-container_insured_1_insured-person_profession_input-select_1_jobRefId\"]//ng-dropdown-panel//div[@role=\"listbox\"]"));
            List<String> optionsList1 = new ArrayList<>(Arrays.asList(optionsProfession1.get(0).getText().split("\n")));
            boolean foundOption1 = false;
            for (String option : optionsList1) {
                if (option.trim().equalsIgnoreCase(flux.getPersonnes().get(index).getProfession())) {
                    WebElement eleOption = driver.findElement(By.xpath("//ng-option//span[normalize-space(text())='" + option.trim() + "']"));
                    actions.moveToElement(eleOption).click().perform();
                    foundOption1 = true;
                    break;
                }
            }
            if (!foundOption1) {
                for (String option : optionsList1) {
                    if (option.trim().toLowerCase().contains(flux.getPersonnes().get(index).getProfession().toLowerCase())) {
                        WebElement eleOption = driver.findElement(By.xpath("//ng-option//span[contains(text(),'" + option.trim() + "')]"));
                        actions.moveToElement(eleOption).click().perform();
                        break;
                    }
                }
            }
            //fumeur
            waitThread(2);
            if (!flux.getInfoAssureComplets().get(index).getFumeur()) {
                WebElement radioFumeur1 = waitForElement(driver, By.xpath("//div[@id='market-place_borrower_client-needs_insureds_insureds-container_insured_1_insured-person_is-smoking_no_input-checkbox']//span[2]"), 15, 1);
                actions.moveToElement(radioFumeur1).click().perform();
            } else {
                WebElement radioFumeur1 = waitForElement(driver, By.xpath("//div[@id='market-place_borrower_client-needs_insureds_insureds-container_insured_1_insured-person_is-smoking_yes_input-checkbox']//span[2]"), 15, 1);
                actions.moveToElement(radioFumeur1).click().perform();
            }
            //Démenager étranger
            if (!flux.getInfoAssureComplets().get(index).getDeplacementEtranger60()) {
                WebElement radioDemenageEtranger1 = waitForElement(driver, By.xpath("//div[@id='market-place_borrower_client-needs_insureds_insureds-container_insured_1_insured-person_moving-foreign-country_no_input-checkbox']//span[2]"), 15, 1);
                actions.moveToElement(radioDemenageEtranger1).click().perform();
            } else {
                WebElement radioDemenageEtranger1 = waitForElement(driver, By.xpath("//div[@id='market-place_borrower_client-needs_insureds_insureds-container_insured_1_insured-person_moving-foreign-country_yes_input-checkbox']//span[2]"), 15, 1);
                actions.moveToElement(radioDemenageEtranger1).click().perform();
            }
            //Deplacement
            if (!flux.getInfoAssureComplets().get(index).getDeplacementPro20000()) {
                WebElement radioDeplacement1 = waitForElement(driver, By.xpath("//div[@id='market-place_borrower_client-needs_insureds_insureds-container_insured_1_insured-person_long-distance-business-trip_no_input-checkbox']//span[2]"), 15, 1);
                actions.moveToElement(radioDeplacement1).click().perform();
            } else {
                WebElement radioDeplacement1 = waitForElement(driver, By.xpath("//div[@id='market-place_borrower_client-needs_insureds_insureds-container_insured_1_insured-person_long-distance-business-trip_yes_input-checkbox']//span[2]"), 15, 1);
                actions.moveToElement(radioDeplacement1).click().perform();
            }
            //Manumentation
            if (!flux.getInfoAssureComplets().get(index).getTravailManuel()) {
                WebElement radioManumentation1 = waitForElement(driver, By.xpath("//div[@id='market-place_borrower_client-needs_insureds_insureds-container_insured_1_insured-person_heavy-handling_no_input-checkbox']//span[2]"), 15, 1);
                actions.moveToElement(radioManumentation1).click().perform();
            } else {
                WebElement radioManumentation1 = waitForElement(driver, By.xpath("//div[@id='market-place_borrower_client-needs_insureds_insureds-container_insured_1_insured-person_heavy-handling_yes_input-checkbox']//span[2]"), 15, 1);
                actions.moveToElement(radioManumentation1).click().perform();
            }
            //Activite hauteur
            if (flux.getInfoAssureComplets().get(index).getTravailHauteur()) {
                if (flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("15 à 20m") || flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("Plus de 20m")) {
                    WebElement radioActiviteHauteur1 = waitForElement(driver, By.xpath("//div[@id='market-place_borrower_client-needs_insureds_insureds-container_insured_1_insured-person_working-at-height_yes_input-checkbox']//span[2]"), 15, 1);
//                actions.moveToElement(radioActiviteHauteur1).click().perform();
                } else {
                    WebElement radioActiviteHauteur1 = waitForElement(driver, By.xpath("//div[@id='market-place_borrower_client-needs_insureds_insureds-container_insured_1_insured-person_working-at-height_no_input-checkbox']//span[2]"), 15, 1);
                    actions.moveToElement(radioActiviteHauteur1).click().perform();
                }
            } else {
                WebElement radioActiviteHauteur1 = waitForElement(driver, By.xpath("//div[@id='market-place_borrower_client-needs_insureds_insureds-container_insured_1_insured-person_working-at-height_no_input-checkbox']//span[2]"), 15, 1);
                actions.moveToElement(radioActiviteHauteur1).click().perform();
            }

            //Credit en cours
            WebElement radioCreditEnCours1 =  waitForElement(driver, By.xpath( "//div[@id='market-place_borrower_client-needs_insureds_insureds-container_insured_1_insured-person_has-existing-loan_no_input-checkbox']//span[2]"), 15, 1);
            actions.moveToElement(radioCreditEnCours1).click().perform();
        }
    }

    private void remplirInformationsPret(FluxData flux, String source) {
        waitThread(5);
        int index = 0;
        //pret
        WebElement optPret = waitForElement1(driver, By.xpath("//ng-select[@id=\"market-place_borrower_client-needs_loans_loans-container_0_loan-component_loan-type_input-select_0_type\"]"),10, 1);
        actions.moveToElement(optPret).click().perform();
        waitThread(2);
        choixTypePret(flux, index);
        //Organisme
        choixBanque(flux, index, By.xpath("//div[@role=\"listbox\"]"));
        //Montant
        inputByXpath(driver,"//input[@id='market-place_borrower_client-needs_loans_loans-container_0_loan-component_currency_input-dual_input']", flux.getPrets().get(index).getMontantPret());
        //Type taux
        if (flux.getPrets().get(index).getTypeTaux().equalsIgnoreCase("Fixe")) {
            WebElement optTaux = waitForElement1(driver, By.xpath("//label[@id=\"radio-input_market-place_borrower_client-needs_loans_loans-container_0_loan-component_rate-type_aon-choice_0_label\"]"),10, 1);
            actions.moveToElement(optTaux).click().perform();
        } else if (flux.getPrets().get(index).getTypeTaux().equalsIgnoreCase("Variable")) {
            WebElement optTaux = waitForElement1(driver, By.xpath("//label[@id=\"radio-input_market-place_borrower_client-needs_loans_loans-container_0_loan-component_rate-type_aon-choice_1_label\"]"),10, 1);
            actions.moveToElement(optTaux).click().perform();
        }
        //Taux
        if (!pretTauxZero) {
            inputByXpath(driver, "//input[@id=\"market-place_borrower_client-needs_loans_loans-container_0_loan-component_interest-rate_input-text_input\"]", flux.getPrets().get(index).getTaux());
        }
        //Duree pret
        inputByXpath(driver,"//input[@id='market-place_borrower_client-needs_loans_loans-container_0_loan-component_duration-in-month_input-text_input']", flux.getPrets().get(index).getDuree());

        if (!flux.getPrets().get(index).getDiffere().isEmpty() && (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Partiel") || flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Total"))) {
            inputByXpath(driver,"//input[@id='market-place_borrower_client-needs_loans_loans-container_0_loan-component_deffered_grace-months-period_input-text_input']", flux.getPrets().get(index).getDureeDiffere());
            if (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Partiel")) {
                WebElement optTaux = waitForElement1(driver, By.xpath("//label[@id=\"radio-input_market-place_borrower_client-needs_loans_loans-container_0_loan-component_deffered_delayed-loan_aon-choice_1_label\"]"), 10, 1);
                actions.moveToElement(optTaux).click().perform();
            }
            if (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Total")) {
                WebElement optTaux = waitForElement1(driver, By.xpath("//label[@id=\"radio-input_market-place_borrower_client-needs_loans_loans-container_0_loan-component_deffered_delayed-loan_aon-choice_0_label\"]"), 10, 1);
                actions.moveToElement(optTaux).click().perform();
            }
        }
        couverture(driver, index, flux);
        if (flux.getPrets().size() == 2) {
            index = 1;
            waitThread(2);
            WebElement ajoutPret = waitForElement(driver, By.xpath( "//div[@id='market-place_borrower_client-needs_loans_loans-container_button_add-new-loan_text']"), 15, 1);
            actions.moveToElement(ajoutPret).click().perform();
            WebElement optPret1 = waitForElement(driver, By.xpath("//ng-select[@id=\"market-place_borrower_client-needs_loans_loans-container_1_loan-component_loan-type_input-select_1_type\"]"),15, 1);
            actions.moveToElement(optPret1).click().perform();
            choixTypePret(flux, index);
            //Organisme
            choixBanque(flux, index, By.xpath("//div[@role=\"listbox\"]"));
            //Montant
            inputByXpath(driver,"//input[@id='market-place_borrower_client-needs_loans_loans-container_1_loan-component_currency_input-dual_input']", flux.getPrets().get(index).getMontantPret());
            //Type taux
            if (flux.getPrets().get(index).getTypeTaux().equalsIgnoreCase("Fixe")) {
                WebElement optTaux = waitForElement1(driver, By.xpath("//label[@id=\"radio-input_market-place_borrower_client-needs_loans_loans-container_1_loan-component_rate-type_aon-choice_0_label\"]"),10, 1);
                actions.moveToElement(optTaux).click().perform();
            } else if (flux.getPrets().get(index).getTypeTaux().equalsIgnoreCase("Variable")) {
                WebElement optTaux = waitForElement1(driver, By.xpath("//label[@id=\"radio-input_market-place_borrower_client-needs_loans_loans-container_1_loan-component_rate-type_aon-choice_1_label\"]"),10, 1);
                actions.moveToElement(optTaux).click().perform();
            }
            //Taux
            if (!pretTauxZero) {
                inputByXpath(driver, "//input[@id='market-place_borrower_client-needs_loans_loans-container_1_loan-component_interest-rate_input-text_input']", flux.getPrets().get(index).getTaux());
            }
            //Duree pret
            inputByXpath(driver,"//input[@id='market-place_borrower_client-needs_loans_loans-container_1_loan-component_duration-in-month_input-text_input']", flux.getPrets().get(index).getDuree());

            if (!flux.getPrets().get(index).getDiffere().isEmpty() && (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Partiel") || flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Total"))) {
                inputByXpath(driver,"//input[@id='market-place_borrower_client-needs_loans_loans-container_1_loan-component_deffered_grace-months-period_input-text_input']", flux.getPrets().get(index).getDureeDiffere());
                if (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Partiel")) {
                    WebElement optTaux = waitForElement1(driver, By.xpath("//label[@id=\"radio-input_market-place_borrower_client-needs_loans_loans-container_1_loan-component_deffered_delayed-loan_aon-choice_1_label\"]"), 10, 1);
                    actions.moveToElement(optTaux).click().perform();
                }
                if (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Total")) {
                    WebElement optTaux = waitForElement1(driver, By.xpath("//label[@id=\"radio-input_market-place_borrower_client-needs_loans_loans-container_1_loan-component_deffered_delayed-loan_aon-choice_0_label\"]"), 10, 1);
                    actions.moveToElement(optTaux).click().perform();
                }
            }
            couverture(driver, index, flux);
        }
    }

    private void couverture(WebDriver driver, int index, FluxData flux) {
        WebElement buttonCouverture;
        if (index == 0) {
            waitThread(2);
            buttonCouverture = waitForElement(driver, By.id("market-place_borrower_client-needs_loans_loans-container_0_loan-component_edit-minimal-warranties"), 10, 1);
            actions.moveToElement(buttonCouverture).click().perform();
        }
        if (index == 1) {
            waitThread(2);
            buttonCouverture = waitForElement(driver, By.id("market-place_borrower_client-needs_loans_loans-container_1_loan-component_edit-minimal-warranties"), 10, 1);
            actions.moveToElement(buttonCouverture).click().perform();
        }
        if (flux.getPersonnes().size() == 1) {
            if (flux.getInfoAssureComplets().get(0).getGarantie().equalsIgnoreCase("TPT")) {
                waitThread(2);
                WebElement checkboxITT = waitForElement(driver, By.xpath("/html/body/modal-container/div[2]/div/loan-warranties-modal/div/div[2]/div[1]/loan-warranties-edition/div/div[2]/div[3]/div/div[2]/div/div/loan-warranty-edition/div/div[1]/lb-aon-toggle/div/div[2]/button"), 10, 1);
                checkboxITT.click();
                WebElement checkboxIPP = waitForElement(driver, By.xpath("/html/body/modal-container/div[2]/div/loan-warranties-modal/div/div[2]/div[1]/loan-warranties-edition/div/div[2]/div[5]/div/div[2]/div/div/loan-warranty-edition/div/div/lb-aon-toggle/div/div[2]/button"), 10, 1);
                checkboxIPP.click();
            }
            if (flux.getInfoAssureComplets().get(0).getGarantie().equalsIgnoreCase("IPT, ITT")) {
                waitThread(2);
                WebElement checkboxIPP = waitForElement(driver, By.xpath("/html/body/modal-container/div[2]/div/loan-warranties-modal/div/div[2]/div[1]/loan-warranties-edition/div/div[2]/div[5]/div/div[2]/div/div/loan-warranty-edition/div/div/lb-aon-toggle/div/div[2]/button"), 10, 1);
                checkboxIPP.click();
            }
            if (flux.getInfoAssureComplets().get(0).getGarantie().equalsIgnoreCase("IPT, ITT, IPP")) {

            }
            if (flux.getInfoAssureComplets().get(0).getGarantieChomage()) {
                waitThread(2);
                WebElement checkboxChomage = waitForElement(driver, By.xpath("/html/body/modal-container/div[2]/div/loan-warranties-modal/div/div[2]/div[1]/loan-warranties-edition/div/div[2]/div[10]/div/div[2]/div/div/loan-warranty-edition/div/div/lb-aon-toggle/div/div[2]/button"), 10, 1);
                checkboxChomage.click();
            }
        }
        if (flux.getPersonnes().size() == 2) {
            if (index == 0) {
                if (flux.getInfoAssureComplets().get(index).getGarantie().equalsIgnoreCase("TPT")) {
                    waitThread(2);
                    WebElement checkboxITT = waitForElement(driver, By.xpath("/html/body/modal-container/div[2]/div/loan-warranties-modal/div/div[2]/div[1]/loan-warranties-edition/div/div[2]/div[3]/div/div[2]/div/div/loan-warranty-edition/div/div[1]/lb-aon-toggle/div/div[2]/button"), 10, 1);
                    checkboxITT.click();
                    WebElement checkboxIPP = waitForElement(driver, By.xpath("/html/body/modal-container/div[2]/div/loan-warranties-modal/div/div[2]/div[1]/loan-warranties-edition/div/div[2]/div[5]/div/div[2]/div/div/loan-warranty-edition/div/div/lb-aon-toggle/div/div[2]/button"), 10, 1);
                    checkboxIPP.click();
                }
                if (flux.getInfoAssureComplets().get(index).getGarantie().equalsIgnoreCase("IPT, ITT")) {
                    waitThread(2);
                    WebElement checkboxIPP = waitForElement(driver, By.xpath("/html/body/modal-container/div[2]/div/loan-warranties-modal/div/div[2]/div[1]/loan-warranties-edition/div/div[2]/div[5]/div/div[2]/div/div/loan-warranty-edition/div/div/lb-aon-toggle/div/div[2]/button"), 10, 1);
                    checkboxIPP.click();
                }
                if (flux.getInfoAssureComplets().get(index).getGarantie().equalsIgnoreCase("IPT, ITT, IPP")) {

                }
                if (flux.getInfoAssureComplets().get(index).getGarantieChomage()) {
                    waitThread(2);
                    WebElement checkboxChomage = waitForElement(driver, By.xpath("/html/body/modal-container/div[2]/div/loan-warranties-modal/div/div[2]/div[1]/loan-warranties-edition/div/div[2]/div[10]/div/div[2]/div/div/loan-warranty-edition/div/div/lb-aon-toggle/div/div[2]/button"), 10, 1);
                    checkboxChomage.click();
                }
            }
            if (index == 1) {
                if (flux.getInfoAssureComplets().get(index).getGarantie().equalsIgnoreCase("TPT")) {
                    waitThread(2);
                    WebElement checkboxITT = waitForElement(driver, By.xpath("/html/body/modal-container/div[2]/div/loan-warranties-modal/div/div[2]/div[1]/loan-warranties-edition/div/div[2]/div[3]/div/div[2]/div/div/loan-warranty-edition/div/div[1]/lb-aon-toggle/div/div[2]/button"), 10, 1);
                    checkboxITT.click();
                    WebElement checkboxIPP = waitForElement(driver, By.xpath("/html/body/modal-container/div[2]/div/loan-warranties-modal/div/div[2]/div[1]/loan-warranties-edition/div/div[2]/div[5]/div/div[2]/div/div/loan-warranty-edition/div/div/lb-aon-toggle/div/div[2]/button"), 10, 1);
                    checkboxIPP.click();
                }
                if (flux.getInfoAssureComplets().get(index).getGarantie().equalsIgnoreCase("IPT, ITT")) {
                    waitThread(2);
                    WebElement checkboxIPP = waitForElement(driver, By.xpath("/html/body/modal-container/div[2]/div/loan-warranties-modal/div/div[2]/div[1]/loan-warranties-edition/div/div[2]/div[5]/div/div[2]/div/div/loan-warranty-edition/div/div/lb-aon-toggle/div/div[2]/button"), 10, 1);
                    checkboxIPP.click();
                }
                if (flux.getInfoAssureComplets().get(index).getGarantie().equalsIgnoreCase("IPT, ITT, IPP")) {

                }
                if (flux.getInfoAssureComplets().get(index).getGarantieChomage()) {
                    waitThread(2);
                    WebElement checkboxChomage = waitForElement(driver, By.xpath("/html/body/modal-container/div[2]/div/loan-warranties-modal/div/div[2]/div[1]/loan-warranties-edition/div/div[2]/div[10]/div/div[2]/div/div/loan-warranty-edition/div/div/lb-aon-toggle/div/div[2]/button"), 10, 1);
                    checkboxChomage.click();
                }
            }
        }
        waitThread(1);
        WebElement buttonValiderCouverture = waitForElement(driver, By.id("market-place_borrower_client-needs_loans-warranties-modal_update-minimal-warranties"), 10, 1);
        buttonValiderCouverture.click();
    }

    private void choixTypePret(FluxData flux, int index) {
        WebElement selectPret = null;
        if (index == 0) {
//            selectPret = waitForElement(driver, By.xpath("//ng-select[@id=\"market-place_borrower_client-needs_loans_loans-container_0_loan-component_loan-type_input-select_0_type\"]"),15, 1);
//            actions.moveToElement(selectPret).click().perform();
            if (!flux.getPrets().get(index).getDiffere().isEmpty() && (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Partiel") || flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Total"))) {
                WebElement optionDiffere = waitForElement(driver, By.xpath("//ng-option[@id=\"market-place_borrower_client-needs_loans_loans-container_0_loan-component_loan-type_input-select_Differe\"]"), 15, 1);
                actions.moveToElement(optionDiffere).click().perform();
            } else {
                if (flux.getPrets().get(index).getType().contains("Amortissable")) {
                    WebElement optionClassique = waitForElement(driver, By.xpath("//ng-option[@id=\"market-place_borrower_client-needs_loans_loans-container_0_loan-component_loan-type_input-select_Classique\"]"), 15, 1);
                    actions.moveToElement(optionClassique).click().perform();
                }
                if (flux.getPrets().get(index).getType().contains("Prêt à taux zéro")) {
                    pretTauxZero = true;
                }
                if (flux.getPrets().get(index).getType().contains("Prêt in fine")) {
                    WebElement optionClassique = waitForElement(driver, By.xpath("//ng-option[@id=\"market-place_borrower_client-needs_loans_loans-container_0_loan-component_loan-type_input-select_Classique\"]"), 15, 1);
                    actions.moveToElement(optionClassique).click().perform();
                }
                if (flux.getPrets().get(index).getType().contains("Prêt relais")) {
                    WebElement optionClassique = waitForElement(driver, By.xpath("//ng-option[@id=\"market-place_borrower_client-needs_loans_loans-container_0_loan-component_loan-type_input-select_Classique\"]"), 15, 1);
                    actions.moveToElement(optionClassique).click().perform();
                }
                if (flux.getPrets().get(index).getType().contains("Prêt à paliers")) {
                    WebElement optionClassique = waitForElement(driver, By.xpath("//ng-option[@id=\"market-place_borrower_client-needs_loans_loans-container_0_loan-component_loan-type_input-select_Classique\"]"), 15, 1);
                    actions.moveToElement(optionClassique).click().perform();
                }
                if (flux.getPrets().get(index).getType().contains("Crédit bail")) {
                    WebElement optionClassique = waitForElement(driver, By.xpath("//ng-option[@id=\"market-place_borrower_client-needs_loans_loans-container_0_loan-component_loan-type_input-select_Classique\"]"), 15, 1);
                    actions.moveToElement(optionClassique).click().perform();
                }
            }
        }
        if (index == 1) {
//            selectPret = waitForElement(driver, By.xpath("//ng-select[@id=\"market-place_borrower_client-needs_loans_loans-container_1_loan-component_loan-type_input-select_0_type\"]"),15, 1);
//            actions.moveToElement(selectPret).click().perform();
            if (!flux.getPrets().get(index).getDiffere().isEmpty() && (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Partiel") || flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Total"))) {
                WebElement optionDiffere = waitForElement(driver, By.xpath("//ng-option[@id=\"market-place_borrower_client-needs_loans_loans-container_1_loan-component_loan-type_input-select_Differe\"]"), 15, 1);
                actions.moveToElement(optionDiffere).click().perform();
            } else {
                if (flux.getPrets().get(index).getType().contains("Amortissable")) {
                    WebElement optionClassique = waitForElement(driver, By.xpath("//ng-option[@id=\"market-place_borrower_client-needs_loans_loans-container_1_loan-component_loan-type_input-select_Classique\"]"), 15, 1);
                    actions.moveToElement(optionClassique).click().perform();
                }
                if (flux.getPrets().get(index).getType().contains("Prêt à taux zéro")) {
                    pretTauxZero = true;
                }
                if (flux.getPrets().get(index).getType().contains("Prêt in fine")) {
                    WebElement optionClassique = waitForElement(driver, By.xpath("//ng-option[@id=\"market-place_borrower_client-needs_loans_loans-container_1_loan-component_loan-type_input-select_Classique\"]"), 15, 1);
                    actions.moveToElement(optionClassique).click().perform();
                }
                if (flux.getPrets().get(index).getType().contains("Prêt relais")) {
                    WebElement optionClassique = waitForElement(driver, By.xpath("//ng-option[@id=\"market-place_borrower_client-needs_loans_loans-container_1_loan-component_loan-type_input-select_Classique\"]"), 15, 1);
                    actions.moveToElement(optionClassique).click().perform();
                }
                if (flux.getPrets().get(index).getType().contains("Prêt à paliers")) {
                    WebElement optionClassique = waitForElement(driver, By.xpath("//ng-option[@id=\"market-place_borrower_client-needs_loans_loans-container_1_loan-component_loan-type_input-select_Classique\"]"), 15, 1);
                    actions.moveToElement(optionClassique).click().perform();
                }
                if (flux.getPrets().get(index).getType().contains("Crédit bail")) {
                    WebElement optionClassique = waitForElement(driver, By.xpath("//ng-option[@id=\"market-place_borrower_client-needs_loans_loans-container_1_loan-component_loan-type_input-select_Classique\"]"), 15, 1);
                    actions.moveToElement(optionClassique).click().perform();
                }
            }
        }
    }

    private void categorieProfession(FluxData flux, int index) {
        WebElement statutProfession;
        if (index == 0) {
            statutProfession = waitForElement(driver, By.xpath("//ng-select[@id='market-place_borrower_client-needs_insureds_insureds-container_insured_0_insured-person_professional-status_input-select_0_professionalStatusRefId']"), 15, 1);
            actions.moveToElement(statutProfession).click().perform();
        }
        if (index == 1) {
            statutProfession = waitForElement(driver, By.xpath("//ng-select[@id='market-place_borrower_client-needs_insureds_insureds-container_insured_1_insured-person_professional-status_input-select_1_professionalStatusRefId']"), 15, 1);
            actions.moveToElement(statutProfession).click().perform();
        }
        if (flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Salarié cadre")) {
            selectDropdownOptionWithScroll(driver, "Salarié cadre, ingénieur, assimilé-cadre");
        } if (flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Salarié non cadre : employé")) {
            selectDropdownOptionWithScroll(driver, "Salarié non cadre (hors employé de bureau), agent de maîtrise");
        } if (flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Fonctionnaire classe a")) {
            selectDropdownOptionWithScroll(driver, "Fonctionnaire Classe A");
        } if (flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Fonctionnaire hors classe a")) {
            selectDropdownOptionWithScroll(driver, "Fonctionnaire hors classe A");
        } if (flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Commerçant")) {
            selectDropdownOptionWithScroll(driver, "Commerçant");
        } if (flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Agriculteur")) {
            selectDropdownOptionWithScroll(driver, "Professions agricoles");
        } if (flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Intermittent")) {
            selectDropdownOptionWithScroll(driver, "Intermittent");
        } if (flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Retraité")) {
            selectDropdownOptionWithScroll(driver, "Retraité : cadre, assimilé-cadre, fonctionnaire classe A, chef d'Entreprise, profession libérale");
        } if (flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Artisan")) {
              selectDropdownOptionWithScroll(driver, "Artisan / Métier du BTP");
        } if (flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Ouvrier")) {
            selectDropdownOptionWithScroll(driver, "Professions du transport, ouvriers, technicien");
        }
    }

    private void choixBanque(FluxData flux, int index, By locator) {
        WebElement orgaPret = null;
        if (index == 0) {
            orgaPret = waitForElement(driver, By.xpath("//ng-select[@id=\"market-place_borrower_client-needs_loans_loans-container_0_loan-component_loan-organization-code_loan-organization-code_input-select_0_loanOrganizationCode\"]"),20, 2);
            actions.moveToElement(orgaPret).click().perform();
        }
        if (index == 1) {
            orgaPret = waitForElement(driver, By.xpath("//ng-select[@id=\"market-place_borrower_client-needs_loans_loans-container_1_loan-component_loan-organization-code_loan-organization-code_input-select_1_loanOrganizationCode\"]"),20, 2);
            actions.moveToElement(orgaPret).click().perform();
        }
        if (orgaPret != null) {
            scrollAndSelect(driver, locator, flux.getPrets().get(index).getBanque());
        }
    }

    private void fermetureNQA(WebDriver driver, String element) {
        waitThread(1);
        WebElement buttonClose = waitForElement(driver, By.id(element),15, 1);
        actions.moveToElement(buttonClose).click().perform();
    }

    @Override
    protected void pageName(String namePage) {}
}
