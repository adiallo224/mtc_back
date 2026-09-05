package com.mtc.mutuaConseil.services.servicesImpl.assuMutuelIndiv;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class EcaHeomieMlService extends BasePlaywrightService implements LaunchedService {

    private static final String PROVIDER = "mutuelIndiv/ecaheomie-mi";

    private final Logger log = LoggerFactory.getLogger(EcaHeomieMlService.class);
    private final TypeAssuranceService typeAssuranceService;
    private final SelectorStore selectors;
    private String currentStep = "démarrage";

    public EcaHeomieMlService(TypeAssuranceService typeAssuranceService, SelectorStore selectors) {
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
        log.info("Début de traitement -- Eca_Heomie_Mutuel_Indiv (Playwright)");

        Tarif tarif = TarifUtils.createDefaultTarif(c, typeAssuranceService, 2L);
        tarif.setNom(c.getNomFournisseur());
        TypeAssurance typeAssurance = new TypeAssurance();
        typeAssurance.setTypeAssurance(EnumTypeAssurance.MUTUELLE_INDIV);
        tarif.setTypeAssurance(typeAssurance);

        try {
            initializeBrowser(false);
            humanLikeNavigate(c.getUrlFournisseur());
            step("connexion", () -> connexion(c));
            step("navigation", this::choixComplementaire);
            step("complementaire_sante", () -> remplirComplementaireSante(flux));
            scrollDown(350);
            step("calcul", this::suivant);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            elementLib.randomWait(4000, 6000);
            currentStep = "lecture_resultats";
            String cout = elementLib.getElementTextByXpath(sel("resultats.cout_xpath"));
            log.info("cout {}", cout);
            tarif.setMontant(List.of(cout));
            String screenshotBytes = captureScreenshot(tarif.getNom(), false, tarif);
            if (screenshotBytes != null) {
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

    private void connexion(Compte c) {
        elementLib.humanTypeById(sel("connexion.login_id"), c.getUsername());
        elementLib.humanTypeById(sel("connexion.password_id"), c.getPassword());
        elementLib.clickByXpath(sel("connexion.submit_xpath"));
        elementLib.randomWait(700, 1300);
    }

    private void choixComplementaire() {
        elementLib.clickByXpath(sel("navigation.nouveau_devis_particulier_xpath"));
        elementLib.randomWait(700, 1300);
        elementLib.clickByXpath(sel("navigation.presentation_sante_xpath"));
        scrollDown(350);
        elementLib.clickByXpath(sel("navigation.nouveau_devis_sante_xpath"));
    }

    private void remplirComplementaireSante(FluxData flux) {
        elementLib.humanTypeById(sel("contrat.date_effet_id"), dateEffet(1));
        choixRegime(flux, 0);
        elementLib.humanTypeById(sel("contrat.date_naissance_assure_id"), flux.getPersonnes().getFirst().getDateNaissance());

        if (flux.getPersonnes().size() == 2) {
            elementLib.clickById(sel("contrat.has_conjoint_checkbox_id"));
            elementLib.randomWait(700, 1300);
            elementLib.humanTypeById(sel("contrat.date_naissance_conjoint_id"), flux.getPersonnes().get(1).getDateNaissance());
        }
        elementLib.humanTypeById(sel("contrat.code_postal_id"), flux.getPersonnes().getFirst().getCodePostal());

        choixNbEnfants(flux);
        remplirEnfant(flux);
        scrollDown(400);

        elementLib.clickById(sel("contrat.budget_id"));
        elementLib.clickById(sel("contrat.couverture_sante_non_id"));
        elementLib.clickById(sel("contrat.beneficiaire_css_non_id"));
        scrollDown(150);
        elementLib.clickById(sel("contrat.soins_generaux_faible_id"));
        elementLib.clickById(sel("contrat.hospitalisation_faible_id"));
        scrollDown(150);
        elementLib.clickById(sel("contrat.optique_faible_id"));
        scrollDown(250);
        elementLib.clickById(sel("contrat.dentaire_faible_id"));
        elementLib.clickById(sel("contrat.appareil_auditif_faible_id"));
        elementLib.clickById(sel("contrat.medecines_douces_non_id"));
    }

    private void remplirEnfant(FluxData flux) {
        if (flux.getEnfants().getFirst().getNom() != null && !flux.getEnfants().getFirst().getNom().isEmpty()) {
            elementLib.humanTypeById(sel("contrat.date_naissance_enfant1_id"), flux.getEnfants().getFirst().getDateNaissance());
            scrollDown(100);
        }
        if (flux.getEnfants().size() >= 2) {
            elementLib.humanTypeById(sel("contrat.date_naissance_enfant2_id"), flux.getEnfants().get(1).getDateNaissance());
            scrollDown(400);
        }
    }

    private void choixNbEnfants(FluxData flux) {
        boolean sansEnfant = flux.getEnfants().getFirst().getNom() == null || flux.getEnfants().getFirst().getNom().isEmpty();
        int nbEnfants = sansEnfant ? 0 : flux.getEnfants().size();
        elementLib.selectByLabel("#" + sel("contrat.nb_enfants_select_id"), String.valueOf(nbEnfants));
    }

    private void suivant() {
        elementLib.clickByXpath(sel("navigation.calculer_tarif_xpath"));
    }

    private void choixRegime(FluxData flux, int index) {
        elementLib.selectByLabel("#" + sel("contrat.regime_select_id"), sel("contrat.regime_valeur"));
    }

    private String dateEffet(int mois) {
        return LocalDate.now()
                .plus(mois, ChronoUnit.MONTHS)
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}
