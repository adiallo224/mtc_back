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
import com.mtc.mutuaConseil.services.LaunchedService;
import com.mtc.mutuaConseil.services.servicesImpl.TypeAssuranceService;
import com.mtc.mutuaConseil.utils.TarifUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class ApiviaMIService extends BasePlaywrightService implements LaunchedService {

    private final TypeAssuranceService typeAssuranceService;

    public ApiviaMIService(TypeAssuranceService typeAssuranceService) {
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        log.info("Début de traitement -- Apivia_Mutuel_Indiv (Playwright)");

        Tarif tarif = TarifUtils.createDefaultTarif(c, typeAssuranceService, 2L);
        tarif.setNom(c.getNomFournisseur());
        TypeAssurance typeAssurance = new TypeAssurance();
        typeAssurance.setTypeAssurance(EnumTypeAssurance.MUTUELLE_INDIV);
        tarif.setTypeAssurance(typeAssurance);

        try {
            initializeBrowser(false);
            connexion(c);
            remplirOffres();
            remplirTarificateur();
            remplirDevoirDeConseils();
            remplirContrat(flux);
            calculer();
            page.waitForLoadState(LoadState.NETWORKIDLE);
            elementLib.randomWait(4000, 6000);
            String cout = getPrixTtcParNiveau(2);
            cout = cout.substring(0, Math.min(8, cout.length()));
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
        elementLib.randomWait(700, 1300);
        elementLib.humanTypeById("username", c.getUsername());
        elementLib.humanTypeById("password", c.getPassword());
        elementLib.clickByRole("Connexion");
        elementLib.randomWait(2500, 3500);
    }

    private void remplirOffres() {
        elementLib.clickByTextElement("Nos offres");
        elementLib.randomWait(700, 1300);
    }

    private void remplirTarificateur() {
        elementLib.clickByXpath("//div[@data-type='particulier individuel']");
        elementLib.randomWait(700, 1300);
    }

    private void remplirDevoirDeConseils() {
        elementLib.clickById("tarification_recueilBesoins_0");
        elementLib.clickByXpath("//span[@class='switch-label']");
        elementLib.randomWait(700, 1300);
    }

    private void remplirContrat(FluxData flux) {
        elementLib.humanTypeById("tarification_codePostal", flux.getPersonnes().getFirst().getCodePostal());
        elementLib.humanTypeById("tarification_dateEffet", dateEffet(1));
        clickBody();
        choixAnneeNaissance(flux, 0);
        elementLib.randomWait(700, 1300);
        choixRegime(flux, 0);
        elementLib.randomWait(700, 1300);
        remplirConjoint(flux);
        remplirEnfants(flux);
        scrollDown(300);
        elementLib.click("//div//label[normalize-space()='Vitamin3']");
    }

    private void remplirConjoint(FluxData flux) {
        elementLib.randomWait(700, 1300);
        if (flux.getPersonnes().size() == 2) {
            choixAnneeNaissance(flux, 1);
            choixRegime(flux, 1);
        }
    }

    private void remplirEnfants(FluxData flux) {
        if (flux.getEnfants().getFirst().getNom() != null && !flux.getEnfants().getFirst().getNom().isEmpty()) {
            elementLib.randomWait(700, 1300);
            ajoutBeneficiaire();
            choixAnneeNaissanceEnfant(flux, 0);
            choixRegimeEnfant(flux, 0);
        }
        if (flux.getEnfants().size() >= 2) {
            elementLib.randomWait(700, 1300);
            ajoutBeneficiaire();
            choixAnneeNaissanceEnfant(flux, 1);
            choixRegimeEnfant(flux, 1);
        }
    }

    private void ajoutBeneficiaire() {
        elementLib.clickByRole("Ajouter un bénéficiaire");
    }

    private void choixAnneeNaissance(FluxData flux, int index) {
        elementLib.randomWait(700, 1300);
        String annee = extractYear(flux.getPersonnes().get(index).getDateNaissance());
        String selectId = index == 0 ? "tarification_assure_dateNaissance" : "tarification_conjoint_dateNaissance";
        selectOptionEquals(selectId, annee);
    }

    private void choixAnneeNaissanceEnfant(FluxData flux, int index) {
        elementLib.randomWait(700, 1300);
        String annee = extractYear(flux.getEnfants().get(index).getDateNaissance());
        String id = index == 0
                ? "tarification_beneficiaires_0_dateNaissance"
                : "tarification_beneficiaires_1_dateNaissance";
        selectOptionEqualsByXpath(id, annee);
    }

    private void choixRegime(FluxData flux, int index) {
        elementLib.randomWait(700, 1300);
        String selectId = index == 0 ? "tarification_assure_regime" : "tarification_conjoint_regime";
        selectOptionEquals(selectId, "Assure social");
    }

    private void choixRegimeEnfant(FluxData flux, int index) {
        elementLib.randomWait(700, 1300);
        String id = index == 0
                ? "tarification_beneficiaires_0_regime"
                : "tarification_beneficiaires_1_regime";
        selectOptionEqualsByXpath(id, "Assure social");
    }

    private void calculer() {
        elementLib.randomWait(700, 1300);
        elementLib.clickById("tarification_tarif");
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

    private void selectOptionEqualsByXpath(String id, String search) {
        Locator select = page.locator("id=" + id);
        Locator opts = select.locator("option");
        int count = opts.count();
        for (int i = 0; i < count; i++) {
            Locator opt = opts.nth(i);
            if (opt.innerText().trim().equalsIgnoreCase(search)) {
                select.selectOption(new SelectOption().setValue(opt.getAttribute("value")));
                break;
            }
        }
    }

    private String dateEffet(int months) {
        LocalDate today = LocalDate.now();
        LocalDate nextMonth = today.plusMonths(months);
        return nextMonth.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private static String extractYear(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        String[] parts = dateStr.split("/");
        return parts.length == 3 ? parts[2] : null;
    }

    private String getPrixTtcParNiveau(int niveau) {
        String selecteur = String.format(
                "tr[data-tarificateur--sante--sante-individuelle-apivia--tarification--tarifs-target='tarifsContainer'] " +
                        "td[data-niveau='Niveau %d'] .formule_prix",
                niveau
        );
        Locator elementPrix = page.locator(selecteur);
        return elementPrix.innerText().trim().replaceAll("\\s+", " ");
    }

}
