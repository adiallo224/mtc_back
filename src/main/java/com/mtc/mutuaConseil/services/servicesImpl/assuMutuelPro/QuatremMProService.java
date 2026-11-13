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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.nonNull;

@Service
public class QuatremMProService extends BasePlaywrightService implements LaunchedService {

    private static final String PROVIDER = "mutuelPro/quatrem-mp";

    private final Logger log = LoggerFactory.getLogger(QuatremMProService.class);
    private final TypeAssuranceService typeAssuranceService;
    private final SelectorStore selectors;
    private String currentStep = "démarrage";

    public QuatremMProService(TypeAssuranceService typeAssuranceService, SelectorStore selectors) {
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
        log.info("Début de traitement -- Quatrem_Mutuel_Pro (Playwright)");

        Tarif tarif = TarifUtils.createDefaultTarif(c,typeAssuranceService, 3L);
        tarif.setNom(c.getNomFournisseur());
        TypeAssurance typeAssurance = new TypeAssurance();
        typeAssurance.setTypeAssurance(EnumTypeAssurance.MUTUELLE_PRO);
        tarif.setTypeAssurance(typeAssurance);

        try {
            initializeBrowser(false);
            step("connexion", () -> connexion(c));
            step("contrat", () -> remplirContrat(flux));
            step("identite", () -> remplirIdentite(flux));
            step("coordonnees", () -> remplirCoordonnees(flux));
            step("couverture", this::remplirCouverture);
            if (flux.getPersonnes().size() >= 2 || (flux.getEnfants().getFirst().getNom() != null && !flux.getEnfants().getFirst().getNom().isEmpty())) {
                step("beneficiaires", () -> remplirBenficiaires(flux));
            }
            scrollDown(300);
            step("calcul", this::suivant);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            elementLib.randomWait(6000, 8000);
            currentStep = "lecture_resultats";
            List<String> couts = getPrixParFormule(c.getNiveau());
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
        elementLib.humanTypeById(sel("connexion.login_id"), c.getUsername());
        elementLib.humanTypeById(sel("connexion.password_id"), c.getPassword());
        elementLib.randomWait(700, 1300);
        elementLib.clickById(sel("connexion.submit_id"));
        elementLib.randomWait(700, 1300);
    }

    private void remplirContrat(FluxData flux) {
        elementLib.humanTypeById(sel("contrat.date_effet_id"), dateEffet(1));
    }

    private void remplirIdentite(FluxData flux) {
        choixCivilite(flux, 0);
        elementLib.humanTypeById(sel("identite.nom_id"), flux.getPersonnes().getFirst().getNom());
        elementLib.humanTypeById(sel("identite.prenom_id"), flux.getPersonnes().getFirst().getPrenom());
        elementLib.humanTypeById(sel("identite.date_naissance_id"), flux.getPersonnes().getFirst().getDateNaissance());
        choixRegime(flux, 0);
    }

    private void remplirCoordonnees(FluxData flux) {
        elementLib.humanTypeById(sel("coordonnees.adresse_id"), flux.getPersonnes().getFirst().getNumeroVoie() + " " + flux.getPersonnes().getFirst().getNomVoie());
        elementLib.humanTypeById(sel("coordonnees.code_postal_id"), flux.getPersonnes().getFirst().getCodePostal());
        elementLib.humanTypeById(sel("coordonnees.ville_id"), flux.getPersonnes().getFirst().getVille());
        elementLib.humanTypeById(sel("coordonnees.email_id"), flux.getPersonnes().getFirst().getEmail());
        elementLib.humanTypeById(sel("coordonnees.telephone_id"), flux.getPersonnes().getFirst().getTelephone());
    }

    private void remplirCouverture() {
        elementLib.clickById(sel("couverture.indicateur_id"));
    }

    private void remplirBenficiaires(FluxData flux) {
        elementLib.randomWait(700, 1300);
        if (flux.getPersonnes().size() >= 2) {
            scrollDown(200);
            ajoutSouscripteur();
            elementLib.randomWait(700, 1300);
            choixLienParente(sel("beneficiaire.conjoint_lien_button_xpath"), sel("beneficiaire.conjoint_lien_menu_xpath"), "conjoint");
            choixLienCiviliteBeneficiaire(sel("beneficiaire.conjoint_civilite_button_xpath"), sel("beneficiaire.conjoint_civilite_menu_xpath"), "conjoint", flux, 0);
            elementLib.humanTypeById(sel("beneficiaire.conjoint_nom_id"), flux.getPersonnes().get(1).getNom());
            elementLib.humanTypeById(sel("beneficiaire.conjoint_prenom_id"), flux.getPersonnes().get(1).getPrenom());
            elementLib.humanTypeById(sel("beneficiaire.conjoint_date_naissance_id"), flux.getPersonnes().get(1).getDateNaissance());
            choixRegimeBeneficiaires(sel("beneficiaire.conjoint_regime_button_xpath"), sel("beneficiaire.conjoint_regime_menu_xpath"));
        }
        if (flux.getEnfants().getFirst().getNom() != null && !flux.getEnfants().getFirst().getNom().isEmpty()) {
            ajoutSouscripteur();
            elementLib.randomWait(700, 1300);
            choixLienParente(sel("beneficiaire.enfant1_lien_button_xpath"), sel("beneficiaire.enfant1_lien_menu_xpath"), "enfant");
            choixLienCiviliteBeneficiaire(sel("beneficiaire.enfant1_civilite_button_xpath"), sel("beneficiaire.enfant1_civilite_menu_xpath"), "enfant", flux, 0);
            elementLib.humanTypeById(sel("beneficiaire.enfant1_nom_id"), flux.getEnfants().getFirst().getNom());
            elementLib.humanTypeById(sel("beneficiaire.enfant1_prenom_id"), flux.getEnfants().getFirst().getPrenom());
            elementLib.humanTypeById(sel("beneficiaire.enfant1_date_naissance_id"), flux.getEnfants().getFirst().getDateNaissance());
            choixRegimeBeneficiaires(sel("beneficiaire.enfant1_regime_button_xpath"), sel("beneficiaire.enfant1_regime_menu_xpath"));
        }
        if (flux.getEnfants().size() == 2) {
            ajoutSouscripteur();
            elementLib.randomWait(700, 1300);
            choixLienParente(sel("beneficiaire.enfant2_lien_button_xpath"), sel("beneficiaire.enfant2_lien_menu_xpath"), "enfant");
            choixLienCiviliteBeneficiaire(sel("beneficiaire.enfant2_civilite_button_xpath"), sel("beneficiaire.enfant2_civilite_menu_xpath"), "enfant", flux, 1);
            elementLib.humanTypeById(sel("beneficiaire.enfant2_nom_id"), flux.getEnfants().get(1).getNom());
            elementLib.humanTypeById(sel("beneficiaire.enfant2_prenom_id"), flux.getEnfants().get(1).getPrenom());
            elementLib.humanTypeById(sel("beneficiaire.enfant2_date_naissance_id"), flux.getEnfants().get(1).getDateNaissance());
            choixRegimeBeneficiaires(sel("beneficiaire.enfant2_regime_button_xpath"), sel("beneficiaire.enfant2_regime_menu_xpath"));
        }
    }

    private void ajoutSouscripteur() {
        elementLib.clickById(sel("beneficiaire.ajouter_id"));
    }

    private void choixLienParente(String pathDropdown, String listLi, String lien) {
        elementLib.randomWait(700, 1300);
        elementLib.clickByXpath(pathDropdown);
        elementLib.randomWait(700, 1300);
        Locator options = page.locator("xpath=" + listLi);
        if (lien.equalsIgnoreCase("conjoint"))
            options.nth(1).click();
        if (lien.equalsIgnoreCase("enfant"))
            options.nth(2).click();
    }

    private void choixLienCiviliteBeneficiaire(String pathDropdown, String listLi, String lien, FluxData flux, int indexEnfant) {
        elementLib.randomWait(700, 1300);
        elementLib.clickByXpath(pathDropdown);
        elementLib.randomWait(700, 1300);
        Locator options = page.locator("xpath=" + listLi);
        if (lien.equalsIgnoreCase("conjoint")) {
            options.nth(1).click();
        } else if (lien.equalsIgnoreCase("enfant")) {
            if (flux.getEnfants().get(indexEnfant).getCivilite().equalsIgnoreCase("Monsieur"))
                options.nth(1).click();
            else
                options.nth(2).click();
        }
    }

    private void choixRegimeBeneficiaires(String pathDropdown, String listLi) {
        elementLib.clickByXpath(pathDropdown);
        elementLib.randomWait(700, 1300);
        page.locator("xpath=" + listLi).nth(1).click();
    }

    private void suivant() {
        elementLib.clickById(sel("beneficiaire.next_id"));
    }

    private void choixRegime(FluxData flux, int index) {
        elementLib.randomWait(700, 1300);
        if (index == 0) {
            elementLib.clickByXpath(sel("identite.regime_button_xpath"));
            elementLib.randomWait(700, 1300);
            page.locator("xpath=" + sel("identite.regime_menu_xpath")).nth(1).click();
        }
    }

    private void choixCivilite(FluxData flux, int index) {
        elementLib.randomWait(700, 1300);
        if (index == 0) {
            elementLib.clickByXpath(sel("identite.civilite_button_xpath"));
            elementLib.randomWait(700, 1300);
            Locator options = page.locator("xpath=" + sel("identite.civilite_menu_xpath"));
            String civilite = flux.getPersonnes().get(index).getCivilite();
            if (civilite.equalsIgnoreCase("Monsieur") || civilite.equalsIgnoreCase("M"))
                options.nth(1).click();
            else if (civilite.equalsIgnoreCase("Madame") || civilite.equalsIgnoreCase("Mme"))
                options.nth(2).click();
        }
    }

    private String dateEffet(int mois) {
        return LocalDate.now()
                .plus(mois, ChronoUnit.MONTHS)
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public String getPrixByNiveau(int niveau) {
        Locator ligneCotisation = page.locator(sel("resultats.ligne_cotisation_css"));

        return ligneCotisation
                .locator(String.format(sel("resultats.cellule_par_niveau_template"), niveau))
                .textContent()
                .trim();
    }

    private List<String> getPrixParFormule(int niveau) {
        List<String> prix = new ArrayList<>();

        Locator cellules = page.locator(sel("resultats.cellules_selector"));
        int count = cellules.count();

        for (int i = niveau-1; i < count; i++) {
            Locator cellule = cellules.nth(i);
            Matcher matcher = Pattern.compile("formule_(\\d+)").matcher(cellule.getAttribute("class"));
            if (matcher.find() && Integer.parseInt(matcher.group(1)) >= niveau) {
                prix.add(cellule.locator(".modelChampNote")
                        .innerText()
                        .replace("EUR", "€")
                        .trim());
            }
        }
        return prix;
    }
}
