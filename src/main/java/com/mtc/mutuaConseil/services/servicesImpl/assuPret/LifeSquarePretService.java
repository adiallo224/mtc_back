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
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LifeSquarePretService extends BaseAutomationService implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(LifeSquarePretService.class);
    private final TypeAssuranceService typeAssuranceService;

    public LifeSquarePretService(TypeAssuranceService typeAssuranceService) {
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {

        log.info("Debut de traitement -- LifeSquarePret");
        Tarif tarifLifeSquarePret = TarifUtils.createDefaultTarif(c, typeAssuranceService, 1L);

        // Déterminer le navigateur à partir du compte si possible (sinon Chromedriver Selenium par défaut)
        BrowserType browserType = BrowserType.SELENIUM_CHROME;
        try {
            if (c != null) {
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
            }

            // Initialise le navigateur via la factory (headless = false pour imiter ton ancien comportement)
            init(browserType, false);

            // Navigation initiale
            humanNavigate(c.getUrlFournisseur());
            element.clickById("popin_tc_privacy_button_3");
            remplirConnexion(c);

            remplirInformationsGenerales(flux);

            remplirInformationsPret(flux);
            remplirInformationsPersonne(flux);

            waitThread(6);
            tarifer();
            //Cout
            waitThread(2);
            String cout = getCout();
            log.info("Cout {} : ", cout);
            tarifLifeSquarePret.setMontant(cout);
            String screenshotBytes = captureScreenshot(tarifLifeSquarePret.getNom(), false, tarifLifeSquarePret);
            if (screenshotBytes != null) {
                tarifLifeSquarePret.setCaptureImg(screenshotBytes);
            }
            tarifLifeSquarePret.setExecution(true);
        } catch (Exception e) {
            log.error("An error occurred : ", e);
            tarifLifeSquarePret.setErreur(e.getMessage());
            String screenshotBytesErreur = captureScreenshot(tarifLifeSquarePret.getNom(), true, tarifLifeSquarePret);
            tarifLifeSquarePret.setCaptureImgErreur(screenshotBytesErreur);
            tarifLifeSquarePret.setEtape("");
        } finally {
            cleanup();
        }
        return tarifLifeSquarePret;
    }

    private void remplirConnexion(Compte compte) {
        element.typeByXpath("/html[1]/body[1]/div[1]/div[2]/div[1]/div[1]/div[2]/div[1]/div[1]/div[1]/form[1]/div[1]/div[1]/div[1]/input[1]", compte.getUsername());
        element.typeByXpath("//input[@type='password']", compte.getPassword());
        element.clickByXpath("//button[text()='Connexion']");
    }

    private void remplirInformationsGenerales(FluxData flux) {
        WaitUtils.sleepMs(2500);
        element.clickJSExecutorByXpath("//div[@class='MuiPaper-root MuiPaper-elevation MuiPaper-elevation0 MuiDrawer-paper MuiDrawer-paperAnchorLeft MuiDrawer-paperAnchorDockedLeft css-9mf24z']//span[@class='MuiTypography-root MuiTypography-body1 MuiListItemText-primary css-1tlwrgn'][normalize-space()='Nouveau projet']");
        WaitUtils.sleepMs(1000);
        element.clickByXpath("//button[@value='NEW_LOAN']");
        WaitUtils.sleepMs(1000);
        element.clickByXpath("/html[1]/body[1]/div[1]/div[2]/main[1]/div[2]/div[1]/div[2]/div[1]/div[1]/div[2]/div[1]/div[1]/div[1]");
        choixObjetPret(flux);
//        elementLib.typeByXpath("//input[@name='effectiveDate']", flux.getPrets().get(0).getDateEffet());
        element.clickByXpath("//button[normalize-space(.)=\"L'emprunteur\"]");
        WaitUtils.sleepMs(1000);
        element.clickByXpath("//button[text()='Plus de banques...']");
        choixBanque(flux);
        WaitUtils.sleepMs(1000);
        suivant();
    }

    private void remplirInformationsPret(FluxData flux) {
        WaitUtils.sleepMs(2000);
        element.clickByXpath("//*[@id=\"root\"]/div[2]/main/div[2]/div/div[2]/div/form/div/div[1]/div/div/div");
        choixTypePret(flux);
        element.typeByXpath("//div[contains(@class,'MuiFormControl-root') and .//label[contains(normalize-space(.),'Montant du prêt')]]//input", flux.getPrets().get(0).getMontantPret());
        element.typeByXpath("//div[contains(@class,'MuiFormControl-root') and .//label[contains(normalize-space(.),'Taux du prêt')]]//input", flux.getPrets().get(0).getTaux());
        element.typeByXpath("//div[contains(@class,'MuiFormControl-root') and .//label[contains(normalize-space(.),'Durée du prêt')]]//input", flux.getPrets().get(0).getDuree());
        element.typeByXpath("//div[contains(@class,'MuiFormControl-root') and .//label[contains(normalize-space(.),'Frais de dossier bancaires')]]//input", "0");
        element.typeByXpath("//div[contains(@class,'MuiFormControl-root') and .//label[contains(normalize-space(.),'Durée différé')]]//input", flux.getPrets().get(0).getDureeDiffere());
        if (flux.getPrets().size() > 1) {
            element.clickByXpath("//button[normalize-space(.)='Ajouter un prêt']");
            WaitUtils.sleepMs(2000);
            element.clickByXpath("//*[@id=\"root\"]/div[2]/main/div[2]/div/div[2]/div/form/div/div[1]/div/div/div");
            choixTypePret(flux);
            element.typeByXpath("//div[contains(@class,'MuiFormControl-root') and .//label[contains(normalize-space(.),'Montant du prêt')]]//input", flux.getPrets().get(1).getMontantPret());
            element.typeByXpath("//div[contains(@class,'MuiFormControl-root') and .//label[contains(normalize-space(.),'Taux du prêt')]]//input", flux.getPrets().get(1).getTaux());
            element.typeByXpath("//div[contains(@class,'MuiFormControl-root') and .//label[contains(normalize-space(.),'Durée du prêt')]]//input", flux.getPrets().get(1).getDuree());
            element.typeByXpath("//div[contains(@class,'MuiFormControl-root') and .//label[contains(normalize-space(.),'Frais de dossier bancaires')]]//input", "0");
            element.typeByXpath("//div[contains(@class,'MuiFormControl-root') and .//label[contains(normalize-space(.),'Durée différé')]]//input", flux.getPrets().get(1).getDureeDiffere());
        }
        suivant();
    }

    private void remplirInformationsPersonne(FluxData flux) {
        WaitUtils.sleepMs(2000);
        int index = 0;
        if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur") ||
                flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mr")) {
            WaitUtils.sleepMs(1000);
            element.clickJSExecutorByXpath("//button[normalize-space(.)='Monsieur']");
        } else if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame") ||
                flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme")) {
            WaitUtils.sleepMs(1000);
            element.clickJSExecutorByXpath("//button[normalize-space(.)='Madame']");
        }
        element.typeByXpath("//input[@id=//label[normalize-space(text())='Nom']/@for]", flux.getPersonnes().get(index).getNom());
        element.typeByXpath("//div[contains(@class,'MuiFormControl-root') and .//label[contains(normalize-space(.),'Prénom')]]//input", flux.getPersonnes().get(index).getPrenom());
        element.typeByXpath("//div[contains(@class,'MuiFormControl-root') and .//label[contains(normalize-space(.),'Date de naissance')]]//input", flux.getPersonnes().get(index).getDateNaissance());
        WaitUtils.sleepMs(1000);
        element.typeByXpath("//div[contains(@class,'MuiFormControl-root') and .//label[contains(normalize-space(.),'Code postal / commune')]]//input", flux.getPersonnes().get(index).getVille());
        WaitUtils.sleepMs(2000);
        element.pressEnter();

        if (flux.getInfoAssureComplets().get(index).getFumeur()) {
            WaitUtils.sleepMs(1000);
            element.clickByXpath("//button[normalize-space(.)='Oui']");
        } else {
            WaitUtils.sleepMs(1000);
            element.clickByXpath("//button[normalize-space(.)='Non']");
        }
        element.clickByXpath("/html[1]/body[1]/div[1]/div[2]/main[1]/div[2]/div[1]/div[2]/div[1]/form[1]/div[1]/div[2]/div[2]/div[1]/div[1]/div[1]");
        choixCategorieProfessionnelle(flux);
        element.clickByXpath("/html[1]/body[1]/div[1]/div[2]/main[1]/div[2]/div[1]/div[2]/div[1]/form[1]/div[1]/div[2]/div[3]/div[1]/div[1]/div[1]");
        choixSecteurARisque(flux);
        element.clickByXpath("/html[1]/body[1]/div[1]/div[2]/main[1]/div[2]/div[1]/div[2]/div[1]/form[1]/div[1]/div[2]/div[4]/div[1]/div[1]/div[1]");
        choixHauteur(flux);

        if (flux.getInfoAssureComplets().get(index).getDeplacementPro20000()) {
            element.clickByXpath("//div[5]//div[1]//div[2]//button[1]");
        } else {
            element.clickByXpath("//div[5]//div[1]//div[2]//button[2]");
        }

        if (flux.getInfoAssureComplets().get(index).getTravailManuel()) {
            element.clickByXpath("//div[6]//div[1]//div[2]//button[1]");
        } else {
            element.clickByXpath("//div[6]//div[1]//div[2]//button[2]");
        }

        if (flux.getPersonnes().size() > 1) {
            WaitUtils.sleepMs(1000);
            index = 1;
            element.clickByXpath("//input[@type='checkbox']");
            WaitUtils.sleepMs(1000);
            element.clickByXpath("//button[normalize-space()='Ajouter co-emprunteur']");
            WaitUtils.sleepMs(2000);
            if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur") ||
                    flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mr")) {
                WaitUtils.sleepMs(1000);
                element.clickJSExecutorByXpath("//button[normalize-space(.)='Monsieur']");
            } else if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame") ||
                    flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme")) {
                WaitUtils.sleepMs(1000);
                element.clickJSExecutorByXpath("//button[normalize-space(.)='Madame']");
            }
            element.typeByXpath("//input[@id=//label[normalize-space(text())='Nom']/@for]", flux.getPersonnes().get(index).getNom());
            element.typeByXpath("//div[contains(@class,'MuiFormControl-root') and .//label[contains(normalize-space(.),'Prénom')]]//input", flux.getPersonnes().get(index).getPrenom());
            element.typeByXpath("//div[contains(@class,'MuiFormControl-root') and .//label[contains(normalize-space(.),'Date de naissance')]]//input", flux.getPersonnes().get(index).getDateNaissance());
            WaitUtils.sleepMs(1000);
            element.typeByXpath("//div[contains(@class,'MuiFormControl-root') and .//label[contains(normalize-space(.),'Code postal / commune')]]//input", flux.getPersonnes().get(index).getVille());
            WaitUtils.sleepMs(2000);
            element.pressEnter();

            if (flux.getInfoAssureComplets().get(index).getFumeur()) {
                WaitUtils.sleepMs(1000);
                element.clickByXpath("//button[normalize-space(.)='Oui']");
            } else {
                WaitUtils.sleepMs(1000);
                element.clickByXpath("//button[normalize-space(.)='Non']");
            }
            element.clickByXpath("/html[1]/body[1]/div[1]/div[2]/main[1]/div[2]/div[1]/div[2]/div[1]/form[1]/div[1]/div[2]/div[2]/div[1]/div[1]/div[1]");
            choixCategorieProfessionnelle(flux);
            element.clickByXpath("/html[1]/body[1]/div[1]/div[2]/main[1]/div[2]/div[1]/div[2]/div[1]/form[1]/div[1]/div[2]/div[3]/div[1]/div[1]/div[1]");
            choixSecteurARisque(flux);
            element.clickByXpath("/html[1]/body[1]/div[1]/div[2]/main[1]/div[2]/div[1]/div[2]/div[1]/form[1]/div[1]/div[2]/div[4]/div[1]/div[1]/div[1]");
            choixHauteur(flux);

            if (flux.getInfoAssureComplets().get(index).getDeplacementPro20000()) {
                element.clickByXpath("//div[5]//div[1]//div[2]//button[1]");
            } else {
                element.clickByXpath("//div[5]//div[1]//div[2]//button[2]");
            }

            if (flux.getInfoAssureComplets().get(index).getTravailManuel()) {
                element.clickByXpath("//div[6]//div[1]//div[2]//button[1]");
            } else {
                element.clickByXpath("//div[6]//div[1]//div[2]//button[2]");
            }
        }
        WaitUtils.sleepMs(1000);
        element.clickByXpath("//input[@type='checkbox']");
        element.clickByXpath("//button[normalize-space()='Terminer']");
    }

    private void tarifer() {
        element.clickByXpath("//button[normalize-space(.)='Tarifs']");
    }

    private void choixObjetPret(FluxData flux) {
        try {
            waitThread(2);
            // Vérifier que nous avons des données de prêt
            if (flux.getPrets() == null || flux.getPrets().isEmpty()) {
                log.warn("Aucune donnée de prêt disponible dans FluxData");
                return;
            }

            // Récupérer l'objet du prêt depuis les données
            String objetPret = flux.getPrets().get(0).getObjet();
            log.info("Objet du prêt à sélectionner : {}", objetPret);

            // Mapping des valeurs d'objet vers les data-value du HTML
            String dataValueToSelect = null;

            switch (objetPret.toLowerCase()) {
                case "résidence principale":
                    dataValueToSelect = "PRIMARY_RESIDENCE";
                    break;
                case "résidence secondaire":
                    dataValueToSelect = "SECONDARY_RESIDENCE";
                    break;
                case "investissement locatif":
                    dataValueToSelect = "RENTAL_INVESTMENT";
                    break;
                case "travaux immobiliers":
                case "travaux":
                    dataValueToSelect = "CONSTRUCTION_WORK";
                    break;
                case "prêt personnel":
                case "personnel":
                    dataValueToSelect = "PERSONAL_LOAN";
                    break;
                case "prêt professionnel":
                case "professionnel":
                    dataValueToSelect = "PROFESSIONAL_LOAN";
                    break;
                case "rachat de crédit immobilier":
                case "rachat immobilier":
                    dataValueToSelect = "REAL_ESTATE_DEBT_CONSOLIDATION";
                    break;
                case "rachat de crédit consommation":
                case "rachat consommation":
                    dataValueToSelect = "OTHER_DEBT_CONSOLIDATION";
                    break;
                default:
                    log.warn("Type d'objet de prêt non reconnu : {}. Sélection par défaut : PRIMARY_RESIDENCE", objetPret);
                    dataValueToSelect = "PRIMARY_RESIDENCE";
                    break;
            }

            // Construire le sélecteur pour l'élément avec le data-value correspondant
            String selectorToClick = String.format("li[data-value='%s']", dataValueToSelect);

            // Vérifier si l'élément existe avant de cliquer
            if (element.isElementPresent(selectorToClick)) {
                element.click(selectorToClick);
                log.info("Option sélectionnée avec succès : {} (data-value: {})", objetPret, dataValueToSelect);

                // Attendre un peu après la sélection
                waitThread(1);
            } else {
                // Fallback : essayer de cliquer par texte visible
                String textSelector = null;
                switch (dataValueToSelect) {
                    case "PRIMARY_RESIDENCE":
                        textSelector = "text=Résidence principale";
                        break;
                    case "SECONDARY_RESIDENCE":
                        textSelector = "text=Résidence secondaire";
                        break;
                    case "RENTAL_INVESTMENT":
                        textSelector = "text=Investissement locatif";
                        break;
                    case "CONSTRUCTION_WORK":
                        textSelector = "text=Travaux immobiliers";
                        break;
                    case "PERSONAL_LOAN":
                        textSelector = "text=Prêt personnel";
                        break;
                    case "PROFESSIONAL_LOAN":
                        textSelector = "text=Prêt professionnel";
                        break;
                    case "REAL_ESTATE_DEBT_CONSOLIDATION":
                        textSelector = "text=Rachat de crédit immobilier";
                        break;
                    case "OTHER_DEBT_CONSOLIDATION":
                        textSelector = "text=Rachat de crédit consommation";
                        break;
                }

                if (textSelector != null && element.isElementPresent(textSelector)) {
                    element.click(textSelector);
                    log.info("Option sélectionnée par texte : {}", textSelector);
                } else {
                    log.error("Impossible de trouver l'option pour l'objet : {}", objetPret);
                }
            }

        } catch (Exception e) {
            log.error("Erreur lors de la sélection de l'objet du prêt : {}", e.getMessage(), e);
            throw new RuntimeException("Échec de la sélection de l'objet du prêt", e);
        }
    }

    private void choixBanque(FluxData flux) {
        try {
            waitThread(2);
            if (flux.getPrets() == null || flux.getPrets().isEmpty()) {
                return;
            }
            String banque = flux.getPrets().get(0).getBanque();

            if (banque == null || banque.trim().isEmpty()) {
                return;
            }

            String b = banque.toLowerCase().trim();
            String banqueToSelect;

            switch (b) {
                case "société générale":
                case "societe generale":
                case "sg":
                    banqueToSelect = "Société Générale";
                    break;

                case "bnp paribas":
                case "bnp":
                case "paribas":
                    banqueToSelect = "BNP Paribas";
                    break;

                case "crédit agricole":
                case "credit agricole":
                case "ca":
                    banqueToSelect = "Crédit Agricole";
                    break;

                case "caisse d'epargne":
                case "caisse d'épargne":
                case "caisse epargne":
                case "ce":
                    banqueToSelect = "Caisse d'Epargne";
                    break;

                case "cic":
                    banqueToSelect = "CIC";
                    break;

                case "banque populaire":
                case "bp":
                    banqueToSelect = "Banque Populaire";
                    break;

                case "boursobank":
                case "bourso":
                    banqueToSelect = "BoursoBank";
                    break;

                case "lcl":
                case "crédit lyonnais":
                case "credit lyonnais":
                    banqueToSelect = "LCL";
                    break;

                default:
                    log.warn("Banque non reconnue : {}. Tentative de recherche directe.", banque);
                    banqueToSelect = banque;
            }

            List<WebElement> allBanks = getElements("img[alt]");

            for (WebElement img : allBanks) {
                String alt = img.getAttribute("alt");
                if (alt != null && alt.toLowerCase().contains(banqueToSelect.toLowerCase())) {
                    String selector = String.format("//img[@alt='%s']", alt);
                    element.clickJSExecutorByXpath(selector);
                    waitThread(1);
                    return;
                }
            }
        } catch (Exception e) {
            log.error("Erreur lors de la sélection de la banque : {}", e.getMessage(), e);
        }
    }

    private void choixTypePret(FluxData flux) {
        try {
            // Vérifier que nous avons des données de prêt
            if (flux.getPrets() == null || flux.getPrets().isEmpty()) {
                log.warn("Aucune donnée de prêt disponible dans FluxData");
                return;
            }

            // Récupérer le type du prêt depuis les données
            String typePret = flux.getPrets().get(0).getType();
            log.info("Type du prêt à sélectionner : {}", typePret);

            if (typePret == null || typePret.trim().isEmpty()) {
                log.warn("Aucun type de prêt spécifié dans les données, sélection par défaut : AMORTIZED");
                typePret = "amortissable";
            }

            // Mapping des valeurs de type vers les data-value du HTML
            String dataValueToSelect = null;

            switch (typePret.toLowerCase().trim()) {
                case "amortissable":
                case "amortizable":
                case "classique":
                case "standard":
                    dataValueToSelect = "AMORTIZED";
                    break;
                case "prêt à taux zéro":
                case "pret a taux zero":
                case "ptz":
                case "taux zéro":
                case "taux zero":
                    dataValueToSelect = "ZERO_INTEREST";
                    break;
                case "in-fine":
                case "in fine":
                case "infine":
                case "bullet":
                    dataValueToSelect = "BULLET";
                    break;
                case "prêt relais":
                case "pret relais":
                case "relais":
                case "bridge":
                    dataValueToSelect = "BRIDGE";
                    break;
                case "prêt à paliers":
                case "pret a paliers":
                case "paliers":
                case "à paliers":
                case "a paliers":
                case "levels":
                    dataValueToSelect = "LEVELS";
                    break;
                default:
                    log.warn("Type de prêt non reconnu : {}. Sélection par défaut : AMORTIZED", typePret);
                    dataValueToSelect = "AMORTIZED";
                    break;
            }

            // Construire le sélecteur pour l'élément avec le data-value correspondant
            String selectorToClick = String.format("li[data-value='%s']", dataValueToSelect);

            // Vérifier si l'élément existe avant de cliquer
            if (element.isElementPresent(selectorToClick)) {
                element.click(selectorToClick);
                log.info("Type de prêt sélectionné avec succès : {} (data-value: {})", typePret, dataValueToSelect);

                // Attendre un peu après la sélection
                waitThread(1);
            } else {
                // Fallback : essayer de cliquer par texte visible
                String textSelector = null;
                switch (dataValueToSelect) {
                    case "AMORTIZED":
                        textSelector = "text=Amortissable";
                        break;
                    case "ZERO_INTEREST":
                        textSelector = "text=Prêt à taux zéro";
                        break;
                    case "BULLET":
                        textSelector = "text=In-fine";
                        break;
                    case "BRIDGE":
                        textSelector = "text=Prêt relais";
                        break;
                    case "LEVELS":
                        textSelector = "text=Prêt à paliers";
                        break;
                }

                if (textSelector != null && element.isElementPresent(textSelector)) {
                    element.click(textSelector);
                    log.info("Type de prêt sélectionné par texte : {}", textSelector);
                } else {
                    log.error("Impossible de trouver l'option pour le type de prêt : {}", typePret);

                    // Log des options disponibles pour debug
                    logAvailableLoanTypes();
                }
            }

        } catch (Exception e) {
            log.error("Erreur lors de la sélection du type de prêt : {}", e.getMessage(), e);
            throw new RuntimeException("Échec de la sélection du type de prêt", e);
        }
    }

    private void logAvailableLoanTypes() {
        try {
            log.info("Types de prêts disponibles dans l'interface :");
            List<String> knownTypes = List.of(
                    "Amortissable (AMORTIZED)",
                    "Prêt à taux zéro (ZERO_INTEREST)",
                    "In-fine (BULLET)",
                    "Prêt relais (BRIDGE)",
                    "Prêt à paliers (LEVELS)"
            );

            knownTypes.forEach(type -> log.info("  - {}", type));
        } catch (Exception e) {
            log.error("Erreur lors du logging des types de prêts disponibles", e);
        }
    }

    private void suivant() {
        element.clickByXpath("//button[normalize-space(.)='Suivant']");
    }

    private String getCout() {
        // Récupérer tous les accordéons
        List<WebElement> offres = getElements("div.MuiPaper-root.MuiAccordion-root");

        if (offres.isEmpty()) {
            return null;
        }

        for (WebElement offre : offres) {
            // Récupérer le H4 à l'intérieur de chaque offre
            WebElement h4;
            try {
                h4 = offre.findElement(By.cssSelector("h4.MuiTypography-title"));
            } catch (Exception e) {
                continue; // si pas trouvé, on passe à l'élément suivant
            }

            String coutTexte = h4.getText().trim();

            if (!coutTexte.isEmpty()) {
                return coutTexte; // Equivalent : on retourne le premier coût trouvé
            }
        }

        return null;
    }

    private void choixSecteurARisque(FluxData flux) {
        try {
            waitThread(2);

            // Vérifier que nous avons des données de personne
            if (flux.getPersonnes() == null || flux.getPersonnes().isEmpty()) {
                log.warn("Aucune donnée de personne disponible dans FluxData");
                selectSecteurRisque("NONE");
                return;
            }

            // Récupérer le statut professionnel depuis les données
            String statutProfession = flux.getPersonnes().get(0).getStatutProfession();
            log.info("Statut professionnel reçu du front : {}", statutProfession);

            // Déterminer le secteur à risque basé sur l'énumération ListeStatutProfession
            String dataValueToSelect = mapStatutProfessionToSecteurRisque(statutProfession);

            log.info("Secteur à risque sélectionné : {}", dataValueToSelect);
            selectSecteurRisque(dataValueToSelect);

        } catch (Exception e) {
            log.error("Erreur lors de la sélection du secteur à risque : {}", e.getMessage(), e);
            // En cas d'erreur, sélectionner par défaut "Pas de secteur d'activité à risque"
            selectSecteurRisque("NONE");
        }
    }

    private String mapStatutProfessionToSecteurRisque(String statutProfession) {
        if (statutProfession == null || statutProfession.isEmpty()) {
            return "NONE";
        }

        // Mapping direct des valeurs d'énumération ListeStatutProfession vers les data-value HTML
        switch (statutProfession) {
            case "FONCTIONNAIRE":
                return "NONE"; // Les fonctionnaires ne sont généralement pas dans un secteur à risque

            case "AUCUN_SECTEUR_ACTIVITE":
                return "NONE";

            case "AGENT_SECURITE":
                return "SECU_SANS_PORT_ARME";

            case "ARMEE":
                return "ARMEE";

            case "ARTISTE":
                return "ART_SPECTACLE";

            case "BUCHERON_DEBARDEUR":
                return "FORESTIER";

            case "CASCADEUR":
                return "ART_SPECTACLE"; // Les cascadeurs sont liés au spectacle

            case "FORAIN":
                return "ASTROLOGIE_CARTOMANCIE";

            case "FORCE_ORDRE_POMPIER":
                // Distinguer entre force de l'ordre et pompier (par défaut force de l'ordre)
                return "FORCE_ORDRE";

            case "GUIDE_MONTAGNE_MONITEUR_SKI":
                return "HAUTE_MONTAGNE";

            case "JOURNALISTE_CAM_PHOTO":
                return "NONE"; // Journaliste standard, pas forcément à risque sauf déplacement

            case "MARIN_PECHEUR":
                return "MARIN";

            case "MEMBRE_ONG":
                return "HUMANITAIRE";

            case "MONTEUR_ELEC_HAUTE_TENSION":
                return "MINE"; // Travaux en hauteur/grande hauteur

            case "PILOTE_PERSONNEL_NAVIGANT":
                return "NAVIGANT_REGULIERE";

            case "POLITIQUE_CONNUE":
                return "POLITIQUE";

            case "PROFESSION_AVEC_PORT_ARME":
                return "SECU_PORT_ARME";

            case "PROFESSION_FORAGE_PLONGE":
                return "MINE"; // Travaux souterrains, grande hauteur

            case "PROFESSION_PRODUIT_DANGEREUX":
                return "PRODUITS_DANGEREUX";

            case "PROFESSION_PARANORMAUX":
                return "ASTROLOGIE_CARTOMANCIE";

            case "PROFESSION_AIMAUX_DANGEREUX":
                return "NONE"; // Pas de catégorie spécifique pour les animaux dangereux

            case "SPORTIF_PROFESSIONNEL":
                return "SPORT";

            default:
                log.warn("Statut professionnel non reconnu : {}. Sélection par défaut : NONE", statutProfession);
                return "NONE";
        }
    }

    private void selectSecteurRisque(String dataValue) {
        try {
            // Construire le sélecteur pour l'élément avec le data-value correspondant
            String selectorToClick = String.format("li[data-value='%s']", dataValue);

            // Vérifier si l'élément existe avant de cliquer
            if (element.isElementPresent(selectorToClick)) {
                element.click(selectorToClick);
                log.info("Secteur à risque sélectionné avec succès : {}", dataValue);

                // Attendre un peu après la sélection
                waitThread(1);
            } else {
                // Fallback : essayer de cliquer par texte visible
                String textSelector = getTextSelectorForSecteurRisque(dataValue);

                if (textSelector != null && element.isElementPresent(textSelector)) {
                    element.click(textSelector);
                    log.info("Secteur à risque sélectionné par texte : {}", textSelector);
                } else {
                    log.error("Impossible de trouver l'option pour le secteur à risque : {}", dataValue);
                }
            }

        } catch (Exception e) {
            log.error("Erreur lors de la sélection du secteur à risque : {}", e.getMessage(), e);
            throw new RuntimeException("Échec de la sélection du secteur à risque", e);
        }
    }

    private String getTextSelectorForSecteurRisque(String dataValue) {
        switch (dataValue) {
            case "NONE":
                return "text=Pas de secteur d'activité à risque";
            case "HUMANITAIRE":
                return "text=Activités humanitaires";
            case "ARTIFICIER":
                return "text=Artificier, usage d'explosifs";
            case "DOCKER":
                return "text=Docker, activités sur chantier naval";
            case "FORCE_ORDRE":
                return "text=Force de l'ordre";
            case "HAUTE_MONTAGNE":
                return "text=Profession de haute montagne (guide, moniteur de ski)";
            case "PRODUITS_DANGEREUX":
                return "text=Métiers de l'industrie avec manipulation de produits dangereux (chimie, biologie, pharmacie)";
            case "MAITRE_NAGEUR":
                return "text=Maitre nageur plage et plan eau";
            case "ARMEE":
                return "text=Armée (Militaire, Gendarme)";
            case "NAVIGANT_NON_REGULIERE":
                return "text=Personnel navigant de compagnie aérienne non régulière";
            case "NAVIGANT_REGULIERE":
                return "text=Personnel navigant de compagnie aérienne régulière";
            case "POLITIQUE":
                return "text=Profession politique et personnel diplomatique";
            case "POMPIER":
                return "text=Pompier professionnel, sauveteur";
            case "MARIN":
                return "text=Profession avec activité en mer";
            case "SECU_PORT_ARME":
                return "text=Profession de la sécurité ou de la surveillance avec port d'arme, douanier";
            case "SECU_SANS_PORT_ARME":
                return "text=Profession de surveillance sans port d'arme";
            case "ART_SPECTACLE":
                return "text=Profession relative à l'art et au spectacle, à l'audiovisuel et information, au cirque";
            case "ASTROLOGIE_CARTOMANCIE":
                return "text=Profession relative à astrologie, magnétisme, cartomancie, forain";
            case "PETROLIER":
                return "text=Secteur pétrolier (raffinage, exploitation, plate-forme)";
            case "SPORT":
                return "text=Sportif professionnel, entraîneur, arbitre, moniteur d'aviation";
            case "MINE":
                return "text=Travaux souterrains, mine, galerie, spéléologie, grande hauteur";
            case "NUCLEAIRE":
                return "text=Les métiers du nucléaire";
            case "FORESTIER":
                return "text=Profession du secteur forestier";
            case "MANIP_TRANSP_PROD_DANGEREUX":
                return "text=Profession avec manipulation et/ou transport de produits dangereux";
            default:
                return null;
        }
    }

    private void choixCategorieProfessionnelle(FluxData flux) {
        try {
            waitThread(2);

            // Vérifier que nous avons des données de personne
            if (flux.getPersonnes() == null || flux.getPersonnes().isEmpty()) {
                log.warn("Aucune donnée de personne disponible dans FluxData");
                return;
            }

            // Récupérer le statut professionnel depuis les données
            String statutProfession = flux.getPersonnes().get(0).getStatutProfession();
            log.info("Catégorie professionnelle reçue du front : {}", statutProfession);

            // Mapping direct des valeurs d'énumération CategorieProfession vers les data-value HTML
            String dataValueToSelect = mapCategorieProfessionToDataValue(statutProfession);

            log.info("Data-value sélectionné : {}", dataValueToSelect);

            // Construire le sélecteur pour l'élément avec le data-value correspondant
            String selectorToClick = String.format("li[data-value='%s']", dataValueToSelect);

            // Vérifier si l'élément existe avant de cliquer
            if (element.isElementPresent(selectorToClick)) {
                element.click(selectorToClick);
                log.info("Catégorie professionnelle sélectionnée avec succès : {} (data-value: {})", statutProfession, dataValueToSelect);

                // Attendre un peu après la sélection
                waitThread(1);
            } else {
                // Fallback : essayer de cliquer par texte visible
                String textSelector = getTextSelectorForProfession(dataValueToSelect);

                if (textSelector != null && element.isElementPresent(textSelector)) {
                    element.click(textSelector);
                    log.info("Catégorie professionnelle sélectionnée par texte : {}", textSelector);
                } else {
                    log.error("Impossible de trouver l'option pour la catégorie professionnelle : {}", statutProfession);
                }
            }

        } catch (Exception e) {
            log.error("Erreur lors de la sélection de la catégorie professionnelle : {}", e.getMessage(), e);
            throw new RuntimeException("Échec de la sélection de la catégorie professionnelle", e);
        }
    }

    private String mapCategorieProfessionToDataValue(String categorieProfession) {
        if (categorieProfession == null || categorieProfession.isEmpty()) {
            log.warn("Catégorie professionnelle null ou vide. Sélection par défaut : EMPLOYE_NON_CADRE_AUTRE");
            return "EMPLOYE_NON_CADRE_AUTRE";
        }
        // Mapping direct des valeurs d'énumération CategorieProfession vers les data-value HTML
        switch (categorieProfession) {
            case "AGRICULTEUR":
                return "EXPLOITANT_AGRICOLE";

            case "ARTISAN":
                return "ARTISAN_AUTRE";

            case "CHEF_ENTREPRISE":
                return "CADRE_OU_ASSIMILE"; // Chef d'entreprise est inclus dans "Salarié cadre ou assimilé cadre, chef d'entreprise"

            case "COMMERCANT":
                return "COMMERCANT";

            case "FONCTIONNAIRE_A_HC":
                return "FONCTIONNAIRE_CLASSE_A";

            case "FONCTIONNAIRE_B_C_ADM":
                return "FONCTIONNAIRE_HORS_CLASSE_A";

            case "INTERMITTENT":
                return "INTERMITTENT_SPECTACLE";

            case "INTERIMAIRE":
                return "SAISONNIER_INTERIMAIRE"; // Intérimaire est inclus dans "Saisonnier, intérimaire"

            case "OUVRIER":
                return "OUVRIER_TECHNICIEN";

            case "PROF_LIB":
                return "PROFESSION_LIBERALE";

            case "MEDICAL_PROF":
                return "MEDECIN"; // Profession libérale médicale -> Médecin

            case "PARAMEDICAL_PROF":
                return "PROFESSION_MEDICALE_AUTRE"; // Profession libérale paramédicale -> Autres professions médicales et paramédicales

            case "CADRE_SAL":
                return "CADRE_OU_ASSIMILE";

            case "NON_CADRE_SAL_EMPLOYE":
                return "EMPLOYE_NON_CADRE_AUTRE"; // Salarié non cadre employé -> Autre employé ou salarié non-cadre

            default:
                log.warn("Catégorie professionnelle non reconnue : {}. Sélection par défaut : EMPLOYE_NON_CADRE_AUTRE", categorieProfession);
                return "EMPLOYE_NON_CADRE_AUTRE";
        }
    }

    private String getTextSelectorForProfession(String dataValue) {
        switch (dataValue) {
            case "CADRE_OU_ASSIMILE":
                return "text=Salarié cadre ou assimilé cadre, chef d'entreprise";
            case "EMPLOYE_BUREAU":
                return "text=Salarié non-cadre employé de bureau ou d'administration";
            case "EMPLOYE_COMMERCE":
                return "text=Salarié non-cadre employé de commerce ou d'artisanat";
            case "EMPLOYE_NON_CADRE_AUTRE":
                return "text=Autre employé ou salarié non-cadre";
            case "OUVRIER_TECHNICIEN":
                return "text=Ouvrier, technicien";
            case "EXPLOITANT_AGRICOLE":
                return "text=Exploitant agricole";
            case "FONCTIONNAIRE_CLASSE_A":
                return "text=Fonctionnaire classe A (cat. cadre)";
            case "FONCTIONNAIRE_HORS_CLASSE_A":
                return "text=Fonctionnaire hors classe A (cat. non cadre)";
            case "GERANT_ARTISAN":
                return "text=Gérant artisan ou commerçant +5 sal. hors BTP, gros œuvre et agricole";
            case "ARTISAN_AUTRE":
                return "text=Artisan autre";
            case "COMMERCANT":
                return "text=Commerçant, vendeur, commercial autre";
            case "PROFESSION_LIBERALE":
                return "text=Profession libérale";
            case "MEDECIN":
                return "text=Médecin hors chirurgien, interne, vétérinaire, pharmacien";
            case "CHIRURGIEN":
                return "text=Dentiste, chirurgien";
            case "PROFESSION_MEDICALE_AUTRE":
                return "text=Autres professions médicales et paramédicales";
            case "JOURNALISTE":
                return "text=Journaliste ou profession de l'information";
            case "TRAVAILLEUR_INDEPENDANT":
                return "text=Travailleur indépendant, auto-entrepreneur";
            case "RETRAITE_CADRE":
                return "text=Retraité cadre";
            case "RETRAITE_AUTRE":
                return "text=Retraité non-cadre";
            case "ETUDIANT":
                return "text=Etudiant";
            case "SANS_PROFESSION":
                return "text=Sans profession";
            case "INTERMITTENT_SPECTACLE":
                return "text=Intermittent";
            case "SAISONNIER_INTERIMAIRE":
                return "text=Saisonnier, intérimaire";
            default:
                return null;
        }
    }

    private void choixHauteur(FluxData flux) {
        try {
            waitThread(2);

            // Vérification des données
            if (flux.getInfoAssureComplets() == null || flux.getInfoAssureComplets().isEmpty()) {
                log.warn("Aucune donnée InfoAssureComplet disponible");
                return;
            }

            String hauteur = flux.getInfoAssureComplets().get(0).getHauteur();

            if (hauteur == null || hauteur.trim().isEmpty()) {
                hauteur = "NONE";
            }

            // ===== NORMALISATION / MAPPING =====
            String h = hauteur.trim().toLowerCase();
            String dataValueToSelect = "NONE"; // défaut

            switch (h) {
                case "0-3":
                case "less_3m":
                    dataValueToSelect = "LESS_3M";
                    break;

                case "3-10":
                case "10-12":
                case "12-15":
                case "less_15m":
                    dataValueToSelect = "LESS_15M";
                    break;

                case "15-20":
                case "10-25":
                case "over_15m":
                    dataValueToSelect = "OVER_15M";
                    break;

                case "none":
                case "":
                    dataValueToSelect = "NONE";
                    break;

                default:
                    try {
                        // Nettoyage chiffres
                        String clean = h.replaceAll("[^0-9-]", "");

                        // Plage (ex : "0-3", "5-12")
                        if (clean.contains("-")) {
                            String[] p = clean.split("-");
                            if (p.length >= 2) {
                                int maxVal = Integer.parseInt(p[1]);
                                if (maxVal <= 3)
                                    dataValueToSelect = "LESS_3M";
                                else if (maxVal <= 15)
                                    dataValueToSelect = "LESS_15M";
                                else
                                    dataValueToSelect = "OVER_15M";
                            }
                        }
                        // Valeur unique (ex : "2", "18")
                        else if (clean.matches("\\d+")) {
                            int v = Integer.parseInt(clean);
                            if (v <= 3)
                                dataValueToSelect = "LESS_3M";
                            else if (v <= 15)
                                dataValueToSelect = "LESS_15M";
                            else
                                dataValueToSelect = "OVER_15M";
                        }
                        // Mots-clés
                        else if (h.contains("aucun") || h.contains("none")) {
                            dataValueToSelect = "NONE";
                        } else if (h.contains("moins") && h.contains("3")) {
                            dataValueToSelect = "LESS_3M";
                        } else if (h.contains("moins") && h.contains("15")) {
                            dataValueToSelect = "LESS_15M";
                        } else if (h.contains("plus") && (h.contains("15") || h.contains("20"))) {
                            dataValueToSelect = "OVER_15M";
                        }

                    } catch (Exception e) {
                        log.warn("Impossible de parser hauteur '{}'", hauteur);
                    }
            }

            // ===== SÉLECTION DANS LE DOM =====
            List<WebElement> options = getElements("li[data-value]");

            for (WebElement li : options) {
                String dv = li.getAttribute("data-value");

                if (dv != null && dv.equalsIgnoreCase(dataValueToSelect)) {
                    String xpath = String.format("//li[@data-value='%s']", dv);
                    element.clickJSExecutorByXpath(xpath);
                    waitThread(1);
                    return;
                }
            }

            // Aucune correspondance trouvée
            log.warn("Aucun élément DOM trouvé pour {}", dataValueToSelect);

        } catch (Exception e) {
            log.error("Erreur lors de la sélection de la hauteur : {}", e.getMessage(), e);
        }
    }

}
