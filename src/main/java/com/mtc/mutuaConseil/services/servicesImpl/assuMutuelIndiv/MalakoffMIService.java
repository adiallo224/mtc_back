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
import java.util.List;

@Service
public class MalakoffMIService extends BasePlaywrightService implements LaunchedService {

    private static final String PROVIDER = "mutuelIndiv/malakoff-mi";

    private final TypeAssuranceService typeAssuranceService;
    private final SelectorStore selectors;
    private String currentStep = "démarrage";

    public MalakoffMIService(TypeAssuranceService typeAssuranceService, SelectorStore selectors) {
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
        log.info("Début de traitement -- Malakoff_Mutuel_Indiv (Playwright)");

        Tarif tarif = TarifUtils.createDefaultTarif(c, typeAssuranceService, 2L);
        tarif.setNom(c.getNomFournisseur());
        TypeAssurance typeAssurance = new TypeAssurance();
        typeAssurance.setTypeAssurance(EnumTypeAssurance.MUTUELLE_INDIV);
        tarif.setTypeAssurance(typeAssurance);

        try {
            initializeBrowser(false);
            humanLikeNavigate(c.getUrlFournisseur());
            step("connexion", () -> connexion(c));
            step("navigation", this::navigation);
            step("contrat", () -> remplirContrat(flux));
            step("commission", this::reglerCommission);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            elementLib.randomWait(4000, 6000);
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
        elementLib.randomWait(2000, 3000);
        elementLib.typeHumanLike(sel("connexion.username_selector"), c.getUsername());
        elementLib.typeHumanLike(sel("connexion.password_selector"), c.getPassword());
        elementLib.randomWait(700, 1300);
        elementLib.clickByRole(sel("connexion.submit_label"));
    }

    private void navigation() {
        elementLib.randomWait(2000, 3000);
        elementLib.clickByTextElement(sel("navigation.tarification_express_text"));
        elementLib.clickByTextElement(sel("navigation.atout_plus_particulier_text"));
        elementLib.randomWait(1500, 2500);
    }

    private void remplirContrat(FluxData flux) {
        elementLib.typeByLabel(sel("tarification.date_effet_label"), dateEffet(1));
        elementLib.typeByLabel(sel("tarification.date_naissance_label"), flux.getPersonnes().getFirst().getDateNaissance());
        elementLib.selectByLabel(sel("tarification.regime_selector"), sel("tarification.regime_valeur"));
        if (flux.getPersonnes().size() >= 2) {
            ajouterBeneficiaire(sel("tarification.lien_conjoint"), flux.getPersonnes().get(1).getDateNaissance());
        }
        for (var enfant : flux.getEnfants()) {
            if (enfant.getNom() == null || enfant.getNom().isEmpty()) break;
            ajouterBeneficiaire(sel("tarification.lien_enfant"), enfant.getDateNaissance());
        }
    }

    /** Lien de parenté, date de naissance et régime du dernier bénéficiaire ajouté. */
    private void ajouterBeneficiaire(String lien, String dateNaissance) {
        elementLib.randomWait(700, 1300);
        elementLib.clickByRole(sel("tarification.ajouter_beneficiaire_label"));
        page.locator(sel("tarification.lien_parente_selector")).last()
                .selectOption(new com.microsoft.playwright.options.SelectOption().setLabel(lien));
        page.locator(sel("tarification.date_naissance_beneficiaire_selector")).last().fill(dateNaissance);
        page.locator(sel("tarification.regime_selector")).last()
                .selectOption(new com.microsoft.playwright.options.SelectOption().setLabel(sel("tarification.regime_valeur")));
    }

    /** Commission LINEAIRE 12 %, case décochée, coefficient de financement à « 1 », puis REINITIALISER. */
    private void reglerCommission() {
        elementLib.randomWait(700, 1300);
        elementLib.selectByLabel(sel("tarification.commission_selector"), sel("tarification.commission_valeur"));
        clickIfExists(sel("tarification.case_a_decocher_selector"));
        Locator coef = page.locator(sel("tarification.coefficient_selector")).first();
        coef.clear();
        coef.fill(sel("tarification.coefficient_valeur"));
        elementLib.clickByRole(sel("tarification.reinitialiser_label"));
    }

    /** Tarifs des 6 formules affichées. */
    private List<String> lirePrix() {
        return page.locator(sel("resultats.prix_selector")).allInnerTexts().stream()
                .map(t -> t.replace(' ', ' ').replaceAll("\\s+", " ").trim())
                .filter(t -> t.contains("€"))
                .toList();
    }

    private String dateEffet(int mois) {
        return LocalDate.now().plusMonths(mois).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}
