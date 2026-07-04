package com.mtc.mutuaConseil.services.servicesImpl.assuMutuelIndiv;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.LoadState;
import com.mtc.mutuaConseil.base.BasePlaywrightService;
import com.mtc.mutuaConseil.models.Compte;
import com.mtc.mutuaConseil.models.FluxData;
import com.mtc.mutuaConseil.models.Tarif;
import com.mtc.mutuaConseil.models.TypeAssurance;
import com.mtc.mutuaConseil.models.enums.EnumTypeAssurance;
import com.mtc.mutuaConseil.services.LaunchedService;
import com.mtc.mutuaConseil.services.servicesImpl.TypeAssuranceService;
import com.mtc.mutuaConseil.utils.TarifUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

@Service
public class HennerMlService extends BasePlaywrightService implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(HennerMlService.class);
    private final TypeAssuranceService typeAssuranceService;

    public HennerMlService(TypeAssuranceService typeAssuranceService) {
        this.typeAssuranceService = typeAssuranceService;
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
            connexion(c);
            remplirCreationDevis();
            remplirChoixDevis();
            remplirContrat(flux);
            remplirSante();
            remplirDevisSante(flux);
            suivant();
            page.waitForLoadState(LoadState.NETWORKIDLE);
            elementLib.randomWait(6000, 8000);
            String cout = getElementTextByXpath("/html/body/app-root/app-auth/div/div/div/div/app-calculator/div/main/app-indiv/app-indiv-pricing/div/div[5]/div[3]/div[2]/div/div[1]/div[2]");
            log.info("cout {}", cout);
            tarif.setMontant(cout);
            elementLib.randomWait(600, 2000);
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
        elementLib.randomWait(700, 1300);
        elementLib.clickByRole("TOUT ACCEPTER");
        elementLib.humanTypeById("mat-input-0", c.getUsername());
        elementLib.humanTypeById("mat-input-1", c.getPassword());
        elementLib.randomWait(700, 1300);
        elementLib.clickByTextElement("Se connecter");
        elementLib.randomWait(700, 1300);
    }

    private void remplirCreationDevis() {
        elementLib.click("a[title=\"Création d'un devis\"]");
    }

    private void remplirChoixDevis() {
        elementLib.clickByXpath("//span[normalize-space()='PARTICULIER']");
    }

    private void remplirContrat(FluxData flux) {
        elementLib.randomWait(700, 1300);
        elementLib.humanTypeByXpath("//input[@data-placeholder='Date de naissance']", flux.getPersonnes().getFirst().getDateNaissance());
        clickBody();
        elementLib.randomWait(700, 1300);
        elementLib.typeByLabel("Code postal", flux.getPersonnes().getFirst().getCodePostal());
        elementLib.randomWait(700, 1300);
        elementLib.typeByLabel("Nom (facultatif)", flux.getPersonnes().getFirst().getNom());
        elementLib.randomWait(700, 1300);
        elementLib.typeByLabel("Prénom (facultatif)", flux.getPersonnes().getFirst().getPrenom());
        elementLib.randomWait(700, 1300);
        elementLib.clickByRole("VALIDER");
    }

    private void remplirSante() {
        elementLib.randomWait(700, 1300);
        elementLib.clickByTextElement("Santé");
    }

    private void remplirDevisSante(FluxData flux) {
        elementLib.humanTypeByXpath("//input[@data-placeholder=\"Date d'effet\"]", dateEffet(1));
        elementLib.randomWait(700, 1300);
        clickBody();
        choixRegime("//span[normalize-space()='Régime']");
        if (flux.getPersonnes().size() >= 2) {
            remplirConjoint(flux);
            scrollDown(100);
        }
        if (flux.getEnfants().getFirst().getNom() != null && !flux.getEnfants().getFirst().getNom().isEmpty()) {
            remplirEnfant(flux,
                "/html/body/app-root/app-auth/div/div/div/div/app-calculator/div/main/app-indiv/app-indiv-recap/app-indiv-client-info/div/div/div[2]/div/div/div/app-indiv-form/div/form/div[1]/div/div[3]/div/div[1]/div[2]/div[1]/mat-form-field/div/div[1]/div[1]/input",
                "/html/body/app-root/app-auth/div/div/div/div/app-calculator/div/main/app-indiv/app-indiv-recap/app-indiv-client-info/div/div/div[2]/div/div/div/app-indiv-form/div/form/div[1]/div/div[3]/div/div[1]/div[2]/div[2]/mat-form-field/div/div[1]/div/mat-select/div/div[1]/span",
                0);
            scrollDown(100);
        }
        if (flux.getEnfants().size() >= 2) {
            remplirEnfant(flux,
                "/html/body/app-root/app-auth/div/div/div/div/app-calculator/div/main/app-indiv/app-indiv-recap/app-indiv-client-info/div/div/div[2]/div/div/div/app-indiv-form/div/form/div[1]/div/div[3]/div/div[2]/div[2]/div[1]/mat-form-field/div/div[1]/div[1]/input",
                "/html/body/app-root/app-auth/div/div/div/div/app-calculator/div/main/app-indiv/app-indiv-recap/app-indiv-client-info/div/div/div[2]/div/div/div/app-indiv-form/div/form/div[1]/div/div[3]/div/div[2]/div[2]/div[2]/mat-form-field/div/div[1]/div/mat-select/div/div[1]/span",
                1);
            scrollDown(250);
        }
    }

    private void remplirConjoint(FluxData flux) {
        ajoutConjoint();
        elementLib.randomWait(700, 1300);
        elementLib.humanTypeByXpath(
            "/html/body/app-root/app-auth/div/div/div/div/app-calculator/div/main/app-indiv/app-indiv-recap/app-indiv-client-info/div/div/div[2]/div/div/div/app-indiv-form/div/form/div[1]/div/div[2]/div/div/div[2]/div[1]/mat-form-field/div/div[1]/div[1]/input",
            flux.getPersonnes().get(1).getDateNaissance());
        elementLib.randomWait(700, 1300);
        clickBody();
        choixRegime("/html/body/app-root/app-auth/div/div/div/div/app-calculator/div/main/app-indiv/app-indiv-recap/app-indiv-client-info/div/div/div[2]/div/div/div/app-indiv-form/div/form/div[1]/div/div[2]/div/div/div[2]/div[2]/mat-form-field/div/div[1]/div/mat-select/div/div[1]/span");
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
        Locator options = page.locator("xpath=//div[@role='listbox']//mat-option[@role='option']");
        for (int i = 0; i < options.count(); i++) {
            if (options.nth(i).textContent().trim().equalsIgnoreCase("Régime Général")) {
                options.nth(i).click();
                break;
            }
        }
    }

    private void ajoutConjoint() {
        elementLib.randomWait(700, 1300);
        elementLib.clickByTextElement("Ajouter un conjoint");
    }

    private void ajoutEnfant() {
        elementLib.randomWait(700, 1300);
        elementLib.clickByTextElement("Ajouter un enfant");
    }

    private void suivant() {
        elementLib.randomWait(700, 1300);
        elementLib.clickByTextElement("TARIFER");
    }

    private String dateEffet(int mois) {
        return LocalDate.now()
                .plus(mois, ChronoUnit.MONTHS)
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}
