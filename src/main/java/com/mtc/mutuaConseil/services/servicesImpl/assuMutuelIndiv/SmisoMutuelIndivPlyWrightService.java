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
import org.springframework.stereotype.Service;

@Service
public class SmisoMutuelIndivPlyWrightService extends BasePlaywrightService implements LaunchedService {

    private final TypeAssuranceService typeAssuranceService;

    public SmisoMutuelIndivPlyWrightService(TypeAssuranceService typeAssuranceService) {
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        log.info("Début de traitement -- Smiso_Mutuel_Indiv (Playwright)");

        Tarif tarif = TarifUtils.createDefaultTarif(c, typeAssuranceService, 2L);
        tarif.setNom(c.getNomFournisseur());
        TypeAssurance typeAssurance = new TypeAssurance();
        typeAssurance.setTypeAssurance(EnumTypeAssurance.MUTUELLE_INDIV);
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
            waitThread(3);
            String cout = getMonthlyPrice("Formule 200%");
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
        elementLib.typeByXpath("//input[@name='username']", c.getUsername());
        elementLib.typeById("password", c.getPassword());
        waitThread(1);
        elementLib.clickById("kc-login");
        waitThread(1);
    }

    private void nouveauProjet() {
        elementLib.click("a[href='/projets/type']");
        elementLib.click("h3:has-text('Un particulier ou un TNS')");
    }

    private void remplirBenficiaires(FluxData flux) {
        waitThread(1);
        elementLib.typeById("postalCode", flux.getPersonnes().getFirst().getCodePostal());
        elementLib.typeByXpath("//input[@placeholder='JJ/MM/AAAA']", flux.getPersonnes().getFirst().getDateNaissance());
        elementLib.click("//div[@id='souscripteur']//input[@id='regimeCode']");
        waitThread(1/2);
        elementLib.click("//li[@id='regimeCode-option-0']");
        if (flux.getPersonnes().size() > 1) {
            elementLib.click("span:has-text('Son conjoint')");
            elementLib.typeByXpath("//div[@id='conjoint']//input[@placeholder='JJ/MM/AAAA']", flux.getPersonnes().get(1).getDateNaissance());
            elementLib.click("//div[@id='conjoint']//input[@id='regimeCode']");
            waitThread(1/2);
            elementLib.click("//li[@id='regimeCode-option-0']");
        }
        if (!flux.getEnfants().getFirst().getNom().isEmpty() && !flux.getEnfants().getFirst().getNom().isBlank()) {
            elementLib.click("span:has-text('Ses enfants')");
            elementLib.typeByXpath("//div[@id='enfant(s)']//input[@placeholder='JJ/MM/AAAA']", flux.getEnfants().getFirst().getDateNaissance());
            elementLib.click("//div[@id='enfant(s)']//input[@id='regimeCode']");
            waitThread(1/2);
            elementLib.click("//li[@id='regimeCode-option-0']");
            if (flux.getEnfants().size() >= 2) {
                elementLib.click("//button[normalize-space()='Ajouter un enfant']");
                elementLib.typeById("//div[@id='enfant2']//input[@placeholder='JJ/MM/AAAA']", flux.getEnfants().get(1).getDateNaissance());
                elementLib.click("//div[@id='enfant2']//input[@id='regimeCode']");
                waitThread(1/2);
                elementLib.click("//li[@id='regimeCode-option-0']");
            }
        }

        elementLib.click("//button[normalize-space()='Valider']");
    }

    private void remplirCouverture() {
        waitThread(1);
        elementLib.clickByXpath("//h3[normalize-space()='Génération 100% Nous']");
        elementLib.click("//button[normalize-space()='Créer un devis']");
    }

    private void remplirInformationsDeContact(FluxData flux) {
        waitThread(1);
        choixCivilite(flux, 0);
        elementLib.typeById("lastname", flux.getPersonnes().getFirst().getNom());
        elementLib.typeById("firstname", flux.getPersonnes().getFirst().getPrenom());
        elementLib.typeByXpath("//input[@name='phone']", flux.getPersonnes().getFirst().getTelephone());
        elementLib.typeById("email", flux.getPersonnes().getFirst().getEmail());
        elementLib.click("//button[normalize-space()='Valider']");
    }

    private void remplirBesoins() {
        waitThread(1);
        Locator cards = page.locator("div.MuiCard-root");

        int count = cards.count();

        for (int i = 0; i < count; i++) {

            Locator card = cards.nth(i);

            Locator equilibré = card.locator("button[aria-label='Equilibré']");

            if (equilibré.count() > 0) {
                equilibré.click();
            }
        }
        elementLib.click("//button[normalize-space()='Valider']");
    }

    private void remplirProposition() {
        waitThread(1);
        elementLib.click("//p[normalize-space()='Ajouter une solution']");
    }

    private void choixCivilite(FluxData flux, int index) {
        waitThread(1);
        if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Monsieur") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("M"))
            elementLib.click("//button[normalize-space()='Monsieur']");
        if (flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Madame") || flux.getPersonnes().get(index).getCivilite().equalsIgnoreCase("Mme"))
            elementLib.click("//button[normalize-space()='Madame']");
    }

    private String getMonthlyPrice(String formule) {
        Locator card = elementLib.waitForElement("div.MuiCard-root:has(p:text('" + formule + "'))", 10);
        Locator monthlyBlock = card.locator("div").filter(
                new Locator.FilterOptions()
                        .setHas(page.locator("span:text('par mois')"))
        );
        return monthlyBlock.locator("span").last().innerText().trim();
    }

}
