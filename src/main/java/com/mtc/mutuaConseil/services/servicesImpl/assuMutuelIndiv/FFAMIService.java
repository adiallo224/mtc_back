package com.mtc.mutuaConseil.services.servicesImpl.assuMutuelIndiv;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
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
import java.util.List;

@Service
public class FFAMIService extends BasePlaywrightService implements LaunchedService {

    private static final String PROVIDER = "mutuelIndiv/ffa-mi";

    private final TypeAssuranceService typeAssuranceService;
    private final SelectorStore selectors;
    private String currentStep = "démarrage";

    public FFAMIService(TypeAssuranceService typeAssuranceService, SelectorStore selectors) {
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
        log.info("Début de traitement -- Ffa_Mutuel_Indiv (Playwright)");

        Tarif tarif = TarifUtils.createDefaultTarif(c, typeAssuranceService, 2L);
        tarif.setNom(c.getNomFournisseur());
        TypeAssurance typeAssurance = new TypeAssurance();
        typeAssurance.setTypeAssurance(EnumTypeAssurance.MUTUELLE_INDIV);
        tarif.setTypeAssurance(typeAssurance);

        try {
            initializeBrowser(false);
            humanLikeNavigate(c.getUrlFournisseur());
            step("connexion", () -> connexion(c));
            step("navigation", () -> navigation(flux));
            step("adherents", () -> remplirAdherents(flux));
            step("tarifer", this::tarifer);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            elementLib.randomWait(4000, 6000);
            currentStep = "lecture_resultats";
            // A5 pour les seniors, A6 sinon
            String formule = estSenior(flux) ? sel("resultats.formule_senior") : sel("resultats.formule_actif");
            List<String> couts = prixParLibelle(sel("resultats.ligne_template"), formule);
            log.info("couts (formule {}) {}", formule, couts);
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

    private boolean estSenior(FluxData flux) {
        return ageEn(flux.getPersonnes().getFirst().getDateNaissance()) >= Integer.parseInt(sel("navigation.age_senior"));
    }

    private void connexion(Compte c) {
        elementLib.randomWait(2000, 3000);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(sel("connexion.cookie_refuse_label")))
                .first().click();
        elementLib.humanTypeById(sel("connexion.username_id"), c.getUsername());
        elementLib.humanTypeById(sel("connexion.password_id"), c.getPassword());
        elementLib.randomWait(700, 1300);
        elementLib.click(sel("connexion.submit_selector"));
    }

    /** Bandeau FFA Santé & Prévoyance > INDIVIDUEL > SANTE PARTICULIER ou SENIOR > TARIFICATION */
    private void navigation(FluxData flux) {
        elementLib.randomWait(2000, 3000);
        elementLib.clickByTextElement(sel("navigation.bandeau_ffa_text"));
        elementLib.randomWait(1500, 2500);
        elementLib.clickByTextElement(sel("navigation.individuel_text"));
        elementLib.clickByTextElement(estSenior(flux) ? sel("navigation.sante_senior_text") : sel("navigation.sante_particulier_text"));
        elementLib.clickByTextElement(sel("navigation.tarification_text"));
        elementLib.randomWait(1500, 2500);
    }

    /** Date de naissance + régime SALARIE pour chaque adhérent (adhérent, conjoint, puis enfants). */
    private void remplirAdherents(FluxData flux) {
        Locator dates = page.locator(sel("tarification.date_naissance_selector"));
        Locator regimes = page.locator(sel("tarification.regime_selector"));
        int nbAdultes = Math.min(flux.getPersonnes().size(), 2);
        for (int i = 0; i < nbAdultes; i++) {
            elementLib.randomWait(700, 1300);
            dates.nth(i).fill(flux.getPersonnes().get(i).getDateNaissance());
            regimes.nth(i).selectOption(new com.microsoft.playwright.options.SelectOption()
                    .setLabel(sel("tarification.regime_valeur")));
        }
        int index = nbAdultes;
        for (var enfant : flux.getEnfants()) {
            if (enfant.getNom() == null || enfant.getNom().isEmpty()) break;
            dates.nth(index).fill(enfant.getDateNaissance());
            if (regimes.count() > index) {
                regimes.nth(index).selectOption(new com.microsoft.playwright.options.SelectOption()
                        .setLabel(sel("tarification.regime_valeur")));
            }
            index++;
        }
        elementLib.typeByLabel(sel("tarification.code_postal_label"), flux.getPersonnes().getFirst().getCodePostal());
        elementLib.typeByLabel(sel("tarification.date_effet_label"), dateEffet(1));
    }

    private void tarifer() {
        elementLib.randomWait(700, 1300);
        elementLib.clickByRole(sel("tarification.tarifer_label"));
    }

    private String dateEffet(int mois) {
        return LocalDate.now().plusMonths(mois).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}
