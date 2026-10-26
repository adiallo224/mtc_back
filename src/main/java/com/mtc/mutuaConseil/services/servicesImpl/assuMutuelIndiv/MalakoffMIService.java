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
            step("etape_suivante", this::etapeSuivante);
            step("commission", this::reglerCommission);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            elementLib.randomWait(4000, 6000);
            currentStep = "lecture_resultats";
            List<String> couts = lirePrix(c.getNiveau());
            log.info("couts (niveau {}) {}", c.getNiveau(), couts);
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
        elementLib.click(sel("connexion.submit_selector"));
    }

    /** Survol du menu Tarification (le sous-menu apparaît), Tarification express, puis ATOUT+ PARTICULIER. */
    private void navigation() {
        elementLib.randomWait(2000, 3000);
        elementLib.hover(page.locator(sel("navigation.menu_tarification_selector")).first());
        elementLib.randomWait(500, 1000);
        elementLib.click(sel("navigation.tarification_express_selector"));
        page.waitForLoadState(LoadState.NETWORKIDLE);
        elementLib.click(sel("navigation.atout_plus_particulier_selector"));
        elementLib.randomWait(1500, 2500);
    }

    /** Date d'effet, date de naissance, régime général, code postal, puis conjoint et enfants. */
    private void remplirContrat(FluxData flux) {
        var personnes = flux.getPersonnes();
        saisirDate(sel("tarification.date_effet_selector"), dateEffet(1));
        saisirDate(sel("tarification.date_naissance_selector"), personnes.getFirst().getDateNaissance());
        page.locator(sel("tarification.code_postal_selector")).fill(personnes.getFirst().getCodePostal());
        String regimeAdherent = personnes.getFirst().getRegime();
        choisirDansMenu(sel("tarification.regime_id"), regimeAdherent);
        int index = 0;
        if (personnes.size() >= 2) {
            ajouterBeneficiaire(index++, sel("tarification.lien_conjoint"), personnes.get(1).getDateNaissance(),
                    personnes.get(1).getRegime());
        }
        // Un enfant n'a pas de régime propre : on reprend celui de l'adhérent
        for (var enfant : flux.getEnfants()) {
            if (enfant.getNom() == null || enfant.getNom().isEmpty()) break;
            ajouterBeneficiaire(index++, sel("tarification.lien_enfant"), enfant.getDateNaissance(), regimeAdherent);
        }
    }

    /** Ajoute un bénéficiaire (index 0-based) : lien de parenté, régime et date de naissance. */
    private void ajouterBeneficiaire(int index, String lien, String dateNaissance, String regime) {
        elementLib.randomWait(700, 1300);
        elementLib.click(sel("tarification.ajouter_beneficiaire_selector"));
        String lienId = String.format(sel("tarification.lien_parente_id_template"), index);
        page.locator("#" + lienId + "-button").waitFor();
        choisirDansMenu(lienId, lien);
        choisirDansMenu(String.format(sel("tarification.regime_beneficiaire_id_template"), index), regime);
        saisirDate(String.format(sel("tarification.date_naissance_beneficiaire_template"), index), dateNaissance);
    }

    private void etapeSuivante() {
        elementLib.randomWait(700, 1300);
        elementLib.click(sel("tarification.etape_suivante_selector"));
        page.waitForLoadState(LoadState.NETWORKIDLE);
        elementLib.randomWait(2000, 3000);
    }

    /** Commission LINEAIRE 12 %, coefficient de financement ramené à « 1 », puis REINITIALISER. */
    private void reglerCommission() {
        choisirDansMenu(sel("commission.select_id"), sel("commission.valeur"));
        elementLib.randomWait(700, 1300);
        Locator coef = page.locator(sel("commission.coefficient_selector")).first();
        if (!coef.isVisible()) {
            elementLib.click(sel("commission.cagnotte_toggle_selector"));
            elementLib.randomWait(500, 1000);
        }
        coef.clear();
        coef.fill(sel("commission.coefficient_valeur"));
        coef.press("Tab");
        elementLib.click(sel("commission.reinitialiser_selector"));
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    /**
     * Choisit une option dans un select masqué habillé par jQuery UI (« #id-button » + « #id-menu »).
     * Si le menu ne répond pas, on positionne la valeur du select et on déclenche « change ».
     */
    private void choisirDansMenu(String selectId, String libelle) {
        try {
            page.locator("#" + selectId + "-button").click();
            page.locator("#" + selectId + "-menu li:has-text(\"" + libelle + "\")").first().click();
        } catch (Exception e) {
            log.warn("Menu '{}' non cliquable, sélection directe de '{}'", selectId, libelle);
            page.locator("#" + selectId).evaluate(
                    "(s, l) => { const o = [...s.options].find(o => o.text.includes(l));"
                            + " if (!o) throw new Error('Option introuvable : ' + l);"
                            + " s.value = o.value; s.dispatchEvent(new Event('change', { bubbles: true })); }",
                    libelle);
        }
    }

    private void saisirDate(String selector, String date) {
        Locator champ = page.locator(selector);
        champ.click();
        champ.fill(date);
        champ.press("Tab");
    }

    /** Tarifs mensuels (hors option) des formules affichées, à partir du niveau demandé. */
    private List<String> lirePrix(int niveau) {
        List<String> prix = page.locator(sel("resultats.prix_selector")).allInnerTexts().stream()
                .map(t -> t.replace(' ', ' ').replace("EUR", "€").replaceAll("\\s+", " ").trim())
                .filter(t -> t.contains("€"))
                .toList();
        return prix.subList(Math.min(Math.max(niveau - 1, 0), prix.size()), prix.size());
    }

    private String dateEffet(int mois) {
        return LocalDate.now().plusMonths(mois).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}
