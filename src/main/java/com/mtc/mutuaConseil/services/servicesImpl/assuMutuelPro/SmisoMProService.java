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
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static java.util.Objects.nonNull;

@Service
public class SmisoMProService extends BasePlaywrightService implements LaunchedService {

    private final TypeAssuranceService typeAssuranceService;

    public SmisoMProService(TypeAssuranceService typeAssuranceService) {
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        log.info("Début de traitement -- Smiso_Mutuel_Pro (Playwright)");
        Tarif tarif = TarifUtils.createDefaultTarif(c, typeAssuranceService, 3L);
        tarif.setNom(c.getNomFournisseur());
        TypeAssurance typeAssurance = new TypeAssurance();
        typeAssurance.setTypeAssurance(EnumTypeAssurance.MUTUELLE_PRO);
        tarif.setTypeAssurance(typeAssurance);

        try {
            initializeBrowser(false);
            connexion(c);
            nouveauProjet();
            remplirBenficiaires(flux);
            remplirCouverture();
            remplirInformationsDeContact(flux);
            remplirBesoins();
            remplirProposition();
            page.waitForLoadState(LoadState.NETWORKIDLE);
            elementLib.randomWait(1000, 3000);
            List<String> couts = getPrixParNiveau(c.getNiveau());
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

    private void connexion(Compte c) {
        humanLikeNavigate(c.getUrlFournisseur());
        clickIfExists("//*[@id=\"bandeauAcceptationCookies\"]/div/div[2]/a[3]");
        elementLib.humanTypeByXpath("//input[@name='username']", c.getUsername());
        elementLib.humanTypeById("password", c.getPassword());
        elementLib.randomWait(300, 1000);
        elementLib.clickById("kc-login");
        elementLib.randomWait(300, 1000);
    }

    private void nouveauProjet() {
        elementLib.humanClick("a[href='/projets/type']");
        elementLib.humanClick("h3:has-text('Un particulier ou un TNS')");
    }

    private void remplirBenficiaires(FluxData flux) {
        elementLib.randomWait(600, 1300);
        elementLib.humanTypeById("postalCode", flux.getPersonnes().getFirst().getCodePostal());
        elementLib.humanTypeByXpath("//input[@placeholder='JJ/MM/AAAA']", flux.getPersonnes().getFirst().getDateNaissance());
        elementLib.humanClick("//div[@id='souscripteur']//input[@id='regimeCode']");
        elementLib.randomWait(600, 1300);
        elementLib.humanClick("//li[@id='regimeCode-option-0']");
        elementLib.clickByTextElement("Travailleur non salarié (TNS)");
        if (flux.getPersonnes().size() > 1) {
            elementLib.humanClick("span:has-text('Son conjoint')");
            elementLib.humanTypeByXpath("//div[@id='conjoint']//input[@placeholder='JJ/MM/AAAA']", flux.getPersonnes().get(1).getDateNaissance());
            elementLib.humanClick("//div[@id='conjoint']//input[@id='regimeCode']");
            elementLib.randomWait(600, 1300);
            elementLib.humanClick("//li[@id='regimeCode-option-0']");
        }
        if (!flux.getEnfants().getFirst().getNom().isEmpty() && !flux.getEnfants().getFirst().getNom().isBlank()) {
            elementLib.humanClick("span:has-text('Ses enfants')");
            elementLib.humanTypeByXpath("//div[@id='enfant(s)']//input[@placeholder='JJ/MM/AAAA']", flux.getEnfants().getFirst().getDateNaissance());
            elementLib.humanClick("//div[@id='enfant(s)']//input[@id='regimeCode']");
            elementLib.randomWait(600, 1300);
            elementLib.humanClick("//li[@id='regimeCode-option-0']");
            if (flux.getEnfants().size() >= 2) {
                elementLib.humanClick("//button[normalize-space()='Ajouter un enfant']");
                elementLib.humanTypeById("//div[@id='enfant2']//input[@placeholder='JJ/MM/AAAA']", flux.getEnfants().get(1).getDateNaissance());
                elementLib.humanClick("//div[@id='enfant2']//input[@id='regimeCode']");
                elementLib.randomWait(600, 1300);
                elementLib.humanClick("//li[@id='regimeCode-option-0']");
            }
        }

        elementLib.click("//button[normalize-space()='Valider']");
    }

    private void remplirCouverture() {
        elementLib.randomWait(600, 1300);
        elementLib.clickByXpath("//h3[normalize-space()=\"Activ' Santé TNS\"]");
        elementLib.humanClick("//button[normalize-space()='Créer un devis']");
    }

    private void remplirInformationsDeContact(FluxData flux) {
        elementLib.randomWait(600, 1300);
        choixCivilite(flux, 0);
        elementLib.humanTypeById("lastname", flux.getPersonnes().getFirst().getNom());
        elementLib.humanTypeById("firstname", flux.getPersonnes().getFirst().getPrenom());
        elementLib.humanTypeByXpath("//input[@name='phone']", flux.getPersonnes().getFirst().getTelephone());
        elementLib.humanTypeById("email", flux.getPersonnes().getFirst().getEmail());
        elementLib.humanClick("//button[normalize-space()='Valider']");
    }

    private void remplirBesoins() {
        elementLib.randomWait(600, 1300);
        Locator cards = page.locator("div.MuiCard-root");

        int count = cards.count();

        for (int i = 0; i < count; i++) {

            Locator card = cards.nth(i);

            Locator equilibre = card.locator("button[aria-label='Equilibré']");

            if (equilibre.count() > 0) {
                elementLib.randomWait(200, 1000);
                equilibre.click();
            }
        }
        elementLib.humanClick("//button[normalize-space()='Valider']");
    }

    private void remplirProposition() {
        elementLib.randomWait(600, 1300);
        elementLib.humanClick("//p[normalize-space()='Ajouter une solution']");
    }

    private void choixCivilite(FluxData flux, int index) {
        elementLib.randomWait(600, 1300);
        if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M"))
            elementLib.humanClick("//button[normalize-space()='Monsieur']");
        if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme"))
            elementLib.humanClick("//button[normalize-space()='Madame']");
    }

    private List<String> getPrixParNiveau(int niveau) {
        Locator cartes = page.locator("div.MuiCard-root")
                .filter(new Locator.FilterOptions()
                        .setHasText(Pattern.compile("Formule \\d+%")));
        List<String> prix = new ArrayList<>();

        for (int i = niveau; i < cartes.count(); i++) {
            String montant = cartes
                    .nth(i)
                    .locator("code span")
                    .first()
                    .innerText()
                    .trim();

            prix.add(montant + " €");
        }
        return prix;
    }

}
