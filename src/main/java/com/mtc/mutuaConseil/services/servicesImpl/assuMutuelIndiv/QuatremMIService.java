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
public class QuatremMIService extends BasePlaywrightService implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(QuatremMIService.class);
    private final TypeAssuranceService typeAssuranceService;

    public QuatremMIService(TypeAssuranceService typeAssuranceService) {
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        log.info("Début de traitement -- QuatremIndiv_Mutuel_Indiv (Playwright)");

        Tarif tarif = TarifUtils.createDefaultTarif(c, typeAssuranceService, 2L);
        tarif.setNom(c.getNomFournisseur());
        TypeAssurance typeAssurance = new TypeAssurance();
        typeAssurance.setTypeAssurance(EnumTypeAssurance.MUTUELLE_INDIV);
        tarif.setTypeAssurance(typeAssurance);

        try {
            initializeBrowser(false);
            connexion(c);
            remplirContrat(flux);
            remplirIdentite(flux);
            remplirCoordonnees(flux);
            remplirCouverture();
            if (flux.getPersonnes().size() >= 2 || (flux.getEnfants().getFirst().getNom() != null && !flux.getEnfants().getFirst().getNom().isEmpty())) {
                remplirBenficiaires(flux);
            }
            scrollDown(300);
            suivant();
            page.waitForLoadState(LoadState.NETWORKIDLE);
            elementLib.randomWait(6000, 8000);
            String cout = getElementTextByXpath("//*[@id='formInfoChoixCotisation']/div/table/tbody/tr[2]/td[4]");
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
        clickIfExists("//*[@id=\"bandeauAcceptationCookies\"]/div/div[2]/a[3]");
        elementLib.humanTypeById("login", c.getUsername());
        elementLib.humanTypeById("pwd", c.getPassword());
        elementLib.randomWait(700, 1300);
        elementLib.clickById("authentificateSubmit");
        elementLib.randomWait(700, 1300);
    }

    private void remplirContrat(FluxData flux) {
        elementLib.humanTypeById("DateEffetSouhaitee", dateEffet(1));
    }

    private void remplirIdentite(FluxData flux) {
        choixCivilite(flux, 0);
        elementLib.humanTypeById("NomSouscripteur", flux.getPersonnes().getFirst().getNom());
        elementLib.humanTypeById("PrenomSouscripteur", flux.getPersonnes().getFirst().getPrenom());
        elementLib.humanTypeById("DateNaissanceSouscripteur", flux.getPersonnes().getFirst().getDateNaissance());
        choixRegime(flux, 0);
    }

    private void remplirCoordonnees(FluxData flux) {
        elementLib.humanTypeById("AdresseSouscripteur", flux.getPersonnes().getFirst().getNumeroVoie() + " " + flux.getPersonnes().getFirst().getNomVoie());
        elementLib.humanTypeById("CodePostalSouscripteur", flux.getPersonnes().getFirst().getCodePostal());
        elementLib.humanTypeById("VilleSouscripteur", flux.getPersonnes().getFirst().getVille());
        elementLib.humanTypeById("EmailSouscripteur", flux.getPersonnes().getFirst().getEmail());
        elementLib.humanTypeById("PortableSouscripteur", flux.getPersonnes().getFirst().getTelephone());
    }

    private void remplirCouverture() {
        elementLib.clickById("RiaIndicateurCouverture-false");
    }

    private void remplirBenficiaires(FluxData flux) {
        elementLib.randomWait(700, 1300);
        if (flux.getPersonnes().size() >= 2) {
            scrollDown(200);
            ajoutSouscripteur();
            elementLib.randomWait(700, 1300);
            choixLienParente("//*[@id='Beneficiaire_0_LienParente-button']", "//*[contains(@id,'Beneficiaire_0_LienParente-menu')]//li", "conjoint");
            choixLienCiviliteBeneficiaire("//*[@id='Beneficiaire_0_Civilite-button']", "//*[contains(@id,'Beneficiaire_0_Civilite-menu')]//li", "conjoint", flux, 0);
            elementLib.humanTypeById("Beneficiaire_0_Nom", flux.getPersonnes().get(1).getNom());
            elementLib.humanTypeById("Beneficiaire_0_Prenom", flux.getPersonnes().get(1).getPrenom());
            elementLib.humanTypeById("Beneficiaire_0_DateNaissance", flux.getPersonnes().get(1).getDateNaissance());
            choixRegimeBeneficiaires("//*[@id='Beneficiaire_0_Regime-button']", "//*[contains(@id,'Beneficiaire_0_Regime-menu')]//li");
        }
        if (flux.getEnfants().getFirst().getNom() != null && !flux.getEnfants().getFirst().getNom().isEmpty()) {
            ajoutSouscripteur();
            elementLib.randomWait(700, 1300);
            choixLienParente("//*[@id='Beneficiaire_1_LienParente-button']", "//*[contains(@id,'Beneficiaire_1_LienParente-menu')]//li", "enfant");
            choixLienCiviliteBeneficiaire("//*[@id='Beneficiaire_1_Civilite-button']", "//*[contains(@id,'Beneficiaire_1_Civilite-menu')]//li", "enfant", flux, 0);
            elementLib.humanTypeById("Beneficiaire_1_Nom", flux.getEnfants().getFirst().getNom());
            elementLib.humanTypeById("Beneficiaire_1_Prenom", flux.getEnfants().getFirst().getPrenom());
            elementLib.humanTypeById("Beneficiaire_1_DateNaissance", flux.getEnfants().getFirst().getDateNaissance());
            choixRegimeBeneficiaires("//*[@id='Beneficiaire_1_Regime-button']", "//*[contains(@id,'Beneficiaire_1_Regime-menu')]//li");
        }
        if (flux.getEnfants().size() == 2) {
            ajoutSouscripteur();
            elementLib.randomWait(700, 1300);
            choixLienParente("//*[@id='Beneficiaire_2_LienParente-button']", "//*[contains(@id,'Beneficiaire_2_LienParente-menu')]//li", "enfant");
            choixLienCiviliteBeneficiaire("//*[@id='Beneficiaire_2_Civilite-button']", "//*[contains(@id,'Beneficiaire_2_Civilite-menu')]//li", "enfant", flux, 1);
            elementLib.humanTypeById("Beneficiaire_2_Nom", flux.getEnfants().get(1).getNom());
            elementLib.humanTypeById("Beneficiaire_2_Prenom", flux.getEnfants().get(1).getPrenom());
            elementLib.humanTypeById("Beneficiaire_2_DateNaissance", flux.getEnfants().get(1).getDateNaissance());
            choixRegimeBeneficiaires("//*[@id='Beneficiaire_2_Regime-button']", "//*[contains(@id,'Beneficiaire_2_Regime-menu')]//li");
        }
    }

    private void ajoutSouscripteur() {
        elementLib.clickById("ajoutGroupeBeneficiaireInfoSouscripteur");
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
        elementLib.clickById("next");
    }

    private void choixRegime(FluxData flux, int index) {
        elementLib.randomWait(700, 1300);
        if (index == 0) {
            elementLib.clickByXpath("//*[@id=\"RegimeSouscripteur-button\"]");
            elementLib.randomWait(700, 1300);
            page.locator("xpath=//*[contains(@id,'RegimeSouscripteur-menu')]//li").nth(1).click();
        }
    }

    private void choixCivilite(FluxData flux, int index) {
        elementLib.randomWait(700, 1300);
        if (index == 0) {
            elementLib.clickByXpath("//*[@id='CiviliteSouscripteur-button']");
            elementLib.randomWait(700, 1300);
            Locator options = page.locator("xpath=//*[contains(@id,'CiviliteSouscripteur-menu')]//li");
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
}
