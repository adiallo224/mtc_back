package com.mtc.mutuaConseil.services.servicesImpl.assuMutuelPro;

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
public class SmisoMProService extends BasePlaywrightService implements LaunchedService {

    private final TypeAssuranceService typeAssuranceService;

    public SmisoMProService(TypeAssuranceService typeAssuranceService) {
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
            String cout = obtenirCotisationParMois("Formule 200%");
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
        elementLib.humanTypeByXpath("//input[@name='username']", c.getUsername());
        elementLib.humanTypeById("password", c.getPassword());
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
        elementLib.humanTypeById("postalCode", flux.getPersonnes().getFirst().getCodePostal());
        elementLib.humanTypeByXpath("//input[@placeholder='JJ/MM/AAAA']", flux.getPersonnes().getFirst().getDateNaissance());
        elementLib.click("//div[@id='souscripteur']//input[@id='regimeCode']");
        waitThread(1/2);
        elementLib.click("//li[@id='regimeCode-option-0']");
        if (flux.getPersonnes().size() > 1) {
            elementLib.click("span:has-text('Son conjoint')");
            elementLib.humanTypeByXpath("//div[@id='conjoint']//input[@placeholder='JJ/MM/AAAA']", flux.getPersonnes().get(1).getDateNaissance());
            elementLib.click("//div[@id='conjoint']//input[@id='regimeCode']");
            waitThread(1/2);
            elementLib.click("//li[@id='regimeCode-option-0']");
        }
        if (!flux.getEnfants().getFirst().getNom().isEmpty() && !flux.getEnfants().getFirst().getNom().isBlank()) {
            elementLib.click("span:has-text('Ses enfants')");
            elementLib.humanTypeByXpath("//div[@id='enfant(s)']//input[@placeholder='JJ/MM/AAAA']", flux.getEnfants().getFirst().getDateNaissance());
            elementLib.click("//div[@id='enfant(s)']//input[@id='regimeCode']");
            waitThread(1/2);
            elementLib.click("//li[@id='regimeCode-option-0']");
            if (flux.getEnfants().size() >= 2) {
                elementLib.click("//button[normalize-space()='Ajouter un enfant']");
                elementLib.humanTypeById("//div[@id='enfant2']//input[@placeholder='JJ/MM/AAAA']", flux.getEnfants().get(1).getDateNaissance());
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
        elementLib.humanTypeById("lastname", flux.getPersonnes().getFirst().getNom());
        elementLib.humanTypeById("firstname", flux.getPersonnes().getFirst().getPrenom());
        elementLib.humanTypeByXpath("//input[@name='phone']", flux.getPersonnes().getFirst().getTelephone());
        elementLib.humanTypeById("email", flux.getPersonnes().getFirst().getEmail());
        elementLib.click("//button[normalize-space()='Valider']");
    }

    private void remplirBesoins() {
        waitThread(1);
        Locator cards = page.locator("div.MuiCard-root");

        int count = cards.count();

        for (int i = 0; i < count; i++) {

            Locator card = cards.nth(i);

            Locator equilibre = card.locator("button[aria-label='Equilibré']");

            if (equilibre.count() > 0) {
                equilibre.click();
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

    private String obtenirCotisationParMois(String formule) {
        return obtenirCotisationParMois(formule, 0);
    }

    private String obtenirCotisationParMois(String formule, int niveauIndex) {
        Locator conteneurCartes = page.locator(".MuiGrid-container");
        Locator carteFormule = conteneurCartes.locator(".MuiGrid-item",
                new Locator.LocatorOptions().setHasText(formule)
        );
        Locator elementPrixMois = carteFormule.locator(".MuiStack-root",
                new Locator.LocatorOptions().setHasText("par mois")
        ).locator("p.MuiTypography-root").nth(niveauIndex);

        return elementPrixMois.innerText().trim().replaceAll("\\s+", " ");
    }

}
