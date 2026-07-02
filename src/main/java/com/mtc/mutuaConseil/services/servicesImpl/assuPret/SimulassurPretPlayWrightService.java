package com.mtc.mutuaConseil.services.servicesImpl.assuPret;

import com.mtc.mutuaConseil.base.BasePlaywrightService;
import com.mtc.mutuaConseil.models.Compte;
import com.mtc.mutuaConseil.models.FluxData;
import com.mtc.mutuaConseil.models.Tarif;
import com.mtc.mutuaConseil.services.LaunchedService;
import com.mtc.mutuaConseil.services.servicesImpl.TypeAssuranceService;
import com.mtc.mutuaConseil.utils.TarifUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SimulassurPretPlayWrightService extends BasePlaywrightService implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(SimulassurPretPlayWrightService.class);
    private final TypeAssuranceService typeAssuranceService;
    private final String source = "SimulassurPret";

    public SimulassurPretPlayWrightService(TypeAssuranceService typeAssuranceService) {
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        log.info("Début de traitement -- SimulassurPret avec Playwright");
        Tarif tarifSimulassurPret = TarifUtils.createDefaultTarif(c, typeAssuranceService, 1L);

        try {
            // Initialisation simplifiée - elementLib est initialisé automatiquement
            initializeBrowser(false);
            // Navigation
            humanLikeNavigate(c.getUrlFournisseur());
            // CONNEXION
            elementLib.humanTypeById("brokerCode", c.getUsername());
            elementLib.humanTypeById("password", c.getPassword());
            elementLib.clickByXpath("//button[normalize-space()='Connexion']");
            elementLib.waitForPageLoad();

            // NOUVEAU DEVIS
            elementLib.clickByXpath("//span[text()=\" Nouveau devis \"]");
            elementLib.waitForPageLoad();

            // REMPLIR INFORMATIONS PERSONNE
            remplirInformationsPersonne(flux);

            // NAVIGATION
            elementLib.clickByXpath("//button[text()=\"Suivant\"]");
            elementLib.waitForPageLoad();

            // REMPLIR INFORMATIONS PRET
            remplirInformationsPret(flux);

            // GESTION SIDEBAR
            if (flux.getPrets().size() == 1) {
                closeSidebarIfSmallScreen();
            }
            elementLib.clickByXpath("//button[text()=\" Valider \"]");

            // VALIDATION FINALE
//            waitThread(3);
//            elementLib.clickByXpath("//button //span[text()=\" Valider \"]");
//            waitThread(8);

            // RÉCUPÉRATION DU COÛT
            String cout = getCoutTotal();
            tarifSimulassurPret.setMontant(cout);
            log.info("Coût : {}", cout);

            // CAPTURE D'ÉCRAN
            String screenshotPath = captureScreenshot(tarifSimulassurPret.getNom(), false, tarifSimulassurPret);
            if (screenshotPath != null) {
                tarifSimulassurPret.setCaptureImg(screenshotPath);
            }
            tarifSimulassurPret.setExecution(true);
        } catch (Exception e) {
            log.error("Une erreur est survenue : ", e);
            tarifSimulassurPret.setErreur(e.getMessage());
            String screenshotPathErreur = captureScreenshot(tarifSimulassurPret.getNom(), true, tarifSimulassurPret);
            tarifSimulassurPret.setCaptureImgErreur(screenshotPathErreur);
            tarifSimulassurPret.setEtape("");
        } finally {
            cleanup(); // Nettoyage automatique
        }
        return tarifSimulassurPret;
    }

    private void remplirInformationsPersonne(FluxData flux) {
        int index = 0;
        // CIVILITÉ
        if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M") ||
                flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur")) {
            elementLib.clickById("customer-1_civility-men");
        } else {
            elementLib.clickById("customer-1_civility-women");
        }
        // INFORMATIONS DE BASE
        elementLib.humanTypeById("customer-1_lastname", flux.getPersonnes().get(index).getNom());
        elementLib.humanTypeById("customer-1_firstname", flux.getPersonnes().get(index).getPrenom());
        elementLib.humanTypeById("customer-1_birthDate", flux.getPersonnes().get(index).getDateNaissance());
        elementLib.humanTypeById("customer-1_zipCode", flux.getPersonnes().get(index).getCodePostal());
        // STATUT PROFESSIONNEL
        selectStatutProfession(flux, index);
        // CONDITIONS SPÉCIFIQUES
        if (flux.getInfoAssureComplets().get(index).getDeplacementPro20000()) {
            elementLib.selectByValue("#customer-1_businessTrip", "true");
        }
        if (flux.getInfoAssureComplets().get(index).getFumeur()) {
            elementLib.clickById("customer-1_smoker");
        }
        if (flux.getInfoAssureComplets().get(index).getTravailManuel()) {
            elementLib.clickById("customer-1_handling");
        }
        if (flux.getInfoAssureComplets().get(index).getTravailHauteur() &&
                (flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("15 à 20m") ||
                        flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("Plus de 20m"))) {
            elementLib.clickByXpath("/html/body/div[1]/div[1]/div[2]/div/div[2]/div/form/div[1]/div[1]/div/label[3]/span[2]");
        }
        if (flux.getInfoAssureComplets().get(index).getMetierExpose()) {
            elementLib.clickById("customer-1_smoker");
        }
        // DEUXIÈME EMPRUNTEUR
        if (flux.getPersonnes().size() == 2) {
            ajouterDeuxiemeEmprunteur(flux);
        }
    }

    private void ajouterDeuxiemeEmprunteur(FluxData flux) {
        int index = 1;
        elementLib.clickByXpath("//button[text()=\" Cliquez pour ajouter un emprunteur \"]");
        // CIVILITÉ DU DEUXIÈME EMPRUNTEUR
        if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M") ||
                flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur")) {
            elementLib.clickById("customer-2_civility-men");
        } else {
            elementLib.clickById("customer-2_civility-women");
        }
        elementLib.humanTypeById("customer-2_lastname", flux.getPersonnes().get(index).getNom());
        elementLib.humanTypeById("customer-2_firstname", flux.getPersonnes().get(index).getPrenom());
        elementLib.humanTypeById("customer-2_birthDate", flux.getPersonnes().get(index).getDateNaissance());
        elementLib.humanTypeById("customer-2_zipCode", flux.getPersonnes().get(index).getCodePostal());

        selectStatutProfession(flux, index);

        if (flux.getInfoAssureComplets().get(index).getDeplacementPro20000()) {
            elementLib.selectByValue("#customer-2_businessTrip", "true");
        }
        if (flux.getInfoAssureComplets().get(index).getFumeur()) {
            elementLib.clickById("customer-2_smoker");
        }
        if (flux.getInfoAssureComplets().get(index).getTravailManuel()) {
            elementLib.clickById("customer-2_handling");
        }
        if (flux.getInfoAssureComplets().get(index).getTravailHauteur() &&
                (flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("15 à 20m") ||
                        flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("Plus de 20m"))) {
            elementLib.clickById("customer-2_height");
        }
        if (flux.getInfoAssureComplets().get(index).getMetierExpose()) {
            elementLib.clickById("customer-1_smoker");
        }
    }

    private void remplirInformationsPret(FluxData flux) {
        closeSidebarIfSmallScreen();
        int index = 0;
        // TYPE DE PROJET
        selectTypeProjet(flux, 0);
        // INFORMATIONS PRÊT
        elementLib.humanTypeById("effectiveDate", flux.getPrets().get(index).getDateEffet());
        choixBanque(flux);
        elementLib.humanTypeById("loan-1_amount", flux.getPrets().get(index).getMontantPret());
        elementLib.humanTypeById("loan-1_rate", flux.getPrets().get(index).getTaux());
        elementLib.humanTypeById("loan-1_duration", flux.getPrets().get(index).getDuree());
        elementLib.humanTypeById("loan-1_delay", flux.getPrets().get(index).getDiffere());
        // DIFFÉRÉ
        if (!flux.getPrets().get(index).getDiffere().isEmpty() &&
                (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Partiel") ||
                        flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Total"))) {
            selectTypeDiffere(flux, index);
        }
        // TYPE DE TAUX
        selectTypeTaux(flux, index);
        elementLib.pressEnterOnBody();
        // DEUXIÈME PRÊT
        if (flux.getPrets().size() == 2) {
            ajouterDeuxiemePret(flux);
        }
    }

    private void ajouterDeuxiemePret(FluxData flux) {
        int index = 1;
        elementLib.clickByXpath("//button[text()=\" Cliquez pour ajouter un prêt \"]");
        elementLib.humanTypeById("loan-2_amount", flux.getPrets().get(index).getMontantPret());
        elementLib.humanTypeById("loan-2_rate", flux.getPrets().get(index).getTaux());
        elementLib.humanTypeById("loan-2_duration", flux.getPrets().get(index).getDuree());
        elementLib.humanTypeById("loan-2_delay", flux.getPrets().get(index).getDiffere());

        if (!flux.getPrets().get(index).getDiffere().isEmpty() &&
                (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Partiel") ||
                        flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Total"))) {
            selectTypeDiffere(flux, index);
        }

        selectTypeTaux(flux, index);
        elementLib.pressEnterOnBody();
    }

    // ==================== MÉTHODES DE SÉLECTION ====================

    private void selectStatutProfession(FluxData flux, int index) {
        String profession = flux.getPersonnes().get(index).getProfessionSpecifique();
        try {
            String selectId = index == 0 ? "customer-1_profession" : "customer-2_profession";
            switch (profession.toLowerCase()) {
                case "salarié cadre":
                    page.selectOption("#" + selectId, "2");
                    break;
                case "salarié non cadre : employé":
                    page.selectOption("#" + selectId, "1");
                    break;
                case "artisan":
                    page.selectOption("#" + selectId, "3");
                    break;
                case "dirigeant d'entreprise":
                    page.selectOption("#" + selectId, "16");
                    break;
                case "fonctionnaire cadre":
                    page.selectOption("#" + selectId, "23");
                    break;
                case "fonctionnaire non cadre":
                    page.selectOption("#" + selectId, "22");
                    break;
                default:
                    elementLib.clickById(selectId);
                    page.waitForTimeout(300);
                    elementLib.click("option:has-text('" + profession + "')");
            }
        } catch (Exception e) {
            log.error("Erreur lors de la sélection de la profession: {}", profession, e);
            throw new RuntimeException("Échec de la sélection de la profession: " + profession, e);
        }
    }

    private void selectTypeProjet(FluxData flux, int index) {
        String typeProjet = flux.getPrets().get(index).getObjet();
        try {
            String selectId = index == 0 ? "projectQualification" : "customer-2_profession";
            switch (typeProjet.toLowerCase()) {
                case "résidence principale":
                    page.selectOption("#" + selectId, "rp");
                    break;
                case "Résidence secondaire":
                    page.selectOption("#" + selectId, "rs");
                    break;
                default:
                    elementLib.clickById(selectId);
                    page.waitForTimeout(300);
                    elementLib.click("option:has-text('" + typeProjet + "')");
            }
        } catch (Exception e) {
            log.error("Erreur lors de la sélection de la profession: {}", typeProjet, e);
            throw new RuntimeException("Échec de la sélection de la profession: " + typeProjet, e);
        }
    }

    private void selectTypeTaux(FluxData flux, int index) {
        String selectId = "loan-" + (index + 1) + "_warranty-1";
        elementLib.clickById(selectId);
        String garantie = null;
        if (flux.getInfoAssureComplets().size() == 1)
            garantie = flux.getInfoAssureComplets().get(0).getGarantie();
        if (flux.getInfoAssureComplets().size() == 2)
            garantie = flux.getInfoAssureComplets().get(1).getGarantie();
        switch (garantie.toUpperCase()) {
            case "IPT":
                elementLib.selectByIndex("#" + selectId, 0);
                break;
            case "IPT, ITT":
                elementLib.selectByIndex("#" + selectId, 1);
                break;
            case "IPT, ITT, IPP":
                elementLib.selectByIndex("#" + selectId, 2);
                break;
        }
    }

    private void choixBanque(FluxData flux) {
        if (flux.getPrets().get(0).getBanque() != null) {
            String banque = flux.getPrets().get(0).getBanque();
            switch (banque.toLowerCase()) {
                case "axa banque":
                    elementLib.selectByVisibleText("#bank", "AXA BANQUE");
                    break;
                case "banque populaire":
                    elementLib.selectByVisibleText("#bank", "BANQUE POPULAIRE");
                    break;
                case "la banque postale":
                    elementLib.selectByVisibleText("#bank", "BANQUE POSTALE");
                    break;
                case "bnp paribas":
                    elementLib.selectByVisibleText("#bank", "BNP PARIBAS");
                    break;
                case "caisse d'épargne":
                    elementLib.selectByVisibleText("#bank", "CAISSE D'ÉPARGNE");
                    break;
                case "cic":
                    elementLib.selectByVisibleText("#bank", "CIC");
                    break;
                case "crédit agricole":
                    elementLib.selectByVisibleText("#bank", "CRÉDIT AGRICOLE");
                    break;
                case "crédit mutuel":
                    elementLib.selectByVisibleText("#bank", "CRÉDIT MUTUEL");
                    break;
                case "lcl":
                    elementLib.selectByVisibleText("#bank", "LCL");
                    break;
                case "société générale":
                    elementLib.selectByVisibleText("#bank", "SOCIÉTÉ GÉNÉRALE");
                    break;
            }
        }
    }

    private void selectTypeDiffere(FluxData flux, int index) {
        String selectId = "loan-" + (index + 1) + "_delayType";
        String typeDiffere = flux.getPrets().get(index).getDiffere();

        if (typeDiffere.equalsIgnoreCase("Total")) {
            elementLib.selectByVisibleText("#" + selectId, "Total");
        } else if (typeDiffere.equalsIgnoreCase("Partiel")) {
            elementLib.selectByVisibleText("#" + selectId, "Partiel");
        }
    }

    // ==================== MÉTHODES UTILITAIRES SPÉCIFIQUES ====================

    private String getCoutTotal() {
        try {
            return elementLib.getElementTextByXpath(
                    "(//div[contains(@class, 'price-list') and contains(@class, 'mb-3.5')])[1]//div[contains(@class, 'text-xl')]"
            );
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du coût", e);
            return "0";
        }
    }

    private void closeSidebarIfSmallScreen() {
        if (elementLib.isElementVisible("xpath=//*[@id=\"form-sidebar\"]//div//button")) {
            elementLib.clickByXpath("//*[@id=\"form-sidebar\"]//div//button");
        }
    }

}