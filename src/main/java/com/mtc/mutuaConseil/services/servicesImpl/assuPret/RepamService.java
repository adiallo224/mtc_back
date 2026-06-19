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
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

import static com.mtc.mutuaConseil.utils.Shared.stringToDouble;

@Service
public class RepamService extends PageElementInteraction implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(RepamService.class);
    private static WebDriver driver;
    private Actions actions;
    private final String source = "RepamPret";
    private JavascriptExecutor js = null;
    private final TypeAssuranceService typeAssuranceService;

    public RepamService(TypeAssuranceService typeAssuranceService){
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        log.info("Debut de traitement -- Repam");
        Tarif tarifRepamPret = TarifUtils.createDefaultTarif(c, typeAssuranceService, 1L);
        driver = new EdgeDriver();
        driver.manage().window().maximize();
        js = (JavascriptExecutor) driver;
        actions = new Actions(driver);
        driver.get(c.getUrlFournisseur());
        waitThread(2);
        try {
            WebElement inputlogin = waitForElement(driver, By.xpath("//input[@placeholder=\"Identifiant\"]"), 25, 3);
            inputlogin.sendKeys(c.getUsername());

            WebElement inputPassword = waitForElement(driver, By.xpath("//input[@placeholder=\"Mot de passe\"]"), 10, 1);
            inputPassword.sendKeys(c.getPassword());

            clicButtonByXpath(driver, "//form[@name=\"loginForm\"]//button", actions);

            //Debut debranchement vers la nouvelle page
            WebElement menuElement = waitForElement(driver, By.xpath("//li[contains(@ng-if, 'user.rights.EMPRUNTEUR')]"), 15, 2);
            actions.moveToElement(menuElement).build().perform();
            WebElement subMenuElement = waitForElement(driver, By.xpath("//div[@class=\"sub-menu\"]//li[contains(@ng-if, 'user.rights.EMPRUNTEUR')]//a[contains(text(), 'Emprunteur')]"), 15, 2);
            actions.moveToElement(subMenuElement).click().perform();
            switchPage(driver);
            //Button créer devis
            waitThread(3);
            WebElement createDevisExpressLink = waitForElement1(driver, By.xpath("//a[contains(., 'Créer un devis express')]"), 15, 2);
            if (createDevisExpressLink == null) {
                createDevisExpressLink = waitForElement1(driver, By.xpath("//div[@id='contenu']//a[contains(., 'Créer un devis express')]"), 15, 2);
                if(createDevisExpressLink == null) {
                    createDevisExpressLink = waitForElement1(driver, By.xpath("//a[@href=\"/partenaires/devisexpress\" and contains(., 'Créer un devis express') ]"), 15, 2);
                    if (createDevisExpressLink == null) {
                        createDevisExpressLink = waitForElement1(driver, By.xpath("//*[@id=\"contenu\"]/div[4]/div[1]/div[1]/div/div/div/div[1]/a"), 15, 2);
                    }
                }
            }
            if (createDevisExpressLink != null) {
                js.executeScript("arguments[0].click();", createDevisExpressLink);
//              actions.moveToElement(createDevisExpressLink).click().perform();
            }

            remplirInformationsPersonne(flux, source);

            remplirInformationsPret(flux, source);

            remplirInformationsGaranties(flux, source);

            //Valider
            clicButtonByName(driver, "btnValidationTarificateurExpress", actions);

            //Page cout
            List<WebElement> valeurElements = driver.findElements(By.xpath("//span[contains(@id, 'placeHolder_CoutSolution_')]"));
            String cout = valeurElements.get(0).getText();
            tarifRepamPret.setMontant(cout);
            String screenshotBytes = captureScreenshot(driver, tarifRepamPret.getNom(), false, tarifRepamPret);
            if (screenshotBytes != null) {
                tarifRepamPret.setCaptureImg(screenshotBytes);
            }
            tarifRepamPret.setExecution(true);
            log.info("Fin de traitement -- Swiss Life");
        } catch (Exception e) {
            log.error("An error occurred : ", e);
            tarifRepamPret.setErreur(e.getMessage());
            String screenshotBytesErreur = captureScreenshot(driver, tarifRepamPret.getNom(), true, tarifRepamPret);
            tarifRepamPret.setCaptureImgErreur(screenshotBytesErreur);
            tarifRepamPret.setEtape("");
        } finally {
            driver.quit();
        }
       return tarifRepamPret;
    }

    private void remplirInformationsPersonne(FluxData flux, String source) {
        int index = 0;
        if (flux.getPersonnes().size() == 2) {
            waitThread(8);
//            WebElement dropdownNombreAssures1 = waitForElement1(driver, By.xpath("//select[@name=\"vtNbEmprunteurs\"]"));
            WebElement dropdownNombreAssures = waitForElement1(driver, By.id("vtNbEmprunteurs"), 30, 0);
            Select selectNombreAssures = new Select(dropdownNombreAssures);
            selectNombreAssures.selectByValue("2");
        }
        //Civilite
//        WebElement dropdownCivilite = waitForElement1(driver, By.id("vtCivE0"));
        waitThread(2);
        WebElement dropdownCivilite = waitForElement1(driver, By.xpath("//select[@id=\"vtCivE0\"]"), 30, 0);
        Select selectCivilite = new Select(dropdownCivilite);
        if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur")) {
            selectCivilite.selectByIndex(1);
        } else if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame")) {
            selectCivilite.selectByIndex(2);
        }

        //Nom
        inputByLocator(driver, By.id("vtNomE0"), flux.getPersonnes().get(index).getNom());
        clicButtonByXpath(driver, "//button[@data-original-title=\"Recopier le nom de l'assuré\"]", actions);
        //Prenom
        inputByLocator(driver, By.id("vtPnomE0"), flux.getPersonnes().get(index).getPrenom());
        //Date naissance
        inputByLocator(driver, By.id("vtDateNaissanceE0"), flux.getPersonnes().get(index).getDateNaissance());

        if (flux.getInfoAssureComplets().get(index).getFumeur()) {
            WebElement radioFumeur = waitForElement(driver, By.xpath("//label[@for=\"vtFumeurE0-switch\"]"), 20, 2);
            actions.moveToElement(radioFumeur).click().perform();
        }

        WebElement proNonTrouve = waitForElement1(driver, By.xpath("//*[@id=\"sectionSearchByProfession0\"]/div[4]/div/button"), 30, 1);
        actions.moveToElement(proNonTrouve).click().perform();
        //Profession
        WebElement select2Container = waitForElement1(driver, By.id("select2-sectionSearchByCategPro_cspCategPro0-container"), 30, 1);
        select2Container.click();

        // Attendre que les options soient visibles
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("select2-results__option")));

        // Trouver toutes les options dans le menu déroulant
        List<WebElement> allOptions = driver.findElements(By.className("select2-results__option"));

        // Imprimer ou utiliser toutes les options
        choixCategorieProfessions(flux, allOptions, index);

        waitThread(1);
        WebElement inputProfession = waitForElement1(driver, By.id("sectionSearchByCategPro_cspIntitulePoste0"), 30, 1);
        inputProfession.sendKeys(flux.getPersonnes().get(index).getProfession());

        //Deplacement professionnelle
        waitThread(1);
        WebElement dropdownDepPro = waitForElement1(driver, By.id("cspDeplacementsPro0"), 30, 1);
        Select selectDepPro = new Select(dropdownDepPro);
        if (!flux.getInfoAssureComplets().get(index).getDeplacementPro20000())
            selectDepPro.selectByValue("4178");
        else
            selectDepPro.selectByValue("4179");

        waitThread(2);
        scrollDown(driver, 0, 300);
        WebElement dropdownTravauxHauteurs = waitForElement1(driver, By.id("cspTravauxEnHauteur0"), 30, 1);
        Select selectTravauxHauteurs = new Select(dropdownTravauxHauteurs);
        if (!flux.getInfoAssureComplets().get(index).getTravailHauteur())
            selectTravauxHauteurs.selectByValue("4180");
        else {
            if ((flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("15 à 20m") || flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("Plus de 20m"))) {
                selectTravauxHauteurs.selectByValue("4181");
            } else {
                selectTravauxHauteurs.selectByValue("4180");
            }
        }

        WebElement dropdownMatiereDanger = waitForElement1(driver, By.id("cspManipMatDangereuses0"), 30, 1);
        Select selectMatiereDanger = new Select(dropdownMatiereDanger);
        if (!flux.getInfoAssureComplets().get(index).getProduitDanger())
            selectMatiereDanger.selectByValue("4182");
        else
            selectTravauxHauteurs.selectByValue("4181");

        //email
        inputByLocator(driver, By.id("vtMailE0"), flux.getPersonnes().get(index).getEmail());
        //portable
        inputByLocator(driver, By.id("vtPortE0"), flux.getPersonnes().get(index).getTelephone());
        //radio pret
        if(flux.getPrets().get(index).getNouveauOuReprise() != null) {
            waitThread(1);
//            WebElement nouveauPretRadio = waitForElement1(driver, By.xpath("//input[@name='velIdTypeAdhesionE0' and @value='4662']"), 30, 1);
            WebElement nouveauPretRadio = waitForElement1(driver, By.xpath("//label[@for='velIdTypeAdhesionE0_4662']"), 30, 1);
            waitThread(1);
            if (!nouveauPretRadio.isSelected())
                actions.moveToElement(nouveauPretRadio).click().perform();
        }

        //Somme à assurer
        WebElement dropdownSommeAssurer = waitForElement1(driver, By.id("vtDevisExpressDeclaratifEncoursLemoineE0"), 30, 1);
        Select selectSommeAssurer = new Select(dropdownSommeAssurer);
        if (stringToDouble(flux.getPrets().get(index).getMontantPret()) >= 200000)
            selectSommeAssurer.selectByValue("12454");
        else {
            selectSommeAssurer.selectByValue("12327");
        }

        if (flux.getPersonnes().size() == 2) {
            index = 1;
            //Civilite
//            WebElement dropdownCivilite1 = waitForElement1(driver, By.id("vtCivE1"));
            WebElement dropdownCivilite1 = waitForElement1(driver, By.xpath("//select[@id=\"vtCivE1\"]"), 30, 1);
            Select selectCivilite1 = new Select(dropdownCivilite1);
            if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur")) {
                selectCivilite1.selectByIndex(1);
            } else if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame")) {
                selectCivilite1.selectByIndex(2);
            }
            //Nom
            inputByLocator(driver, By.id("vtNomE1"), flux.getPersonnes().get(index).getNom());
            clicButtonByXpath(driver, "//div[@id=\"tarificateur_express_tableauDetailInfosAssures1\"]//button[@data-original-title=\"Recopier le nom de l'assuré\"]", actions);
            //Prenom
            inputByLocator(driver, By.id("vtPnomE1"), flux.getPersonnes().get(index).getPrenom());
            //Date naissance
            inputByLocator(driver, By.id("vtDateNaissanceE1"), flux.getPersonnes().get(index).getDateNaissance());

            if(flux.getInfoAssureComplets().get(index).getFumeur()) {
                WebElement radioFumeur = waitForElement(driver, By.xpath("//label[@for=\"vtFumeurE0-switch\"]"), 20, 2);
                actions.moveToElement(radioFumeur).click().perform();
            }

            WebElement proNonTrouve1 = waitForElement1(driver, By.xpath("//*[@id=\"sectionSearchByProfession1\"]/div[4]/div/button"), 30, 1);
            actions.moveToElement(proNonTrouve1).click().perform();
            //Profession
            WebElement select2Container1 = waitForElement1(driver, By.id("select2-sectionSearchByCategPro_cspCategPro1-container"), 30, 1);
            select2Container1.click();

            // Attendre que les options soient visibles
            WebDriverWait wait1 = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait1.until(ExpectedConditions.visibilityOfElementLocated(By.className("select2-results__option")));

            // Trouver toutes les options dans le menu déroulant
            List<WebElement> allOptions1 = driver.findElements(By.className("select2-results__option"));

            // Imprimer ou utiliser toutes les options
            choixCategorieProfessions(flux, allOptions1, index);

            waitThread(1);
            WebElement inputProfession1 = waitForElement1(driver, By.id("sectionSearchByCategPro_cspIntitulePoste1"), 30, 1);
            inputProfession1.sendKeys(flux.getPersonnes().get(index).getProfession());

            //Deplacement professionnelle
            waitThread(1);
            WebElement dropdownDepPro1 = waitForElement1(driver, By.id("cspDeplacementsPro1"), 30, 1);
            Select selectDepPro1 = new Select(dropdownDepPro1);
            if (flux.getInfoAssureComplets().get(index).getDeplacementPro20000())
                selectDepPro1.selectByValue("4178");
            else
                selectDepPro1.selectByValue("4179");

            //Travaux manuels
            WebElement dropdownTravauxHauteur1 = waitForElement1(driver, By.id("cspTravauxEnHauteur1"), 30, 1);
            Select selectTravauxHauteur1 = new Select(dropdownTravauxHauteur1);
            if (!flux.getInfoAssureComplets().get(index).getTravailHauteur()) {
                selectTravauxHauteur1.selectByValue("4180");
            } else {
                if ((flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("15 à 20m") || flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("Plus de 20m"))) {
                    selectTravauxHauteurs.selectByValue("4181");
                } else {
                    selectTravauxHauteurs.selectByValue("4180");
                }
            }

            WebElement dropdownManipMatDangereuses1 = waitForElement1(driver, By.id("cspManipMatDangereuses1"), 30, 1);
            Select selectManipMatDangereuses1 = new Select(dropdownManipMatDangereuses1);
            if (!flux.getInfoAssureComplets().get(index).getProduitDanger())
                selectManipMatDangereuses1.selectByValue("4182");
            else
                selectManipMatDangereuses1.selectByValue("4183");

            //email
            inputByLocator(driver, By.id("vtMailE1"), flux.getPersonnes().get(index).getEmail());
            //portable
            inputByLocator(driver, By.id("vtPortE1"), flux.getPersonnes().get(index).getTelephone());
            //radio pret
            if(flux.getPrets().get(0).getNouveauOuReprise() != null) {
                waitThread(1);
//                WebElement nouveauPretRadio = waitForElement1(driver, By.xpath("//input[@name='velIdTypeAdhesionE1' and @value='4662']"), 30, 1);
                WebElement nouveauPretRadio = waitForElement1(driver, By.xpath("//label[@for='velIdTypeAdhesionE1_4662']"), 30, 1);
                waitThread(1);
                if (!nouveauPretRadio.isSelected())
                    actions.moveToElement(nouveauPretRadio).click().perform();
            }

            //Somme à assurer
            if (stringToDouble(flux.getPrets().get(0).getMontantPret()) >= 200000) {
                WebElement dropdownSommeAssurer1 = waitForElement1(driver, By.id("vtDevisExpressDeclaratifEncoursLemoineE1"), 30, 1);
                Select selectSommeAssurer1 = new Select(dropdownSommeAssurer1);
                selectSommeAssurer1.selectByValue("12454");
            } else if (stringToDouble(flux.getPrets().get(0).getMontantPret()) < 200000) {
                WebElement dropdownSommeAssurer1 = waitForElement1(driver, By.id("vtDevisExpressDeclaratifEncoursLemoineE1"), 30, 1);
                Select selectSommeAssurer1 = new Select(dropdownSommeAssurer1);
                selectSommeAssurer1.selectByValue("12327");
            }
        }
    }

    private void remplirInformationsPret(FluxData flux, String source) {
        int index = 0;
        //Prets
        //TypeFinancement
        WebElement dropdownTypeFinancement = waitForElement1(driver, By.id("vtTypeCredit"), 30, 1);
        Select selectTypeFinancement = new Select(dropdownTypeFinancement);
        selectTypeFinancement.selectByValue("12768");
        //Objet de financement
        objetFinancement(flux, index);
        //Nombre de prets
        WebElement dropdownNbPrets = waitForElement1(driver, By.id("vtNbPrets"), 30, 1);
        Select selectNbPrets = new Select(dropdownNbPrets);
        if (flux.getPrets().size() == 1) {
            selectNbPrets.selectByValue("1");
        } if (flux.getPrets().size() == 2) {
            selectNbPrets.selectByValue("2");
        }
        //Type de prets
        typePret(flux, index);
        //Montant
        inputByLocator(driver, By.id("vtMontant0"), flux.getPrets().get(index).getMontantPret());
        //Taux
        inputByLocator(driver, By.id("vtTaux0"), flux.getPrets().get(index).getTaux());
        //Duree
        inputByLocator(driver, By.id("vtDuree0"), flux.getPrets().get(index).getDuree());
        //Differé
        if (flux.getPrets().get(index).getDureeDiffere() != null) {
            inputByLocator(driver, By.id("vtDureeDif0"), flux.getPrets().get(index).getDureeDiffere());
        }
        if (flux.getPrets().size() == 2){
            index = 1;
            //Type de prets
            typePret(flux, index);
            //Montant
            inputByLocator(driver, By.id("vtMontant1"), flux.getPrets().get(index).getMontantPret());
            //Taux
            inputByLocator(driver, By.id("vtTaux1"), flux.getPrets().get(index).getTaux());
            //Duree
            inputByLocator(driver, By.id("vtDuree1"), flux.getPrets().get(index).getDuree());
            //Differé
            if (flux.getPrets().get(index).getDureeDiffere() != null) {
                inputByLocator(driver, By.id("vtDureeDif1"), flux.getPrets().get(index).getDureeDiffere());
            }
        }
    }

    private void remplirInformationsGaranties(FluxData flux, String source) {
        int index = 0;
        //Date garantie
        inputByLocator(driver, By.id("vtDebutGarantie"), flux.getPrets().get(index).getDateEffet());
        //Garanties de base
        WebElement buttonGb = waitForElement1(driver, By.cssSelector(".toggle-group > .toggle-on[for='vtValeurOptionGarantie8-switch']"), 30, 1);
        actions.moveToElement(buttonGb).click().perform();

        //Garanties optionnelles
        //IPP
        WebElement buttonGOption = waitForElement1(driver, By.cssSelector(".toggle-group > .toggle-on[for='vtValeurOptionGarantie9-switch']"), 30, 1);
        actions.moveToElement(buttonGOption).click().perform();

        //IPPRO
        WebElement buttonIPPRO = waitForElement1(driver, By.cssSelector("input[id='vtValeurOptionGarantie65-switch-0']"), 30, 1);
        actions.moveToElement(buttonIPPRO).click().perform();

//        //ARRET
//        WebElement buttonArret = waitForElement1(driver, By.cssSelector(".toggle-group > .toggle-on[for='vtValeurOptionGarantie10-switch']"));
//        actions.moveToElement(buttonArret).click().perform();
//
        //Perte emploi
//        if (flux.getInfoAssureComplets().get(0).getGarantieChomage()) {
//            WebElement buttonPerteEmploi = waitForElement1(driver, By.cssSelector(".toggle-group > .toggle-on[for='vtValeurOptionGarantie24-switch']"));
//            actions.moveToElement(buttonPerteEmploi).click().perform();
//        }

        //Franchise
        //        WebElement dropdownFranchise = waitForElement1(driver, By.id("vtFranchise"));
        //        Select selectFranchise = new Select(dropdownFranchise);
        //        selectFranchise.selectByValue("2353");

        //Franchise
        //        WebElement dropdownNiveauCouverture = waitForElement1(driver, By.id("select2-vtQuotiteE0-container"));
        //        Select selectNiveauCouverture = new Select(dropdownNiveauCouverture);
        //        selectNiveauCouverture.selectByVisibleText("100%");
        if (flux.getPrets().size() == 2){
            index = 1;
        }
    }

    private void typePret(FluxData flux, int index) {
        WebElement dropdownTypePret = null;
        if (index == 0)
            dropdownTypePret = waitForElement1(driver, By.id("vtTypeDuPret0"), 30, 1);
        if (index == 1)
            dropdownTypePret = waitForElement1(driver, By.id("vtTypeDuPret1"), 30, 1);
        Select selectTypePrets1 = new Select(dropdownTypePret);
        if (flux.getPrets().get(index).getType().equalsIgnoreCase("Amortissable"))
            selectTypePrets1.selectByValue("74");

    }

    private void objetFinancement(FluxData flux, int index) {
        WebElement dropdownObjetFinancement = waitForElement1(driver, By.id("vtObjetDuPret"), 30, 1);
        Select selectObjetFinancement = new Select(dropdownObjetFinancement);
        if(flux.getPrets().get(index).getObjet().equalsIgnoreCase("Résidence principale"))
            selectObjetFinancement.selectByValue("2404");
        if(flux.getPrets().get(index).getObjet().equalsIgnoreCase("Résidence secondaire"))
            selectObjetFinancement.selectByValue("2405");
        if(flux.getPrets().get(index).getObjet().equalsIgnoreCase("Investissement locatif"))
            selectObjetFinancement.selectByValue("2407");
        if(flux.getPrets().get(index).getObjet().equalsIgnoreCase("Prêt à objet professionnel"))
            selectObjetFinancement.selectByValue("2408");
        if(flux.getPrets().get(index).getObjet().equalsIgnoreCase("Travaux"))
            selectObjetFinancement.selectByValue("2406");
    }

    private void choixCategorieProfessions(FluxData flux, List<WebElement> allOptions, int index) {
        for (WebElement option : allOptions) {
            String professionSpecifique = flux.getPersonnes().get(index).getProfessionSpecifique();
            if (professionSpecifique.equalsIgnoreCase("Salarié cadre") && option.getText().equalsIgnoreCase("Ingénieurs et cadres")) {
                option.click();
                break;
            } if (professionSpecifique.equalsIgnoreCase("Ouvrier") && option.getText().equalsIgnoreCase("Ouvriers")) {
                option.click();
                break;
            } if (professionSpecifique.equalsIgnoreCase("Artisan") && option.getText().equalsIgnoreCase("Artisans")) {
                option.click();
                break;
            } if (professionSpecifique.equalsIgnoreCase("Commerçant") && option.getText().equalsIgnoreCase("Commerçants et chefs d'entreprises")) {
                option.click();
                break;
            } if (professionSpecifique.equalsIgnoreCase("Salarié non cadre : employé") && option.getText().equalsIgnoreCase("Employés")) {
                option.click();
                break;
            } if (professionSpecifique.equalsIgnoreCase("Agriculteur") && option.getText().equalsIgnoreCase("Professions Agricoles")) {
                option.click();
                break;
            } if (professionSpecifique.equalsIgnoreCase("Profession libérale") && option.getText().equalsIgnoreCase("Professions libérales")) {
                option.click();
                break;
            }
        }
    }

    @Override
    protected void pageName(String namePage) {}
}
