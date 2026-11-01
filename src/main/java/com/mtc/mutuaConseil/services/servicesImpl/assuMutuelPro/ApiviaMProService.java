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
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.nonNull;

/**
 * Apivia - Mutuelle Pro (TNS).
 * Parcours : Nos offres > Santé individuelle > Nouvelle tarification > recueil de besoins/devoir de conseil
 * > code postal / date d'effet / assuré (régime TNS) / conjoint / enfants > Vitamin3-Pro > Obtenir un tarif
 * > lecture des niveaux Vitamin4 et Vitamin5.
 */
@Service
public class ApiviaMProService extends BasePlaywrightService implements LaunchedService {

    private static final String PROVIDER = "mutuelPro/apivia-mp";

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
            step("nouvelle_tarification", this::nouvelleTarification);
            step("devoir_de_conseils", this::remplirDevoirDeConseils);
            step("beneficiaires", () -> remplirBeneficiaires(flux));
            step("couverture", this::choisirCouverture);
            step("calcul", this::calculer);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            elementLib.randomWait(4000, 6000);
            currentStep = "lecture_resultats";
            List<String> couts = getPrixParNiveaux();
            log.info("couts {}", couts);
            tarif.setMontant(couts);
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

    private void connexion(Compte c) {
        humanLikeNavigate(c.getUrlFournisseur());
        elementLib.randomWait(700, 1300);
        elementLib.humanTypeById(sel("connexion.username_id"), c.getUsername());
        elementLib.humanTypeById(sel("connexion.password_id"), c.getPassword());
        elementLib.clickByRole(sel("connexion.bouton_connexion_label"));
        elementLib.randomWait(2500, 3500);
    }

    private void nouvelleTarification() {
        elementLib.clickByTextElement(sel("navigation.nos_offres_text"));
        elementLib.randomWait(700, 1300);
        elementLib.humanClick(sel("navigation.sante_individuelle_xpath"));
        elementLib.randomWait(700, 1300);
    }

    private void remplirDevoirDeConseils() {
        elementLib.clickById(sel("devoir_conseil.checkbox_id"));
        elementLib.clickByXpath(sel("devoir_conseil.switch_label_xpath"));
        elementLib.randomWait(700, 1300);
    }

    private void remplirBeneficiaires(FluxData flux) {
        elementLib.humanTypeById(sel("beneficiaires.code_postal_id"), flux.getPersonnes().getFirst().getCodePostal());
        elementLib.humanTypeById(sel("beneficiaires.date_effet_id"), dateEffet(1));
        clickBody();
        remplirAssure(flux, 0);
        if (flux.getPersonnes().size() == 2) {
            remplirAssure(flux, 1);
        }
        remplirEnfants(flux);
    }

    private void remplirAssure(FluxData flux, int index) {
        elementLib.randomWait(700, 1300);
        String annee = extractYear(flux.getPersonnes().get(index).getDateNaissance());
        selectOptionEquals(index == 0 ? sel("beneficiaires.assure_date_naissance_id") : sel("beneficiaires.conjoint_date_naissance_id"), annee);
        elementLib.randomWait(700, 1300);
        selectOptionEquals(index == 0 ? sel("beneficiaires.assure_regime_id") : sel("beneficiaires.conjoint_regime_id"), sel("beneficiaires.regime_tns_valeur"));
    }

    private void remplirEnfants(FluxData flux) {
        if (flux.getEnfants().isEmpty() || flux.getEnfants().getFirst().getNom() == null
                || flux.getEnfants().getFirst().getNom().isBlank()) {
            return;
        }
        for (int i = 0; i < Math.min(flux.getEnfants().size(), 2); i++) {
            elementLib.randomWait(700, 1300);
            elementLib.clickByRole(sel("beneficiaires.ajouter_label"));
            selectOptionEqualsById(sel("beneficiaires.enfant" + i + "_date_naissance_id"), extractYear(flux.getEnfants().get(i).getDateNaissance()));
            elementLib.randomWait(700, 1300);
            selectOptionEqualsById(sel("beneficiaires.enfant" + i + "_regime_id"), sel("beneficiaires.regime_tns_valeur"));
        }
    }

    private void choisirCouverture() {
        scrollDown(300);
        elementLib.click(sel("couverture.vitamin3_pro_xpath"));
    }

    private void calculer() {
        elementLib.randomWait(700, 1300);
        elementLib.clickById(sel("calcul.bouton_id"));
    }

    private List<String> getPrixParNiveaux() {
        List<String> prix = new ArrayList<>();
        for (String niveau : sel("resultats.niveaux").split(",")) {
            String selecteur = String.format(sel("resultats.prix_selector_template"), Integer.parseInt(niveau.trim()));
            String cout = page.locator(selecteur).innerText().trim().replaceAll("\\s+", " ");
            prix.add(cout.substring(0, Math.min(8, cout.length())));
        }
        return prix;
    }

    private void selectOptionEquals(String selectId, String search) {
        selectOption(page.locator("#" + selectId), search);
    }

    private void selectOptionEqualsById(String id, String search) {
        selectOption(page.locator("id=" + id), search);
    }

    private void selectOption(Locator select, String search) {
        Locator opts = select.locator("option");
        int count = opts.count();
        for (int i = 0; i < count; i++) {
            Locator opt = opts.nth(i);
            if (opt.innerText().trim().equalsIgnoreCase(search)) {
                select.selectOption(new SelectOption().setValue(opt.getAttribute("value")));
                return;
            }
        }
        log.warn("Option '{}' introuvable dans le select", search);
    }

    private String dateEffet(int months) {
        return LocalDate.now().plusMonths(months).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private static String extractYear(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        String[] parts = dateStr.split("/");
        return parts.length == 3 ? parts[2] : null;
    }
}
