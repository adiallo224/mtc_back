package com.mtc.mutuaConseil.services.servicesImpl.assuPret;

import com.mtc.mutuaConseil.base.*;
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
public class AfiescaService extends BaseAutomationService implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(AfiescaService.class);
    private final TypeAssuranceService typeAssuranceService;
    private final String source = "AfiescaPret";

    public AfiescaService(TypeAssuranceService typeAssuranceService) {
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        log.info("Début de traitement -- AfiescaService (unifié)");
        Tarif tarif = TarifUtils.createDefaultTarif(c, typeAssuranceService, 1L);

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

            // Connexion — champs username/password puis clic connexion
            element.typeByXpath("//input[@name='username']", c.getUsername());
            WaitUtils.humanDelay();
            element.typeByXpath("//input[@name='password']", c.getPassword());
            WaitUtils.humanDelay();
            element.clickByXpath("//button//span[text()='Connexion']");
            WaitUtils.sleepMs(1000);

            try {
                if (element.isVisibleXpath("//button[@id='close-webchat-button']")) {
                    element.clickByXpath("//button[@id='close-webchat-button']");
                    WaitUtils.humanDelay();
                } else if (element.isVisibleXpath("//iframe[@id='clustaar_webchat_launcher']")) {
                    element.clickByXpath("//iframe[@id='clustaar_webchat_launcher']");
                    WaitUtils.humanDelay();
                }
            } catch (Exception e) {
                log.debug("Fermeture iframe non indispensable : {}", e.getMessage());
            }

            WaitUtils.sleepMs(1200);

            // Cliquer sur Souscription puis sélectionner l'offre PERENIM_PREMIUM
            element.clickByXpath("//a[@href='#/souscription']//span[@translate='SIM_SOUSCRIPTION_MENU_TITRE']");
            WaitUtils.humanDelay();
            element.clickByXpath("//*[@id='PERENIM_PREMIUM']");
            WaitUtils.humanDelay();

            // Remplir les informations (personne + prêt)
            remplirInformationsPersonne(flux);
            remplirInformationsPret(flux);

            // Attendre et accéder à la tarification
            WaitUtils.sleepMs(4000);
            element.clickByXpath("//li[@id='TARIFICATION']//a");
            WaitUtils.sleepMs(3000);

            // Récupérer le résultat
            String montant = null;
            try {
                if (element.isVisibleXpath("//table//tr//td[@id='resultatTotal']")) {
                    montant = element.getTextByXpath("//table//tr//td[@id='resultatTotal']").trim();
                } else if (element.isVisibleXpath("//td[@id='resultatTotal']")) {
                    montant = element.getTextByXpath("//td[@id='resultatTotal']").trim();
                }
            } catch (Exception e) {
                log.warn("Impossible de lire resultatTotal : {}", e.getMessage());
            }

            if (montant != null && !montant.isEmpty()) {
                log.info("Cout : {}", montant);
                tarif.setMontant(montant);
                String path = captureScreenshot(tarif.getNom(), false, tarif);
                if (path != null) tarif.setCaptureImg(path);
                tarif.setExecution(true);
            }
        } catch (Exception e) {
            log.error("Erreur AfiescaService unifié", e);
            tarif.setErreur(e.getMessage());
            String path = captureScreenshot(tarif.getNom(), true, tarif);
            tarif.setCaptureImgErreur(path);
            tarif.setEtape("");
        } finally {
            cleanup();
        }
       return tarif;
    }

    /* --------------------------
       Méthodes auxiliaires refactorisées (utilisent ElementLib)
       -------------------------- */

    private void remplirInformationsPersonne(FluxData flux) {
        if (flux == null || flux.getPersonnes() == null || flux.getPersonnes().isEmpty()) return;

        int index = 0;

        try {
            String dateNaissance = flux.getPersonnes().get(index).getDateNaissance();
            if (dateNaissance != null && !dateNaissance.isEmpty()) {
                element.typeByXpath("//div[@id='critereAssure1']//div[contains(@class,'critere-datenaissance')]//input[@name='dateNaissance']", dateNaissance);
                WaitUtils.humanDelay();
            }

            // Nationalité / Souscripteur / Opération
            element.clickByXpath("//div[@id='critereAssure1']//div[@ng-model='critere.Nationalite']");
            WaitUtils.humanDelay();
            element.clickByXpath("//div[@id='critereAssure1']//div[contains(text(), 'Française')]");
            WaitUtils.humanDelay();
            element.clickByXpath("//div[@id='critereAssure1']//div[@class='estSouscripteurAssure__reponse']//span//span[@translate='GEN_OUI']");
            WaitUtils.humanDelay();
            element.clickByXpath("//div[@id='critereAssure1']//span[@translate='OPERATION_IMMO']");
            WaitUtils.humanDelay();

            // Fumeur
            boolean fumeur = false;
            try { fumeur = flux.getInfoAssureComplets().get(index).getFumeur(); } catch (Exception ignored) {}
            if (fumeur) element.clickByXpath("//div[@id='critereAssure1']//div[@class='estfumeur__reponse']//div//span//span[@translate='GEN_OUI']");
            else element.clickByXpath("//div[@id='critereAssure1']//div[@class='estfumeur__reponse']//div//span//span[@translate='GEN_NON']");
            WaitUtils.humanDelay();

            // Profession (choix générique vers le premier item si le mapping n'est précis)
            String profession = "";
            try { profession = flux.getPersonnes().get(index).getProfessionSpecifique(); } catch (Exception ignored) {}
            // Pour simplifier on clique le premier li (conforme au code d'origine qui choisit souvent li[1])
            element.clickByXpath("//div[@id='critereAssure1']//div[@class='col-xs-12 padding-sm']//ul[1]/li[1]");
            WaitUtils.humanDelay();

            // Scrolling si nécessaire
            element.scrollIntoView("//div[@id='critereAssure1']");
            WaitUtils.humanDelay();

            // Travail manuel / hauteur / autres risques -> radio Oui/Non
            boolean travailManuel = false;
            boolean travailHauteur = false;
            boolean travailManuelLourde = false;
            boolean produitDanger = false;
            boolean metierExpose = false;
            try {
                travailManuel = flux.getInfoAssureComplets().get(index).getTravailManuel();
                travailHauteur = flux.getInfoAssureComplets().get(index).getTravailHauteur();
                travailManuelLourde = flux.getInfoAssureComplets().get(index).getTravailManuelManuLourde();
                produitDanger = flux.getInfoAssureComplets().get(index).getProduitDanger();
                metierExpose = flux.getInfoAssureComplets().get(index).getMetierExpose();
            } catch (Exception ignored) {}

            if (!travailManuel || !travailHauteur || !travailManuelLourde || !produitDanger || !metierExpose) {
                scrollDown(0, 200);
                element.clickByXpath("//div[@id='critereAssure1']//div[@class='questionpro__reponse']//span[@translate='GEN_NON']");
            } else {
                element.clickByXpath("//div[@id='critereAssure1']//div[@class='questionpro__reponse']//span[@translate='GEN_OUI']");
            }
            WaitUtils.humanDelay();

            // Activité suivante (bootstrap combo)
            element.clickByXpath("//div[@id='critereAssure1']//div[@theme='bootstrap']");
            WaitUtils.humanDelay();
            element.clickByXpath("//div[@id='critereAssure1']//span//p[contains(text(), 'Aucune activité spécifique')]");
            WaitUtils.humanDelay();

            // Deuxième personne (si présente)
            if (flux.getPersonnes().size() == 2) {
                index = 1;
                WaitUtils.sleepMs(1000);
                // Cliquer pour ajouter nouvel assuré
                element.clickByXpath("//div[@id='critereAssure2']//a[@id='critereAjouteAssure']//span[@translate='SIM_NOUVEL_ASSURE']");
                WaitUtils.humanDelay();

                String dateNaissance2 = flux.getPersonnes().get(index).getDateNaissance();
                if (dateNaissance2 != null && !dateNaissance2.isEmpty()) {
                    element.typeByXpath("//div[@id='critereAssure2']//div[contains(@class,'critere-datenaissance')]//input[@name='dateNaissance']", dateNaissance2);
                    WaitUtils.humanDelay();
                }

                element.clickByXpath("//div[@id='critereAssure2']//div[@ng-model='critere.Nationalite']");
                WaitUtils.humanDelay();
                element.clickByXpath("//div[@id='critereAssure2']//div[contains(text(), 'Française')]");
                WaitUtils.humanDelay();
                element.clickByXpath("//div[@id='critereAssure2']//div[@class='estSouscripteurAssure__reponse']//span[@translate='GEN_OUI']");
                WaitUtils.humanDelay();
                element.clickByXpath("//div[@id='critereAssure2']//span[@translate='OPERATION_IMMO']");
                WaitUtils.humanDelay();

                boolean fumeur2 = false;
                try { fumeur2 = flux.getInfoAssureComplets().get(index).getFumeur(); } catch (Exception ignored) {}
                if (fumeur2) element.clickByXpath("//div[@id='critereAssure2']//div[@class='estfumeur__reponse']//div//span//span[@translate='GEN_OUI']");
                else element.clickByXpath("//div[@id='critereAssure2']//div[@class='estfumeur__reponse']//div//span//span[@translate='GEN_NON']");
                WaitUtils.humanDelay();

                // Choix profession (on renvoie au même comportement simplifié)
                element.clickByXpath("//div[@id='critereAssure2']//div[@class='col-xs-12 padding-sm']//ul[1]/li[1]");
                WaitUtils.humanDelay();

                element.scrollIntoView("//div[@id='critereAssure2']");
                WaitUtils.humanDelay();

                boolean travailManuel2 = false;
                try {
                    travailManuel2 = flux.getInfoAssureComplets().get(index).getTravailManuel();
                    travailHauteur = flux.getInfoAssureComplets().get(index).getTravailHauteur();
                    travailManuelLourde = flux.getInfoAssureComplets().get(index).getTravailManuelManuLourde();
                    produitDanger = flux.getInfoAssureComplets().get(index).getProduitDanger();
                    metierExpose = flux.getInfoAssureComplets().get(index).getMetierExpose();
                } catch (Exception ignored) {}

                if (!travailManuel2 || !travailHauteur || !travailManuelLourde || !produitDanger || !metierExpose) {
                    scrollDown(0, 200);
                    element.clickByXpath("//div[@id='critereAssure2']//div[@class='questionpro__reponse']//span[@translate='GEN_NON']");
                } else {
                    element.clickByXpath("//div[@id='critereAssure2']//div[@class='questionpro__reponse']//span[@translate='GEN_OUI']");
                }
                WaitUtils.humanDelay();

                element.clickByXpath("//div[@id='critereAssure2']//div[@theme='bootstrap']");
                WaitUtils.humanDelay();
                element.clickByXpath("//div[@id='critereAssure2']//span//p[contains(text(), 'Aucune activité spécifique')]");
                WaitUtils.humanDelay();
            }

        } catch (Exception e) {
            log.warn("Erreur remplissage informations personne : {}", e.getMessage());
        }
    }

    private void remplirInformationsPret(FluxData flux) {
        if (flux == null || flux.getPrets() == null || flux.getPrets().isEmpty()) return;

        int index = 0;
        try {
            element.clickByXpath("//li[@id='PRETS']//a");
            WaitUtils.humanDelay();

            element.clickByXpath("//button[@id='buttonBeneficiaireEffectif']//span[@translate='SIM_BTN_ORG_BANCAIRE']");
            WaitUtils.humanDelay();

            element.clickByXpath("//ng-select[@formcontrolname='Groupe']");
            WaitUtils.humanDelay();

            // Choix banque (implémentation extraite)
            choixBanque(flux, index);
            WaitUtils.humanDelay();

            element.clickByXpath("//button[@type='submit']");
            WaitUtils.sleepMs(1000);

            element.clickByXpath("//div//span[@translate='SIM_NATURE_NOUVEAU']");
            WaitUtils.humanDelay();

            // Type de prêt
            typePret(flux, index);
            WaitUtils.humanDelay();

            // Montant / Taux / Durée
            element.typeByXpath("//input[contains(@ng-model, 'pret.Montant')]", flux.getPrets().get(index).getMontantPret());
            WaitUtils.humanDelay();
            element.typeByXpath("//input[contains(@ng-model, 'pret.Taux')]", flux.getPrets().get(index).getTaux());
            WaitUtils.humanDelay();
            element.typeByXpath("//input[contains(@ng-model, 'pret.Duree')]", flux.getPrets().get(index).getDuree());
            WaitUtils.humanDelay();
            scrollDown(0, 300);

            // Scroll (approx)
            element.scrollIntoView("//td//span[@translate='PRET_TITRE_AMORTISSABLE']");
            WaitUtils.humanDelay();

            objetPret(flux, index);
            garantiesPret1(flux, index);
            WaitUtils.humanDelay();

            // Si deuxième prêt
            if (flux.getPrets().size() == 2) {
                index = 1;
                WaitUtils.sleepMs(800);
                scrollDown(0, -300);
                element.clickByXpath("//li[contains(@class, 'pret-item pret-item-new')]//span[@translate='GEN_NOUVEAU']");
                WaitUtils.humanDelay();
                typePret(flux, index);
                WaitUtils.humanDelay();

                element.typeByXpath("//input[contains(@ng-model, 'pret.Montant')]", flux.getPrets().get(index).getMontantPret());
                WaitUtils.humanDelay();
                element.typeByXpath("//input[contains(@ng-model, 'pret.Taux')]", flux.getPrets().get(index).getTaux());
                WaitUtils.humanDelay();
                element.typeByXpath("//input[contains(@ng-model, 'pret.Duree')]", flux.getPrets().get(index).getDuree());
                WaitUtils.humanDelay();

                objetPret(flux, index);
                garantiesPret2(flux, index);
                scrollDown(0, 300);
            }

            // Si 2 personnes et 1 prêt, appliquer garantiesPret2 si nécessaire
            if (flux.getPersonnes().size() == 2) {
                garantiesPret2(flux, 1);
            }

        } catch (Exception e) {
            log.warn("Erreur remplissage informations pret : {}", e.getMessage());
        }
    }

    private void garantiesPret1(FluxData flux, int index) {
        try {
            WaitUtils.sleepMs(500);
            String garantie = "";
            try { garantie = flux.getInfoAssureComplets().get(index).getGarantie(); } catch (Exception ignored) {}
            if (garantie == null) garantie = "";

            if (garantie.equalsIgnoreCase("IPT") || garantie.contains("IPT")) {
                element.clickByXpath("//div[@ng-form='formSpecificite0']//label[contains(@title, 'Invalidité Permanente Totale')]//span[@translate='GARANTIE_IPT']");
                WaitUtils.sleepMs(300);
            }
            if (garantie.equalsIgnoreCase("IPT, ITT") || garantie.contains("ITT")) {
                element.clickByXpath("//div[@ng-form='formSpecificite0']//label[contains(@title, 'Incapacité Temporaire et Totale')]//span[@translate='GARANTIE_ITT']");
                WaitUtils.sleepMs(300);
            }
            if (garantie.equalsIgnoreCase("IPT, ITT, IPP") || garantie.contains("IPP")) {
                element.clickByXpath("//*[@id='caracteristiques-Pret']/div[2]/fieldset/saisie-specificites/div/aegaranties/div/div/div[5]/label");
                WaitUtils.sleepMs(300);
            }
        } catch (Exception e) {
            log.debug("garantiesPret1 warning: {}", e.getMessage());
        }
    }

    private void garantiesPret2(FluxData flux, int index) {
        try {
            // Cas plusieurs personnes
            if (flux.getPersonnes().size() == 2) {
                element.clickByXpath("//div[@ng-form='formSpecificite1']//label[contains(@title, 'Invalidité Permanente Totale')]//span[@translate='GARANTIE_IPT']");
                WaitUtils.sleepMs(300);
                element.clickByXpath("//div[@ng-form='formSpecificite1']//label[contains(@title, 'Incapacité Temporaire et Totale')]//span[@translate='GARANTIE_ITT']");
                WaitUtils.sleepMs(300);
            }

            // Cas 2 prêts / 1 personne -> mapping selon string
            if (flux.getPrets().size() == 2 && flux.getPersonnes().size() == 1) {
                String garantie = "";
                try { garantie = flux.getInfoAssureComplets().get(0).getGarantie(); } catch (Exception ignored) {}
                if (garantie == null) garantie = "";

                if (garantie.contains("IPT")) {
                    element.clickByXpath("//*[@id='caracteristiques-Pret']/div[2]/fieldset/saisie-specificites/div/aegaranties/div/div/div[3]/label");
                    WaitUtils.sleepMs(300);
                }
                if (garantie.contains("ITT")) {
                    element.clickByXpath("//*[@id='caracteristiques-Pret']/div[2]/fieldset/saisie-specificites/div/aegaranties/div/div/div[4]/label");
                    WaitUtils.sleepMs(300);
                }
                if (garantie.contains("IPP")) {
                    element.clickByXpath("//*[@id='caracteristiques-Pret']/div[2]/fieldset/saisie-specificites/div/aegaranties/div/div/div[5]/label");
                    WaitUtils.sleepMs(300);
                }
            }
        } catch (Exception e) {
            log.debug("garantiesPret2 warning: {}", e.getMessage());
        }
    }

    private void typePret(FluxData flux, int index) {
        try {
            String type = flux.getPrets().get(index).getType();
            if (type == null) type = "";

            if (type.equalsIgnoreCase("Amortissable") || type.equalsIgnoreCase("Crédit bail") || type.equalsIgnoreCase("Prêt à paliers")) {
                element.clickByXpath("//td//span[@translate='PRET_TITRE_AMORTISSABLE']");
            } else if (type.equalsIgnoreCase("Prêt relais")) {
                element.clickByXpath("//td//span[@translate='PRET_TITRE_PR']");
            } else if (type.equalsIgnoreCase("Prêt in fine")) {
                element.clickByXpath("//td//span[@translate='PRET_TITRE_INFINE']");
            } else {
                // fallback: click amortissable
                element.clickByXpath("//td//span[@translate='PRET_TITRE_AMORTISSABLE']");
            }
            WaitUtils.humanDelay();
        } catch (Exception e) {
            log.debug("typePret warning: {}", e.getMessage());
        }
    }

    private void objetPret(FluxData flux, int index) {
        try {
            String objet = flux.getPrets().get(index).getObjet();
            if (objet == null) objet = "";

            if (objet.equalsIgnoreCase("Résidence principale")) {
                element.clickByXpath("//td//span[@translate='PRET_OBJET_ACH_RES_PRIN']");
            } else if (objet.equalsIgnoreCase("Résidence secondaire")) {
                element.clickByXpath("//td//span[@translate='PRET_OBJET_ACH_RES_SEC']");
            } else if (objet.equalsIgnoreCase("Travaux")) {
                element.clickByXpath("//td//span[@translate='PRET_OBJET_TRAVAUX']");
            } else if (objet.equalsIgnoreCase("Investissement locatif")) {
                element.clickByXpath("//td//span[@translate='PRET_OBJET_INVEST_LOCAT']");
            } else if (objet.equalsIgnoreCase("Crédit à la consommation")) {
                element.clickByXpath("//td//span[@translate='PRET_OBJET_PRET_CONSO']");
            } else if (objet.equalsIgnoreCase("Prêt à objet professionnel")) {
                element.clickByXpath("//td//span[@translate='PRET_OBJET_PRET_PRO']");
            } else if (objet.equalsIgnoreCase("Autre immobilier")) {
                element.clickByXpath("//td//span[@translate='PRET_OBJET_AUTRES_ACHATS_IMMO']");
            } else {
                // fallback: click first option
                element.clickByXpath("//td//span[@translate='PRET_OBJET_ACH_RES_PRIN']");
            }
            WaitUtils.humanDelay();
        } catch (Exception e) {
            log.debug("objetPret warning: {}", e.getMessage());
        }
    }

    private void choixBanque(FluxData flux, int index) {
        try {
            String banque = "";
            try { banque = flux.getPrets().get(index).getBanque(); } catch (Exception ignored) {}
            if (banque == null) banque = "";

            String b = banque.toLowerCase();
            if (b.contains("bnp")) {
                element.clickByXpath("//div//span[contains(text(), 'BNP PARIBAS')]");
            } else if (b.contains("axa")) {
                element.clickByXpath("//div//span[contains(text(), 'AXA')]");
            } else if (b.contains("banque populaire") || b.contains("populaire")) {
                element.clickByXpath("//div//span[contains(text(), 'BANQUE POPULAIRE')]");
            } else if (b.contains("bours") || b.contains("boursorama")) {
                element.clickByXpath("//div//span[contains(text(), 'BOURSORAMA')]");
            } else if (b.contains("caisse") || b.contains("epargne")) {
                element.clickByXpath("//div//span[contains(text(), 'CAISSE EPARGNE')]");
            } else if (b.contains("cic")) {
                element.clickByXpath("//div//span[contains(text(), 'CIC')]");
            } else if (b.contains("credit agricole") || b.contains("agricole")) {
                element.clickByXpath("//div//span[contains(text(), 'CREDIT AGRICOLE')]");
            } else if (b.contains("credit mutuel") || b.contains("mutuel")) {
                element.clickByXpath("//div//span[contains(text(), 'CREDIT MUTUEL')]");
            } else if (b.contains("lcl")) {
                element.clickByXpath("//div//span[contains(text(), 'LCL')]");
            } else if (b.contains("societe generale") || b.contains("société générale") || b.contains("société")) {
                element.clickByXpath("//div//span[contains(text(), 'SOCIETE GENERALE')]");
            } else {
                element.clickByXpath("//ng-select[@formcontrolname='Groupe']//div[contains(@class,'ng-option')][1]");
            }
            WaitUtils.humanDelay();
        } catch (Exception e) {
            log.debug("choixBanque warning: {}", e.getMessage());
        }
    }

}
