package com.mtc.mutuaConseil.services.servicesImpl.assuMutuelPro;


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
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Actions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.nonNull;

@Service
public class RepamMProService extends BasePlaywrightService implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(RepamMProService.class);
    private final TypeAssuranceService typeAssuranceService;

    public RepamMProService(TypeAssuranceService typeAssuranceService){
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        log.info("Début de traitement -- Repam_Mutuel_Pro (Playwright)");

        Tarif tarif = TarifUtils.createDefaultTarif(c, typeAssuranceService, 3L);
        tarif.setNom(c.getNomFournisseur());
        TypeAssurance typeAssurance = new TypeAssurance();
        typeAssurance.setTypeAssurance(EnumTypeAssurance.MUTUELLE_PRO);
        tarif.setTypeAssurance(typeAssurance);

        try {
            initializeBrowser(false);
            humanLikeNavigate(c.getUrlFournisseur());
            choixCookies();
            connexion(c);
            remplirClient(flux);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            elementLib.randomWait(2500, 3500);
            List<String> couts = getPrixTtcParNiveau(c.getNiveau());
            tarif.setMontant(couts);
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

    private void choixCookies() {
        clickIfExists("[data-testid='acceptButton']");
    }

    private void connexion(Compte c) {
        elementLib.randomWait(700, 1300);
        elementLib.humanTypeByXpath("//input[@name='username']", c.getUsername());
        elementLib.humanTypeByXpath("//input[@name='password']", c.getPassword());
        elementLib.clickByXpath("//button[@type='submit']");
        elementLib.randomWait(700, 1300);
        choixFermeturePopup();
        elementLib.randomWait(700, 1300);
        choixSante();
        elementLib.randomWait(700, 1300);
        elementLib.switchToNewWindow();
        this.page = elementLib.getPage();
        elementLib.clickByXpath("//button[.//div[text()='Nouvelle proposition']]");
        elementLib.randomWait(700, 1300);
        choixOffre();
        elementLib.randomWait(700, 1300);
        elementLib.clickByXpath("//button[.//div[text()='Oui']]");
    }

    private void remplirClient(FluxData flux) {
        elementLib.randomWait(700, 1300);
        elementLib.humanTypeByXpath("//input[@placeholder='JJ/MM/AAAA']", dateEffet(1));
        elementLib.randomWait(700, 1300);
        elementLib.clickByXpath("//button[.//div[text()='Continuer']]");

        elementLib.clickByXpath("//input[@id='customer.isMember-1']");
        elementLib.humanTypeByXpath("//input[@name='customer.lastName']", flux.getPersonnes().getFirst().getNom());
        elementLib.humanTypeByXpath("//input[@name='customer.firstName']", flux.getPersonnes().getFirst().getPrenom());
        elementLib.humanTypeByXpath("//input[@name='customer.birthDate']", flux.getPersonnes().getFirst().getDateNaissance());
        elementLib.humanTypeByXpath("//input[@name='customer.address.postCode']", flux.getPersonnes().getFirst().getCodePostal());
        choixProfession(flux);
        elementLib.randomWait(700, 1300);

        if (flux.getPersonnes().size() >= 2) {
            elementLib.clickByXpath("//button[.//div[text()='Ajouter un bénéficiaire']]");
            elementLib.randomWait(700, 1300);
            elementLib.clickByXpath("//label[normalize-space()='Conjoint']");
            elementLib.humanTypeByXpath("(//input[contains(@name,'beneficiaries') and contains(@name,'birthDate')])[last()]", flux.getPersonnes().get(1).getDateNaissance());
            choixRegime();
            elementLib.clickByXpath("//input[@id='beneficiaries.0.isAlsaceMoselle-1']");
        }
        if (flux.getEnfants().getFirst().getNom() != null && !flux.getEnfants().getFirst().getNom().isEmpty()) {
            elementLib.clickByXpath("//button[.//div[text()='Ajouter un bénéficiaire']]");
            elementLib.randomWait(700, 1300);
            elementLib.clickByXpath("//label[normalize-space()='Enfant']");
            elementLib.humanTypeByXpath("(//input[contains(@name,'beneficiaries') and contains(@name,'birthDate')])[last()]", flux.getEnfants().getFirst().getDateNaissance());
            choixRegime();
            if (flux.getPersonnes().size() >= 2)
                elementLib.clickByXpath("//input[@id='beneficiaries.1.isAlsaceMoselle-1']");
            else
                elementLib.clickByXpath("//input[@id='beneficiaries.0.isAlsaceMoselle-1']");
        }
        if (flux.getEnfants().size() >= 2) {
            elementLib.clickByXpath("//button[.//div[text()='Ajouter un bénéficiaire']]");
            elementLib.randomWait(700, 1300);
            elementLib.clickByXpath("//label[normalize-space()='Enfant']");
            elementLib.humanTypeByXpath("(//input[contains(@name,'beneficiaries') and contains(@name,'birthDate')])[last()]", flux.getEnfants().get(1).getDateNaissance());
            choixRegime();
            elementLib.clickByXpath("//input[@id='beneficiaries.2.isAlsaceMoselle-1']");
            scrollDown(200);
        }
        continuer();
    }

    private void continuer() {
        elementLib.clickByXpath("//button[.//div[normalize-space()='Enregistrer et continuer']]");
    }

    private void choixProfession(FluxData flux) {
        elementLib.clickByXpath("//input[@type='hidden' and @name='customer.profession']/preceding-sibling::div[contains(@class,'-control')]");
        elementLib.randomWait(700, 1300);
        String profession = flux.getPersonnes().getFirst().getProfessionSpecifique();
        String option = switch (profession.toLowerCase()) {
            case "artisan"                                                      -> "Artisan";
            case "chef d'entreprise"                                           -> "Chef d'entreprise";
            case "commerçant"                                                  -> "Commerçant";
            case "agriculteur"                                                 -> "Exploitant Agricole";
            case "profession libérale médicale", "profession libérale paramédicale" -> "Profession Libérale de santé";
            case "profession libérale"                                         -> "Profession Libérale Non Réglementée";
            default                                                            -> "Autre";
        };
        if ("Autre".equals(option) && !profession.equalsIgnoreCase("autre")) {
            log.warn("Profession '{}' non prise en charge par ce produit TNS, sélection de 'Autre'", profession);
        }
        elementLib.clickByXpath("//div[@role='listbox']//div[@role='option' and .//span[normalize-space()='" + option + "']]");
    }

    private void choixRegime() {
        elementLib.clickByXpath("(//input[@type='hidden' and contains(@name,'.regime')])[last()]/preceding-sibling::div[contains(@class,'-control')]");
        elementLib.randomWait(700, 1300);
        elementLib.clickByXpath("//div[@role='listbox']//div[@role='option' and .//span[normalize-space()='Régime général']]");
    }

    private void choixOffre() {
        page.locator("div.shadow-md.rounded-lg")
                .filter(new Locator.FilterOptions().setHas(page.locator("p:text-is('Santé Professionnel')")))
                .locator("button")
                .filter(new Locator.FilterOptions().setHasText("Sélectionner cette offre"))
                .click();
    }

    private void choixSante() {
        page.locator("button[data-testid='action-component']")
                .filter(new Locator.FilterOptions().setHasText("Santé Individuelle"))
                .first()
                .click();
    }

    private void choixFermeturePopup() {
        page.locator("button.nrg-button-tertiary").first().click();
    }

    private String dateEffet(int mois) {
        return LocalDate.now()
                .plus(mois, ChronoUnit.MONTHS)
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private List<String> getPrixTtcParNiveau(int niveau) {
        Locator grille = page.locator("div.grid.grid-cols-6").first();

        List<String> prix = new ArrayList<>();

        for (int i = niveau - 1; i < 6; i++) {
            String texte = grille
                    .locator("> div")
                    .nth(i)
                    .locator("p.font-gotham-book")
                    .innerText()
                    .replace("/mois", "")
                    .replace("\u00A0", " ")
                    .trim();
            prix.add(texte);
        }

        return prix;
    }
}
