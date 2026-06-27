package com.mtc.mutuaConseil.services.servicesImpl.assuMutuelIndiv;

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
public class EcaHeomieMutuelIndivPlayWrightService extends BasePlaywrightService implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(EcaHeomieMutuelIndivPlayWrightService.class);
    private final TypeAssuranceService typeAssuranceService;

    public EcaHeomieMutuelIndivPlayWrightService(TypeAssuranceService typeAssuranceService) {
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        log.info("Début de traitement -- Eca_Heomie_Mutuel_Indiv (Playwright)");

        Tarif tarif = TarifUtils.createDefaultTarif(c, typeAssuranceService, 2L);
        tarif.setNom(c.getNomFournisseur());
        TypeAssurance typeAssurance = new TypeAssurance();
        typeAssurance.setTypeAssurance(EnumTypeAssurance.MUTUELLE_INDIV);
        tarif.setTypeAssurance(typeAssurance);

        try {
            initializeBrowser(false);
            humanLikeNavigate(c.getUrlFournisseur());
            connexion(c);
            choixComplementaire();
            remplirComplementaireSante(flux);
            scrollDown(350);
            suivant();
            waitThread(5);
            String cout = elementLib.getElementTextByXpath("//*[@id='tarif_sante_OPTION_BUDGET_150_B']");
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
        elementLib.humanTypeById("login-name", c.getUsername());
        elementLib.humanTypeById("login-password", c.getPassword());
        elementLib.clickByXpath("//form//button[@type='submit']");
        waitThread(1);
    }

    private void choixComplementaire() {
        elementLib.clickByXpath("//a[@href='https://partenaire.heomi.fr/nouveau-devis/particulier']");
        waitThread(1);
        elementLib.clickByXpath("//a[@href='https://partenaire.heomi.fr/particulier/presentation/SANTE']");
        scrollDown(350);
        elementLib.clickByXpath("//a[@href='https://partenaire.heomi.fr/particulier/nouveauDevis/SANTE']");
    }

    private void remplirComplementaireSante(FluxData flux) {
        elementLib.humanTypeById("date_effet_sante", dateEffet(1));
        choixRegime(flux, 0);
        elementLib.humanTypeById("dn_assure", flux.getPersonnes().getFirst().getDateNaissance());

        if (flux.getPersonnes().size() == 2) {
            elementLib.clickById("has_conjoint_sante-0");
            waitThread(1);
            elementLib.humanTypeById("dn_conjoint", flux.getPersonnes().get(1).getDateNaissance());
        }
        elementLib.humanTypeById("code_postal", flux.getPersonnes().getFirst().getCodePostal());

        choixNbEnfants(flux);
        remplirEnfant(flux);
        scrollDown(400);

        elementLib.clickById("budget_entre_50_100");
        elementLib.clickById("couverture_sante_non");
        elementLib.clickById("beneficiaire_css_non");
        scrollDown(150);
        elementLib.clickById("soins_generaux_faible");
        elementLib.clickById("hospitalisation_faible");
        scrollDown(150);
        elementLib.clickById("optique_faible");
        scrollDown(250);
        elementLib.clickById("dentaire_faible");
        elementLib.clickById("appareil_auditif_faible");
        elementLib.clickById("medecines_douces_non");
    }

    private void remplirEnfant(FluxData flux) {
        if (flux.getEnfants().getFirst().getNom() != null && !flux.getEnfants().getFirst().getNom().isEmpty()) {
            elementLib.humanTypeById("dn_enfant_sante_1", flux.getEnfants().getFirst().getDateNaissance());
            scrollDown(100);
        }
        if (flux.getEnfants().size() >= 2) {
            elementLib.humanTypeById("dn_enfant_sante_2", flux.getEnfants().get(1).getDateNaissance());
            scrollDown(400);
        }
    }

    private void choixNbEnfants(FluxData flux) {
        boolean sansEnfant = flux.getEnfants().getFirst().getNom() == null || flux.getEnfants().getFirst().getNom().isEmpty();
        int nbEnfants = sansEnfant ? 0 : flux.getEnfants().size();
        elementLib.selectByLabel("#nbr_enfants_sante", String.valueOf(nbEnfants));
    }

    private void suivant() {
        elementLib.clickByXpath("//*[@id='calculer_tarif']");
    }

    private void choixRegime(FluxData flux, int index) {
        elementLib.selectByLabel("#regime_social_sante", "Régime Général");
    }

    private String dateEffet(int mois) {
        return LocalDate.now()
                .plus(mois, ChronoUnit.MONTHS)
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }
}
