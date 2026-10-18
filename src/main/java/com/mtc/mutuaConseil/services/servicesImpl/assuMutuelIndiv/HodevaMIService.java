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
            step("tarificateur", this::ouvrirTarificateur);
            step("assure", () -> remplirAssure(flux));
            step("beneficiaires", () -> remplirBeneficiaires(flux));
            step("tarifer", () -> elementLib.clickByRole(sel("tarification.obtenir_tarif_label")));
            page.waitForLoadState(LoadState.NETWORKIDLE);
            elementLib.randomWait(4000, 6000);
            currentStep = "lecture_resultats";
            List<String> couts = lirePrix(c);
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
        elementLib.humanTypeById(sel("connexion.username_id"), c.getUsername());
        elementLib.humanTypeById(sel("connexion.password_id"), c.getPassword());
        elementLib.randomWait(700, 1300);
        elementLib.clickByXpath(sel("connexion.submit_xpath"));
    }

    /** Le lien « SANTE SENIOR » s'ouvre dans un nouvel onglet : on bascule dessus. */
    private void ouvrirTarificateur() {
        elementLib.randomWait(1500, 2500);
        elementLib.clickAndSwitchToNewWindow(sel("navigation.sante_senior_selector"));
        this.page = elementLib.getPage();
        elementLib.randomWait(1500, 2500);
    }

    /** L'offre (#ff_elem529) se règle seule : SENIOR (plus de 50 ans) ou TOUT PUBLIC (moins de 50 ans). */
    private void remplirAssure(FluxData flux) {
        var assure = flux.getPersonnes().getFirst();
        elementLib.randomWait(1500, 2500);
        if (assure.getCivilite().equalsIgnoreCase("M") || assure.getCivilite().equalsIgnoreCase("Monsieur")) {
            elementLib.click(sel("tarification.civilite_monsieur_selector"));
        } else {
            elementLib.click(sel("tarification.civilite_madame_selector"));
        }
        elementLib.typeHumanLike(sel("tarification.nom_selector"), assure.getNom());
        elementLib.typeHumanLike(sel("tarification.prenom_selector"), assure.getPrenom());
        saisirDateNaissance("assure", assure.getDateNaissance());
        elementLib.typeHumanLike(sel("tarification.code_postal_selector"), assure.getCodePostal());
        page.locator(sel("tarification.assure_regime_selector")).selectOption(sel("tarification.regime_valeur"));
    }

    /**
     * Conjoint (Oui/Non) puis nombre d'enfants. Chaque choix affiche une section dédiée
     * (Conjoint, Enfant 1, Enfant 2). Seuls les 2 premiers enfants sont pris en compte par Hodeva
     * (gratuité à partir du 3ème).
     */
    private void remplirBeneficiaires(FluxData flux) {
        boolean aConjoint = flux.getPersonnes().size() >= 2;
        elementLib.randomWait(700, 1300);
        elementLib.click(aConjoint ? sel("tarification.conjoint_oui_selector") : sel("tarification.conjoint_non_selector"));
        if (aConjoint) {
            elementLib.randomWait(700, 1300);
            remplirBeneficiaire("conjoint", flux.getPersonnes().get(1).getDateNaissance());
        }

        List<String> naissancesEnfants = flux.getEnfants().stream()
                .filter(e -> e.getNom() != null && !e.getNom().isEmpty())
                .map(e -> e.getDateNaissance())
                .limit(2)
                .toList();
        page.locator(sel("tarification.nb_enfants_selector")).selectOption(String.valueOf(naissancesEnfants.size()));
        elementLib.randomWait(1000, 2000);
        for (int i = 0; i < naissancesEnfants.size(); i++) {
            remplirBeneficiaire("enfant" + (i + 1), naissancesEnfants.get(i));
        }
    }

    /** Date de naissance + régime de la section {@code prefixe} (conjoint, enfant1, enfant2). */
    private void remplirBeneficiaire(String prefixe, String dateNaissance) {
        saisirDateNaissance(prefixe, dateNaissance);
        page.locator(sel("tarification." + prefixe + "_regime_selector")).selectOption(sel("tarification.regime_valeur"));
    }

    /** Date jj/MM/aaaa répartie dans les 3 champs (JJ, MM, AAAA) de la section {@code prefixe}. */
    private void saisirDateNaissance(String prefixe, String dateNaissance) {
        String[] parts = dateNaissance.split("/");
        elementLib.randomWait(500, 1000);
        elementLib.typeHumanLike(sel("tarification." + prefixe + "_jour_selector"), parts[0]);
        elementLib.typeHumanLike(sel("tarification." + prefixe + "_mois_selector"), parts[1]);
        elementLib.typeHumanLike(sel("tarification." + prefixe + "_annee_selector"), parts[2]);
    }

    /**
     * Sélectionne le niveau du compte (clic sur « Niveau N ») puis relève les cotisations mensuelles
     * (hors renfort) de ce niveau jusqu'au dernier : 5 formules, 6 pour la gamme SENIOR.
     * Ex : niveau 5 -> prix du niveau 5 ; niveau 4 -> prix des niveaux 4 et 5.
     */
    private List<String> lirePrix(Compte compte) {
        elementLib.waitForElement(sel("resultats.attente_selector"), 30);
        int niveau = Math.max(compte.getNiveau(), 1);
        elementLib.click(String.format(sel("resultats.niveau_template"), niveau));
        elementLib.randomWait(700, 1300);

        List<String> prix = new java.util.ArrayList<>();
        int max = Integer.parseInt(sel("resultats.nb_formules_max"));
        for (int i = niveau; i <= max; i++) {
            Locator formule = page.locator(String.format(sel("resultats.prix_template"), i));
            if (formule.count() == 0) break;
            prix.add(formule.first().innerText().replace(' ', ' ').trim());
        }
        if (prix.isEmpty()) {
            log.warn("Aucun prix trouvé à partir du niveau {}", niveau);
        }
        return prix;
    }
}
