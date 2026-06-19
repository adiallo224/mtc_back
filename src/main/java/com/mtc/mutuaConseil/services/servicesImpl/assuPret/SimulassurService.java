package com.mtc.mutuaConseil.services.servicesImpl.assuPret;

import com.mtc.mutuaConseil.base.BaseAutomationService;
import com.mtc.mutuaConseil.base.BrowserType;
import com.mtc.mutuaConseil.base.WaitUtils;
import com.mtc.mutuaConseil.models.Compte;
import com.mtc.mutuaConseil.models.FluxData;
import com.mtc.mutuaConseil.models.Tarif;
import com.mtc.mutuaConseil.services.LaunchedService;
import com.mtc.mutuaConseil.services.servicesImpl.TypeAssuranceService;
import com.mtc.mutuaConseil.utils.TarifUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
public class SimulassurService extends BaseAutomationService implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(SimulassurService.class);
    private WebDriver driver;
    private final TypeAssuranceService typeAssuranceService;

    public SimulassurService(TypeAssuranceService typeAssuranceService){
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        log.info("Debut de traitement -- SimulassurPret");
        Tarif tarifSimulassurPret = TarifUtils.createDefaultTarif(c, typeAssuranceService, 1L);

        // Déterminer le navigateur à partir du compte si possible (sinon Chromedriver Selenium par défaut)
        BrowserType browserType = BrowserType.SELENIUM_CHROME;
        try {
            // Essayer de récupérer un nom de navigateur dans le compte (méthode optionnelle)
            try {
                String browserName = (String) (c.getClass().getMethod("getBrowser").invoke(c));
                if (browserName != null) {
                    String normalized = browserName.trim().toLowerCase();
                    if (normalized.contains("firefox")) browserType = BrowserType.SELENIUM_FIREFOX;
                    else if (normalized.contains("edge")) browserType = BrowserType.SELENIUM_EDGE;
                    else browserType = BrowserType.SELENIUM_CHROME;
                }
            } catch (NoSuchMethodException nsme) {
                // getBrowser() non présent : ignorer (utiliser le défaut)
            } catch (Exception ignore) {
                // si invocation échoue, on continue avec défaut
            }

            // Initialise le navigateur via la factory (headless = false pour imiter ton ancien comportement)
            init(browserType, false);
            driver = getWebDriver();
            if (driver == null) {
                log.error("Impossible d'obtenir le WebDriver depuis le BrowserAdapter");
            }
            // Navigation initiale
            humanNavigate(c.getUrlFournisseur());
            WaitUtils.sleepMs(2000);

            element.typeByXpath("//input[@id='brokerCode']", c.getUsername());

            element.typeByXpath("//input[@id='password']", c.getPassword());

            element.clickByActions("//*[@id=\"app\"]/div[1]/div/form/div[3]/button");

            element.clickByActions("//span[text()=\" Nouveau devis \"]");

            remplirInformationsPersonne(flux);

            element.clickByActions("//button[text()=\"Suivant\"]");

            remplirInformationsPret(flux);

//            if (flux.getPrets().size() == 1) {
//                closeSidebarIfSmallScreen(driver);
//            }

            WaitUtils.sleepMs(3000);
            element.clickByActions("//button[text()=\" Valider \"]");
            WaitUtils.sleepMs(8000);

            String cout = getCoutTotal(20, 1);
            tarifSimulassurPret.setMontant(cout);
            log.info("Cout : {}", cout);
            String screenshotBytes = captureScreenshot(tarifSimulassurPret.getNom(), false, tarifSimulassurPret);
            if (screenshotBytes != null) {
                tarifSimulassurPret.setCaptureImg(screenshotBytes);
            }
            tarifSimulassurPret.setExecution(true);
        } catch (Exception e) {
            log.error("An error occurred : ", e);
            tarifSimulassurPret.setErreur(e.getMessage());
            String screenshotBytesErreur = captureScreenshot(tarifSimulassurPret.getNom(), true, tarifSimulassurPret);
            tarifSimulassurPret.setCaptureImgErreur(screenshotBytesErreur);
            tarifSimulassurPret.setEtape("");
        } finally {
            cleanup();
        }
        return tarifSimulassurPret;
    }

    private void remplirInformationsPersonne(FluxData flux) {
        int index = 0;
        if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur")) {
            element.clickByActions("//input[@id=\"customer-1_civility-men\"]");
        } else if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame")){
            element.clickByActions("//input[@id=\"customer-1_civility-women\"]");
        }
        element.typeById("customer-1_lastname", flux.getPersonnes().get(index).getNom());
        element.typeById("customer-1_firstname", flux.getPersonnes().get(index).getPrenom());
        element.typeById("customer-1_birthDate", flux.getPersonnes().get(index).getDateNaissance());
        element.typeById("customer-1_zipCode", flux.getPersonnes().get(index).getCodePostal());
        selectStatutProfession(flux, index);
        if (flux.getInfoAssureComplets().get(index).getDeplacementPro20000()) {
            WebElement dropdownDeplacement = driver.findElement(By.id("customer-1_businessTrip"));
            dropdownDeplacement.click();
            Select selectDeplacement = new Select(dropdownDeplacement);
            selectDeplacement.selectByValue("true");
        }
        if (flux.getInfoAssureComplets().get(index).getFumeur()) {
            element.clickById("customer-1_smoker");
        }
        if (flux.getInfoAssureComplets().get(index).getTravailManuel()) {
            element.clickById("customer-1_handling");
        }
        if (flux.getInfoAssureComplets().get(index).getTravailHauteur()) {
            if ((flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("15 à 20m") || flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("Plus de 20m"))) {
                element.clickByXpath("/html/body/div[1]/div[1]/div[2]/div/div[2]/div/form/div[1]/div[1]/div/label[3]/span[2]");
            }
        }

        if (flux.getInfoAssureComplets().get(index).getMetierExpose()) {
            element.clickById("customer-1_smoker");
        }
        // verification à faire
        if (flux.getPersonnes().size() == 2){
            index = 1;
            element.clickByXpath("//button[text()=\" Cliquez pour ajouter un emprunteur \"]");
            if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur")) {
                element.clickByXpath("//input[@id=\"customer-2_civility-men\"]");
            } else if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame")){
                element.clickByXpath("//input[@id=\"customer-2_civility-women\"]");
            }
            element.typeById("customer-2_lastname", flux.getPersonnes().get(index).getNom());
            element.typeById("customer-2_firstname", flux.getPersonnes().get(index).getPrenom());
            element.typeById("customer-2_birthDate",flux.getPersonnes().get(index).getDateNaissance());
            element.typeById("customer-2_zipCode", flux.getPersonnes().get(index).getCodePostal());
            selectStatutProfession(flux, index);
            if (flux.getInfoAssureComplets().get(index).getDeplacementPro20000()) {
                WebElement dropdownDeplacement1 = driver.findElement(By.id("customer-2_businessTrip"));
                dropdownDeplacement1.click();
                Select selectDeplacement1 = new Select(dropdownDeplacement1);
                selectDeplacement1.selectByValue("true");
            }
            if (flux.getInfoAssureComplets().get(index).getFumeur()) {
                element.clickById("customer-2_smoker");
            }
            if (flux.getInfoAssureComplets().get(index).getTravailManuel()) {
                element.clickById("customer-2_handling");
            }
            if (flux.getInfoAssureComplets().get(index).getTravailHauteur()) {
                if ((flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("15 à 20m") || flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("Plus de 20m"))) {
                    element.clickById("customer-2_height");
                }
            }
            if (flux.getInfoAssureComplets().get(index).getMetierExpose()) {
                element.clickById("customer-1_smoker");
            }
        }
        WaitUtils.sleepMs(2000);
    }

    private void remplirInformationsPret(FluxData flux) {
        closeSidebarIfSmallScreen(driver);
        int index = 0;
        selectTypeProjet(flux);
        element.typeById("effectiveDate", flux.getPrets().get(index).getDateEffet());
        choixBanque(flux);
        element.typeById("loan-1_amount", flux.getPrets().get(index).getMontantPret());
        element.typeById("loan-1_rate", flux.getPrets().get(index).getTaux());
        element.typeById("loan-1_duration", flux.getPrets().get(index).getDuree());
        element.typeById("loan-1_delay", flux.getPrets().get(index).getDiffere());
        if (!flux.getPrets().get(index).getDiffere().isEmpty() && (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Partiel") || flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Total"))) {
            selectTypeDiffere(flux, index);
        }
        selectTypeTaux(flux, index);
        // verification à faire
        if (flux.getPrets().size() == 2){
            index = 1;
            WaitUtils.sleepMs(3000);
            scrollDown(0, 300);
            element.clickByXpath("//button[text()=\" Cliquez pour ajouter un prêt \"]");
            element.typeById("loan-2_amount", flux.getPrets().get(index).getMontantPret());
            element.typeById("loan-2_rate", flux.getPrets().get(index).getTaux());
            element.typeById("loan-2_duration", flux.getPrets().get(index).getDuree());
            element.typeById("loan-2_delay", flux.getPrets().get(index).getDiffere());
            if (!flux.getPrets().get(index).getDiffere().isEmpty() && (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Partiel") || flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Total"))) {
                selectTypeDiffere(flux, index);
            }
            selectTypeTaux(flux, index);
        }
        selectTypeTaux(flux, 0);
    }

    private String getCoutTotal(int timeOut, int pollingIntervalSeconds) {
        WebElement element = waitForElement(driver, By.xpath("(//div[contains(@class, 'price-list') and contains(@class, 'mb-3.5')])[1]//div[contains(@class, 'text-xl')]"), timeOut, pollingIntervalSeconds);
        return element.getText();
    }

    public void closeSidebarIfSmallScreen(WebDriver driver) {
        Dimension screenSize = driver.manage().window().getSize();
        int width = screenSize.getWidth();
        int height = screenSize.getHeight();
        if (width <= 1552 && height <= 832) {
            WebElement buttonClose = waitForElement(driver, By.xpath("//*[@id=\"form-sidebar\"]//div//button"), 60, 1);
            if (buttonClose.isEnabled()) {
                buttonClose.click();
            }
        }
    }

    private void selectStatutProfession(FluxData flux, int index) {
        WebElement dropdownProfession = null;
        if (index == 0) {
            dropdownProfession = driver.findElement(By.id("customer-1_profession"));
        }
        if (index == 1) {
            dropdownProfession = driver.findElement(By.id("customer-2_profession"));
        }
        if (dropdownProfession != null) {
//            dropdownProfession.click();
            Select select = new Select(dropdownProfession);
            List<WebElement> allOptions = select.getOptions();
            for (WebElement option : allOptions) {
                String professionSpecifique = flux.getPersonnes().get(index).getProfessionSpecifique();
                if (professionSpecifique.equalsIgnoreCase("Salarié cadre") && option.getText().equalsIgnoreCase("Salarié Cadre")) {
                    select.selectByVisibleText("Salarié cadre");
                    break;
                } if (professionSpecifique.equalsIgnoreCase("Salarié non cadre : employé") && option.getText().equalsIgnoreCase("Salarié non Cadre")) {
                    select.selectByVisibleText("Salarié non cadre");
                    break;
                } if (professionSpecifique.equalsIgnoreCase("Artisan") && option.getText().equalsIgnoreCase("Artisan")) {
                    select.selectByVisibleText("Artisan");
                    break;
                } if (professionSpecifique.equalsIgnoreCase("Commerçant") && option.getText().equalsIgnoreCase("Commerçant")) {
                    select.selectByVisibleText("Commerçant");
                    break;
                } if (professionSpecifique.equalsIgnoreCase("Chef d\\'entreprise") && option.getText().equalsIgnoreCase("Dirigeant d'entreprise")) {
                    select.selectByVisibleText("Dirigeant d'entreprise");
                    break;
                } if (professionSpecifique.equalsIgnoreCase("Fonctionnaire classe a") && option.getText().equalsIgnoreCase("Fonctionnaire cadre")) {
                    select.selectByVisibleText("Fonctionnaire cadre");
                    break;
                } if (professionSpecifique.equalsIgnoreCase("Intermittent") && option.getText().equalsIgnoreCase("Intermittent")) {
                    select.selectByVisibleText("Intermittent");
                    break;
                } if (professionSpecifique.equalsIgnoreCase("Profession libérale") && option.getText().equalsIgnoreCase("Profession libérale")) {
                    select.selectByVisibleText("Profession libérale");
                    break;
                } if (professionSpecifique.equalsIgnoreCase("Agriculteur") && option.getText().equalsIgnoreCase("Exp. agricole/viticole")) {
                    select.selectByVisibleText("Exp. agricole/viticole");
                    break;
                } if (professionSpecifique.equalsIgnoreCase("Retraité") && option.getText().equalsIgnoreCase("Retraité cadre")) {
                    select.selectByVisibleText("Retraité cadre");
                    break;
                }
            }
        }
        element.pressEnter();
    }

    private void selectTypeProjet(FluxData flux) {
        WebElement dropdownTypeProjet = driver.findElement(By.id("projectQualification"));
        Select select1 = new Select(dropdownTypeProjet);
        List<WebElement> allTypeProjet = select1.getOptions();
        for (WebElement option : allTypeProjet) {
             String typeProjet = flux.getPrets().get(0).getObjet();
             if (typeProjet.equalsIgnoreCase("Résidence principale") && option.getText().equalsIgnoreCase("Résidence principale")) {
                 select1.selectByVisibleText(option.getText());
                 break;
             }  if (typeProjet.equalsIgnoreCase("Résidence secondaire") && option.getText().equalsIgnoreCase("Résidence secondaire")) {
                    select1.selectByVisibleText(option.getText());
                    break;
            }
        }
        element.pressEnter();
    }

    private void selectTypeTaux(FluxData flux, int index) {
        WebElement dropdownTypeTaux = null;
        if (index == 0) {
            dropdownTypeTaux = driver.findElement(By.id("loan-1_warranty-1"));
            dropdownTypeTaux.click();
            WaitUtils.sleepMs(1000);
        }if (index == 1) {
            dropdownTypeTaux = driver.findElement(By.id("loan-2_warranty-1"));
            dropdownTypeTaux.click();
            WaitUtils.sleepMs(1000);
        }
        if (dropdownTypeTaux != null) {
            Select select = new Select(dropdownTypeTaux);
            List<WebElement> allTypeTaux = select.getOptions();
            for (WebElement option : allTypeTaux) {
                if (flux.getPersonnes().size() == 1) {
                    if (flux.getPrets().size() == 1 ) {
                        if (flux.getInfoAssureComplets().get(0).getGarantie().equalsIgnoreCase("IPT")) {
                            element.clickByXpath("//*[@id=\"loan-1_warranty-1\"]/option[1]");
                            break;
                        }
                        if (flux.getInfoAssureComplets().get(0).getGarantie().equalsIgnoreCase("IPT, ITT")) {
                            element.clickByXpath("//*[@id=\"loan-1_warranty-1\"]/option[2]");
                            break;
                        }
                        if (flux.getInfoAssureComplets().get(0).getGarantie().equalsIgnoreCase("IPT, ITT, IPP")) {
                            element.clickByXpath("//*[@id=\"loan-1_warranty-1\"]/option[3]");
                            break;
                        }
                    }
                    if (flux.getPrets().size() == 2) {
                        if (index == 0) {
                            if (flux.getInfoAssureComplets().get(0).getGarantie().equalsIgnoreCase("IPT")) {
                                element.clickByXpath("//*[@id=\"loan-1_warranty-1\"]/option[1]");
                                break;
                            }
                            if (flux.getInfoAssureComplets().get(0).getGarantie().equalsIgnoreCase("IPT, ITT")) {
                                element.clickByXpath("//*[@id=\"loan-1_warranty-1\"]/option[2]");
                                break;
                            }
                            if (flux.getInfoAssureComplets().get(0).getGarantie().equalsIgnoreCase("IPT, ITT, IPP")) {
                                element.clickByXpath("//*[@id=\"loan-1_warranty-1\"]/option[3]");
                                break;
                            }
                        }
                        if (index == 1) {
                            if (flux.getInfoAssureComplets().get(0).getGarantie().equalsIgnoreCase("IPT")) {
                                element.clickByXpath("//*[@id=\"loan-2_warranty-1\"]/option[1]");
                                break;
                            }
                            if (flux.getInfoAssureComplets().get(0).getGarantie().equalsIgnoreCase("IPT, ITT")) {
                                element.clickByXpath("//*[@id=\"loan-2_warranty-1\"]/option[2]");
                                break;
                            }
                            if (flux.getInfoAssureComplets().get(0).getGarantie().equalsIgnoreCase("IPT, ITT, IPP")) {
                                element.clickByXpath("//*[@id=\"loan-2_warranty-1\"]/option[3]");
                                break;
                            }
                        }
                    }
                }
                if (flux.getPersonnes().size() == 2) {
                    if (index == 0) {
                        if (flux.getInfoAssureComplets().get(index).getGarantie().equalsIgnoreCase("IPT")) {
                            element.clickByXpath("//*[@id=\"loan-1_warranty-1\"]/option[1]");
                            break;
                        }
                        if (flux.getInfoAssureComplets().get(index).getGarantie().equalsIgnoreCase("IPT, ITT")) {
                            element.clickByXpath("//*[@id=\"loan-1_warranty-1\"]/option[2]");
                            break;
                        }
                        if (flux.getInfoAssureComplets().get(index).getGarantie().equalsIgnoreCase("IPT, ITT, IPP")) {
                            element.clickByXpath("//*[@id=\"loan-1_warranty-1\"]/option[3]");
                            break;
                        }
                    }
                    if (index == 1) {
                        if (flux.getInfoAssureComplets().get(index).getGarantie().equalsIgnoreCase("IPT")) {
                            element.clickByXpath("//*[@id=\"loan-2_warranty-1\"]/option[1]");
                            break;
                        }
                        if (flux.getInfoAssureComplets().get(index).getGarantie().equalsIgnoreCase("IPT, ITT")) {
                            element.clickByXpath("//*[@id=\"loan-2_warranty-1\"]/option[2]");
                            break;
                        }
                        if (flux.getInfoAssureComplets().get(index).getGarantie().equalsIgnoreCase("IPT, ITT, IPP")) {
                            element.clickByXpath("//*[@id=\"loan-2_warranty-1\"]/option[3]");
                            break;
                        }
                    }
                }
            }
        }
        element.pressEnter();;
    }

    private void choixBanque(FluxData flux) {
        if (flux.getPrets().get(0).getBanque() != null) {
            WebElement choixBanque = driver.findElement(By.id("bank"));
            Select select = new Select(choixBanque);
            List<WebElement> allBanques = select.getOptions();
            for (WebElement option : allBanques) {
                String banque = flux.getPrets().get(0).getBanque();
                if (banque.equalsIgnoreCase("Axa banque") && option.getText().equalsIgnoreCase("AXA BANQUE")) {
                    select.selectByVisibleText(option.getText());
                    break;
                }
                if (banque.equalsIgnoreCase("Banque populaire") && option.getText().equalsIgnoreCase("BANQUE POPULAIRE")) {
                    select.selectByVisibleText(option.getText());
                    break;
                }
                if (banque.equalsIgnoreCase("La Banque Postale") && option.getText().equalsIgnoreCase("BANQUE POSTALE")) {
                    select.selectByVisibleText(option.getText());
                    break;
                }
                if (banque.equalsIgnoreCase("BNP Paribas") && option.getText().equalsIgnoreCase("BNP PARIBAS")) {
                    select.selectByVisibleText(option.getText());
                    break;
                }
                if (banque.equalsIgnoreCase("Caisse d\\'épargne") && option.getText().equalsIgnoreCase("CAISSE D'ÉPARGNE")) {
                    select.selectByVisibleText(option.getText());
                    break;
                }
                if (banque.equalsIgnoreCase("CIC") && option.getText().equalsIgnoreCase("CIC")) {
                    select.selectByVisibleText(option.getText());
                    break;
                }
                if (banque.equalsIgnoreCase("Crédit agricole") && option.getText().equalsIgnoreCase("CRÉDIT AGRICOLE")) {
                    select.selectByVisibleText(option.getText());
                    break;
                }
                if (banque.equalsIgnoreCase("Crédit mutuel") && option.getText().equalsIgnoreCase("CRÉDIT MUTUEL")) {
                    select.selectByVisibleText(option.getText());
                    break;
                }
                if (banque.equalsIgnoreCase("LCL") && option.getText().equalsIgnoreCase("LCL")) {
                    select.selectByVisibleText(option.getText());
                    break;
                }
                if (banque.equalsIgnoreCase("Société Générale") && option.getText().equalsIgnoreCase("SOCIÉTÉ GÉNÉRALE")) {
                    select.selectByVisibleText(option.getText());
                    break;
                }
            }
            element.pressEnter();
        }
    }

    private void selectTypeDiffere(FluxData flux, int index) {
        WebElement dropdownTypeDiffere = null;
        if (index == 0) {
            dropdownTypeDiffere = driver.findElement(By.id("loan-1_delayType"));
        }
        if (index == 1) {
            dropdownTypeDiffere = driver.findElement(By.id("loan-2_delayType"));
        }
        if (dropdownTypeDiffere!= null) {
            Select select = new Select(dropdownTypeDiffere);
            List<WebElement> allTypeProjet = select.getOptions();
            for (WebElement option : allTypeProjet) {
                String typeProjet = flux.getPrets().get(index).getDiffere();
                if (typeProjet.equalsIgnoreCase("Total") && option.getText().equalsIgnoreCase("Total")) {
                    select.selectByVisibleText(option.getText());
                    break;
                }
                if (typeProjet.equalsIgnoreCase("Partiel") && option.getText().equalsIgnoreCase("Partiel")) {
                    select.selectByVisibleText(option.getText());
                    break;
                }
            }
        }
        element.pressEnter();
    }

}
