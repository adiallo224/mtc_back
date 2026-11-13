package com.mtc.mutuaConseil.services.servicesImpl.assuMutuelIndiv;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.LoadState;
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
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class AprilMIService extends BasePlaywrightService implements LaunchedService {

    private static final String PROVIDER = "mutuelIndiv/april-mi";
    private static final int SMS_TIMEOUT_SECONDS = 180;

    private final TypeAssuranceService typeAssuranceService;
    private final SelectorStore selectors;
    private String currentStep = "démarrage";

    public AprilMIService(TypeAssuranceService typeAssuranceService, SelectorStore selectors) {
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
        log.info("Début de traitement -- April_Mutuel_Indiv (Playwright)");

        Tarif tarif = TarifUtils.createDefaultTarif(c, typeAssuranceService, 2L);
        tarif.setNom(c.getNomFournisseur());
        TypeAssurance typeAssurance = new TypeAssurance();
        typeAssurance.setTypeAssurance(EnumTypeAssurance.MUTUELLE_INDIV);
        tarif.setTypeAssurance(typeAssurance);

        try {
            // Navigateur visible obligatoire : le code SMS est saisi à la main
            initializeBrowser(false);
            humanLikeNavigate(c.getUrlFournisseur());
            step("connexion", () -> connexion(c));
            step("sms", () -> waitForManualInput(sel("sms.input_selector"), SMS_TIMEOUT_SECONDS));
            step("navigation", this::navigation);
            step("contrat", () -> remplirContrat(flux));
            step("assures", () -> remplirAssures(flux));
            step("voir_offres", () -> elementLib.clickById(sel("contrat.voir_offres_id")));
            page.waitForLoadState(LoadState.NETWORKIDLE);
            elementLib.randomWait(5000, 8000);
            clickIfExists("#" + sel("resultats.chat_close_id"));
            step("hospitalisation", this::reglerHospitalisation);
            currentStep = "lecture_resultats";
            List<String> couts = lirePrix();
            log.info("couts {}", couts);
            tarif.setMontant(couts);
            String screenshotPath = captureScreenshot(tarif.getNom(), false, tarif);
            if (screenshotPath != null) {
                tarif.setCaptureImg(screenshotPath);
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

    private void connexion(Compte c) {
        elementLib.randomWait(2500, 3500);
        elementLib.humanTypeByXpath(sel("connexion.username_xpath"), c.getUsername());
        elementLib.humanTypeByXpath(sel("connexion.password_xpath"), c.getPassword());
        elementLib.randomWait(700, 1300);
        elementLib.clickById(sel("connexion.login_button_id"));
    }

    /** OFFRES \ PARTICULIERS \ SANTE \ FAIRE UN DEVIS */
    private void navigation() {
        elementLib.randomWait(1500, 2500);
        elementLib.clickByTextElement(sel("navigation.offres_text"));
        elementLib.clickByTextElement(sel("navigation.particuliers_text"));
        elementLib.clickByTextElement(sel("navigation.sante_text"));
        elementLib.clickByTextElement(sel("navigation.faire_devis_text"));
        elementLib.randomWait(1500, 2500);
    }

    private void remplirContrat(FluxData flux) {
        var assure = flux.getPersonnes().getFirst();
        elementLib.clickById(sel("contrat.deja_adherent_non_id"));
        if (assure.getCivilite().equalsIgnoreCase("M") || assure.getCivilite().equalsIgnoreCase("Monsieur")) {
            elementLib.clickById(sel("contrat.civilite_monsieur_id"));
        } else {
            elementLib.clickById(sel("contrat.civilite_madame_id"));
        }
        elementLib.humanTypeById(sel("contrat.date_effet_id"), dateEffet(1));
        elementLib.humanTypeById(sel("contrat.prenom_id"), assure.getPrenom());
        elementLib.humanTypeById(sel("contrat.nom_id"), assure.getNom());
        elementLib.humanTypeById(sel("contrat.date_naissance_id"), assure.getDateNaissance());
        elementLib.humanTypeById(sel("contrat.code_postal_id"), assure.getCodePostal());
        choixRegime();
        choixStatut();
        scrollDown(300);
    }

    private void choixRegime() {
        elementLib.click(sel("contrat.regime_selector"));
        elementLib.randomWait(700, 1300);
        page.getByRole(com.microsoft.playwright.options.AriaRole.OPTION,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName(sel("contrat.regime_valeur"))).first().click();
    }

    private void choixStatut() {
        elementLib.clickById(sel("contrat.statut_select_id"));
        elementLib.randomWait(700, 1300);
        page.getByRole(com.microsoft.playwright.options.AriaRole.OPTION,
                new com.microsoft.playwright.Page.GetByRoleOptions().setName(sel("contrat.statut_defaut")).setExact(true)).first().click();
    }

    private void remplirAssures(FluxData flux) {
        if (flux.getPersonnes().size() >= 2) {
            ajouterAssure();
            elementLib.clickById(sel("assures.conjoint_relation_id"));
            elementLib.humanTypeById(sel("assures.conjoint_naissance_id"), flux.getPersonnes().get(1).getDateNaissance());
        }
        if (!flux.getEnfants().isEmpty() && flux.getEnfants().getFirst().getNom() != null
                && !flux.getEnfants().getFirst().getNom().isEmpty()) {
            ajouterAssure();
            elementLib.humanTypeById(sel("assures.enfant1_naissance_id"), flux.getEnfants().getFirst().getDateNaissance());
            if (flux.getEnfants().size() >= 2) {
                ajouterAssure();
                elementLib.humanTypeById(sel("assures.enfant2_naissance_id"), flux.getEnfants().get(1).getDateNaissance());
            }
        }
    }

    private void ajouterAssure() {
        elementLib.randomWait(1000, 2000);
        elementLib.clickById(sel("assures.ajouter_id"));
    }

    /** Hospitalisation : « + » jusqu'à 250 %, puis « Actualiser ma recherche ». */
    private void reglerHospitalisation() {
        Locator plus = page.locator(sel("hospitalisation.plus_selector")).first();
        int clics = Integer.parseInt(sel("hospitalisation.nb_clics"));
        for (int i = 0; i < clics && plus.count() > 0 && plus.isEnabled(); i++) {
            plus.click();
            elementLib.randomWait(300, 700);
        }
        elementLib.clickByRole(sel("hospitalisation.actualiser_label"));
        page.waitForLoadState(LoadState.NETWORKIDLE);
        elementLib.randomWait(4000, 6000);
    }

    /** Relève toutes les propositions affichées à l'écran. */
    private List<String> lirePrix() {
        List<String> prix = new ArrayList<>();
        int max = Integer.parseInt(sel("resultats.nb_offres_max"));
        for (int i = 1; i <= max; i++) {
            Locator offre = page.locator(String.format(sel("resultats.prix_template"), i));
            if (offre.count() == 0) break;
            prix.add(offre.first().innerText().replace(' ', ' ').trim());
        }
        return prix;
    }

    private String dateEffet(int mois) {
        return LocalDate.now().plusMonths(mois).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}
