package com.mtc.mutuaConseil.services.servicesImpl.assuMutuelPro;

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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.Objects.nonNull;

@Service
public class SmisoMProService extends BasePlaywrightService implements LaunchedService {

    private static final String PROVIDER = "mutuelPro/smiso-mp";

    private final TypeAssuranceService typeAssuranceService;
    private final SelectorStore selectors;
    private String currentStep = "démarrage";

    public SmisoMProService(TypeAssuranceService typeAssuranceService, SelectorStore selectors) {
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
        log.info("Début de traitement -- Smiso_Mutuel_Pro (Playwright)");
        Tarif tarif = TarifUtils.createDefaultTarif(c, typeAssuranceService, 3L);
        tarif.setNom(c.getNomFournisseur());
        TypeAssurance typeAssurance = new TypeAssurance();
        typeAssurance.setTypeAssurance(EnumTypeAssurance.MUTUELLE_PRO);
        tarif.setTypeAssurance(typeAssurance);

        try {
            initializeBrowser(false);
            step("connexion", () -> connexion(c));
            step("nouveau_projet", this::nouveauProjet);
            step("beneficiaires", () -> remplirBenficiaires(flux));
            step("couverture", this::remplirCouverture);
            step("contact", () -> remplirInformationsDeContact(flux));
            step("besoins", this::remplirBesoins);
            step("proposition", this::remplirProposition);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            elementLib.randomWait(1000, 3000);
            currentStep = "lecture_resultats";
            List<String> couts = getPrixParNiveau(c.getNiveau());
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
        clickIfExists(sel("connexion.cookie_accept_xpath"));
        elementLib.humanTypeByXpath(sel("connexion.username_xpath"), c.getUsername());
        elementLib.humanTypeById(sel("connexion.password_id"), c.getPassword());
        elementLib.randomWait(300, 1000);
        elementLib.clickById(sel("connexion.submit_id"));
        elementLib.randomWait(300, 1000);
    }

    private void nouveauProjet() {
        elementLib.humanClick(sel("navigation.nouveau_projet_selector"));
        elementLib.humanClick(sel("navigation.particulier_tns_selector"));
    }

    private void remplirBenficiaires(FluxData flux) {
        elementLib.randomWait(600, 1300);
        elementLib.humanTypeById(sel("beneficiaires.postal_code_id"), flux.getPersonnes().getFirst().getCodePostal());
        elementLib.humanTypeByXpath(sel("beneficiaires.date_naissance_xpath"), flux.getPersonnes().getFirst().getDateNaissance());
        elementLib.humanClick(sel("beneficiaires.regime_souscripteur_xpath"));
        elementLib.randomWait(600, 1300);
        elementLib.humanClick(sel("beneficiaires.regime_option_xpath"));
        elementLib.clickByTextElement(sel("beneficiaires.tns_text"));
        if (flux.getPersonnes().size() > 1) {
            elementLib.humanClick(sel("beneficiaires.conjoint_toggle_selector"));
            elementLib.humanTypeByXpath(sel("beneficiaires.conjoint_date_naissance_xpath"), flux.getPersonnes().get(1).getDateNaissance());
            elementLib.humanClick(sel("beneficiaires.conjoint_regime_xpath"));
            elementLib.randomWait(600, 1300);
            elementLib.humanClick(sel("beneficiaires.regime_option_xpath"));
        }
        if (!flux.getEnfants().getFirst().getNom().isEmpty() && !flux.getEnfants().getFirst().getNom().isBlank()) {
            elementLib.humanClick(sel("beneficiaires.enfants_toggle_selector"));
            elementLib.humanTypeByXpath(sel("beneficiaires.enfant1_date_naissance_xpath"), flux.getEnfants().getFirst().getDateNaissance());
            elementLib.humanClick(sel("beneficiaires.enfant1_regime_xpath"));
            elementLib.randomWait(600, 1300);
            elementLib.humanClick(sel("beneficiaires.regime_option_xpath"));
            if (flux.getEnfants().size() >= 2) {
                elementLib.humanClick(sel("beneficiaires.ajouter_enfant_xpath"));
                elementLib.humanTypeById(sel("beneficiaires.enfant2_date_naissance_xpath"), flux.getEnfants().get(1).getDateNaissance());
                elementLib.humanClick(sel("beneficiaires.enfant2_regime_xpath"));
                elementLib.randomWait(600, 1300);
                elementLib.humanClick(sel("beneficiaires.regime_option_xpath"));
            }
        }

        elementLib.click(sel("beneficiaires.valider_xpath"));
    }

    private void remplirCouverture() {
        elementLib.randomWait(600, 1300);
        elementLib.clickByXpath(sel("couverture.generation_xpath"));
        elementLib.humanClick(sel("couverture.creer_devis_xpath"));
    }

    private void remplirInformationsDeContact(FluxData flux) {
        elementLib.randomWait(600, 1300);
        choixCivilite(flux, 0);
        elementLib.humanTypeById(sel("contact.lastname_id"), flux.getPersonnes().getFirst().getNom());
        elementLib.humanTypeById(sel("contact.firstname_id"), flux.getPersonnes().getFirst().getPrenom());
        elementLib.humanTypeByXpath(sel("contact.phone_xpath"), flux.getPersonnes().getFirst().getTelephone());
        elementLib.humanTypeById(sel("contact.email_id"), flux.getPersonnes().getFirst().getEmail());
        elementLib.humanClick(sel("contact.valider_xpath"));
    }

    private void remplirBesoins() {
        elementLib.randomWait(600, 1300);
        Locator cards = page.locator(sel("besoins.cartes_css"));

        int count = cards.count();

        for (int i = 0; i < count; i++) {

            Locator card = cards.nth(i);

            Locator equilibre = card.locator(sel("besoins.equilibre_selector"));

            if (equilibre.count() > 0) {
                elementLib.randomWait(200, 1000);
                equilibre.click();
            }
        }
        elementLib.humanClick(sel("besoins.valider_xpath"));
    }

    private void remplirProposition() {
        elementLib.randomWait(600, 1300);
        elementLib.humanClick(sel("proposition.ajouter_solution_xpath"));
    }

    private void choixCivilite(FluxData flux, int index) {
        elementLib.randomWait(600, 1300);
        if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M"))
            elementLib.humanClick(sel("contact.civilite_monsieur_xpath"));
        if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme"))
            elementLib.humanClick(sel("contact.civilite_madame_xpath"));
    }

    private List<String> getPrixParNiveau(int niveau) {
        Locator cartes = page.locator(sel("resultats.cartes_css"))
                .filter(new Locator.FilterOptions()
                        .setHasText(Pattern.compile(sel("resultats.formule_pattern"))));
        List<String> prix = new ArrayList<>();

        for (int i = niveau; i < cartes.count(); i++) {
            String montant = cartes
                    .nth(i)
                    .locator("code span")
                    .first()
                    .innerText()
                    .trim();

            prix.add(montant + " €");
        }
        return prix;
    }

}
