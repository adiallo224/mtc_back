package com.mtc.mutuaConseil.services.servicesImpl.assuMutuelPro;

import com.microsoft.playwright.Locator;
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

@Service
public class ApiviaMProService extends BasePlaywrightService implements LaunchedService {

    private static final String PROVIDER = "mutuelPro/apivia-mp";

    private final Logger log = LoggerFactory.getLogger(ApiviaMProService.class);
    private final TypeAssuranceService typeAssuranceService;
    private final SelectorStore selectors;
    private String currentStep = "démarrage";

    public ApiviaMProService(TypeAssuranceService typeAssuranceService, SelectorStore selectors) {
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
        log.info("Début de traitement -- Apivia_Mutuel_Pro (Playwright)");

        Tarif tarif = TarifUtils.createDefaultTarif(c, typeAssuranceService, 3L);
        tarif.setNom(c.getNomFournisseur());
        TypeAssurance typeAssurance = new TypeAssurance();
        typeAssurance.setTypeAssurance(EnumTypeAssurance.MUTUELLE_PRO);
        tarif.setTypeAssurance(typeAssurance);

        try {
            initializeBrowser(false);
            step("connexion", () -> connexion(c));
            step("offres", this::remplirOffres);
            step("tarificateur", this::remplirTarificateur);
            step("devoir_de_conseils", this::remplirDevoirDeConseils);
            step("contrat", () -> remplirContrat(flux));
            step("calcul", this::calculer);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            elementLib.randomWait(4000, 6000);
            currentStep = "lecture_resultats";
            String cout = getPrixTtcParNiveau(2);
            cout = cout.substring(0, Math.min(8, cout.length()));
            log.info("cout {}", cout);
            tarif.setMontant(cout);
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
        humanLikeNavigate(c.getUrlFournisseur());
        elementLib.randomWait(700, 1300);
        elementLib.humanTypeById(sel("connexion.username_id"), c.getUsername());
        elementLib.humanTypeById(sel("connexion.password_id"), c.getPassword());
        elementLib.clickByRole(sel("connexion.bouton_connexion_label"));
        elementLib.randomWait(2500, 3500);
    }

    private void remplirOffres() {
        elementLib.clickByTextElement(sel("navigation.nos_offres_text"));
        elementLib.randomWait(700, 1300);
    }

    private void remplirTarificateur() {
        elementLib.humanClick(sel("navigation.particulier_individuel_xpath"));
        elementLib.randomWait(700, 1300);
    }

    private void remplirDevoirDeConseils() {
        elementLib.clickById(sel("devoir_conseil.checkbox_id"));
        elementLib.clickByXpath(sel("devoir_conseil.switch_label_xpath"));
        elementLib.randomWait(700, 1300);
    }

    private void remplirContrat(FluxData flux) {
        elementLib.humanTypeById(sel("contrat.code_postal_id"), flux.getPersonnes().getFirst().getCodePostal());
        elementLib.humanTypeById(sel("contrat.date_effet_id"), dateEffet(1));
        clickBody();
        choixAnneeNaissance(flux, 0);
        elementLib.randomWait(700, 1300);
        choixRegime(flux, 0);
        elementLib.randomWait(700, 1300);
        remplirConjoint(flux);
        remplirEnfants(flux);
        scrollDown(300);
        elementLib.click(sel("contrat.vitamin3_xpath"));
    }

    private void remplirConjoint(FluxData flux) {
        elementLib.randomWait(700, 1300);
        if (flux.getPersonnes().size() == 2) {
            choixAnneeNaissance(flux, 1);
            choixRegime(flux, 1);
        }
    }

    private void remplirEnfants(FluxData flux) {
        if (flux.getEnfants().getFirst().getNom() != null && !flux.getEnfants().getFirst().getNom().isEmpty()) {
            elementLib.randomWait(700, 1300);
            ajoutBeneficiaire();
            choixAnneeNaissanceEnfant(flux, 0);
            choixRegimeEnfant(flux, 0);
        }
        if (flux.getEnfants().size() >= 2) {
            elementLib.randomWait(700, 1300);
            ajoutBeneficiaire();
            choixAnneeNaissanceEnfant(flux, 1);
            choixRegimeEnfant(flux, 1);
        }
    }

    private void ajoutBeneficiaire() {
        elementLib.clickByRole(sel("beneficiaire.ajouter_label"));
    }

    private void choixAnneeNaissance(FluxData flux, int index) {
        elementLib.randomWait(700, 1300);
        String annee = extractYear(flux.getPersonnes().get(index).getDateNaissance());
        String selectId = index == 0 ? sel("contrat.assure_date_naissance_id") : sel("contrat.conjoint_date_naissance_id");
        selectOptionEquals(selectId, annee);
    }

    private void choixAnneeNaissanceEnfant(FluxData flux, int index) {
        elementLib.randomWait(700, 1300);
        String annee = extractYear(flux.getEnfants().get(index).getDateNaissance());
        String id = index == 0
                ? sel("beneficiaire.enfant0_date_naissance_id")
                : sel("beneficiaire.enfant1_date_naissance_id");
        selectOptionEqualsByXpath(id, annee);
    }

    private void choixRegime(FluxData flux, int index) {
        elementLib.randomWait(700, 1300);
        String selectId = index == 0 ? sel("contrat.assure_regime_id") : sel("contrat.conjoint_regime_id");
        selectOptionEquals(selectId, sel("contrat.regime_valeur"));
    }

    private void choixRegimeEnfant(FluxData flux, int index) {
        elementLib.randomWait(700, 1300);
        String id = index == 0
                ? sel("beneficiaire.enfant0_regime_id")
                : sel("beneficiaire.enfant1_regime_id");
        selectOptionEqualsByXpath(id, sel("contrat.regime_valeur"));
    }

    private void calculer() {
        elementLib.randomWait(700, 1300);
        elementLib.clickById(sel("calcul.bouton_id"));
    }

    private void selectOptionEquals(String selectId, String search) {
        Locator opts = page.locator("#" + selectId + " option");
        int count = opts.count();
        for (int i = 0; i < count; i++) {
            Locator opt = opts.nth(i);
            if (opt.innerText().trim().equalsIgnoreCase(search)) {
                page.locator("#" + selectId).selectOption(new SelectOption().setValue(opt.getAttribute("value")));
                break;
            }
        }
    }

    private void selectOptionEqualsByXpath(String id, String search) {
        Locator select = page.locator("id=" + id);
        Locator opts = select.locator("option");
        int count = opts.count();
        for (int i = 0; i < count; i++) {
            Locator opt = opts.nth(i);
            if (opt.innerText().trim().equalsIgnoreCase(search)) {
                select.selectOption(new SelectOption().setValue(opt.getAttribute("value")));
                break;
            }
        }
    }

    private String dateEffet(int months) {
        LocalDate today = LocalDate.now();
        LocalDate nextMonth = today.plusMonths(months);
        return nextMonth.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private static String extractYear(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        String[] parts = dateStr.split("/");
        return parts.length == 3 ? parts[2] : null;
    }

    private String getPrixTtcParNiveau(int niveau) {
        String selecteur = String.format(sel("resultats.prix_selector_template"), niveau);
        Locator elementPrix = page.locator(selecteur);
        return elementPrix.innerText().trim().replaceAll("\\s+", " ");
    }
}
