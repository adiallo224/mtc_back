package com.mtc.mutuaConseil.services.servicesImpl.assuMutuelPro;


import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
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

import static java.util.Objects.nonNull;

@Service
public class HennerMProService extends BasePlaywrightService implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(HennerMProService.class);
    private final TypeAssuranceService typeAssuranceService;

    public HennerMProService(TypeAssuranceService typeAssuranceService) {
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        log.info("Début de traitement -- Henner_Mutuel_Pro (Playwright)");

        Tarif tarif = TarifUtils.createDefaultTarif(c, typeAssuranceService, 3L);
        tarif.setNom(c.getNomFournisseur());
        TypeAssurance typeAssurance = new TypeAssurance();
        typeAssurance.setTypeAssurance(EnumTypeAssurance.MUTUELLE_PRO);
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
            String cout = getPrixByFormule(4);
            log.info("cout {}", cout);
            tarif.setMontant(cout);
            elementLib.randomWait(1000, 3000);
            String screenshotBytes = captureScreenshot(tarif.getNom(), false, tarif);
            if (nonNull(screenshotBytes)) {
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
        elementLib.clickByXpath("//span[normalize-space()='TNS']");
    }

    private void remplirContrat(FluxData flux) {
        elementLib.randomWait(700, 1300);
        elementLib.humanTypeByXpath("//input[@data-placeholder='Date de naissance']", flux.getPersonnes().getFirst().getDateNaissance());
        clickBody();
        elementLib.randomWait(700, 1300);
        elementLib.typeByLabel("Code postal", flux.getPersonnes().getFirst().getCodePostal());
        choixStatut(flux);
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

    private void choixStatut(FluxData flux) {
        elementLib.randomWait(700, 1300);
        elementLib.clickByXpath("//mat-form-field[.//label[contains(normalize-space(.), 'Statut')]]//mat-select[@role='combobox']");
        elementLib.randomWait(700, 1300);
        String professionSpecifique = flux.getPersonnes().getFirst().getProfessionSpecifique();
        boolean autoEntrepreneur = Boolean.TRUE.equals(flux.getPersonnes().getFirst().getAutoEntrepreneur());
        String libelleCible = autoEntrepreneur
                ? "Auto-entrepreneur dépendant des régimes micro-BNC ou micro-BIC"
                : mapProfessionSpecifiqueToStatut(professionSpecifique);
        if (libelleCible.isEmpty()) {
            log.warn("Aucun statut TNS correspondant pour professionSpecifique={}", professionSpecifique);
            return;
        }
        Locator options = page.locator("xpath=//div[@role='listbox']//mat-option[@role='option']");
        for (int i = 0; i < options.count(); i++) {
            if (options.nth(i).textContent().trim().equalsIgnoreCase(libelleCible)) {
                options.nth(i).click();
                break;
            }
        }
    }

    private String mapProfessionSpecifiqueToStatut(String professionSpecifique) {
        if (professionSpecifique == null) {
            return "";
        }
        return switch (professionSpecifique.trim().toLowerCase()) {
            case "agriculteur" -> "Exploitant agricole";
            case "artisan", "commerçant", "ouvrier" ->
                    "Artisan ou commerçant soumis à l’impôt sur le bénéfice industriel et commercial (BIC)";
            case "chef d'entreprise" ->
                    "Gérant non-salarié d’une EURL, SARL ou SELARL relevant de l’article 62 du CGI";
            case "profession libérale", "profession libérale médicale", "profession libérale paramédicale" ->
                    "Professionnel libéral soumis à l’impôt sur le bénéfice non commercial (BNC)";
            case "salarié cadre", "salarié non cadre : employé" -> "Mandataire social assimilé à un salarié";
            default -> "";
        };
    }

    public String getPrixFormuleActive() {
        return page.locator(
                        ".henner-bar.active")
                .locator("xpath=ancestor::div[contains(@class,'indiv-pricing--content--bloc-item')]")
                .locator(".indiv-pricing--content--bloc-item-element-top--price-amount")
                .textContent()
                .trim();
    }

    public String getPrixByFormule(int formule) {
        Locator label = page.locator(
                ".indiv-pricing--content--bloc-item-element-levels-label",
                new Page.LocatorOptions().setHasText("Fomule " + formule)
        );

        Locator card = label.locator("xpath=ancestor::div[contains(@class,'indiv-pricing--content--bloc-item')]");

        return card.locator(
                ".indiv-pricing--content--bloc-item-element-top--price-amount"
        ).textContent().trim();
    }
}
