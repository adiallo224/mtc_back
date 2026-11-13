package com.mtc.mutuaConseil.services.servicesImpl.assuMutuelIndiv;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class HennerMlService extends BasePlaywrightService implements LaunchedService {

    private static final String PROVIDER = "mutuelIndiv/henner-mi";

    private final Logger log = LoggerFactory.getLogger(HennerMlService.class);
    private final TypeAssuranceService typeAssuranceService;
    private final SelectorStore selectors;
    private String currentStep = "démarrage";

    public HennerMlService(TypeAssuranceService typeAssuranceService, SelectorStore selectors) {
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
        log.info("Début de traitement -- Henner_Mutuel_Indiv (Playwright)");

        Tarif tarif = TarifUtils.createDefaultTarif(c, typeAssuranceService, 2L);
        tarif.setNom(c.getNomFournisseur());
        TypeAssurance typeAssurance = new TypeAssurance();
        typeAssurance.setTypeAssurance(EnumTypeAssurance.MUTUELLE_INDIV);
        tarif.setTypeAssurance(typeAssurance);

        try {
            initializeBrowser(false);
            humanLikeNavigate(c.getUrlFournisseur());
            step("connexion", () -> connexion(c));
            step("creation_devis", this::remplirCreationDevis);
            step("choix_devis", this::remplirChoixDevis);
            step("contrat", () -> remplirContrat(flux));
            step("sante", this::remplirSante);
            step("devis_sante", () -> remplirDevisSante(flux));
            step("calcul", this::suivant);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            elementLib.randomWait(8000, 12000);
            currentStep = "lecture_resultats";
            String cout = getPrixByFormule();
            log.info("cout {}", cout);
            tarif.setMontant(List.of(cout));
            elementLib.randomWait(1000, 3000);
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
        elementLib.randomWait(700, 1300);
        elementLib.clickByRole(sel("connexion.accept_cookies_label"));
        elementLib.humanTypeById(sel("connexion.username_id"), c.getUsername());
        elementLib.humanTypeById(sel("connexion.password_id"), c.getPassword());
        elementLib.randomWait(700, 1300);
        elementLib.clickByTextElement(sel("connexion.connexion_text"));
        elementLib.randomWait(700, 1300);
    }

    private void remplirCreationDevis() {
        elementLib.click(sel("navigation.creation_devis_selector"));
    }

    private void remplirChoixDevis() {
        elementLib.clickByXpath(sel("navigation.particulier_xpath"));
    }

    private void remplirContrat(FluxData flux) {
        elementLib.randomWait(700, 1300);
        elementLib.humanTypeByXpath(sel("contrat.date_naissance_xpath"), flux.getPersonnes().getFirst().getDateNaissance());
        clickBody();
        elementLib.randomWait(700, 1300);
        elementLib.typeByLabel(sel("contrat.code_postal_label"), flux.getPersonnes().getFirst().getCodePostal());
        elementLib.randomWait(700, 1300);
        elementLib.typeByLabel(sel("contrat.nom_label"), flux.getPersonnes().getFirst().getNom());
        elementLib.randomWait(700, 1300);
        elementLib.typeByLabel(sel("contrat.prenom_label"), flux.getPersonnes().getFirst().getPrenom());
        elementLib.randomWait(700, 1300);
        elementLib.clickByRole(sel("contrat.valider_label"));
    }

    private void remplirSante() {
        elementLib.randomWait(700, 1300);
        elementLib.clickByTextElement(sel("navigation.sante_text"));
    }

    private void remplirDevisSante(FluxData flux) {
        elementLib.humanTypeByXpath(sel("devis_sante.date_effet_xpath"), dateEffet(1));
        elementLib.randomWait(700, 1300);
        clickBody();
        choixRegime(sel("devis_sante.regime_assure_xpath"));
        if (flux.getPersonnes().size() >= 2) {
            remplirConjoint(flux);
            scrollDown(100);
        }
        if (flux.getEnfants().getFirst().getNom() != null && !flux.getEnfants().getFirst().getNom().isEmpty()) {
            remplirEnfant(flux,
                sel("devis_sante.enfant1_date_naissance_xpath"),
                sel("devis_sante.enfant1_regime_xpath"),
                0);
            scrollDown(100);
        }
        if (flux.getEnfants().size() >= 2) {
            remplirEnfant(flux,
                sel("devis_sante.enfant2_date_naissance_xpath"),
                sel("devis_sante.enfant2_regime_xpath"),
                1);
            scrollDown(250);
        }
    }

    private void remplirConjoint(FluxData flux) {
        ajoutConjoint();
        elementLib.randomWait(700, 1300);
        elementLib.humanTypeByXpath(
            sel("devis_sante.conjoint_date_naissance_xpath"),
            flux.getPersonnes().get(1).getDateNaissance());
        elementLib.randomWait(700, 1300);
        clickBody();
        choixRegime(sel("devis_sante.conjoint_regime_xpath"));
    }

    private void remplirEnfant(FluxData flux, String xpathDateNaissance, String xpathRegime, int index) {
        ajoutEnfant();
        elementLib.humanTypeByXpath(xpathDateNaissance, flux.getEnfants().get(index).getDateNaissance());
        elementLib.randomWait(700, 1300);
        clickBody();
        choixRegime(xpathRegime);
    }

    private void choixRegime(String xpath) {
        elementLib.randomWait(700, 1300);
        elementLib.clickByXpath(xpath);
        elementLib.randomWait(700, 1300);
        Locator options = page.locator("xpath=" + sel("devis_sante.regime_options_xpath"));
        for (int i = 0; i < options.count(); i++) {
            if (options.nth(i).textContent().trim().equalsIgnoreCase(sel("devis_sante.regime_valeur"))) {
                options.nth(i).click();
                break;
            }
        }
    }

    private void ajoutConjoint() {
        elementLib.randomWait(700, 1300);
        elementLib.clickByTextElement(sel("devis_sante.ajouter_conjoint_text"));
    }

    private void ajoutEnfant() {
        elementLib.randomWait(700, 1300);
        elementLib.clickByTextElement(sel("devis_sante.ajouter_enfant_text"));
    }

    private void suivant() {
        elementLib.randomWait(700, 1300);
        elementLib.clickByTextElement(sel("navigation.tarifer_text"));
    }

    private String dateEffet(int mois) {
        return LocalDate.now()
                .plus(mois, ChronoUnit.MONTHS)
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public String getPrixFormuleActive() {
        return page.locator(
                        sel("resultats.formule_active_css"))
                .locator("xpath=" + sel("resultats.formule_active_ancestor_xpath"))
                .locator(sel("resultats.formule_active_montant_css"))
                .textContent()
                .trim();
    }

    public String getPrixByFormule() {
        Locator label = page.locator(
                sel("resultats.formule_label_css"),
                new Page.LocatorOptions().setHasText(sel("resultats.formule_recherchee_texte"))
        );

        String texte = label.first().textContent().trim();
        Matcher matcher = Pattern.compile("\\(([\\d.,]+)\\s*€\\)").matcher(texte);
        if (matcher.find()) {
            return matcher.group(1) + "€";
        }

        log.warn("Montant introuvable dans le libellé de formule : '{}'", texte);
        return texte;
    }

}
