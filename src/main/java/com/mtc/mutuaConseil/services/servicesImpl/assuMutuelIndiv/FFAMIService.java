package com.mtc.mutuaConseil.services.servicesImpl.assuMutuelIndiv;

import com.microsoft.playwright.Frame;
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
import java.util.List;

@Service
public class FFAMIService extends BasePlaywrightService implements LaunchedService {

    private static final String PROVIDER = "mutuelIndiv/ffa-mi";
    private static final int FIND_TIMEOUT_MS = 20000;

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

    /** Le site Luxior est en frames : cherche le sélecteur dans toutes les frames jusqu'à l'avoir trouvé. */
    private Locator find(String selector) {
        long fin = System.currentTimeMillis() + FIND_TIMEOUT_MS;
        while (System.currentTimeMillis() < fin) {
            for (Frame frame : page.frames()) {
                try {
                    Locator l = frame.locator(selector);
                    if (l.count() > 0) {
                        return l.first();
                    }
                } catch (Exception ignored) {
                    // frame en cours de rechargement
                }
            }
            page.waitForTimeout(500);
        }
        throw new RuntimeException("Élément introuvable : " + selector);
    }

    private void clic(String selector) {
        find(selector).click();
    }

    private void saisirDate(String selector, String date) {
        Locator champ = find(selector);
        champ.click();
        champ.fill(date);
        champ.press("Tab");
    }

    /** Choisit l'option dont le libellé correspond au régime (sans tenir compte de la casse ni des accents). */
    private void choisirRegime(String selector, String regime) {
        String cible = normaliser(regime);
        for (int essai = 1; essai <= 4; essai++) {
            // La saisie de la date déclenche un script qui peut recréer le select : on le relocalise à chaque essai
            page.waitForTimeout(600);
            try {
                Locator select = find(selector);
                List<String> options = select.locator("option").allInnerTexts();
                String libelle = options.stream()
                        .filter(o -> !o.isBlank() && (normaliser(o).equals(cible) || normaliser(o).startsWith(cible)))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException(
                                "Régime '" + regime + "' introuvable pour " + selector + " (options : " + options + ")"));
                select.selectOption(new SelectOption().setLabel(libelle),
                        new Locator.SelectOptionOptions().setTimeout(5000));
                // On laisse un éventuel rechargement se produire, puis on vérifie que la sélection a tenu
                page.waitForTimeout(800);
                String choisi = find(selector).locator("option:checked").first().innerText();
                if (normaliser(choisi).equals(normaliser(libelle))) {
                    return;
                }
                log.warn("Régime '{}' perdu sur {} (essai {}), nouvelle tentative", libelle, selector, essai);
            } catch (RuntimeException e) {
                if (e.getMessage() != null && e.getMessage().startsWith("Régime '")) {
                    throw e;
                }
                log.warn("Sélection du régime sur {} échouée (essai {}) : {}", selector, essai, e.getMessage());
            }
        }
        throw new RuntimeException("Impossible de sélectionner le régime '" + regime + "' sur " + selector);
    }

    private String normaliser(String s) {
        if (s == null || s.isBlank()) {
            return "\u0000";
        }
        return java.text.Normalizer.normalize(s.trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase();
    }

    private boolean estSenior(FluxData flux) {
        return ageEn(flux.getPersonnes().getFirst().getDateNaissance()) >= Integer.parseInt(sel("navigation.age_senior"));
    }

    private void connexion(Compte c) {
        elementLib.randomWait(2000, 3000);
        elementLib.humanTypeById(sel("connexion.username_id"), c.getUsername());
        elementLib.humanTypeById(sel("connexion.password_id"), c.getPassword());
        elementLib.randomWait(700, 1300);
        elementLib.click(sel("connexion.submit_selector"));
    }

    /** SANTE > INDIVIDUEL > Santé particulier (< 56 ans) ou Santé sénior (>= 56 ans) > TARIFICATION */
    private void navigation(FluxData flux) {
        elementLib.randomWait(2000, 3000);
        clic(sel("navigation.sante_selector"));
        elementLib.randomWait(1500, 2500);
        String sousMenu = estSenior(flux) ? sel("navigation.sante_senior_selector") : sel("navigation.sante_particulier_selector");
        // INDIVIDUEL est déjà déplié par défaut ; on ne le clique que si le sous-menu est masqué
        if (!find(sousMenu).isVisible()) {
            clic(sel("navigation.individuel_selector"));
        }
        clic(sousMenu);
        elementLib.randomWait(1500, 2500);
        clic(sel("navigation.tarification_selector"));
        elementLib.randomWait(1500, 2500);
    }

    /** Date de naissance + régime Salarié pour chaque adhérent, puis code postal et date d'effet. */
    private void remplirAdherents(FluxData flux) {
        var personnes = flux.getPersonnes();
        saisirDate(sel("tarification.adherent_date_selector"), personnes.getFirst().getDateNaissance());
        choisirRegime(sel("tarification.adherent_regime_selector"), personnes.getFirst().getRegime());
        if (personnes.size() >= 2) {
            elementLib.randomWait(700, 1300);
            saisirDate(sel("tarification.conjoint_date_selector"), personnes.get(1).getDateNaissance());
            choisirRegime(sel("tarification.conjoint_regime_selector"), personnes.get(1).getRegime());
        }
        int index = 0;
        for (var enfant : flux.getEnfants()) {
            if (enfant.getNom() == null || enfant.getNom().isEmpty()) break;
            elementLib.randomWait(700, 1300);
            if (index > 0) {
                clic(String.format(sel("tarification.ajouter_enfant_template"), index));
            }
            saisirDate(String.format(sel("tarification.enfant_date_template"), index), enfant.getDateNaissance());
            index++;
        }
        find(sel("tarification.code_postal_selector")).fill(personnes.getFirst().getCodePostal());
        saisirDate(sel("tarification.date_effet_selector"), dateEffet(1));
    }

    private void tarifer() {
        elementLib.randomWait(700, 1300);
        clic(sel("tarification.tarifer_selector"));
    }

    /** Tarifs mensuels des formules du niveau demandé jusqu'à la 6 (ligne « Tarifs » standard). */
    private List<String> lirePrix(int niveau) {
        List<String> cellules = find(sel("resultats.ligne_tarifs_selector")).locator("td").allInnerTexts().stream()
                .map(t -> t.replace(' ', ' ').replaceAll("\\s+", " ").trim())
                .filter(t -> t.contains("€"))
                .toList();
        return cellules.subList(Math.min(Math.max(niveau - 1, 0), cellules.size()), cellules.size());
    }

    private String dateEffet(int mois) {
        return LocalDate.now().plusMonths(mois).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}
