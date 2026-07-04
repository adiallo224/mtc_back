package com.mtc.mutuaConseil.services.servicesImpl.assuMutuelIndiv;

import com.mtc.mutuaConseil.base.BaseAutomationService;
import com.mtc.mutuaConseil.base.BrowserType;
import com.mtc.mutuaConseil.base.WaitUtils;
import com.mtc.mutuaConseil.models.Compte;
import com.mtc.mutuaConseil.models.FluxData;
import com.mtc.mutuaConseil.models.Tarif;
import com.mtc.mutuaConseil.models.TypeAssurance;
import com.mtc.mutuaConseil.models.enums.EnumTypeAssurance;
import com.mtc.mutuaConseil.services.LaunchedService;
import com.mtc.mutuaConseil.services.servicesImpl.TypeAssuranceService;
import com.mtc.mutuaConseil.utils.TarifUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AlptisMI_OldDService extends BaseAutomationService implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(AlptisMI_OldDService.class);
    public static WebDriver driver;
    private final String source = "AlptisMutuelIndivService";
    private JavascriptExecutor js = null;
    private static Actions actions;
    private final TypeAssuranceService typeAssuranceService;

    public AlptisMI_OldDService(TypeAssuranceService typeAssuranceService){
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        log.info("Début de traitement -- Alptis_Mutuel_Indiv -- ");
        BrowserType browserType = BrowserType.SELENIUM_FIREFOX;
        long idTypeAssu = 0L;
        EnumTypeAssurance enumTypeAssurance = null;
        if (verif(flux, "Pro")) {
            idTypeAssu = 3L;
            enumTypeAssurance = EnumTypeAssurance.MUTUELLE_PRO;
        }
        if (verif(flux, "Indiv")) {
            idTypeAssu = 2L;
            enumTypeAssurance = EnumTypeAssurance.MUTUELLE_INDIV;
        }
        Tarif tarif = TarifUtils.createDefaultTarif(c, typeAssuranceService, idTypeAssu);
        tarif.setNom(c.getNomFournisseur());
        TypeAssurance typeAssurance = new TypeAssurance();
        typeAssurance.setTypeAssurance(enumTypeAssurance);
        tarif.setTypeAssurance(typeAssurance);
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
            // Initialiser le driver depuis le BrowserAdapter pour compatibilité avec le code existant
            driver = getWebDriver();
            if (driver == null) {
                log.error("Impossible d'obtenir le WebDriver depuis le BrowserAdapter");
            }
            // Initialiser le JavascriptExecutor depuis le BrowserAdapter
            js = getJavascriptExecutor();
            if (js == null) {
                log.error("Impossible d'obtenir le JavascriptExecutor depuis le BrowserAdapter");
            }
            connexion(c);
            navigation();
            remplirContrat(flux);
            remplirAdherents(flux);
            remplirConjoint(flux);
            remplirEnfant(flux);
            recherche();
            scrollDown(0, 500);
            waitThread(1);
            WebElement sectionPrix = waitForElement(driver, By.xpath("//section[@class='pc-results__recommendations'][1]"), 25, 1);
            waitThread(1);
            WebElement divTotal = sectionPrix.findElement(By.xpath("//div[@class='pc-offer-mobile__infos']//div[@class='pc-offer-price']"));
            waitThread(1);
            WebElement totalElement = divTotal.findElement(By.xpath("//span[@class='pc-price']//strong"));
            waitThread(1);
            String cout = totalElement.getText();
            log.info("cout {}", cout);
            tarif.setMontant(cout);
            String screenshotBytes = captureScreenshot(tarif.getNom(), false, tarif);
            if (screenshotBytes != null) {
                scrollDown(0, 500);
                tarif.setCaptureImg(screenshotBytes);
            }
            tarif.setExecution(true);
        } catch (Exception e) {
             log.error("An error occurred", e);
             tarif.setErreur(e.getMessage());
             String screenshotBytesErreur = captureScreenshot(tarif.getNom(), true, tarif);
             tarif.setCaptureImgErreur(screenshotBytesErreur);
             tarif.setEtape("");
        } finally {
             driver.quit();
        }
        return tarif;
    }

    public void connexion(Compte c) {
        humanNavigate(c.getUrlFournisseur());
        WaitUtils.sleepMs(3000);
        element.clickById("axeptio_btn_dismiss");
        element.typeById("username", c.getUsername());
        element.typeById("password", c.getPassword());
        element.clickByName("login");
    }

    private void remplirContrat(FluxData flux) {
        waitThread(2);
        if (flux.getPersonnes().size() == 1 && (!flux.getEnfants().isEmpty()) && flux.getEnfants().getFirst().getCivilite().isEmpty() && flux.getEnfants().getFirst().getNom() != null && !flux.getEnfants().getFirst().getNom().isEmpty()) {
            WebElement buttonAdherentSeul = waitForElement(driver, By.xpath("//label[@for='who_me' and normalize-space()='Adhérent seul']"), 10, 1);
            buttonAdherentSeul.click();
        }
        if (flux.getPersonnes().size() == 1 && (!flux.getEnfants().isEmpty()) && !flux.getEnfants().getFirst().getCivilite().isEmpty() && flux.getEnfants().getFirst().getNom() != null && !flux.getEnfants().getFirst().getNom().isEmpty()) {
            WebElement buttonAdherentEnfant = waitForElement(driver, By.xpath("//div[@class='pc-radio-button']//label[normalize-space(text())='Adhérent et enfant(s)']"), 10, 1);
            buttonAdherentEnfant.click();
        }
        if (flux.getPersonnes().size() == 2 && (!flux.getEnfants().isEmpty()) && !flux.getEnfants().getFirst().getCivilite().isEmpty() && flux.getEnfants().getFirst().getNom() != null && !flux.getEnfants().getFirst().getNom().isEmpty()) {
            WebElement buttonCoupleEnfant = waitForElement(driver, By.xpath("//div[@class='pc-radio-button']//label[normalize-space(text())='Couple et enfant(s)']"), 10, 1);
            buttonCoupleEnfant.click();
        }
        if (flux.getPersonnes().size() == 2 && (flux.getEnfants().getFirst().getNom() != null && flux.getEnfants().getFirst().getNom().equalsIgnoreCase(""))) {
            WebElement buttonCouple = waitForElement(driver, By.xpath("//div[@class='pc-radio-button']//label[normalize-space(text())='Couple']"), 10, 1);
            buttonCouple.click();
        }
        // Remplacement d’un contrat santé souscrit chez un autre assureur

//        WebElement inputDateStart = waitForElement(driver, By.id("startDate"), 10, 2);
//        inputDateStart.sendKeys("");

        WebElement buttonContratSanteSouscrit = waitForElement(driver, By.xpath("//div[@class='pc-radio-button']//label[normalize-space(text())='Non']"), 10, 1);
        buttonContratSanteSouscrit.click();
    }

    private void remplirAdherents(FluxData flux) {
        waitThread(2);
        WebElement civilite = null;
        if (flux.getPersonnes().getFirst().getCivilite().equalsIgnoreCase("M") || flux.getPersonnes().getFirst().getCivilite().equalsIgnoreCase("Monsieur")) {
            civilite = waitForElement(driver, By.xpath("//div[@class='pc-radio-button']//label[normalize-space(text())='Monsieur']"), 10, 1);
            civilite.click();
        } else if (flux.getPersonnes().getFirst().getCivilite().equalsIgnoreCase("F") || flux.getPersonnes().getFirst().getCivilite().equalsIgnoreCase("Madame")) {
            civilite = waitForElement(driver, By.xpath("//div[@class='pc-radio-button']//label[normalize-space(text())='Madame']"), 10, 1);
            civilite.click();
        }
        WebElement inputNom = waitForElement(driver, By.id("insured_lastname"), 10, 2);
        inputNom.sendKeys(flux.getPersonnes().getFirst().getNom());
        WebElement inputPrenom = waitForElement(driver, By.id("insured_firstname"), 10, 2);
        inputPrenom.sendKeys(flux.getPersonnes().getFirst().getPrenom());
        WebElement inputDateNaissance = waitForElement(driver, By.id("birthdate"), 10, 2);
        inputDateNaissance.sendKeys(flux.getPersonnes().getFirst().getDateNaissance());
        choixCategorieSocioPro(flux, 0);
        choixRegime(flux, 0);
        if (flux.getPersonnes().getFirst().getProfessionSpecifique().equalsIgnoreCase("Chef d'entreprise")
            || flux.getPersonnes().getFirst().getProfessionSpecifique().equalsIgnoreCase("Commerçant")
            || flux.getPersonnes().getFirst().getProfessionSpecifique().equalsIgnoreCase("Profession libérale")
            || flux.getPersonnes().getFirst().getProfessionSpecifique().equalsIgnoreCase("Profession libérale médicale")
                || flux.getPersonnes().getFirst().getProfessionSpecifique().equalsIgnoreCase("Profession libérale paramédicale")
                || flux.getPersonnes().getFirst().getProfessionSpecifique().equalsIgnoreCase("Agriculteur")
                || flux.getPersonnes().getFirst().getProfessionSpecifique().equalsIgnoreCase("Artisan")
        ){
            waitThread(1);
            WebElement buttonCadreSalarie = waitForElement(driver, By.xpath("//label[@for='cadre_exercice_SALARIE']"), 10, 1);
            buttonCadreSalarie.click();
            if (flux.getPersonnes().getFirst().getProfessionSpecifique().equalsIgnoreCase("Chef d'entreprise")) {
                waitThread(1);
                WebElement buttonArtisanCommercant = waitForElement(driver, By.xpath("//label[@for='statut_professionnel_ARTISAN_COMMERCANT']"), 10, 1);
                buttonArtisanCommercant.click();
            }
        }
        WebElement inputCodePostal = waitForElement(driver, By.id("postalCode"), 10, 2);
        inputCodePostal.sendKeys(flux.getPersonnes().getFirst().getCodePostal());
    }

    private void choixRegime(FluxData flux, int index) {
        waitThread(2);
        WebElement dropdownRegime = null;
        if (index == 0) {
            dropdownRegime = waitForElement(driver, By.id("insured_regime"), 10, 1);
        } if (index == 1) {
            dropdownRegime = waitForElement(driver, By.id("partner_regime"), 10, 1);
        }
        if (dropdownRegime != null) {
            Select selectRegime = new Select(dropdownRegime);
//            if (flux.getPersonnes().get(index).getRegime().equalsIgnoreCase("Salarié"))
                selectRegime.selectByVisibleText("Sécurité Sociale");
//            if (flux.getPersonnes().get(index).getRegime().equalsIgnoreCase("IPT, ITT"))
//                selectRegime.selectByVisibleText("Sécurité sociale des indépendants");
//            if (flux.getPersonnes().get(index).getRegime().equalsIgnoreCase("IPT"))
//                selectRegime.selectByVisibleText("Alsace Moselle");
//            if (flux.getPersonnes().get(index).getRegime().equalsIgnoreCase("Aucune"))
//                selectRegime.selectByVisibleText("Régime Agricole - MSA");
        }
    }

    private void choixCategorieSocioPro(FluxData flux, int index) {
        WebElement dropdownCategorieSocioPro = null;
        if (index == 0) {
            dropdownCategorieSocioPro = waitForElement(driver, By.id("insured_category_select"), 15, 1);
        } if (index == 1) {
            dropdownCategorieSocioPro = waitForElement(driver, By.id("partner_category_select"), 15, 1);
        }
        if (dropdownCategorieSocioPro != null) {
            Select selectCategorieSocioPro = new Select(dropdownCategorieSocioPro);
            List<WebElement> options = selectCategorieSocioPro.getOptions();
            for (WebElement opt : options) {
                if (opt.getText().contains("Agriculteurs exploitants") && flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Agriculteur")) {
                    selectCategorieSocioPro.selectByVisibleText("Agriculteurs exploitants");
                } else if (opt.getText().contains("Artisans") && flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Artisan")) {
                    selectCategorieSocioPro.selectByVisibleText("Artisans");
                } else if (opt.getText().contains("Cadres") && flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Salarié cadre")) {
                    selectCategorieSocioPro.selectByVisibleText("Cadres");
                }
//                else if (opt.getText().contains("Cadres et employés de la fonction publique") && flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Salarié non cadre : employé")) {
//                    selectCategorieSocioPro.selectByVisibleText("Cadres et employés de la fonction publique");
//                }
                else if (opt.getText().contains("Chefs d'entreprise") && flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Chef d'entreprise")) {
                    selectCategorieSocioPro.selectByVisibleText("Chefs d'entreprise");
                }
                else if (opt.getText().contains("Commerçants et assimilés") && flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Commerçant")) {
                    selectCategorieSocioPro.selectByVisibleText("Commerçants et assimilés");
                }
                else if (opt.getText().contains("Employés, agents de maitrise") && flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Salarié non cadre : employé")) {
                    selectCategorieSocioPro.selectByVisibleText("Employés, agents de maitrise");
                }
                else if (opt.getText().contains("Ouvriers") && flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Ouvrier")) {
                    selectCategorieSocioPro.selectByVisibleText("Ouvriers");
                } else if (opt.getText().contains("Professions libérales et assimilés") && (flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Profession libérale") ||
                           flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Profession libérale médicale") ||
                           flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Profession libérale paramédicale"))) {
                    selectCategorieSocioPro.selectByVisibleText("Professions libérales et assimilés");
                }
            }
        }
    }

    private void recherche() {
        WebElement radioCivilite = waitForElement(driver, By.xpath("//button[normalize-space(text())='Découvrir les offres']"), 15, 1);
        radioCivilite.click();
    }

    private boolean verif(FluxData flux, String type ) {
        if (type.equalsIgnoreCase("Pro")) {
            return flux.getPersonnes().getFirst().getProfessionSpecifique().equalsIgnoreCase("Chef d'entreprise") ||
                    flux.getPersonnes().getFirst().getProfessionSpecifique().equalsIgnoreCase("Artisan") ||
                    flux.getPersonnes().getFirst().getProfessionSpecifique().equalsIgnoreCase("Agriculteur") ||
                    flux.getPersonnes().getFirst().getProfessionSpecifique().equalsIgnoreCase("Commerçant") ||
                    flux.getPersonnes().getFirst().getProfessionSpecifique().equalsIgnoreCase("Profession libérale") ||
                    flux.getPersonnes().getFirst().getProfessionSpecifique().equalsIgnoreCase("Profession libérale médicale") ||
                    flux.getPersonnes().getFirst().getProfessionSpecifique().equalsIgnoreCase("Profession libérale paramédicale");
        } else if (type.equalsIgnoreCase("Indiv")) {
            return flux.getPersonnes().getFirst().getProfessionSpecifique().equalsIgnoreCase("Salarié cadre") ||
                    flux.getPersonnes().getFirst().getProfessionSpecifique().equalsIgnoreCase("Salarié non cadre : employé") ||
                    flux.getPersonnes().getFirst().getProfessionSpecifique().equalsIgnoreCase("Ouvrier") ||
                    flux.getPersonnes().getFirst().getProfessionSpecifique().equalsIgnoreCase("Fonctionnaire classe a") ||
                    flux.getPersonnes().getFirst().getProfessionSpecifique().equalsIgnoreCase("Fonctionnaire hors classe a") ||
                    flux.getPersonnes().getFirst().getProfessionSpecifique().equalsIgnoreCase("Intermittent") ||
                    flux.getPersonnes().getFirst().getProfessionSpecifique().equalsIgnoreCase("Intérimaire");
        }
        return false;
    }

    private void remplirConjoint(FluxData flux) {
        if ( (flux.getPersonnes().size() == 2 && (!flux.getEnfants().isEmpty())
              && !flux.getEnfants().getFirst().getCivilite().isEmpty()
              && flux.getEnfants().getFirst().getNom() != null
              && !flux.getEnfants().getFirst().getNom().isEmpty())
            || (flux.getPersonnes().size() == 2 && (flux.getEnfants().getFirst().getNom() != null
                && flux.getEnfants().getFirst().getNom().equalsIgnoreCase("")))) {
            WebElement inputDateNaissanceConjoint = waitForElement(driver, By.id("partner-birthdate"), 10, 1);
            inputDateNaissanceConjoint.sendKeys(flux.getPersonnes().get(1).getDateNaissance());
            choixCategorieSocioPro(flux, 1);
            choixRegime(flux, 1);
        }
    }

    private void remplirEnfant(FluxData flux) {
        if (flux.getEnfants().getFirst().getNom() != null && !flux.getEnfants().getFirst().getNom().isEmpty()) {
            ajoutEnfant();
            WebElement inputDateNaissanceEnfant1 = waitForElement(driver, By.id("child_0_birthdate"), 10, 1);
            inputDateNaissanceEnfant1.sendKeys(flux.getEnfants().getFirst().getDateNaissance());
        }
        if (flux.getEnfants().size() == 2) {
            ajoutEnfant();
            WebElement inputDateNaissanceEnfant2 = waitForElement(driver, By.id("child_1_birthdate"), 10, 1);
            inputDateNaissanceEnfant2.sendKeys(flux.getEnfants().get(1).getDateNaissance());
        }
    }

    private void ajoutEnfant() {
        WebElement ajoutEnfant = waitForElement(driver, By.xpath("//*[@id=\"children_count\"]/button[2]/i"), 10, 1);
        ajoutEnfant.click();
    }

    private void navigation() {
        WebElement choixSectionIndivPro = waitForElement(driver, By.xpath("//span[text()='Santé individuelle']"), 10, 1);
        choixSectionIndivPro.click();
        waitThread(2);
        WebElement accederComparateur = waitForElement(driver, By.xpath("//span[normalize-space()='Accéder au comparateur']"), 10, 1);
        accederComparateur.click();
        switchPage(); // le comparateur s'ouvre dans un nouvel onglet
        driver = getWebDriver(); // rafraîchir la référence après le switchTo
        waitThread(3);
    }

}
