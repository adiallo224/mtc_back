package com.mtc.mutuaConseil.services.servicesImpl.assuMutuelPro;

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
public class FFAMProService extends BasePlaywrightService implements LaunchedService {

    private static final String PROVIDER = "mutuelPro/ffa-mp";
    private static final int FIND_TIMEOUT_MS = 20000;

    private final TypeAssuranceService typeAssuranceService;
    private final SelectorStore selectors;
    private String currentStep = "démarrage";

    public FFAMProService(TypeAssuranceService typeAssuranceService, SelectorStore selectors) {
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
        log.info("Début de traitement -- Ffa_Mutuelle_Pro (Playwright)");

        Tarif tarif = TarifUtils.createDefaultTarif(c, typeAssuranceService, 3L);
        tarif.setNom(c.getNomFournisseur());
        TypeAssurance typeAssurance = new TypeAssurance();
        typeAssurance.setTypeAssurance(EnumTypeAssurance.MUTUELLE_PRO);
        tarif.setTypeAssurance(typeAssurance);

        try {
            initializeBrowser(false);
            humanLikeNavigate(c.getUrlFournisseur());
            step("connexion", () -> connexion(c));
            step("navigation", this::navigation);
            step("adherents", () -> remplirAdherents(flux));
            step("tarifer", this::tarifer);
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

    private void connexion(Compte c) {
        elementLib.randomWait(2000, 3000);
        elementLib.humanTypeById(sel("connexion.username_id"), c.getUsername());
        elementLib.humanTypeById(sel("connexion.password_id"), c.getPassword());
        elementLib.randomWait(700, 1300);
        elementLib.click(sel("connexion.submit_selector"));
    }

    /** Bandeau « ACCES FFA SANTE & PREVOYANCE » > T.N.S > SANTE > TARIFICATION */
    private void navigation() {
        elementLib.randomWait(2000, 3000);
        clic(sel("navigation.sante_selector"));
        elementLib.randomWait(1500, 2500);
        // Il faut déplier le groupe T.N.S avant de pouvoir cliquer sur son sous-menu
        clic(sel("navigation.tns_selector"));
        elementLib.randomWait(1000, 2000);
        clic(sel("navigation.sante_tns_selector"));
        elementLib.randomWait(1500, 2500);
        clic(sel("navigation.tarification_selector"));
        elementLib.randomWait(1500, 2500);
    }

    /** Date de naissance + régime Salarié pour chaque adhérent, puis code postal et date d'effet. */
    private void remplirAdherents(FluxData flux) {
        var personnes = flux.getPersonnes();
        saisirDate(sel("tarification.adherent_date_selector"), personnes.getFirst().getDateNaissance());
        // Mutuelle Pro : l'adhérent est toujours TNS (option « TNS En Activité »)
        choisirRegime(sel("tarification.adherent_regime_selector"), sel("tarification.regime_tns_libelle"));
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
        elementLib.randomWait(1300, 2300);
        clic(sel("tarification.tarifer_selector"));
    }

    /** Tarif mensuel de la formule à retenir (A6 = 6e formule de la ligne « Tarifs » standard). */
    private List<String> lirePrix() {
        List<String> cellules = find(sel("resultats.ligne_tarifs_selector")).locator("td").allInnerTexts().stream()
                .map(t -> t.replace(' ', ' ').replaceAll("\\s+", " ").trim())
                .filter(t -> t.contains("€"))
                .toList();
        int formule = Integer.parseInt(sel("resultats.formule_retenue"));
        if (cellules.size() < formule) {
            throw new RuntimeException("Formule " + formule + " introuvable (tarifs lus : " + cellules + ")");
        }
        return List.of(cellules.get(formule - 1));
    }

    private String dateEffet(int mois) {
        return LocalDate.now().plusMonths(mois).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}
