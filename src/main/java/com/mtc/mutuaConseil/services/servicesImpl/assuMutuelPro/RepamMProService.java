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
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Actions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.nonNull;

@Service
public class RepamMProService extends BasePlaywrightService implements LaunchedService {

    private static final String PROVIDER = "mutuelPro/repam-mp";

    private final Logger log = LoggerFactory.getLogger(RepamMProService.class);
    private final TypeAssuranceService typeAssuranceService;
    private final SelectorStore selectors;
    private String currentStep = "démarrage";

    public RepamMProService(TypeAssuranceService typeAssuranceService, SelectorStore selectors){
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
        log.info("Début de traitement -- Repam_Mutuel_Pro (Playwright)");

        Tarif tarif = TarifUtils.createDefaultTarif(c, typeAssuranceService, 3L);
        tarif.setNom(c.getNomFournisseur());
        TypeAssurance typeAssurance = new TypeAssurance();
        typeAssurance.setTypeAssurance(EnumTypeAssurance.MUTUELLE_PRO);
        tarif.setTypeAssurance(typeAssurance);

        try {
            initializeBrowser(false);
            humanLikeNavigate(c.getUrlFournisseur());
            step("cookies", this::choixCookies);
            step("connexion", () -> connexion(c));
            step("client", () -> remplirClient(flux));
            page.waitForLoadState(LoadState.NETWORKIDLE);
            elementLib.randomWait(2500, 3500);
            currentStep = "lecture_resultats";
            List<String> couts = getPrixTtcParNiveau(c.getNiveau());
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

    private void choixCookies() {
        clickIfExists(sel("connexion.cookies_accept_selector"));
    }

    private void connexion(Compte c) {
        elementLib.randomWait(700, 1300);
        elementLib.humanTypeByXpath(sel("connexion.username_xpath"), c.getUsername());
        elementLib.humanTypeByXpath(sel("connexion.password_xpath"), c.getPassword());
        elementLib.clickByXpath(sel("connexion.submit_xpath"));
        elementLib.randomWait(700, 1300);
        choixFermeturePopup();
        elementLib.randomWait(700, 1300);
        choixSante();
        elementLib.randomWait(700, 1300);
        elementLib.switchToNewWindow();
        this.page = elementLib.getPage();
        elementLib.clickByXpath(sel("connexion.nouvelle_proposition_xpath"));
        elementLib.randomWait(700, 1300);
        choixOffre();
        elementLib.randomWait(700, 1300);
        elementLib.clickByXpath(sel("connexion.oui_xpath"));
    }

    private void remplirClient(FluxData flux) {
        elementLib.randomWait(700, 1300);
        elementLib.humanTypeByXpath(sel("client.date_effet_xpath"), dateEffet(1));
        elementLib.randomWait(700, 1300);
        elementLib.clickByXpath(sel("client.continuer_xpath"));

        elementLib.clickByXpath(sel("client.is_member_xpath"));
        elementLib.humanTypeByXpath(sel("client.nom_xpath"), flux.getPersonnes().getFirst().getNom());
        elementLib.humanTypeByXpath(sel("client.prenom_xpath"), flux.getPersonnes().getFirst().getPrenom());
        elementLib.humanTypeByXpath(sel("client.date_naissance_xpath"), flux.getPersonnes().getFirst().getDateNaissance());
        elementLib.humanTypeByXpath(sel("client.code_postal_xpath"), flux.getPersonnes().getFirst().getCodePostal());
        choixProfession(flux);
        elementLib.randomWait(700, 1300);

        if (flux.getPersonnes().size() >= 2) {
            elementLib.clickByXpath(sel("client.ajouter_beneficiaire_xpath"));
            elementLib.randomWait(700, 1300);
            elementLib.clickByXpath(sel("client.label_conjoint_xpath"));
            elementLib.humanTypeByXpath(sel("client.beneficiaire_date_naissance_xpath"), flux.getPersonnes().get(1).getDateNaissance());
            choixRegime();
            elementLib.clickByXpath(sel("client.alsace_moselle_conjoint_xpath"));
        }
        if (flux.getEnfants().getFirst().getNom() != null && !flux.getEnfants().getFirst().getNom().isEmpty()) {
            elementLib.clickByXpath(sel("client.ajouter_beneficiaire_xpath"));
            elementLib.randomWait(700, 1300);
            elementLib.clickByXpath(sel("client.label_enfant_xpath"));
            elementLib.humanTypeByXpath(sel("client.beneficiaire_date_naissance_xpath"), flux.getEnfants().getFirst().getDateNaissance());
            choixRegime();
            if (flux.getPersonnes().size() >= 2)
                elementLib.clickByXpath(sel("client.alsace_moselle_enfant1_avec_conjoint_xpath"));
            else
                elementLib.clickByXpath(sel("client.alsace_moselle_enfant1_sans_conjoint_xpath"));
        }
        if (flux.getEnfants().size() >= 2) {
            elementLib.clickByXpath(sel("client.ajouter_beneficiaire_xpath"));
            elementLib.randomWait(700, 1300);
            elementLib.clickByXpath(sel("client.label_enfant_xpath"));
            elementLib.humanTypeByXpath(sel("client.beneficiaire_date_naissance_xpath"), flux.getEnfants().get(1).getDateNaissance());
            choixRegime();
            elementLib.clickByXpath(sel("client.alsace_moselle_enfant2_xpath"));
            scrollDown(200);
        }
        continuer();
    }

    private void continuer() {
        elementLib.clickByXpath(sel("client.enregistrer_continuer_xpath"));
    }

    private void choixProfession(FluxData flux) {
        elementLib.clickByXpath(sel("client.profession_dropdown_xpath"));
        elementLib.randomWait(700, 1300);
        String profession = flux.getPersonnes().getFirst().getProfessionSpecifique();
        String cle = switch (profession.toLowerCase()) {
            case "artisan"                                                      -> "artisan";
            case "chef d'entreprise"                                           -> "chef_entreprise";
            case "commerçant"                                                  -> "commercant";
            case "agriculteur"                                                 -> "agriculteur";
            case "profession libérale médicale", "profession libérale paramédicale" -> "liberale_sante";
            case "profession libérale"                                         -> "liberale_non_reglementee";
            default                                                            -> "autre";
        };
        if ("autre".equals(cle) && !profession.equalsIgnoreCase("autre")) {
            log.warn("Profession '{}' non prise en charge par ce produit TNS, sélection de 'Autre'", profession);
        }
        String option = sel("profession." + cle);
        elementLib.clickByXpath(String.format(sel("profession.option_xpath_template"), option));
    }

    private void choixRegime() {
        elementLib.clickByXpath(sel("regime.dropdown_xpath"));
        elementLib.randomWait(700, 1300);
        elementLib.clickByXpath(sel("regime.option_xpath"));
    }

    private void choixOffre() {
        page.locator(sel("offre.carte_css"))
                .filter(new Locator.FilterOptions().setHas(page.locator("p:text-is('" + sel("offre.carte_texte") + "')")))
                .locator("button")
                .filter(new Locator.FilterOptions().setHasText(sel("offre.bouton_texte")))
                .click();
    }

    private void choixSante() {
        page.locator(sel("connexion.sante_individuelle_css"))
                .filter(new Locator.FilterOptions().setHasText(sel("connexion.sante_individuelle_texte")))
                .first()
                .click();
    }

    private void choixFermeturePopup() {
        page.locator(sel("connexion.fermeture_popup_css")).first().click();
    }

    private String dateEffet(int mois) {
        return LocalDate.now()
                .plus(mois, ChronoUnit.MONTHS)
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private List<String> getPrixTtcParNiveau(int niveau) {
        Locator grille = page.locator(sel("resultats.grille_css")).first();

        List<String> prix = new ArrayList<>();

        for (int i = niveau - 1; i < 6; i++) {
            String texte = grille
                    .locator("> div")
                    .nth(i)
                    .locator("p.font-gotham-book")
                    .innerText()
                    .replace("/mois", "")
                    .replace("\u00A0", " ")
                    .trim();
            prix.add(texte);
        }

        return prix;
    }
}
