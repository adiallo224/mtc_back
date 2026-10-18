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

import java.util.List;

@Service
public class HodevaMIService extends BasePlaywrightService implements LaunchedService {

    private static final String PROVIDER = "mutuelIndiv/hodeva-mi";

    private final TypeAssuranceService typeAssuranceService;
    private final SelectorStore selectors;
    private String currentStep = "démarrage";

    public HodevaMIService(TypeAssuranceService typeAssuranceService, SelectorStore selectors) {
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
        log.info("Début de traitement -- Hodeva_Mutuel_Indiv (Playwright)");

        Tarif tarif = TarifUtils.createDefaultTarif(c, typeAssuranceService, 2L);
        tarif.setNom(c.getNomFournisseur());
        TypeAssurance typeAssurance = new TypeAssurance();
        typeAssurance.setTypeAssurance(EnumTypeAssurance.MUTUELLE_INDIV);
        tarif.setTypeAssurance(typeAssurance);

        try {
            initializeBrowser(false);
            humanLikeNavigate(c.getUrlFournisseur());
            step("connexion", () -> connexion(c));
            step("tarificateur", () -> elementLib.clickByTextElement(sel("navigation.tarificateur_senior_text")));
            step("assure", () -> remplirAssure(flux));
            step("beneficiaires", () -> remplirBeneficiaires(flux));
            step("tarifer", () -> elementLib.clickByRole(sel("tarification.obtenir_tarif_label")));
            page.waitForLoadState(LoadState.NETWORKIDLE);
            elementLib.randomWait(4000, 6000);
            currentStep = "lecture_resultats";
            List<String> couts = lirePrix();
            log.info("couts {}", couts);
            tarif.setMontant(couts);
            // Le fichier demande une copie d'écran des 5 formules (6 pour la gamme SENIOR)
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
        elementLib.randomWait(2000, 3000);
        elementLib.typeHumanLike(sel("connexion.username_selector"), c.getUsername());
        elementLib.typeHumanLike(sel("connexion.password_selector"), c.getPassword());
        elementLib.randomWait(700, 1300);
        elementLib.clickByRole(sel("connexion.submit_label"));
    }

    /** L'offre se règle seule : SENIOR (plus de 50 ans, valeur par défaut) ou TOUT PUBLIC (moins de 50 ans). */
    private void remplirAssure(FluxData flux) {
        var assure = flux.getPersonnes().getFirst();
        elementLib.randomWait(1500, 2500);
        if (assure.getCivilite().equalsIgnoreCase("M") || assure.getCivilite().equalsIgnoreCase("Monsieur")) {
            elementLib.clickByTextElement(sel("tarification.civilite_monsieur_text"));
        } else {
            elementLib.clickByTextElement(sel("tarification.civilite_madame_text"));
        }
        elementLib.typeByLabel(sel("tarification.nom_label"), assure.getNom());
        elementLib.typeByLabel(sel("tarification.prenom_label"), assure.getPrenom());
        elementLib.typeByLabel(sel("tarification.date_naissance_label"), assure.getDateNaissance());
        elementLib.typeByLabel(sel("tarification.code_postal_label"), assure.getCodePostal());
    }

    private void remplirBeneficiaires(FluxData flux) {
        if (flux.getPersonnes().size() >= 2) {
            page.getByLabel(sel("tarification.conjoint_checkbox_label")).first().check();
            remplirDernierBeneficiaire(flux.getPersonnes().get(1).getDateNaissance());
        }
        for (var enfant : flux.getEnfants()) {
            if (enfant.getNom() == null || enfant.getNom().isEmpty()) break;
            page.getByLabel(sel("tarification.enfants_checkbox_label")).first().check();
            remplirDernierBeneficiaire(enfant.getDateNaissance());
        }
    }

    private void remplirDernierBeneficiaire(String dateNaissance) {
        elementLib.randomWait(700, 1300);
        page.locator(sel("tarification.date_naissance_beneficiaire_selector")).last().fill(dateNaissance);
        Locator regime = page.locator(sel("tarification.regime_beneficiaire_selector")).last();
        if (regime.count() > 0) {
            regime.selectOption(new com.microsoft.playwright.options.SelectOption()
                    .setLabel(sel("tarification.regime_valeur")));
        }
    }

    /** Prix des 5 formules (6 pour la gamme SENIOR) affichées. */
    private List<String> lirePrix() {
        return page.locator(sel("resultats.carte_selector")).allInnerTexts().stream()
                .map(t -> t.replace(' ', ' ').replaceAll("\\s+", " ").trim())
                .filter(t -> t.contains("€"))
                .toList();
    }
}
