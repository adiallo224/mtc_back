package com.mtc.mutuaConseil.services.servicesImpl.assuMutuelPro;


import com.microsoft.playwright.Locator;
import com.mtc.mutuaConseil.base.BasePlaywrightService;
import com.mtc.mutuaConseil.models.Compte;
import com.mtc.mutuaConseil.models.FluxData;
import com.mtc.mutuaConseil.models.Tarif;
import com.mtc.mutuaConseil.selectors.SelectorNotFoundException;
import com.mtc.mutuaConseil.selectors.SelectorStore;
import com.mtc.mutuaConseil.services.LaunchedService;
import com.mtc.mutuaConseil.services.servicesImpl.TypeAssuranceService;
import com.mtc.mutuaConseil.utils.TarifUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class LoomaMProService extends BasePlaywrightService implements LaunchedService {

    private static final String PROVIDER = "mutuelPro/looma-mp";

    private final Logger log = LoggerFactory.getLogger(LoomaMProService.class);
    private final TypeAssuranceService typeAssuranceService;
    private final SelectorStore selectors;
    private String currentStep = "démarrage";

    public LoomaMProService(TypeAssuranceService typeAssuranceService, SelectorStore selectors) {
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
        log.info("Début de traitement -- Looma_Mutuel_Pro (Playwright)");
        Tarif tarif = TarifUtils.createDefaultTarif(c, typeAssuranceService, 3L);

        try {
            initializeBrowser(false);
            step("connexion", () -> connexion(c));
            step("projet", this::projet);
            step("profil", () -> profil(flux));
            step("garantie_tarifs", () -> garantieTarifs(flux));
            elementLib.clickById(sel("navigation.etape_suivante_id"));
            elementLib.randomWait(1500, 2500);
            currentStep = "lecture_resultats";
            String cout = getPrixByNiveau(3, flux);
            log.info("cout {}", cout);
            tarif.setMontant(cout);
            scrollDown(-400);
            String screenshotBytes = captureScreenshot(tarif.getNom(), false, tarif);
            if (screenshotBytes != null) {
                tarif.setCaptureImg(screenshotBytes);
            }
            tarif.setExecution(true);
            log.info("Fin de traitement -- Looma_Mutuel_Pro");
        } catch (Exception e) {
            log.error("Échec à l'étape '{}'", currentStep, e);
            tarif.setErreur(e.getMessage());
            String screenshotBytesErreur = captureScreenshot(tarif.getNom(), true, tarif);
            tarif.setCaptureImgErreur(screenshotBytesErreur);
            tarif.setEtape(currentStep);
        } finally {
            cleanup();
        }
        return tarif;
    }

    private void connexion(Compte c) {
        humanLikeNavigate(c.getUrlFournisseur());
        elementLib.humanTypeById(sel("connexion.identifiant_id"), c.getUsername());
        elementLib.humanTypeById(sel("connexion.password_id"), c.getPassword());
        elementLib.clickByXpath(sel("connexion.connexion_xpath"));
        elementLib.randomWait(1500, 2500);
    }

    private void projet() {
        elementLib.clickByXpath(sel("navigation.projets_xpath"));
        elementLib.randomWait(700, 1300);
        elementLib.clickByXpath(sel("navigation.nouveau_projet_xpath"));
        elementLib.randomWait(700, 1300);
        elementLib.clickByXpath(sel("navigation.tns_xpath"));
        elementLib.randomWait(700, 1300);
    }

    private void profil(FluxData flux) {
        elementLib.humanTypeById(sel("profil.nom_id"), flux.getPersonnes().get(0).getNom());
        elementLib.humanTypeById(sel("profil.prenom_id"), flux.getPersonnes().get(0).getPrenom());
        elementLib.humanTypeById(sel("profil.date_naissance_id"), flux.getPersonnes().get(0).getDateNaissance());
//        clickBody();
        elementLib.randomWait(700, 1300);
        elementLib.clickById(sel("profil.suivant_id"));
    }

    private void garantieTarifs(FluxData flux) {
        // Données projet
        choixRisque();
        elementLib.humanTypeById(sel("garantie.date_effet_id"), dateEffet(1));
        // Données personnelles TNS
        choixSituation(flux);
        if (!flux.getEnfants().get(0).getNom().isEmpty()) {
            for (int i = 0; i < flux.getEnfants().size(); i++) {
                elementLib.clickByXpath(sel("garantie.ajouter_enfant_xpath"));
            }
        }
        elementLib.humanTypeById(sel("garantie.adresse_id"), flux.getPersonnes().get(0).getNumeroVoie() + " " + flux.getPersonnes().get(0).getNomVoie());
        elementLib.humanTypeById(sel("garantie.ville_id"), flux.getPersonnes().get(0).getVille());
        choisirVilleSuggestion(flux);
        // Données professionnelles TNS
        choixStatut(flux);
        elementLib.humanTypeById(sel("garantie.siret_id"), flux.getEntreprise().getSiret());
        elementLib.humanTypeById(sel("garantie.code_naf_id"), flux.getEntreprise().getCodeAPE());
        choisirPremiereSuggestionCodeNaf();
    }

    private void choixRisque() {
        elementLib.selectByValue("#" + sel("garantie.risque_select_id"), sel("garantie.risque_valeur"));
    }

    private void choixSituation(FluxData flux) {
        if (flux.getPersonnes().size() == 1) {
            elementLib.selectByValue("#" + sel("garantie.situation_select_id"), sel("garantie.situation_seul_valeur"));
        }
        if (flux.getPersonnes().size() == 2) {
            elementLib.selectByValue("#" + sel("garantie.situation_select_id"), sel("garantie.situation_couple_valeur"));
        }
    }

    private void choixStatut(FluxData flux) {
        String profession = flux.getPersonnes().get(0).getProfessionSpecifique();
        String valeur;
        try {
            valeur = sel("statuts." + profession);
        } catch (SelectorNotFoundException e) {
            valeur = null;
        }
        if (valeur != null) {
            elementLib.selectByValue("#" + sel("garantie.statut_select_id"), valeur);
        } else {
            log.warn("Statut professionnel non reconnu : {}", profession);
        }
    }

    private void choisirVilleSuggestion(FluxData flux) {
        Locator suggestionsContainer = page.locator(sel("garantie.ville_suggestions_container_id"));
        suggestionsContainer.waitFor(new Locator.WaitForOptions().setTimeout(10000));
        Locator suggestions = suggestionsContainer.locator(sel("garantie.ville_suggestion_item_css"));
        String codePostal = flux.getPersonnes().get(0).getCodePostal();
        String ville = flux.getPersonnes().get(0).getVille().toUpperCase();
        int count = suggestions.count();
        for (int i = 0; i < count; i++) {
            Locator suggestion = suggestions.nth(i);
            String texte = suggestion.textContent();
            if (texte.contains(codePostal) && texte.contains(ville)) {
                suggestion.click();
                break;
            }
        }
    }

    private void choisirPremiereSuggestionCodeNaf() {
        Locator suggestionsContainer = page.locator(sel("garantie.naf_suggestions_container_id"));
        suggestionsContainer.waitFor(new Locator.WaitForOptions().setTimeout(10000));
        suggestionsContainer.locator(sel("garantie.naf_suggestion_item_css")).first().click();
    }

    private String dateEffet(int mois) {
        LocalDate today = LocalDate.now();
        LocalDate nextMonth = today.plusMonths(mois);
        return nextMonth.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public String getPrixByNiveau(int niveau, FluxData flux) {

        String structure = getStructure(flux);

        Locator ligne = page.locator(sel("resultats.lignes_css"))
                .filter(new Locator.FilterOptions().setHasText(structure));

        return ligne.locator(String.format(sel("resultats.cellule_niveau_template"), niveau))
                .textContent()
                .trim();
    }

    private String getStructure(FluxData flux) {

        int nbAdultes = flux.getPersonnes() == null
                ? 0
                : flux.getPersonnes().size();

        int nbEnfants = flux.getEnfants() == null
                ? 0
                : (int) flux.getEnfants().stream()
                .filter(e -> e.getNom() != null && !e.getNom().isBlank())
                .count();

        if (nbAdultes == 1 && nbEnfants == 0) {
            return "Isolé";
        }

        if (nbAdultes == 1 && nbEnfants == 1) {
            return "Duo";
        }

        if (nbAdultes == 2 && nbEnfants == 0) {
            return "Couple";
        }

        if ((nbAdultes >= 1 && nbEnfants >= 2)
                || (nbAdultes == 2 && nbEnfants >= 1)) {
            return "Famille";
        }

        throw new IllegalArgumentException(
                "Structure non gérée : adultes=" + nbAdultes
                        + ", enfants=" + nbEnfants);
    }

}
