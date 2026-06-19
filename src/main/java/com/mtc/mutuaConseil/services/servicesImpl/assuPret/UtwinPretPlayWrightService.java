package com.mtc.mutuaConseil.services.servicesImpl.assuPret;

import com.microsoft.playwright.Locator;
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

import java.util.List;

@Service
public class UtwinPretPlayWrightService extends BasePlaywrightService implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(UtwinPretPlayWrightService.class);
    private final String source = "UtwinPret";
    private final TypeAssuranceService typeAssuranceService;

    public UtwinPretPlayWrightService(TypeAssuranceService typeAssuranceService){
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        log.info("Debut de traitement -- Utwin");
        Tarif tarifUtwinPret = TarifUtils.createDefaultTarif(c, typeAssuranceService, 1L);

        waitThread(6);
        try {
            // Initialisation simplifiée - elementLib est initialisé automatiquement
            initializeBrowser(false);
            // Navigation
            humanLikeNavigate(c.getUrlFournisseur());

            elementLib.typeByXpath("//*[@id=\"username\"]", c.getUsername());

            elementLib.typeByXpath("//*[@id=\"password\"]", c.getPassword());

            elementLib.clickByXpath("//*[@id=\"kc-login\"]");
            waitThread(12);
            //Personne physique
            elementLib.clickByXpath("//span[text()='Personne Physique']");
//            WebElement personnePhysique = waitForElement(driver, By.xpath("//span[text()='Personne Physique']"), 25, 5);
//            personnePhysique.click();

            remplirInformationsPersonne(flux, source);

            elementLib.scrollDown(300);

            remplirInformationsPret(flux, source);

            //Suivant
            waitThread(2);
            elementLib.clickByXpath("//button[@class=\"btn navButton-darkblue float-right\" and contains(text(), \"Suivant\")]");

            choixGaranties(flux);
            waitThread(2);
            scrollDown(650);

            //Tarifer
            elementLib.clickByXpath("//button[@class=\"btn navButton-orange navButtonText mr-md-3 px-4 text-uppercase font-weight-bold\" and contains(text(), \"Tarifer\")]\n");
            //Cout
            waitThread(10);
            String cout = elementLib.getElementText("//div[@class='col-3 bloc-offre'][1]//div[@class=\"tarifsEtTaux\"]//span[@class=\"big text-nowrap\"]");
            log.info("Cout {} : ", cout);
            tarifUtwinPret.setMontant(cout);
            String screenshotBytes = captureScreenshot(tarifUtwinPret.getNom(), false, tarifUtwinPret);
            if (screenshotBytes != null) {
                tarifUtwinPret.setCaptureImg(screenshotBytes);
            }
            tarifUtwinPret.setExecution(true);
        } catch (Exception e) {
            log.error("An error occurred : ", e);
            tarifUtwinPret.setErreur(e.getMessage());
            String screenshotBytesErreur = captureScreenshot(tarifUtwinPret.getNom(), true, tarifUtwinPret);
            tarifUtwinPret.setCaptureImgErreur(screenshotBytesErreur);
            tarifUtwinPret.setEtape("");
        } finally {
            cleanup();
        }
        return tarifUtwinPret;
    }

    private void remplirInformationsPersonne(FluxData flux, String source) {
        int index = 0;
        //Civilite
        if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur")) {
            elementLib.clickByXpath("//div[@id=\"assures0\"]//app-form-radio-button[@formcontrolname=\"civilite\"]//span[contains(text(),'Monsieur')]");
        } else if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame")){
            elementLib.clickByXpath("//div[@id=\"assures0\"]//app-form-radio-button[@formcontrolname=\"civilite\"]//span[contains(text(),'Madame')]");
        }
        //Nom
        elementLib.typeByXpath("//div[@id='assures0']//input[@placeholder='ex : DUPONT']", flux.getPersonnes().get(index).getNom());
        //Prenom
        elementLib.typeByXpath("//div[@id='assures0']//input[@placeholder='ex : Jean-Michel']", flux.getPersonnes().get(index).getPrenom());
        //Date naissance
        elementLib.typeByXpath("//div[@id='assures0']//input[@placeholder='jj/mm/aaaa']", flux.getPersonnes().get(index).getDateNaissance());
        // Choix statut profession
        choixStatutProfession(flux, index);

        if (flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Salarié non cadre : employé")){
            choixProfessionMedicaleParamedicale(flux, index);
        }

        waitThread(1);
        //Profession spécifique
        elementLib.clickByXpath("//div[@id='assures0']//app-form-dropdownlist[@formcontrolname=\"metierARisque\"]//div//ng-select");
        elementLib.clickByXpath("//span[contains(text(),\"Non, je n'exerce aucune profession de cette liste\")]");
        elementLib.pressEnter();

        //Exacte Profession
        elementLib.typeByXpath("//input[@placeholder='Profession exacte']", flux.getPersonnes().get(index).getProfession());
        //Code postal
        elementLib.typeByXpath("//input[@formcontrolname=\"codePostal\"]", flux.getPersonnes().get(index).getCodePostal());

        if (flux.getInfoAssureComplets().get(index).getFumeur()){
            elementLib.clickByXpath("//*[@id=\"assures0\"]/fieldset/div/app-form-container/div/div/div/div[2]/div/div/div/div[2]/div/div[1]/div[2]/switch-button/div/div");
        }
        if (flux.getInfoAssureComplets().get(index).getInstrumentPrecis()){
            elementLib.clickByXpath("//div[@id='assures0']//switch-button[@formcontrolname=\"utiliseInstrumentPrecision\"]");
            waitThread(2);
        }
        if (flux.getInfoAssureComplets().get(index).getTravailHauteur()){
            if(flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("3 à 10m")
                    || flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("10 à 12m")
                    || flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("12 à 15m")
                    || flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("15 à 20m")
                    || flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("Plus de 20m")
            ) {
                elementLib.clickByXpath("//*[@id=\"assures0\"]/fieldset/div/app-form-container/div/div/div/div[2]/div/div/div/div[2]/div/div[3]/div[2]/switch-button/div/div");
            }
        }
        if (flux.getInfoAssureComplets().get(index).getTravailManuelManuLourde() || flux.getInfoAssureComplets().get(index).getTravailManuel()){
            elementLib.clickByXpath("//*[@id=\"assures0\"]/fieldset/div/app-form-container/div/div/div/div[2]/div/div/div/div[2]/div/div[4]/div[2]/switch-button/div/div");
        }
        if (flux.getInfoAssureComplets().get(index).getDeplacementPro20000()){
            elementLib.clickByXpath("//*[@id=\"assures0\"]/fieldset/div/app-form-container/div/div/div/div[2]/div/div/div/div[2]/div/div[5]/div[2]/switch-button/div/div");
        }
        if (flux.getInfoAssureComplets().get(index).getDeplacementEtranger60()){
            elementLib.clickByXpath("//div[@id='assures0']//switch-button[@formcontrolname=\"deplacementEtranger\"]");
        }
        if (flux.getInfoAssureComplets().get(index).getDeplacementPaysRisque()){
            elementLib.clickByXpath("//div[@id='assures0']//switch-button[@formcontrolname=\"deplacementPaysARisque\"]");
        }

        if (flux.getPersonnes().size() == 2) {
            index = 1;
            elementLib.clickByXpath("//span[normalize-space()='Ajouter un(e) assuré(e)']");
            //Civilite
            if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur")) {
                elementLib.clickByXpath("//div[@id=\"assures1\"]//app-form-radio-button[@formcontrolname=\"civilite\"]//span[contains(text(),'Monsieur')]");
            } else if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame")){
                elementLib.clickByXpath("//div[@id=\"assures1\"]//app-form-radio-button[@formcontrolname=\"civilite\"]//span[contains(text(),'Madame')]");
            }
            //Nom
            elementLib.typeByXpath("//div[@id='assures1']//input[@placeholder='ex : DUPONT']", flux.getPersonnes().get(index).getNom());
            //Prenom
            elementLib.typeByXpath("//div[@id='assures1']//input[@placeholder='ex : Jean-Michel']", flux.getPersonnes().get(index).getPrenom());
            //Date naissance
            elementLib.typeByXpath("//div[@id='assures1']//input[@placeholder='jj/mm/aaaa']", flux.getPersonnes().get(index).getDateNaissance());
            //Profession
            choixStatutProfession(flux, index);

            if (flux.getPersonnes().get(index).getProfessionSpecifique().equalsIgnoreCase("Salarié non cadre : employé")){
                choixProfessionMedicaleParamedicale(flux, index);
            }

            //Profession spécifique
            waitThread(2);
            elementLib.clickByXpath("//div[@id='assures1']//app-form-dropdownlist[@formcontrolname=\"metierARisque\"]//div//ng-select");
            elementLib.clickByXpath("//span[contains(text(),\"Non, je n'exerce aucune profession de cette liste\")]");
            elementLib.pressEnter();

            //Exacte Profession
            elementLib.typeByXpath("//div[@id='assures1']//input[@placeholder='Profession exacte']", flux.getPersonnes().get(index).getProfession());

            //Code postal
            elementLib.typeByXpath("//div[@id='assures1']//input[@formcontrolname=\"codePostal\"]", flux.getPersonnes().get(index).getCodePostal());

            if (flux.getInfoAssureComplets().get(index).getFumeur()){
                elementLib.clickByXpath("//*[@id=\"assures1\"]/fieldset/div/app-form-container/div/div/div/div[2]/div/div/div/div[2]/div/div[1]/div[2]/switch-button/div/div");
            }
            if (flux.getInfoAssureComplets().get(index).getInstrumentPrecis()){
                elementLib.clickByXpath("//div[@id='assures1']//switch-button[@formcontrolname=\"utiliseInstrumentPrecision\"]");
            }
            if (flux.getInfoAssureComplets().get(index).getTravailHauteur()) {
                if(flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("3 à 10m")
                        || flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("10 à 12m")
                        || flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("12 à 15m")
                        || flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("15 à 20m")
                        || flux.getInfoAssureComplets().get(index).getHauteur().equalsIgnoreCase("Plus de 20m")
                ) {
                    elementLib.clickByXpath("//*[@id=\"assures1\"]/fieldset/div/app-form-container/div/div/div/div[2]/div/div/div/div[2]/div/div[3]/div[2]/switch-button/div/div");
                }
            }
            if (flux.getInfoAssureComplets().get(index).getTravailManuelManuLourde() || flux.getInfoAssureComplets().get(index).getTravailManuel()){
                elementLib.clickByXpath("//*[@id=\"assures1\"]/fieldset/div/app-form-container/div/div/div/div[2]/div/div/div/div[2]/div/div[4]/div[2]/switch-button/div/div");
            }
            if (flux.getInfoAssureComplets().get(index).getDeplacementPro20000()){
                elementLib.clickByXpath("//*[@id=\"assures1\"]/fieldset/div/app-form-container/div/div/div/div[2]/div/div/div/div[2]/div/div[5]/div[2]/switch-button/div/div");
            }
            if (flux.getInfoAssureComplets().get(index).getDeplacementEtranger60()){
                elementLib.clickByXpath("//div[@id='assures1']//switch-button[@formcontrolname=\"deplacementEtranger\"]");
            }
            if (flux.getInfoAssureComplets().get(index).getDeplacementPaysRisque()){
                elementLib.clickByXpath("//div[@id='assures1']//switch-button[@formcontrolname=\"deplacementPaysARisque\"]");
            }
        }
    }

    private void remplirInformationsPret(FluxData flux, String source) {
        int index = 0;
        //Type projet
        choixObjetPret(flux, index);

        waitThread(2);
        elementLib.clickByXpath("//app-form-radio-button[@formcontrolname=\"typeAffaireId\"]//div//label[1]");

        //Date prévisionnelle
        scrollDown(300);

        elementLib.typeByXpath("//input[@formcontrolname='datePrevisionDeblocage']", flux.getPrets().get(index).getDateEffet());

        //Localisation
        elementLib.clickByXpath("//app-form-dropdownlist[@formcontrolname=\"paysId\"]//div//ng-select");
        elementLib.clickByXpath("//span[@class=\"ng-select-single-dropdown-option\" and contains(text(), \"France métropolitaine (continentale & Corse)\")]\n");
        elementLib.pressEnter();

        scrollDown(300);

        //Type de pret
        choixTypePret(flux, index);

        //Taux
        elementLib.typeByXpath("//input[@formcontrolname='taux']", flux.getPrets().get(index).getTaux());
        //Montant
        elementLib.typeByXpath("//input[@formcontrolname='montantInitial']", flux.getPrets().get(index).getMontantPret());
        //Durée
        elementLib.typeByXpath("//input[@formcontrolname='dureeInitiale']", flux.getPrets().get(index).getDuree());

        if (flux.getPrets().get(index).getDureeDiffere() != null) {
            elementLib.typeByXpath("//input[@formcontrolname='dontDiffereInitial']", flux.getPrets().get(index).getDureeDiffere());
        }

        ChoixTypeTaux(flux, index);

        scrollDown(300);

        if (flux.getPrets().size() == 2) {
            index = 1;
            elementLib.clickByXpath("//button//span[contains(text(), \"Ajouter un prêt\")]");

            //Type de pret
            choixTypePret(flux, index);

            //Taux
            elementLib.typeByXpath("//div[@id=\"pret1\"]//input[@formcontrolname='taux']", flux.getPrets().get(index).getTaux());
            //Montant
            elementLib.typeByXpath("//div[@id=\"pret1\"]//input[@formcontrolname='montantInitial']", flux.getPrets().get(index).getMontantPret());
            //Durée
            elementLib.typeByXpath("//div[@id=\"pret1\"]//input[@formcontrolname='dureeInitiale']", flux.getPrets().get(index).getDuree());

            if (flux.getPrets().get(index).getDureeDiffere() != null) {
                elementLib.typeByXpath("//div[@id=\"pret1\"]//app-form-input[@label=\"Dont différé\"]//input[@formcontrolname='dontDiffereInitial']",
                flux.getPrets().get(index).getDureeDiffere());
            }
            ChoixTypeTaux(flux, index);
        }
        //Banque
        choixBanque(flux, index);
    }

    private void ChoixTypeTaux(FluxData flux, int index) {
        if (index == 0) {
            if (flux.getPrets().get(index).getTypeTaux().equalsIgnoreCase("Fixe")){
                elementLib.clickByXpath("//div[@id=\"pret0\"]//app-form-radio-button[@formcontrolname=\"typeTaux\"]//span[contains(text(), \"Fixe\")]");
            }
            if (flux.getPrets().get(index).getTypeTaux().equalsIgnoreCase("Variable")){
                elementLib.clickByXpath("//div[@id=\"pret0\"]//app-form-radio-button[@formcontrolname=\"typeTaux\"]//span[contains(text(), \"Variable\")]");
            }
        }
        if (index == 1) {
            if (flux.getPrets().get(index).getTypeTaux().equalsIgnoreCase("Fixe")){
                elementLib.clickByXpath("//div[@id=\"pret1\"]//app-form-radio-button[@formcontrolname=\"typeTaux\"]//span[contains(text(), \"Fixe\")]");
            }
            if (flux.getPrets().get(index).getTypeTaux().equalsIgnoreCase("Variable")){
                elementLib.clickByXpath("//div[@id=\"pret1\"]//app-form-radio-button[@formcontrolname=\"typeTaux\"]//span[contains(text(), \"Variable\")]");
            }
        }
    }

    private void choixStatutProfession(FluxData flux, int index) {
        // Sélection du dropdown selon l'index
        String dropdownSelector;
        if (index == 0) {
            dropdownSelector = "//div[@id='assures0']//app-form-dropdownlist[@formcontrolname=\"statutProfessionnel\"]//div//ng-select";
        } else if (index == 1) {
            dropdownSelector = "//div[@id='assures1']//app-form-dropdownlist[@formcontrolname=\"statutProfessionnel\"]//div//ng-select";
        } else {
            return; // Gérer les autres cas si nécessaire
        }

        // Cliquer sur le dropdown pour l'ouvrir
        elementLib.clickByXpath(dropdownSelector);

        // Attendre que les options se chargent
        elementLib.waitThread(1);

        // Récupérer toutes les options disponibles
        List<Locator> options = page.locator(".ng-option").all();

        // Parcourir les options et sélectionner la bonne
        for (Locator option : options) {
            String optionText = option.textContent();
            String professionSpecifique = flux.getPersonnes().get(index).getProfessionSpecifique();

            if (optionText.contains("Salarié Cadre/ Assimilé-cadre / Ingénieur") &&
                    professionSpecifique.equalsIgnoreCase("Salarié cadre")) {
                option.scrollIntoViewIfNeeded();
                option.click();
                elementLib.scrollDown(-450);
                break;
            }

            if (optionText.contains("Professions agricoles") &&
                    professionSpecifique.equalsIgnoreCase("Agriculteur")) {
                option.scrollIntoViewIfNeeded();
                option.click();
                elementLib.scrollDown(-450);
                break;
            }

            if (optionText.contains("Commerçant") &&
                    professionSpecifique.equalsIgnoreCase("Commerçant")) {
                option.scrollIntoViewIfNeeded();
                option.click();
                elementLib.scrollDown(-450);
                break;
            }

            if (optionText.contains("Salarié non cadre (hors employé de bureau & personnel navigant") &&
                    professionSpecifique.equalsIgnoreCase("Salarié non cadre : employé")) {
                option.scrollIntoViewIfNeeded();
                option.click();
                elementLib.scrollDown(-450);
                break;
            }

            if (optionText.contains("Artisan") &&
                    professionSpecifique.equalsIgnoreCase("Artisan")) {
                option.scrollIntoViewIfNeeded();
                option.click();
                elementLib.scrollDown(-450);
                break;
            }

            if (optionText.contains("Profession libérale médicale ou paramédicale / Médecin (ou Interne) généraliste/ spécialiste") &&
                    professionSpecifique.equalsIgnoreCase("Profession libérale médicale")) {
                option.scrollIntoViewIfNeeded();
                option.click();
                break;
            }

            if (optionText.contains("Profession libérale (hors médical/ paramédical)") &&
                    professionSpecifique.equalsIgnoreCase("Profession libérale")) {
                option.scrollIntoViewIfNeeded();
                option.click();
                break;
            }

            if (optionText.contains("Dirigeant de société/ Gérant/ Chef d'entreprise (de moins de 10 salariés) artisan/commerçant") &&
                    professionSpecifique.equalsIgnoreCase("Chef d\\'entreprise")) {
                option.scrollIntoViewIfNeeded();
                option.click();
                break;
            }

            if (optionText.contains("Fonctionnaire") &&
                    professionSpecifique.equalsIgnoreCase("Fonctionnaire classe a")) {
                option.scrollIntoViewIfNeeded();
                option.click();
                break;
            }
        }
    }

    private void choixProfessionMedicaleParamedicale(FluxData flux, int index) {
        // Sélection du dropdown selon l'index
        String dropdownSelector;
        if (index == 0) {
            dropdownSelector = "//div[@id='assures0']//app-form-dropdownlist[@formcontrolname=\"professionMedicale\"]//div//ng-select";
        } else if (index == 1) {
            dropdownSelector = "//div[@id='assures1']//app-form-dropdownlist[@formcontrolname=\"professionMedicale\"]//div//ng-select";
        } else {
            return; // Gérer les autres cas si nécessaire
        }

        // Attendre et cliquer sur le dropdown pour l'ouvrir (équivalent waitForElement avec timeout 25, 5)
        try {
            elementLib.waitForElementVisible(dropdownSelector, 25);
            elementLib.clickByXpath(dropdownSelector);
        } catch (Exception e) {
            log.error("Impossible d'ouvrir le dropdown profession médicale pour l'index {}", index);
            return;
        }

        // Attendre que les options se chargent
        elementLib.waitThread(1);

        // Récupérer toutes les options disponibles
        List<Locator> options = page.locator(".ng-option").all();

        // Parcourir les options et sélectionner la bonne
        for (Locator option : options) {
            String optionText = option.textContent();
            String profession = flux.getPersonnes().get(index).getProfession();

            // Condition 1: Chirurgiens
            if ((optionText.contains("Chirurgiens") || optionText.contains("chirurgiens-dentistes")) &&
                    (profession.contains("Chirurgiens") || profession.contains("chirurgiens-dentistes"))) {
                option.scrollIntoViewIfNeeded();
                option.click();
                elementLib.scrollDown(-300);
                break;
            }

            // Condition 2: Médecins et professions associées
            else if ((optionText.contains("Médecins") || optionText.contains("médecins spécialistes") ||
                    optionText.contains("interne") || optionText.contains("vétérinaires") ||
                    optionText.contains("pharmacien")) &&
                    (profession.contains("Médecins") || profession.contains("médecins spécialistes") ||
                            profession.contains("interne") || profession.contains("vétérinaires") ||
                            profession.contains("pharmacien"))) {
                option.scrollIntoViewIfNeeded();
                option.click();
                elementLib.scrollDown(-300);
                break;
            }

            // Condition 3: Ostéopathes
            else if (optionText.contains("Ostéopathes") && profession.contains("Ostéopathes")) {
                option.scrollIntoViewIfNeeded();
                option.click();
                elementLib.scrollDown(-300);
                break;
            }

            // Condition 4: Sages-femmes et kinés
            else if (optionText.contains("sages-femmes") &&
                    (profession.contains("sages-femmes") || profession.contains("kiné"))) {
                option.scrollIntoViewIfNeeded();
                option.click();
                elementLib.scrollDown(-300);
                break;
            }

            // Condition 5: Autres
            else if (optionText.contains("autres") && profession.contains("autres")) {
                option.scrollIntoViewIfNeeded();
                option.click();
                elementLib.scrollDown(-300);
                break;
            }

            // Condition par défaut (fallback) - équivalent du else final
            else {
                option.scrollIntoViewIfNeeded();
                option.click();
                elementLib.scrollDown(-450);
                break;
            }
        }
    }

    private void choixObjetPret(FluxData flux, int index) {
        // Sélecteur du dropdown
        String dropdownSelector = "//app-form-dropdownlist[@formcontrolname=\"typeProjetId\"]//div//ng-select";

        // Attendre et cliquer sur le dropdown pour l'ouvrir
        try {
            elementLib.waitForElementVisible(dropdownSelector, 15);
            elementLib.clickByXpath(dropdownSelector);
        } catch (Exception e) {
            log.error("Impossible d'ouvrir le dropdown objet prêt pour l'index {}", index);
            return;
        }

        // Attendre que les options se chargent
        elementLib.waitThread(1);

        // Récupérer l'objet du prêt
        String objetPret = flux.getPrets().get(index).getObjet();
        String optionSelector = null;

        // Déterminer le sélecteur de l'option selon l'objet du prêt
        if (objetPret.equalsIgnoreCase("Résidence principale")) {
            optionSelector = "//span[@class=\"ng-select-single-dropdown-option\" and contains(text(), \"Résidence principale\")]";
        }
        else if (objetPret.equalsIgnoreCase("Résidence secondaire")) {
            optionSelector = "//span[@class=\"ng-select-single-dropdown-option\" and contains(text(), \"Acquisition résidence secondaire\")]";
        }
        else if (objetPret.equalsIgnoreCase("Investissement professionnel")) {
            optionSelector = "//span[@class=\"ng-select-single-dropdown-option\" and contains(text(), \"Investissements locatifs à usage d'habitation ou mixte (hors SCPI)\")]";
        }
        else if (objetPret.equalsIgnoreCase("Prêt à objet professionnel")) {
            optionSelector = "//span[@class=\"ng-select-single-dropdown-option\" and contains(text(), \"Prêt professionnel\")]";
        }

        // Sélectionner l'option si elle a été trouvée
        if (optionSelector != null) {
            try {
                elementLib.waitForElementVisible(optionSelector, 15);
                Locator option = page.locator("xpath=" + optionSelector);
                option.hover();
                option.press("Enter");
            } catch (Exception e) {
                log.error("Impossible de sélectionner l'option pour l'objet: {}", objetPret);
            }
        } else {
            log.warn("Aucune option trouvée pour l'objet prêt: {}", objetPret);
        }
    }

    private void choixTypePret(FluxData flux, int index) {
        // Sélection du dropdown selon l'index
        String dropdownSelector;
        if (index == 0) {
            dropdownSelector = "//app-form-dropdownlist[@formcontrolname=\"typePretId\"]//div//ng-select";
        } else if (index == 1) {
            dropdownSelector = "//div[@id=\"pret1\"]//app-form-dropdownlist[@formcontrolname=\"typePretId\"]//div//ng-select";
        } else {
            return; // Gérer les autres cas si nécessaire
        }

        // Attendre et cliquer sur le dropdown pour l'ouvrir (équivalent waitForElement avec timeout 15, 2)
        try {
            elementLib.waitForElementVisible(dropdownSelector, 15);
            elementLib.clickByXpath(dropdownSelector);
        } catch (Exception e) {
            log.error("Impossible d'ouvrir le dropdown type prêt pour l'index {}", index);
            return;
        }

        // Attendre que les options se chargent
        elementLib.waitThread(1);

        // Récupérer le type du prêt
        String typePret = flux.getPrets().get(index).getType();
        String optionSelector = null;

        // Déterminer le sélecteur de l'option selon le type du prêt
        if (typePret.equalsIgnoreCase("Amortissable")) {
            optionSelector = "//span[@class=\"ng-select-single-dropdown-option\" and contains(text(), \"Amortissable\")]";
        }
        else if (typePret.equalsIgnoreCase("Prêt relais")) {
            optionSelector = "//span[@class=\"ng-select-single-dropdown-option\" and contains(text(), \"Prêt relais\")]";
        }
        else if (typePret.equalsIgnoreCase("Prêt à paliers")) {
            optionSelector = "//span[@class=\"ng-select-single-dropdown-option\" and contains(text(), \"Prêt à palier\")]";
        }
        else if (typePret.equalsIgnoreCase("Crédit bail")) {
            optionSelector = "//span[@class=\"ng-select-single-dropdown-option\" and contains(text(), \"Crédit-bail\")]";
        }

        // Sélectionner l'option si elle a été trouvée
        if (optionSelector != null) {
            try {
                elementLib.waitForElementVisible(optionSelector, 15);
                Locator option = page.locator("xpath=" + optionSelector);
                option.hover();
                option.press("Enter");
            } catch (Exception e) {
                log.error("Impossible de sélectionner l'option pour le type: {}", typePret);
            }
        } else {
            log.warn("Aucune option trouvée pour le type prêt: {}", typePret);
        }
    }

    private void choixBanque(FluxData flux, int index) {
        // Sélecteur du dropdown
        String dropdownSelector = "//app-form-dropdownlist[@formcontrolname=\"banqueId\"]//div//ng-select";

        // Attendre et cliquer sur le dropdown pour l'ouvrir
        try {
            elementLib.waitForElementVisible(dropdownSelector, 15);
            elementLib.clickByXpath(dropdownSelector);
        } catch (Exception e) {
            log.error("Impossible d'ouvrir le dropdown banque pour l'index {}", index);
            return;
        }

        // Attendre que les options se chargent
        elementLib.waitThread(1);

        // Récupérer toutes les options disponibles
        List<Locator> options = page.locator(".ng-option").all();

        // Récupérer le nom de la banque recherchée
        String banqueRecherchee = flux.getPrets().get(index).getBanque();
        boolean banqueTrouvee = false;

        // Parcourir les options et sélectionner la bonne
        for (Locator option : options) {
            String optionText = option.textContent();

            if (banqueRecherchee.equalsIgnoreCase(optionText)) {
                try {
                    option.scrollIntoViewIfNeeded();
                    option.click();
                    banqueTrouvee = true;
                    log.info("Banque sélectionnée: {}", banqueRecherchee);
                    break;
                } catch (Exception e) {
                    log.error("Erreur lors de la sélection de la banque: {}", banqueRecherchee, e);
                }
            }
        }

        // Log si la banque n'a pas été trouvée
        if (!banqueTrouvee) {
            log.warn("Banque '{}' non trouvée dans les options disponibles pour l'index {}", banqueRecherchee, index);
        }
    }

    private void choixGaranties(FluxData flux) {
        // Attendre 1 seconde
        elementLib.waitThread(1);

        // Traitement pour le premier onglet (tab-0)
        if (flux.getInfoAssureComplets().get(0).getGarantie().equalsIgnoreCase("IPT, ITT")) {
            traiterGarantiesOnglet("tab-0-panel", 20, 10);
        }

        // Traitement pour le deuxième onglet si il y a 2 prêts
        if (flux.getPrets().size() == 2) {
            elementLib.waitThread(1);

            // Cliquer sur l'onglet du deuxième prêt
            String tabSelector = "//a[@id=\"tab-1\"]";

            try {
                elementLib.waitForElementVisible(tabSelector, 10);
                elementLib.clickByXpath(tabSelector);

                // Traitement des garanties pour le deuxième onglet
                if (flux.getInfoAssureComplets().get(0).getGarantie().equalsIgnoreCase("IPT, ITT")) {
                    traiterGarantiesOnglet("tab-1-panel", 25, 10);
                }

            } catch (Exception e) {
                log.error("Erreur lors du traitement des garanties pour le deuxième onglet", e);
            }
        }
    }

    /**
     * Méthode utilitaire pour traiter les garanties d'un onglet spécifique
     */
    private void traiterGarantiesOnglet(String tabId, int timeoutInput, int timeoutSwitch) {
        try {
            // Sélecteur pour l'input IPP
            String inputIPPSelector = "//*[@id='" + tabId + "']/div/div/div/div/div[2]/div/table/tbody/tr[3]/td[5]/input";

            // Attendre et remplir l'input IPP
            elementLib.waitForElementVisible(inputIPPSelector, timeoutInput);
            elementLib.typeByXpath(inputIPPSelector, "0");
            elementLib.waitThread(1);

            // Sélecteur pour le switch button
            String switchButtonSelector = "//*[@id=\"" + tabId + "\"]/div/div/div/div/div[2]/div/table/tbody/tr[11]/td[5]/div/div/switch-button";

            // Attendre et cliquer sur le switch button
            elementLib.waitForElementVisible(switchButtonSelector, timeoutSwitch);
            elementLib.clickByXpath(switchButtonSelector);

            log.info("Garanties traitées avec succès pour l'onglet: {}", tabId);

        } catch (Exception e) {
            log.error("Erreur lors du traitement des garanties pour l'onglet: {}", tabId, e);
        }
    }

}
