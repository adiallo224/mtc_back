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
public class RepamMutuelIndivPlayWrightService extends BasePlaywrightService implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(RepamMutuelIndivPlayWrightService.class);
    private final TypeAssuranceService typeAssuranceService;

    public RepamMutuelIndivPlayWrightService(TypeAssuranceService typeAssuranceService) {
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        log.info("Début de traitement -- Repam_Mutuel_Indiv (Playwright)");

        Tarif tarif = TarifUtils.createDefaultTarif(c, typeAssuranceService, 2L);
        tarif.setNom(c.getNomFournisseur());
        TypeAssurance typeAssurance = new TypeAssurance();
        typeAssurance.setTypeAssurance(EnumTypeAssurance.MUTUELLE_INDIV);
        tarif.setTypeAssurance(typeAssurance);

        try {
            initializeBrowser(false);
            humanLikeNavigate(c.getUrlFournisseur());
            choixCookies();
            connexion(c);
            remplirClient(flux);
            waitThread(3);
            String cout = elementLib.getPrixByNiveau(5, "div.grid.grid-cols-6 > div", "p.font-gotham-book");
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

    private void choixCookies() {
        clickIfExists("[data-testid='acceptButton']");
    }

    private void connexion(Compte c) {
        waitThread(1);
        elementLib.humanTypeByXpath("//input[@name='username']", c.getUsername());
        elementLib.humanTypeByXpath("//input[@name='password']", c.getPassword());
        elementLib.clickByXpath("//button[@type='submit']");
        waitThread(1);
        elementLib.clickByXpath("//button[@type='button' and contains(@class,'nrg-button-tertiary')]");
        waitThread(1);
        elementLib.clickByXpath("//p[contains(normalize-space(), 'Santé Individuelle')]");
        elementLib.switchToNewWindow();
        this.page = elementLib.getPage();
        elementLib.clickByXpath("//button[.//div[text()='Nouvelle proposition']]");
        waitThread(1);
        elementLib.clickByXpath("//button[.//div[text()='Sélectionner cette offre']]");
        waitThread(1);
        elementLib.clickByXpath("//button[.//div[text()='Oui']]");
    }

    private void remplirClient(FluxData flux) {
        waitThread(1);
        elementLib.humanTypeByXpath("//input[@placeholder='JJ/MM/AAAA']", dateEffet(1));
        waitThread(1);
        elementLib.clickByXpath("//button[.//div[text()='Continuer']]");

        elementLib.clickByXpath("//input[@id='customer.isMember-1']");
        elementLib.humanTypeByXpath("//input[@name='customer.lastName']", flux.getPersonnes().getFirst().getNom());
        elementLib.humanTypeByXpath("//input[@name='customer.firstName']", flux.getPersonnes().getFirst().getPrenom());
        elementLib.humanTypeByXpath("//input[@name='customer.birthDate']", flux.getPersonnes().getFirst().getDateNaissance());
        elementLib.humanTypeByXpath("//input[@name='customer.address.postCode']", flux.getPersonnes().getFirst().getCodePostal());
        choixProfession(flux);
        waitThread(1);

        if (flux.getPersonnes().size() >= 2) {
            elementLib.clickByXpath("//button[.//div[text()='Ajouter un bénéficiaire']]");
            waitThread(1);
            elementLib.clickByXpath("//label[normalize-space()='Conjoint']");
            elementLib.humanTypeByXpath("(//input[contains(@name,'beneficiaries') and contains(@name,'birthDate')])[last()]", flux.getPersonnes().get(1).getDateNaissance());
            choixRegime();
            elementLib.clickByXpath("//input[@id='beneficiaries.0.isAlsaceMoselle-1']");
        }
        if (flux.getEnfants().getFirst().getNom() != null && !flux.getEnfants().getFirst().getNom().isEmpty()) {
            elementLib.clickByXpath("//button[.//div[text()='Ajouter un bénéficiaire']]");
            waitThread(1);
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
            waitThread(1);
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
        waitThread(1);
        String profession = flux.getPersonnes().getFirst().getProfessionSpecifique();
        String option = switch (profession.toLowerCase()) {
            case "artisan"                        -> "Artisan";
            case "chef d'entreprise"              -> "Chef d'entreprise";
            case "agriculteur"                    -> "Exploitant Agricole";
            case "salarié cadre"                  -> "Cadre";
            case "commerçant"                     -> "Commerçant";
            case "salarié non cadre : employé"    -> "Cadre et employé de la fonction publique";
            case "ouvrier"                        -> "Ouvrier";
            default                               -> null;
        };
        if (option != null) {
            elementLib.clickByXpath("//div[@role='listbox']//div[@role='option' and .//span[normalize-space()='" + option + "']]");
        } else {
            log.warn("Profession non reconnue : '{}'", profession);
        }
    }

    private void choixRegime() {
        elementLib.clickByXpath("(//input[@type='hidden' and contains(@name,'.regime')])[last()]/preceding-sibling::div[contains(@class,'-control')]");
        waitThread(1);
        elementLib.clickByXpath("//div[@role='listbox']//div[@role='option' and .//span[normalize-space()='Régime général']]");
    }

    private String dateEffet(int mois) {
        return LocalDate.now()
                .plus(mois, ChronoUnit.MONTHS)
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

}
