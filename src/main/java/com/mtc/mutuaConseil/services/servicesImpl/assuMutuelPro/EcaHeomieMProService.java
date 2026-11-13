package com.mtc.mutuaConseil.services.servicesImpl.assuMutuelPro;


import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.SelectOption;
import com.mtc.mutuaConseil.base.BasePlaywrightService;
import com.mtc.mutuaConseil.models.Compte;
import com.mtc.mutuaConseil.models.FluxData;
import com.mtc.mutuaConseil.models.Tarif;
import com.mtc.mutuaConseil.models.TypeAssurance;
import com.mtc.mutuaConseil.models.enums.EnumTypeAssurance;
import com.mtc.mutuaConseil.selectors.SelectorStore;
import com.mtc.mutuaConseil.services.LaunchedService;
import com.mtc.mutuaConseil.services.servicesImpl.TypeAssuranceService;
import com.mtc.mutuaConseil.utils.TarifUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import static java.util.Objects.nonNull;

@Service
public class EcaHeomieMProService extends BasePlaywrightService implements LaunchedService {

    private static final String PROVIDER = "mutuelPro/ecaheomie-mp";

    private final Logger log = LoggerFactory.getLogger(EcaHeomieMProService.class);
    private final TypeAssuranceService typeAssuranceService;
    private final SelectorStore selectors;
    private String currentStep = "démarrage";

    public EcaHeomieMProService(TypeAssuranceService typeAssuranceService, SelectorStore selectors) {
        this.typeAssuranceService = typeAssuranceService;
        this.selectors = selectors;
    }

    private String sel(String key) {
        return selectors.get(PROVIDER, key);
    }

    private void step(String name, Runnable action) {
        currentStep = name;
        log.debug("Étape: {}", name);
        action.run();
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        log.info("Début de traitement -- Eca_Heomie_Mutuel_Pro (Playwright)");

        Tarif tarif = TarifUtils.createDefaultTarif(c, typeAssuranceService, 3L);
        tarif.setNom(c.getNomFournisseur());
        TypeAssurance typeAssurance = new TypeAssurance();
        typeAssurance.setTypeAssurance(EnumTypeAssurance.MUTUELLE_PRO);
        tarif.setTypeAssurance(typeAssurance);

        try {
            initializeBrowser(false);
            humanLikeNavigate(c.getUrlFournisseur());
            step("connexion", () -> connexion(c));
            step("navigation", this::choixComplementaire);
            step("besoins", () -> remplirBesoins(flux));
            step("situation_professionnelle", () -> remplirSituationProfessionnelle(flux));
            step("situation_personnelle", () -> remplirSituationPersonnelle(flux));
            step("calcul", this::suivant);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            elementLib.randomWait(4000, 6000);
            step("choix_formule", () -> choixFormule(2));
            scrollDown(300);
            currentStep = "lecture_resultats";
            String cout = elementLib.getElementTextByXpath(sel("resultats.cout_xpath"));
            log.info("cout {}", cout);
            tarif.setMontant(cout);
            String screenshotBytes = captureScreenshot(tarif.getNom(), false, tarif);
            if (nonNull(screenshotBytes)) {
                tarif.setCaptureImg(screenshotBytes);
            }
            tarif.setExecution(true);
        } catch (Exception e) {
            log.error("Échec à l'étape '{}'", currentStep, e);
            tarif.setErreur(e.getMessage());
            String screenshotPath = captureScreenshot(tarif.getNom(), true, tarif);
            tarif.setCaptureImgErreur(screenshotPath);
            tarif.setEtape(currentStep);
        } finally {
            cleanup();
        }
        return tarif;
    }

    public void choixFormule(int niveau) {
        if (niveau < 1 || niveau > 5) {
            throw new IllegalArgumentException("Le niveau doit être compris entre 1 et 5.");
        }
        String value = String.format(sel("formule.valeur_template"), niveau * 100);
        page.locator("#" + sel("formule.select_id")).selectOption(value);
    }

    private void connexion(Compte c) {
        elementLib.humanTypeById(sel("connexion.login_id"), c.getUsername());
        elementLib.humanTypeById(sel("connexion.password_id"), c.getPassword());
        elementLib.clickByXpath(sel("connexion.submit_xpath"));
        elementLib.randomWait(700, 1300);
    }

    private void choixComplementaire() {
        elementLib.clickByXpath(sel("navigation.nouveau_devis_entreprise_xpath"));
        elementLib.randomWait(700, 1300);
        elementLib.clickByXpath(sel("navigation.presentation_sante_tns_xpath"));
    }

    private void remplirBesoins(FluxData flux) {
        elementLib.randomWait(700, 1300);
        elementLib.humanTypeById(sel("contrat.date_effet_id"), dateEffet(1));
        elementLib.scrollDown(400);
    }

    private void remplirSituationProfessionnelle(FluxData flux) {
        choixProfession(flux);
        choixStatut(flux);
        choixRegime();
        scrollDown(400);
    }

    private void remplirSituationPersonnelle(FluxData flux) {
        elementLib.typeById(sel("contrat.date_naissance_assure_id"), flux.getPersonnes().getFirst().getDateNaissance());
        if (flux.getPersonnes().size() == 2) {
            elementLib.clickById(sel("contrat.has_conjoint_checkbox_id"));
            elementLib.randomWait(700, 1300);
            elementLib.humanTypeById(sel("contrat.date_naissance_conjoint_id"), flux.getPersonnes().get(1).getDateNaissance());
        }
        choixNbEnfants(flux);
        remplirEnfant(flux);
        elementLib.typeById(sel("contrat.code_postal_id"), flux.getPersonnes().getFirst().getCodePostal());
        scrollDown(400);
        elementLib.clickById(sel("contrat.soins_generaux_faible_id"));
        elementLib.clickById(sel("contrat.hospitalisation_faible_id"));
        scrollDown(150);
        elementLib.clickById(sel("contrat.optique_faible_id"));
        scrollDown(250);
        elementLib.clickById(sel("contrat.dentaire_faible_id"));
        elementLib.clickById(sel("contrat.appareil_auditif_tns_faible_id"));
        elementLib.clickById(sel("contrat.medecines_douces_tns_non_id"));
    }

    private void remplirEnfant(FluxData flux) {
        if (flux.getEnfants().getFirst().getNom() != null && !flux.getEnfants().getFirst().getNom().isEmpty()) {
            elementLib.humanTypeById(sel("contrat.date_naissance_enfant0_id"), flux.getEnfants().getFirst().getDateNaissance());
        }
        if (flux.getEnfants().size() >= 2) {
            elementLib.humanTypeById(sel("contrat.date_naissance_enfant1_id"), flux.getEnfants().get(1).getDateNaissance());
        }
    }

    private void choixNbEnfants(FluxData flux) {
        boolean sansEnfant = flux.getEnfants().getFirst().getNom() == null || flux.getEnfants().getFirst().getNom().isEmpty();
        int nbEnfants = sansEnfant ? 0 : flux.getEnfants().size();
        elementLib.selectByLabel("#" + sel("contrat.nb_enfants_select_id"), String.valueOf(nbEnfants));
    }

    private void suivant() {
        elementLib.clickByTextElement(sel("navigation.etape_suivante_text"));
    }

    private String dateEffet(int mois) {
        return LocalDate.now()
                .plus(mois, ChronoUnit.MONTHS)
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private void choixProfession(FluxData flux) {
        elementLib.clickByXpath(sel("profession.dropdown_xpath"));
        elementLib.randomWait(700, 1300);
        String profession = flux.getPersonnes().getFirst().getProfession();
        elementLib.humanTypeByXpath(sel("profession.search_input_xpath"), profession);
        /*
        String professionSite = switch (profession.toLowerCase().trim()) {
            case "agent d'assurances" -> "Agent d'assurance";
            case "agent immobilier" -> "Agent immobilier";
            case "agriculteur" -> "Exploitant agricole et viticole";
            case "ambulancier" -> "Ambulancier";
            case "architecte" -> "Architecte";
            case "assistante sociale" -> "Assistante sociale";
            case "aubergiste" -> "Aubergiste";
            case "audioprothésiste" -> "Audioprothésiste";
            case "aviculteur" -> "Aviculteur";
            case "avocat" -> "Avocat";
            case "boulanger" -> "Boulanger";
            case "brocanteur" -> "Brocanteur";
            case "buraliste" -> "Buraliste / Débitant de tabac";
            case "cafetier" -> "Cafetier";
            case "cariste" -> "Cariste";
            case "carreleur" -> "Carreleur";
            case "carrossier" -> "Carrossier";
            case "cartographe" -> "Cartographe";
            case "caviste" -> "Caviste";
            case "charcutier" -> "Charcutier";
            case "charpentier" -> "Charpentier";
            case "chaudronnier" -> "Chaudronnier";
            case "chauffagiste" -> "Chauffagiste";
            case "chauffeur livreur" -> "Chauffeur-livreur";
            case "chirurgien" -> "Chirurgien";
            case "chirurgien-dentiste" -> "Chirurgien dentiste";
            case "coiffeur" -> "Coiffeur";
            case "coloriste conseil" -> "Coloriste conseil";
            case "commerçant" -> "Commerçant de détail (hors alimentaire)";
            case "commissaire aux comptes" -> "Commissaire aux comptes";
            case "commissaire-priseur" -> "Commissaire-priseur";
            case "concepteur rédacteur" -> "Concepteur-rédacteur";
            case "conseiller artistique" -> "Conseiller artistique";
            case "conseiller commercial" -> "Conseiller commercial";
            case "conseiller conjugal" -> "Conseiller conjugal";
            case "conseiller en gestion" -> "Conseiller en gestion";
            case "conseiller en gestion de patrimoine" -> "Conseiller en gestion de patrimoine";
            case "conseiller financier" -> "Conseiller financier";
            case "conseiller fiscal" -> "Conseiller fiscal";
            case "conseiller littéraire" -> "Conseiller littéraire";
            case "consultant" -> "Autre conseiller - consultant";
            case "consultant en management" -> "Consultant en management";
            case "consultant en stratégie" -> "Consultant en stratégie";
            case "consultant it" -> "Consultant IT";
            case "consultant marketing" -> "Consultant Marketing";
            case "cordonnier" -> "Cordonnier";
            case "courtier d’assurances" -> "Courtier d'assurances";
            case "courtier en travaux" -> "Courtier en travaux";
            case "cuisinier" -> "Cuisinier";
            case "décorateur" -> "Décorateur";
            case "diagnostiqueur immobilier" -> "Diagnostiqueur immobilier";
            case "diététicien" -> "Diététicien";
            case "documentaliste" -> "Documentaliste";
            case "droguiste" -> "Droguiste";
            case "encadreur" -> "Encadreur";
            case "enseignant" -> "Enseignant - Professeur (hors sport)";
            case "ergonome" -> "Ergonome Web";
            case "expert-comptable" -> "Expert comptable";
            case "expert automobile" -> "Expert Auto";
            case "fleuriste" -> "Fleuriste";
            case "formateur" -> "Formateur";
            case "frigoriste" -> "Frigoriste";
            case "garagiste" -> "Garagiste";
            case "graphiste" -> "Graphiste - Illustrateur (CIPAV)";
            case "gynécologue" -> "Gynécologue médical";
            case "horticulteur" -> "Horticulteur (affilié MSA)";
            case "huissier de justice" -> "Huissier de justice";
            case "imprimeur" -> "Imprimeur";
            case "informaticien" -> "Informaticien (autres)";
            case "ingénieur en informatique" -> "Ingénieur informaticien";
            case "journaliste" -> "Journaliste audiovisuel";
            case "juriste" -> "Juriste";
            case "libraire" -> "Libraire";
            case "maçon" -> "Maçon";
            case "marchand de biens" -> "Marchand de biens immobiliers";
            case "maraîcher" -> "Maraîcher";
            case "marbrier" -> "Marbrier";
            case "médecin" -> "Médecin généraliste";
            case "menuisier" -> "Menuisier";
            case "notaire" -> "Notaire";
            case "opticien" -> "Opticien";
            case "orthophoniste" -> "Orthophoniste";
            case "paysagiste" -> "Paysagiste";
            case "pharmacien" -> "Pharmacien";
            case "photographe" -> "Photographe";
            case "plombier" -> "Plombier";
            case "professeur" -> "Enseignant - Professeur (hors sport)";
            case "programmeur" -> "Analyste programmeur";
            case "psychologue" -> "Psychologue";
            default -> "Agent d'assurance";
        };
        */
        page.locator(sel("profession.first_result_css")).first().click();

    }

    private void choixStatut(FluxData flux) {
        elementLib.clickByXpath(sel("statut.select_xpath"));
        elementLib.randomWait(700, 1300);
        String statutSource = flux.getPersonnes().getFirst().getProfessionSpecifique();

        String cle = switch (statutSource.toUpperCase()) {
            case "ARTISAN" -> "artisan";
            case "CHEF_ENTREPRISE" -> "chef_entreprise";
            case "COMMERCANT",
                 "PROF_LIB",
                 "MEDICAL_PROF",
                 "PARAMEDICAL_PROF",
                 "AGRICULTEUR" -> "auto_entrepreneur";
            default -> "defaut";
        };
        page.locator("#" + sel("statut.select_id")).selectOption(sel("statut." + cle));
    }

    private void choixRegime() {
        page.locator("#" + sel("regime.select_id")).selectOption(
                new SelectOption().setLabel(sel("regime.valeur"))
        );
    }

}
