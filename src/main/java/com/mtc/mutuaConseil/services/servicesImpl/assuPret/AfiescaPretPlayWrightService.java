package com.mtc.mutuaConseil.services.servicesImpl.assuPret;

import com.microsoft.playwright.FrameLocator;
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
public class AfiescaPretPlayWrightService extends BasePlaywrightService implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(AfiescaPretPlayWrightService.class);
    private final TypeAssuranceService typeAssuranceService;
    private final String source = "AfiescaPret";

    public AfiescaPretPlayWrightService(TypeAssuranceService typeAssuranceService) {
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        log.info("Debut de traitement -- AfiescaService");
        Tarif tarifAfiesca = TarifUtils.createDefaultTarif(c, typeAssuranceService, 1L);

        try{
            // Initialisation simplifiée - elementLib est initialisé automatiquement
            initializeBrowser(false);
            // Navigation
            humanLikeNavigate(c.getUrlFournisseur());
            // Connexion
            elementLib.humanTypeByXpath("//input[@name=\"username\"]", c.getUsername());
            elementLib.humanTypeByXpath("//input[@name=\"password\"]", c.getPassword());
            elementLib.click("//button//span[text()=\"Connexion\"]");

            // Gestion de l'iframe et du chat
            FrameLocator iframe = page.frameLocator("iframe#clustaar_webchat_launcher");
            iframe.locator("button#close-webchat-button").click();

            elementLib.click("//a[@href=\"#/souscription\"]//span[@translate=\"SIM_SOUSCRIPTION_MENU_TITRE\"]");

            elementLib.click("//*[@id=\"PERENIM_PREMIUM\"]");

            remplirInformationsPersonne(flux, source);
            remplirInformationsPret(flux, source);

            waitThread(5);
            elementLib.click("//li[@id=\"TARIFICATION\"]//a");

            String cout = elementLib.getElementText("//table//tr//td[@id=\"resultatTotal\"]");

            log.info("Cout : {}", cout);
            tarifAfiesca.setMontant(cout);

            String screenshotPath = captureScreenshot(tarifAfiesca.getNom(), false, tarifAfiesca);
            if (screenshotPath != null) {
                tarifAfiesca.setCaptureImg(screenshotPath);
            }
            tarifAfiesca.setExecution(true);
        } catch (Exception e) {
            log.error("An error occurred", e);
            tarifAfiesca.setErreur(e.getMessage());
            String screenshotBytesErreur = captureScreenshot(tarifAfiesca.getNom(), true, tarifAfiesca);
            tarifAfiesca.setCaptureImgErreur(screenshotBytesErreur);
            tarifAfiesca.setEtape("");
        } finally {
            cleanup();
        }
        return tarifAfiesca;
    }

    private void remplirInformationsPersonne(FluxData flux, String source) {
        int index = 0;

        elementLib.humanTypeByXpath("//div[@id=\"critereAssure1\"]//div[contains(@class,'critere-datenaissance')]//input[@name=\"dateNaissance\"]",
        flux.getPersonnes().get(index).getDateNaissance());
        elementLib.click("//div[@id=\"critereAssure1\"]//div[@ng-model=\"critere.Nationalite\" and contains(@class, \"ng-isolate-scope\")]");
        elementLib.click("//div[@id=\"critereAssure1\"]//div[contains(text(), 'Française')]");
        elementLib.click("//div[@id=\"critereAssure1\"]//div[@class=\"estSouscripteurAssure__reponse\"]//span//span[@translate=\"GEN_OUI\"]");
        elementLib.click("//div[@id=\"critereAssure1\"]//span[@translate=\"OPERATION_IMMO\"]");

        if(!flux.getInfoAssureComplets().get(index).getFumeur()) {
            elementLib.click("//div[@id=\"critereAssure1\"]//div[@class=\"estfumeur__reponse\"]//div//span//span[@translate=\"GEN_NON\"]");
        } else {
            elementLib.click("//div[@id=\"critereAssure1\"]//div[@class=\"estfumeur__reponse\"]//div//span//span[@translate=\"GEN_OUI\"]");
        }
        // Logique de profession
        elementLib.click("//div[@id=\"critereAssure1\"]//div[@class=\"col-xs-12 padding-sm\"]//ul[1]/li[1]");

        scrollDown(300);

        if (!flux.getInfoAssureComplets().get(index).getTravailManuel() || !flux.getInfoAssureComplets().get(index).getTravailHauteur() ||
            !flux.getInfoAssureComplets().get(index).getTravailManuelManuLourde() || !flux.getInfoAssureComplets().get(index).getProduitDanger() ||
            !flux.getInfoAssureComplets().get(index).getMetierExpose()) {
            elementLib.click("//div[@id=\"critereAssure1\"]//div[@class=\"questionpro__reponse\"]//span[@translate=\"GEN_NON\"]");
        } else {
            elementLib.click("//div[@id=\"critereAssure1\"]//div[@class=\"questionpro__reponse\"]//span[@translate=\"GEN_OUI\"]");
        }
        elementLib.click("//div[@id=\"critereAssure1\"]//div[@theme=\"bootstrap\"]");
        elementLib.click("//div[@id=\"critereAssure1\"]//span//p[contains(text(), 'Aucune activité spécifique')]");
        // Deuxième personne si nécessaire
        if (flux.getPersonnes().size() == 2) {
            index = 1;
            elementLib.click("//div[@id=\"critereAssure2\"]//a[@id=\"critereAjouteAssure\"]//span[@translate=\"SIM_NOUVEL_ASSURE\"]");
            elementLib.humanTypeByXpath("//div[@id=\"critereAssure2\"]//div[contains(@class,'critere-datenaissance')]//input[@name=\"dateNaissance\"]",
            flux.getPersonnes().get(index).getDateNaissance());
            elementLib.click("//div[@id=\"critereAssure2\"]//div[@ng-model=\"critere.Nationalite\"]");
            elementLib.click("//div[@id=\"critereAssure2\"]//div[contains(text(), 'Française')]");
            elementLib.click("//div[@id=\"critereAssure2\"]//div[@class=\"estSouscripteurAssure__reponse\"]//span[@translate=\"GEN_OUI\"]");
            elementLib.click("//div[@id=\"critereAssure2\"]//span[@translate=\"OPERATION_IMMO\"]");

            if(!flux.getInfoAssureComplets().get(index).getFumeur()) {
                elementLib.click("//div[@id=\"critereAssure2\"]//div[@class=\"estfumeur__reponse\"]//div//span//span[@translate=\"GEN_NON\"]");
            } else {
                elementLib.click("//div[@id=\"critereAssure2\"]//div[@class=\"estfumeur__reponse\"]//div//span//span[@translate=\"GEN_OUI\"]");
            }
            elementLib.click("//div[@id=\"critereAssure2\"]//div[@class=\"col-xs-12 padding-sm\"]//ul[1]/li[1]");

            scrollDown( 300);

            if (!flux.getInfoAssureComplets().get(index).getTravailManuel() || !flux.getInfoAssureComplets().get(index).getTravailHauteur() ||
                !flux.getInfoAssureComplets().get(index).getTravailManuelManuLourde() || !flux.getInfoAssureComplets().get(index).getProduitDanger() ||
                !flux.getInfoAssureComplets().get(index).getMetierExpose()) {
                elementLib.click("//div[@id=\"critereAssure2\"]//div[@class=\"questionpro__reponse\"]//span[@translate=\"GEN_NON\"]");
            } else {
                elementLib.click("//div[@id=\"critereAssure2\"]//div[@class=\"questionpro__reponse\"]//span[@translate=\"GEN_OUI\"]");
            }

            elementLib.click("//div[@id=\"critereAssure2\"]//div[@theme=\"bootstrap\"]");
            elementLib.click("//div[@id=\"critereAssure2\"]//span//p[contains(text(), 'Aucune activité spécifique')]");
        }
    }

    private void remplirInformationsPret(FluxData flux, String source) {
        int index = 0;
        elementLib.click( "//li[@id=\"PRETS\"]//a");
        elementLib.click("//button[@id=\"buttonBeneficiaireEffectif\"]//span[@translate=\"SIM_BTN_ORG_BANCAIRE\"]");
        elementLib.click( "//ng-select[@formcontrolname='Groupe']");
        choixBanque(flux, index);
        elementLib.click("//button[@type='submit']");
        elementLib.click("//div//span[@translate=\"SIM_NATURE_NOUVEAU\"]");
        typePret(flux, index);

        elementLib.humanTypeByXpath("//input[contains(@ng-model, 'pret.Montant')]",flux.getPrets().get(index).getMontantPret());
        elementLib.humanTypeByXpath("//input[contains(@ng-model, 'pret.Taux')]", flux.getPrets().get(index).getTaux());
        elementLib.humanTypeByXpath("//input[contains(@ng-model, 'pret.Duree')]", flux.getPrets().get(index).getDuree());
        scrollDown(450);
        objetPret(flux, index);
        garantiesPret1(flux, index);
        scrollDown(-450);

        if (flux.getPrets().size() == 2) {
            index = 1;
            elementLib.clickByXpath("//li[contains(@class, 'pret-item pret-item-new')]//span[@translate=\"GEN_NOUVEAU\"]");
            typePret(flux, index);

            elementLib.humanTypeByXpath("//input[contains(@ng-model, 'pret.Montant')]", flux.getPrets().get(index).getMontantPret());
            elementLib.humanTypeByXpath("//input[contains(@ng-model, 'pret.Taux')]", flux.getPrets().get(index).getTaux());
            elementLib.humanTypeByXpath("//input[contains(@ng-model, 'pret.Duree')]", flux.getPrets().get(index).getDuree());

            objetPret(flux, index);
            garantiesPret2(flux, index);
        }

        if (flux.getPersonnes().size() == 2){
            garantiesPret2(flux, 1);
        }
    }

    private void garantiesPret1(FluxData flux, int index) {
        if (flux.getInfoAssureComplets().get(index).getGarantie().equalsIgnoreCase("IPT")) {
            elementLib.clickByXpath("//div[@ng-form=\"formSpecificite0\"]//label[contains(@title, 'Invalidité Permanente Totale')]//span[@translate=\"GARANTIE_IPT\"]");
        }
        // ... autres garanties
    }

    private void garantiesPret2(FluxData flux, int index) {
        if (flux.getPersonnes().size() == 2) {
            elementLib.clickByXpath("//div[@ng-form=\"formSpecificite1\"]//label[contains(@title, 'Invalidité Permanente Totale')]//span[@translate=\"GARANTIE_IPT\"]");
            elementLib.clickByXpath("//div[@ng-form=\"formSpecificite1\"]//label[contains(@title, 'Incapacité Temporaire et Totale')]//span[@translate=\"GARANTIE_ITT\"]");
        }
        // ... autres cas
    }

    private void typePret(FluxData flux, int index) {
        String type = flux.getPrets().get(index).getType();
        if (type.equalsIgnoreCase("Amortissable") || type.equalsIgnoreCase("Crédit bail") || type.equalsIgnoreCase("Prêt à paliers")) {
            elementLib.clickByXpath("//td//span[@translate=\"PRET_TITRE_AMORTISSABLE\"]");
        } else if (type.equalsIgnoreCase("Prêt relais")) {
            elementLib.clickByXpath("//td//span[@translate=\"PRET_TITRE_PR\"]");
        } else if (type.equalsIgnoreCase("Prêt in fine")) {
            elementLib.clickByXpath("//td//span[@translate=\"PRET_TITRE_INFINE\"]");
        }
    }

    private void objetPret(FluxData flux, int index) {
        String objet = flux.getPrets().get(index).getObjet();
        if (objet.equalsIgnoreCase("Résidence principale")) {
            elementLib.clickByXpath("//td//span[@translate=\"PRET_OBJET_ACH_RES_PRIN\"]");
        } else if (objet.equalsIgnoreCase("Résidence secondaire")) {
            elementLib.clickByXpath("//td//span[@translate=\"PRET_OBJET_ACH_RES_SEC\"]");
        }
        // Logique similaire à typePret avec les sélecteurs appropriés
    }

    private void choixBanque(FluxData flux, int index) {
        String banque = flux.getPrets().get(index).getBanque();
        if (banque.equalsIgnoreCase("BNP Paribas")) {
            elementLib.click("//div//span[contains(text(), 'BNP PARIBAS')]");
        } else if (banque.equalsIgnoreCase("Axa banque")) {
            elementLib.click("//div//span[contains(text(), 'AXA')]");
        } else if (banque.equalsIgnoreCase("Banque populaire")) {
            elementLib.click("//div//span[contains(text(), 'BANQUE POPULAIRE')]");
        } else if (banque.equalsIgnoreCase("Caisse d'épargne")) {
            elementLib.click("//div//span[contains(text(), 'CAISSE EPARGNE')]");
        } else if (banque.equalsIgnoreCase("CIC")) {
            elementLib.click("//div//span[contains(text(), 'CIC')]");
        } else if (banque.equalsIgnoreCase("Crédit agricole")) {
            elementLib.click("//div//span[contains(text(), 'CREDIT AGRICOLE')]");
        } else if (banque.equalsIgnoreCase("Crédit mutuel")) {
            elementLib.click("//div//span[contains(text(), 'CREDIT MUTUEL')]");
        } else if (banque.equalsIgnoreCase("La Banque Postale")) {
            elementLib.click("//div//span[contains(text(), 'BANQUE POSTALE')]");
        } else if (banque.equalsIgnoreCase("LCL")) {
            elementLib.click("//div//span[contains(text(), 'LCL')]");
        } else if (banque.equalsIgnoreCase("Société Générale")) {
            elementLib.click("//div//span[contains(text(), 'SOCIETE GENERALE')]");
        } else if (banque.equalsIgnoreCase("FORTUNEO")) {
            elementLib.click("//div//span[contains(text(), 'FORTUNEO')]");
        } else if (banque.equalsIgnoreCase("BoursoBank")) {
            elementLib.click("//div//span[contains(text(), 'BOURSORAMA')]");
        } else if (banque.equalsIgnoreCase("BFORBANK")) {
            elementLib.click("//div//span[contains(text(), 'BFORBANQUE')]");
        }
    }

}
