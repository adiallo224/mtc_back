package com.mtc.mutuaConseil.services.servicesImpl.assuMutuelIndiv;

import com.microsoft.playwright.Locator;
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
public class HennerMutuelIndivPlayWrightService extends BasePlaywrightService implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(HennerMutuelIndivPlayWrightService.class);
    private final TypeAssuranceService typeAssuranceService;

    public HennerMutuelIndivPlayWrightService(TypeAssuranceService typeAssuranceService) {
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
            waitThread(7);
            String cout = getElementTextByXpath("/html/body/app-root/app-auth/div/div/div/div/app-calculator/div/main/app-indiv/app-indiv-pricing/div/div[5]/div[3]/div[2]/div/div[1]/div[2]");
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
        waitThread(1);
        elementLib.clickByRole("TOUT ACCEPTER");
        elementLib.humanTypeById("mat-input-0", c.getUsername());
        elementLib.humanTypeById("mat-input-1", c.getPassword());
        waitThread(1);
        elementLib.clickByXpath("//span[normalize-space()='Se connecter']");
        waitThread(1);
    }

    private void remplirCreationDevis() {
        elementLib.click("a[title=\"Création d'un devis\"]");
    }

    private void remplirChoixDevis() {
        elementLib.clickByXpath("//span[normalize-space()='PARTICULIER']");
    }

    private void remplirContrat(FluxData flux) {
        waitThread(1);
        elementLib.humanTypeByXpath("//input[@data-placeholder='Date de naissance']", flux.getPersonnes().getFirst().getDateNaissance());
        waitThread(1);
        elementLib.typeByLabel("Code postal", flux.getPersonnes().getFirst().getCodePostal());
        waitThread(1);
        elementLib.typeByLabel("Nom (facultatif)", flux.getPersonnes().getFirst().getNom());
        waitThread(1);
        elementLib.typeByLabel("Prénom (facultatif)", flux.getPersonnes().getFirst().getPrenom());
        waitThread(1);
        elementLib.clickByRole("VALIDER");
    }

    private void remplirSante() {
        waitThread(1);
        elementLib.clickByTextElement("Santé");
    }

    private void remplirDevisSante(FluxData flux) {
        waitThread(1);
        elementLib.humanTypeByXpath("//input[@data-placeholder=\"Date d'effet\"]", dateEffet(1));
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
        waitThread(1);
        elementLib.humanTypeByXpath(
            "/html/body/app-root/app-auth/div/div/div/div/app-calculator/div/main/app-indiv/app-indiv-recap/app-indiv-client-info/div/div/div[2]/div/div/div/app-indiv-form/div/form/div[1]/div/div[2]/div/div/div[2]/div[1]/mat-form-field/div/div[1]/div[1]/input",
            flux.getPersonnes().get(1).getDateNaissance());
        choixRegime("/html/body/app-root/app-auth/div/div/div/div/app-calculator/div/main/app-indiv/app-indiv-recap/app-indiv-client-info/div/div/div[2]/div/div/div/app-indiv-form/div/form/div[1]/div/div[2]/div/div/div[2]/div[2]/mat-form-field/div/div[1]/div/mat-select/div/div[1]/span");
    }

    private void remplirEnfant(FluxData flux, String xpathDateNaissance, String xpathRegime, int index) {
        ajoutEnfant();
        waitThread(1);
        elementLib.humanTypeByXpath(xpathDateNaissance, flux.getEnfants().get(index).getDateNaissance());
        choixRegime(xpathRegime);
    }

    private void choixRegime(String xpath) {
        waitThread(1);
        elementLib.clickByXpath(xpath);
        waitThread(1);
        Locator options = page.locator("xpath=//div[@role='listbox']//mat-option[@role='option']");
        for (int i = 0; i < options.count(); i++) {
            if (options.nth(i).textContent().trim().equalsIgnoreCase("Régime Général")) {
                options.nth(i).click();
                break;
            }
        }
    }

    private void ajoutConjoint() {
        waitThread(1);
        elementLib.clickByTextElement("Ajouter un conjoint");
    }

    private void ajoutEnfant() {
        waitThread(1);
        elementLib.clickByTextElement("Ajouter un enfant");
    }

    private void suivant() {
        waitThread(1);
        elementLib.clickByTextElement("TARIFER");
    }

    private String dateEffet(int mois) {
        return LocalDate.now()
                .plus(mois, ChronoUnit.MONTHS)
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}
