package com.mtc.mutuaConseil.services.servicesImpl.assuMutuelPro;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.LoadState;
import com.mtc.mutuaConseil.base.BasePlaywrightService;
import com.mtc.mutuaConseil.models.Compte;
import com.mtc.mutuaConseil.models.FluxData;
import com.mtc.mutuaConseil.models.Personne;
import com.mtc.mutuaConseil.models.Tarif;
import com.mtc.mutuaConseil.models.TypeAssurance;
import com.mtc.mutuaConseil.models.enums.EnumTypeAssurance;
import com.mtc.mutuaConseil.selectors.SelectorStore;
import com.mtc.mutuaConseil.services.LaunchedService;
import com.mtc.mutuaConseil.services.servicesImpl.TypeAssuranceService;
import com.mtc.mutuaConseil.utils.Shared;
import com.mtc.mutuaConseil.utils.TarifUtils;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.Objects.nonNull;

@Service
public class EntoriaMutuelProService extends BasePlaywrightService implements LaunchedService {

    private static final String PROVIDER = "mutuelPro/entoria-mp";

    private final TypeAssuranceService typeAssuranceService;
    private final SelectorStore selectors;
    private String currentStep = "démarrage";

    public EntoriaMutuelProService(TypeAssuranceService typeAssuranceService, SelectorStore selectors) {
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
        log.info("Début de traitement -- Entoria_Mutuel_Pro (Playwright)");
        Tarif tarif = TarifUtils.createDefaultTarif(c, typeAssuranceService, 3L);
        tarif.setNom(c.getNomFournisseur());
        TypeAssurance typeAssurance = new TypeAssurance();
        typeAssurance.setTypeAssurance(EnumTypeAssurance.MUTUELLE_PRO);
        tarif.setTypeAssurance(typeAssurance);

        try {
            initializeBrowser(false);
            step("connexion", () -> connexion(c));
            step("navigation", this::ouvrirTarificateur);
            step("contrat", () -> remplirContrat(flux));
//            step("famille", () -> remplirFamille(flux));
//            step("proposition", this::attendreResultats);
            currentStep = "lecture_resultats";
            tarif.setMontant(getPrixParNiveau());
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

    // ==================== ÉTAPES ====================

    private void connexion(Compte c) {
        humanLikeNavigate(c.getUrlFournisseur());
        elementLib.humanTypeById(sel("connexion.username_id"), c.getUsername());
        elementLib.humanTypeById(sel("connexion.password_id"), c.getPassword());
        elementLib.randomWait(300, 1000);
        elementLib.clickById(sel("connexion.submit_id"));
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    private void ouvrirTarificateur() {
        elementLib.randomWait(600, 1300);
        elementLib.humanClick("xpath=" + sel("navigation.offres_xpath"));
        elementLib.humanClick("xpath=" + sel("navigation.sante_tns_tarifer_xpath"));
    }

    private void remplirContrat(FluxData flux) {
        Personne assure = flux.getPersonnes().getFirst();
        elementLib.randomWait(600, 1300);
        elementLib.typeHumanLike(sel("contrat.date_naissance_selector"), assure.getDateNaissance());
        choisirProfession(assure.getProfession());
        choisirDansListe(sel("contrat.statut_selector"), sel("contrat.statut_valeur"), true);
        choisirDansListe(sel("contrat.regime_selector"), sel("contrat.regime_valeur"), true);
        choisirDansListe(sel("contrat.departement_selector"),
                Shared.getDepartementFromCodePostal(assure.getCodePostal()), false);
        // Client assuré en prévoyance : NON (question optionnelle selon le profil)
        String prevoyanceNon = "xpath=" + sel("contrat.prevoyance_non_xpath");
        if (elementLib.isElementPresent(prevoyanceNon)) {
            elementLib.humanClick(prevoyanceNon);
        }
        elementLib.humanClick("xpath=" + sel("contrat.tarifer_xpath"));
    }

    /** Conjoint et/ou enfants : on indique seulement leur présence et le nombre d'enfants (pas de dates). */
    private void remplirFamille(FluxData flux) {
        elementLib.randomWait(600, 1300);
        if (flux.getPersonnes().size() > 1) {
            elementLib.humanClick(sel("famille.conjoint_selector"));
        }
        long nbEnfants = flux.getEnfants() == null ? 0 : flux.getEnfants().stream()
                .filter(e -> nonNull(e.getNom()) && !e.getNom().isBlank())
                .count();
        if (nbEnfants > 0) {
            elementLib.humanClick(sel("famille.enfants_selector"));
            for (long i = 0; i < nbEnfants; i++) {
                elementLib.humanClick("xpath=" + sel("famille.enfant_plus_xpath"));
            }
        }
    }

    private void attendreResultats() {
        elementLib.waitForElement("xpath=" + sel("resultats.section_xpath"), 25);
        page.waitForLoadState(LoadState.NETWORKIDLE);
        elementLib.randomWait(1000, 3000);
    }

    // ==================== OUTILS ====================

    /** Ouvre la liste d'autocomplétion Material de l'input {@code inputSelector} et clique l'option voulue. */
    private void choisirDansListe(String inputSelector, String valeur, boolean exactSeulement) {
        if (!ouvrirEtChoisir(inputSelector, valeur, exactSeulement)) {
            throw new IllegalStateException("Option '" + valeur + "' introuvable dans la liste " + inputSelector);
        }
    }

    /**
     * Profession : si elle n'est pas dans la liste, on clique sur « Vous n'avez pas trouvé la profession ? »
     * puis on la renseigne manuellement.
     */
    private void choisirProfession(String profession) {
        if (profession == null || profession.isBlank()) {
            throw new IllegalArgumentException("Profession non renseignée");
        }
        if (ouvrirEtChoisir(sel("contrat.profession_selector"), profession, false)) {
            return;
        }
        log.warn("Profession '{}' absente de la liste : saisie via « profession introuvable »", profession);
        elementLib.humanClick(sel("contrat.profession_introuvable_selector"));
        String champ = sel("contrat.profession_introuvable_input_selector");
        elementLib.typeHumanLike(champ, profession);
        elementLib.pressEnter();
        String valider = sel("contrat.profession_introuvable_valider_selector");
        if (elementLib.isElementVisible(valider)) {
            elementLib.humanClick(valider);
        }
    }

    /**
     * Ouvre la liste d'autocomplétion de {@code inputSelector} et clique l'option voulue :
     * correspondance exacte d'abord, puis « contient » sauf si {@code exactSeulement}.
     * Retourne false si aucune option ne correspond (la liste reste ouverte).
     */
    private boolean ouvrirEtChoisir(String inputSelector, String valeur, boolean exactSeulement) {
        elementLib.randomWait(300, 800);
        elementLib.humanClick(inputSelector);
        String optionsXpath = "xpath=" + sel("contrat.options_xpath");
        elementLib.waitForElement(optionsXpath, 10);
        Locator options = page.locator(optionsXpath);

        // Comparaison côté Java (un Pattern.quote "\Q..\E" n'est pas valide en JavaScript côté Playwright).
        List<String> textes = options.allInnerTexts();
        String recherche = valeur.trim().replaceAll("\\s+", " ");
        int index = -1;
        for (int i = 0; i < textes.size() && index < 0; i++) {
            if (textes.get(i).trim().replaceAll("\\s+", " ").equalsIgnoreCase(recherche)) {
                index = i;
            }
        }
        if (index < 0 && !exactSeulement) {
            for (int i = 0; i < textes.size() && index < 0; i++) {
                if (textes.get(i).toLowerCase().contains(recherche.toLowerCase())) {
                    index = i;
                }
            }
        }
        if (index < 0) {
            log.warn("Option '{}' introuvable parmi {} options", valeur, textes.size());
            return false;
        }
        Locator option = options.nth(index);
        option.scrollIntoViewIfNeeded();
        elementLib.randomWait(200, 600);
        option.click();
        return true;
    }

    /** Prix des niveaux retenus (Confort, Sérénité, Excellence), dans cet ordre. */
    private List<String> getPrixParNiveau() {
        List<String> prix = prixParLibelle(sel("resultats.ligne_xpath"), sel("resultats.niveaux"));
        log.info("Coûts Entoria : {}", prix);
        if (prix.isEmpty()) {
            throw new IllegalStateException("Aucun prix trouvé pour les niveaux " + sel("resultats.niveaux"));
        }
        return prix;
    }
}
