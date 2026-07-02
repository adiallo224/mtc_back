package com.mtc.mutuaConseil.services.servicesImpl.assuMutuelIndiv;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.SelectOption;
import com.mtc.mutuaConseil.base.BasePlaywrightService;
import com.mtc.mutuaConseil.models.Compte;
import com.mtc.mutuaConseil.models.FluxData;
import com.mtc.mutuaConseil.models.Tarif;
import com.mtc.mutuaConseil.models.TypeAssurance;
import com.mtc.mutuaConseil.models.enums.EnumTypeAssurance;
import com.mtc.mutuaConseil.services.LaunchedService;
import com.mtc.mutuaConseil.services.servicesImpl.TypeAssuranceService;
import com.mtc.mutuaConseil.utils.TarifUtils;
import org.springframework.stereotype.Service;


@Service
public class AMI3FMutuelIndivPlayWrightService extends BasePlaywrightService implements LaunchedService {

    private final TypeAssuranceService typeAssuranceService;

    public AMI3FMutuelIndivPlayWrightService(TypeAssuranceService typeAssuranceService) {
        this.typeAssuranceService = typeAssuranceService;
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
            connexion(c);
            choixTarification();
            remplirComplementaireSante(flux);
            waitThread(7);
            String cout = getPrixTtcParFormule("Formule F2");
            log.info("cout {}", cout);
            tarif.setMontant(cout);
            String screenshotBytes = captureScreenshot(tarif.getNom(), false, tarif);
            if (screenshotBytes != null) {
                tarif.setCaptureImg(screenshotBytes);
            }
            tarif.setExecution(true);
        } catch (Exception e) {
            log.error("An error occurred", e);
            tarif.setErreur(e.getMessage());
            String screenshotPath = captureScreenshot(tarif.getNom(), true, tarif);
            tarif.setCaptureImgErreur(screenshotPath);
            tarif.setEtape("");
        } finally {
            cleanup();
        }
        return tarif;
    }

    private void connexion(Compte c) {
        humanLikeNavigate(c.getUrlFournisseur());
        elementLib.humanTypeByXpath("//input[@name='login']", c.getUsername());
        elementLib.humanTypeByXpath("//input[@name='password']", c.getPassword());
        waitThread(1);
        elementLib.click("//button[@type='submit']");
        waitThread(1);
    }

    private void choixTarification() {
        waitThread(2);
        elementLib.click("//*[@id='navmenu-tarif']");
        elementLib.click("//span[normalize-space()='Complémentaire Santé']");
        page.evaluate("window.scrollBy(0, 450)");
        waitThread(2);
        elementLib.click("//span[normalize-space()='ACCÉDER A LA TARIFICATION']");
    }

    private void remplirComplementaireSante(FluxData flux) {
        elementLib.humanTypeById("nom_assure_1", flux.getPersonnes().getFirst().getNom());
        elementLib.humanTypeById("prenom_assure_1", flux.getPersonnes().getFirst().getPrenom());
        elementLib.humanTypeById("nom_naiss_assure_1", flux.getPersonnes().getFirst().getNom());
        elementLib.humanTypeById("dt_naiss_assure_1", flux.getPersonnes().getFirst().getDateNaissance());
        choixPays(flux, 0);
        elementLib.humanTypeById("cp_naiss_assure_1", flux.getPersonnes().getFirst().getCodePostal());
        choixVille(flux, 0);
        choixSexe(flux, 0);
        choixSituationFamilliale(flux, 0);
        choixRegime(flux, 0);
        elementLib.humanTypeById("profession_assure_1", flux.getPersonnes().getFirst().getProfession());
        elementLib.clickById("div_radio_is_ppe_assure_1_false");
        elementLib.clickById("radio_ppe_famille_assure_1_false");

        if (flux.getPersonnes().size() >= 2) {
            elementLib.clickById("btn-add-conjoint");
            elementLib.humanTypeById("nom_assure_2", flux.getPersonnes().get(1).getNom());
            elementLib.humanTypeById("prenom_assure_2", flux.getPersonnes().get(1).getPrenom());
            elementLib.humanTypeById("nom_naiss_assure_2", flux.getPersonnes().get(1).getNom());
            elementLib.humanTypeById("dt_naiss_assure_2", flux.getPersonnes().get(1).getDateNaissance());
            choixPays(flux, 1);
            elementLib.humanTypeById("cp_naiss_assure_2", flux.getPersonnes().get(1).getCodePostal());
            choixVille(flux, 1);
            choixSexe(flux, 1);
            choixRegime(flux, 1);
            elementLib.humanTypeById("profession_assure_2", flux.getPersonnes().get(1).getProfession());
            elementLib.clickById("div_radio_is_ppe_assure_2_false");
            elementLib.clickById("radio_ppe_famille_assure_2_false");

            if (flux.getEnfants().getFirst().getNom() != null) {
                waitThread(1);
                elementLib.clickById("btn-add-enfant");
                waitThread(1);
                choixSexeEnfant(flux, 0);
                elementLib.humanTypeById("nom_enfant_1", flux.getEnfants().getFirst().getNom());
                elementLib.humanTypeById("prenom_enfant_1", flux.getEnfants().getFirst().getPrenom());
                elementLib.humanTypeById("dt_naiss_enfant_1", flux.getEnfants().getFirst().getDateNaissance());
                choixPaysEnfant(flux, 0);
                elementLib.humanTypeById("cp_naiss_enfant_1", flux.getPersonnes().getFirst().getCodePostal());
                choixVilleEnfants(flux, 0);
                choixRegimeEnfant(flux, 0);
            }
            if (flux.getEnfants().size() == 2) {
                elementLib.clickById("btn-add-enfant");
                choixSexeEnfant(flux, 1);
                elementLib.humanTypeById("nom_enfant_2", flux.getPersonnes().get(1).getNom());
                elementLib.humanTypeById("prenom_enfant_2", flux.getPersonnes().get(1).getPrenom());
                elementLib.humanTypeById("dt_naiss_enfant_2", flux.getPersonnes().get(1).getDateNaissance());
                choixPaysEnfant(flux, 1);
                elementLib.humanTypeById("cp_naiss_enfant_2", flux.getPersonnes().get(0).getCodePostal());
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
        waitThread(2);
        elementLib.humanTypeById("r_adresse_1", flux.getPersonnes().get(index).getNumeroVoie() + " " + flux.getPersonnes().get(index).getNomVoie());
        elementLib.humanTypeById("r_code_postal", flux.getPersonnes().get(index).getCodePostal());
        choixVille1(flux, index);
        suivant();
    }

    private void suivant() {
        elementLib.click("//input[@src='img/suivant.jpg']");
    }

    private void choixRegime(FluxData flux, int index) {
        String selectId = index == 0 ? "regime_assure_1" : "regime_assure_2";
        selectOptionContaining(selectId, "Régime général");
    }

    private void choixRegimeEnfant(FluxData flux, int index) {
        String selectId = index == 0 ? "regime_enfant_1" : "regime_enfant_2";
        selectOptionContaining(selectId, "de l'adhérent principal");
    }

    private void choixPays(FluxData flux, int index) {
        String selectId = index == 0 ? "pays_naiss_assure_1" : "pays_naiss_assure_2";
        selectOptionContaining(selectId, flux.getPersonnes().get(index).getPays());
    }

    private void choixPaysEnfant(FluxData flux, int index) {
        String selectId = index == 0 ? "pays_naiss_enfant_1" : "pays_naiss_assure_2";
        selectOptionContaining(selectId, flux.getPersonnes().get(index).getPays());
    }

    private void choixVille(FluxData flux, int index) {
        String selectId = index == 0 ? "ville_naiss_assure_1" : "ville_naiss_assure_2";
        selectOptionEquals(selectId, flux.getPersonnes().get(index).getVille());
    }

    private void choixVilleEnfants(FluxData flux, int index) {
        String selectId = index == 0 ? "ville_naiss_enfant_1" : "ville_naiss_enfant_2";
        selectOptionEquals(selectId, flux.getPersonnes().get(index).getVille());
    }

    private void choixVille1(FluxData flux, int index) {
        selectOptionEquals("r_ville", flux.getPersonnes().get(index).getVille());
    }

    private void choixSexe(FluxData flux, int index) {
        String selectId = index == 0 ? "sexe_assure_1" : "sexe_assure_2";
        String civilite = flux.getPersonnes().get(index).getCivilite();
        if (civilite.equalsIgnoreCase("Monsieur")) selectOptionEquals(selectId, "Homme");
        else if (civilite.equalsIgnoreCase("Madame")) selectOptionEquals(selectId, "Femme");
    }

    private void choixSexeEnfant(FluxData flux, int index) {
        String selectId = index == 0 ? "sexe_enfant_1" : "sexe_enfant_2";
        String civilite = flux.getPersonnes().get(index).getCivilite();
        if (civilite.equalsIgnoreCase("Monsieur")) selectOptionEquals(selectId, "Masculin");
        else if (civilite.equalsIgnoreCase("Madame")) selectOptionEquals(selectId, "Féminin");
    }

    private void choixSituationFamilliale(FluxData flux, int index) {
        if (flux.getPersonnes().size() == 1) selectOptionEquals("i_sitfam", "Célibataire");
        else selectOptionContaining("i_sitfam", "Marié");
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

    private String getPrixTtcParFormule(String formule) {
        Locator row = page.locator("tr.formule-disponible").filter(
                new Locator.FilterOptions().setHas(page.locator("td[data-sort-val='libelle']").filter(
                        new Locator.FilterOptions().setHasText(formule)
                ))
        );

        if (row.count() > 0) {
            return row.locator("td.ttc span").innerText().trim();
        }

        return null;
    }

}
