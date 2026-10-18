package com.mtc.mutuaConseil.services.servicesImpl.assuMutuelIndiv;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import com.mtc.mutuaConseil.base.BasePlaywrightService;
import com.mtc.mutuaConseil.models.Compte;
import com.mtc.mutuaConseil.models.FluxData;
import com.mtc.mutuaConseil.models.Tarif;
import com.mtc.mutuaConseil.models.TypeAssurance;
import com.mtc.mutuaConseil.models.enums.EnumTypeAssurance;
import com.mtc.mutuaConseil.selectors.SelectorStore;
import com.mtc.mutuaConseil.services.LaunchedService;
import com.mtc.mutuaConseil.services.servicesImpl.AuthenticatorService;
import com.mtc.mutuaConseil.services.servicesImpl.TypeAssuranceService;
import com.mtc.mutuaConseil.utils.TarifUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class ApicilMIService extends BasePlaywrightService implements LaunchedService {

    private static final String PROVIDER = "mutuelIndiv/apicil-mi";

    private final TypeAssuranceService typeAssuranceService;
    private final AuthenticatorService authenticatorService;
    private final SelectorStore selectors;
    private String currentStep = "démarrage";

    public ApicilMIService(TypeAssuranceService typeAssuranceService,
                           AuthenticatorService authenticatorService,
                           SelectorStore selectors) {
        this.typeAssuranceService = typeAssuranceService;
        this.authenticatorService = authenticatorService;
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
        log.info("Début de traitement -- Apicil_Mutuel_Indiv (Playwright)");

        Tarif tarif = TarifUtils.createDefaultTarif(c, typeAssuranceService, 2L);
        tarif.setNom(c.getNomFournisseur());
        TypeAssurance typeAssurance = new TypeAssurance();
        typeAssurance.setTypeAssurance(EnumTypeAssurance.MUTUELLE_INDIV);
        tarif.setTypeAssurance(typeAssurance);

        try {
            initializeBrowser(false);
            humanLikeNavigate(c.getUrlFournisseur());
            step("connexion", () -> connexion(c));
            step("totp", () -> saisirCodeAuthenticator(c));
            step("nouveau_projet", this::nouveauProjet);
            step("identite", () -> remplirIdentite(flux));
            step("coordonnees", () -> remplirCoordonnees(flux));
            step("tarification", this::remplirTarification);
            step("beneficiaires", () -> remplirBeneficiaires(flux));
            step("suivant_resultats", this::suivant);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            elementLib.randomWait(4000, 6000);
            currentStep = "lecture_resultats";
            List<String> couts = prixParLibelle(sel("resultats.ligne_template"), sel("resultats.formules"));
            log.info("couts (formules) {}", couts);
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
        elementLib.randomWait(2500, 4500);
        clickIfExists(sel("connexion.cookie_accept_selector"));
        elementLib.typeHumanLike(sel("connexion.username_selector"), c.getUsername());
        elementLib.typeHumanLike(sel("connexion.password_selector"), c.getPassword());
        elementLib.randomWait(700, 1300);
        elementLib.click(sel("connexion.login_button_selector"));
    }

    /** Code Authenticator généré à partir du secret TOTP stocké dans le compte (authKeyQrCode). */
    private void saisirCodeAuthenticator(Compte c) {
        Locator champ = page.locator(sel("totp.input_selector")).first();
        try {
            champ.waitFor(new Locator.WaitForOptions().setTimeout(15_000));
        } catch (Exception e) {
            log.info("Pas de demande de code Authenticator");
            return;
        }
        if (c.getAuthKeyQrCode() == null || c.getAuthKeyQrCode().isBlank()) {
            throw new IllegalStateException("Code Authenticator demandé mais aucun secret TOTP (authKeyQrCode) sur le compte");
        }
        String code = authenticatorService.getTOTPCode(c.getAuthKeyQrCode());
        elementLib.typeHumanLike(sel("totp.input_selector"), code);
        elementLib.randomWait(700, 1300);
        elementLib.clickByRole(sel("totp.valider_label"));
        page.waitForLoadState(LoadState.NETWORKIDLE);
    }

    private void nouveauProjet() {
        elementLib.randomWait(1500, 2500);
        elementLib.clickByTextElement(sel("navigation.nouveau_projet_text"));
        elementLib.randomWait(1500, 2500);
        // « Les particuliers » est sélectionné par défaut : on le (re)clique si présent puis Suivant
        clickIfExists("text=" + sel("projet.particuliers_text"));
        suivant();
    }

    private void remplirIdentite(FluxData flux) {
        elementLib.randomWait(1000, 2000);
        elementLib.typeByLabel(sel("projet.nom_label"), flux.getPersonnes().getFirst().getNom());
        elementLib.typeByLabel(sel("projet.prenom_label"), flux.getPersonnes().getFirst().getPrenom());
        elementLib.typeByLabel(sel("projet.date_naissance_label"), flux.getPersonnes().getFirst().getDateNaissance());
        suivant();
    }

    private void remplirCoordonnees(FluxData flux) {
        elementLib.randomWait(1000, 2000);
        String civilite = flux.getPersonnes().getFirst().getCivilite();
        if (civilite.equalsIgnoreCase("M") || civilite.equalsIgnoreCase("Monsieur")) {
            elementLib.clickByTextElement(sel("projet.civilite_monsieur_text"));
        } else {
            elementLib.clickByTextElement(sel("projet.civilite_madame_text"));
        }
        elementLib.typeByLabel(sel("projet.code_postal_label"), flux.getPersonnes().getFirst().getCodePostal());
        elementLib.typeByLabel(sel("projet.ville_label"), flux.getPersonnes().getFirst().getVille());
        suivant();
    }

    private void remplirTarification() {
        elementLib.randomWait(1500, 2500);
        elementLib.clickByTextElement(sel("tarification.metier_sante_text"));
        elementLib.click(sel("tarification.sante_100_oui_selector"));
        elementLib.clickByRole(sel("tarification.tarifer_label"));
        elementLib.randomWait(2000, 3000);
    }

    private void remplirBeneficiaires(FluxData flux) {
        elementLib.typeByLabel(sel("tarification.date_effet_label"), dateEffet(1));
        if (flux.getPersonnes().size() >= 2) {
            elementLib.clickByRole(sel("tarification.ajouter_conjoint_label"));
            remplirDernierBeneficiaire(
                    flux.getPersonnes().get(1).getNom(),
                    flux.getPersonnes().get(1).getPrenom(),
                    flux.getPersonnes().get(1).getDateNaissance());
        }
        for (var enfant : flux.getEnfants()) {
            if (enfant.getNom() == null || enfant.getNom().isEmpty()) break;
            elementLib.clickByRole(sel("tarification.ajouter_enfant_label"));
            remplirDernierBeneficiaire(enfant.getNom(), enfant.getPrenom(), enfant.getDateNaissance());
        }
        scrollDown(300);
    }

    private void remplirDernierBeneficiaire(String nom, String prenom, String dateNaissance) {
        elementLib.randomWait(700, 1300);
        page.locator(sel("tarification.nom_beneficiaire_selector")).last().fill(nom);
        page.locator(sel("tarification.prenom_beneficiaire_selector")).last().fill(prenom);
        page.locator(sel("tarification.naissance_beneficiaire_selector")).last().fill(dateNaissance);
    }

    private void suivant() {
        elementLib.randomWait(700, 1300);
        page.getByRole(AriaRole.BUTTON, new Page.GetByRoleOptions().setName(sel("navigation.suivant_label")))
                .first().click();
        elementLib.randomWait(1000, 2000);
    }

    private String dateEffet(int mois) {
        return LocalDate.now().plusMonths(mois).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}
