package com.mtc.mutuaConseil.services.servicesImpl.assuMutuelIndiv;

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

import java.util.List;


@Service
public class Ami3fMIService extends BasePlaywrightService implements LaunchedService {

    private static final String PROVIDER = "mutuelIndiv/ami3f-mi";

    private final TypeAssuranceService typeAssuranceService;
    private final SelectorStore selectors;
    private String currentStep = "démarrage";

    public Ami3fMIService(TypeAssuranceService typeAssuranceService, SelectorStore selectors) {
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
        log.info("Début de traitement -- AMI3F_Mutuel_Indiv (Playwright)");

        Tarif tarif = TarifUtils.createDefaultTarif(c, typeAssuranceService, 2L);
        tarif.setNom(c.getNomFournisseur());
        TypeAssurance typeAssurance = new TypeAssurance();
        typeAssurance.setTypeAssurance(EnumTypeAssurance.MUTUELLE_INDIV);
        tarif.setTypeAssurance(typeAssurance);

        try {
            initializeBrowser(false);
            step("connexion", () -> connexion(c));
            step("tarification", this::choixTarification);
            step("complementaire_sante", () -> remplirComplementaireSante(flux));
            page.waitForLoadState(LoadState.NETWORKIDLE);
            elementLib.randomWait(6000, 8000);
            currentStep = "lecture_resultats";
            List<String> couts = getPrixTtcParFormule(c.getNiveau());
            tarif.setMontant(couts);
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
        humanLikeNavigate(c.getUrlFournisseur());
        elementLib.humanTypeByXpath(sel("connexion.login_xpath"), c.getUsername());
        elementLib.humanTypeByXpath(sel("connexion.password_xpath"), c.getPassword());
        elementLib.randomWait(700, 1300);
        elementLib.click(sel("connexion.submit_xpath"));
        elementLib.randomWait(700, 1300);
    }

    private void choixTarification() {
        elementLib.randomWait(1500, 2500);
        elementLib.click(sel("navigation.menu_tarif_xpath"));
        elementLib.click(sel("navigation.complementaire_sante_xpath"));
        page.evaluate("window.scrollBy(0, 450)");
        elementLib.randomWait(1500, 2500);
        elementLib.click(sel("navigation.acceder_tarification_xpath"));
    }

    private void remplirComplementaireSante(FluxData flux) {
        elementLib.humanTypeById(sel("assure1.nom_id"), flux.getPersonnes().getFirst().getNom());
        elementLib.humanTypeById(sel("assure1.prenom_id"), flux.getPersonnes().getFirst().getPrenom());
        elementLib.humanTypeById(sel("assure1.nom_naissance_id"), flux.getPersonnes().getFirst().getNom());
        elementLib.humanTypeById(sel("assure1.date_naissance_id"), flux.getPersonnes().getFirst().getDateNaissance());
        choixPays(flux, 0);
        elementLib.humanTypeById(sel("assure1.cp_naissance_id"), flux.getPersonnes().getFirst().getCodePostal());
        choixVille(flux, 0);
        choixSexe(flux, 0);
        choixSituationFamilliale(flux, 0);
        choixRegime(flux, 0);
        elementLib.humanTypeById(sel("assure1.profession_id"), flux.getPersonnes().getFirst().getProfession());
        elementLib.clickById(sel("assure1.ppe_id"));
        elementLib.clickById(sel("assure1.ppe_famille_id"));

        if (flux.getPersonnes().size() >= 2) {
            elementLib.clickById(sel("assure2.ajouter_conjoint_id"));
            elementLib.humanTypeById(sel("assure2.nom_id"), flux.getPersonnes().get(1).getNom());
            elementLib.humanTypeById(sel("assure2.prenom_id"), flux.getPersonnes().get(1).getPrenom());
            elementLib.humanTypeById(sel("assure2.nom_naissance_id"), flux.getPersonnes().get(1).getNom());
            elementLib.humanTypeById(sel("assure2.date_naissance_id"), flux.getPersonnes().get(1).getDateNaissance());
            choixPays(flux, 1);
            elementLib.humanTypeById(sel("assure2.cp_naissance_id"), flux.getPersonnes().get(1).getCodePostal());
            choixVille(flux, 1);
            choixSexe(flux, 1);
            choixRegime(flux, 1);
            elementLib.humanTypeById(sel("assure2.profession_id"), flux.getPersonnes().get(1).getProfession());
            elementLib.clickById(sel("assure2.ppe_id"));
            elementLib.clickById(sel("assure2.ppe_famille_id"));

            if (flux.getEnfants().getFirst().getNom() != null) {
                elementLib.randomWait(700, 1300);
                elementLib.clickById(sel("enfant1.ajouter_id"));
                elementLib.randomWait(700, 1300);
                choixSexeEnfant(flux, 0);
                elementLib.humanTypeById(sel("enfant1.nom_id"), flux.getEnfants().getFirst().getNom());
                elementLib.humanTypeById(sel("enfant1.prenom_id"), flux.getEnfants().getFirst().getPrenom());
                elementLib.humanTypeById(sel("enfant1.date_naissance_id"), flux.getEnfants().getFirst().getDateNaissance());
                choixPaysEnfant(flux, 0);
                elementLib.humanTypeById(sel("enfant1.cp_naissance_id"), flux.getPersonnes().getFirst().getCodePostal());
                choixVilleEnfants(flux, 0);
                choixRegimeEnfant(flux, 0);
            }
            if (flux.getEnfants().size() == 2) {
                elementLib.clickById(sel("enfant2.ajouter_id"));
                choixSexeEnfant(flux, 1);
                elementLib.humanTypeById(sel("enfant2.nom_id"), flux.getPersonnes().get(1).getNom());
                elementLib.humanTypeById(sel("enfant2.prenom_id"), flux.getPersonnes().get(1).getPrenom());
                elementLib.humanTypeById(sel("enfant2.date_naissance_id"), flux.getPersonnes().get(1).getDateNaissance());
                choixPaysEnfant(flux, 1);
                elementLib.humanTypeById(sel("enfant2.cp_naissance_id"), flux.getPersonnes().get(0).getCodePostal());
                choixVilleEnfants(flux, 1);
                choixRegimeEnfant(flux, 1);
            }
            suivant();
        }
        if (flux.getPersonnes().size() == 1) {
            suivant();
        }
        adresseRisques(flux, 0);
    }

    private void adresseRisques(FluxData flux, int index) {
        elementLib.randomWait(1500, 2500);
        elementLib.humanTypeById(sel("adresse_risque.adresse_id"), flux.getPersonnes().get(index).getNumeroVoie() + " " + flux.getPersonnes().get(index).getNomVoie());
        elementLib.humanTypeById(sel("adresse_risque.code_postal_id"), flux.getPersonnes().get(index).getCodePostal());
        choixVille1(flux, index);
        suivant();
    }

    private void suivant() {
        elementLib.click(sel("navigation.suivant_xpath"));
    }

    private void choixRegime(FluxData flux, int index) {
        String selectId = index == 0 ? sel("assure1.regime_select_id") : sel("assure2.regime_select_id");
        selectOptionContaining(selectId, sel("textes.regime_general_contains"));
    }

    private void choixRegimeEnfant(FluxData flux, int index) {
        String selectId = index == 0 ? sel("enfant1.regime_select_id") : sel("enfant2.regime_select_id");
        selectOptionContaining(selectId, sel("textes.regime_enfant_contains"));
    }

    private void choixPays(FluxData flux, int index) {
        String selectId = index == 0 ? sel("assure1.pays_naissance_select_id") : sel("assure2.pays_naissance_select_id");
        selectOptionContaining(selectId, flux.getPersonnes().get(index).getPays());
    }

    private void choixPaysEnfant(FluxData flux, int index) {
        String selectId = index == 0 ? sel("enfant1.pays_naissance_select_id") : sel("enfant2.pays_naissance_select_id");
        selectOptionContaining(selectId, flux.getPersonnes().get(index).getPays());
    }

    private void choixVille(FluxData flux, int index) {
        String selectId = index == 0 ? sel("assure1.ville_naissance_select_id") : sel("assure2.ville_naissance_select_id");
        selectOptionEquals(selectId, flux.getPersonnes().get(index).getVille());
    }

    private void choixVilleEnfants(FluxData flux, int index) {
        String selectId = index == 0 ? sel("enfant1.ville_naissance_select_id") : sel("enfant2.ville_naissance_select_id");
        selectOptionEquals(selectId, flux.getPersonnes().get(index).getVille());
    }

    private void choixVille1(FluxData flux, int index) {
        selectOptionEquals(sel("adresse_risque.ville_select_id"), flux.getPersonnes().get(index).getVille());
    }

    private void choixSexe(FluxData flux, int index) {
        String selectId = index == 0 ? sel("assure1.sexe_select_id") : sel("assure2.sexe_select_id");
        String civilite = flux.getPersonnes().get(index).getCivilite();
        if (civilite.equalsIgnoreCase("Monsieur")) selectOptionEquals(selectId, sel("textes.sexe_homme"));
        else if (civilite.equalsIgnoreCase("Madame")) selectOptionEquals(selectId, sel("textes.sexe_femme"));
    }

    private void choixSexeEnfant(FluxData flux, int index) {
        String selectId = index == 0 ? sel("enfant1.sexe_select_id") : sel("enfant2.sexe_select_id");
        String civilite = flux.getPersonnes().get(index).getCivilite();
        if (civilite.equalsIgnoreCase("Monsieur")) selectOptionEquals(selectId, sel("textes.sexe_masculin"));
        else if (civilite.equalsIgnoreCase("Madame")) selectOptionEquals(selectId, sel("textes.sexe_feminin"));
    }

    private void choixSituationFamilliale(FluxData flux, int index) {
        if (flux.getPersonnes().size() == 1) selectOptionEquals(sel("assure1.situation_familiale_select_id"), sel("textes.situation_celibataire"));
        else selectOptionContaining(sel("assure1.situation_familiale_select_id"), sel("textes.situation_marie_contains"));
    }

    private void selectOptionContaining(String selectId, String search) {
        Locator opts = page.locator("#" + selectId + " option");
        int count = opts.count();
        for (int i = 0; i < count; i++) {
            Locator opt = opts.nth(i);
            if (opt.innerText().trim().contains(search)) {
                page.locator("#" + selectId).selectOption(new SelectOption().setValue(opt.getAttribute("value")));
                break;
            }
        }
    }

    private void selectOptionEquals(String selectId, String search) {
        Locator opts = page.locator("#" + selectId + " option");
        int count = opts.count();
        for (int i = 0; i < count; i++) {
            Locator opt = opts.nth(i);
            if (opt.innerText().trim().equalsIgnoreCase(search)) {
                page.locator("#" + selectId).selectOption(new SelectOption().setValue(opt.getAttribute("value")));
                break;
            }
        }
    }

    private List<String> getPrixTtcParFormule(int idFormule) {
        String template = sel("resultats.prix_selector_template");
        StringBuilder selecteur = new StringBuilder();

        for (int i = idFormule; i <= 7; i++) {
            if (!selecteur.isEmpty()) {
                selecteur.append(", ");
            }
            selecteur.append(String.format(template, i));
        }

        return page.locator(selecteur.toString())
                .allInnerTexts()
                .stream()
                .map(String::trim)
                .toList();
    }

}
