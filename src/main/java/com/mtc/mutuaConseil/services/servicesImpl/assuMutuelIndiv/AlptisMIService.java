package com.mtc.mutuaConseil.services.servicesImpl.assuMutuelIndiv;

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
public class AlptisMIService extends BasePlaywrightService implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(AlptisMIService.class);
    private final TypeAssuranceService typeAssuranceService;

    public AlptisMIService(TypeAssuranceService typeAssuranceService) {
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        log.info("Début de traitement -- Alptis_Mutuel_Indiv (Playwright)");

        long idTypeAssu = verif(flux, "Pro") ? 3L : 2L;
        EnumTypeAssurance enumTypeAssurance = verif(flux, "Pro")
                ? EnumTypeAssurance.MUTUELLE_PRO
                : EnumTypeAssurance.MUTUELLE_INDIV;

        Tarif tarif = TarifUtils.createDefaultTarif(c, typeAssuranceService, idTypeAssu);
        tarif.setNom(c.getNomFournisseur());
        TypeAssurance typeAssurance = new TypeAssurance();
        typeAssurance.setTypeAssurance(enumTypeAssurance);
        tarif.setTypeAssurance(typeAssurance);

        try {
            initializeBrowser(false);
            humanLikeNavigate(c.getUrlFournisseur());
            connexion(c);
            navigation();
            remplirContrat(flux);
            remplirAdherents(flux);
            remplirConjoint(flux);
            remplirEnfant(flux);
            recherche();
            // Attendre les résultats
            scrollDown(500);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            elementLib.randomWait(700, 1300);
            String cout = elementLib.getElementTextByXpath(
                    "//section[contains(@class,'pc-results__recommendations')][1]" +
                    "//div[@class='pc-offer-mobile__infos']//div[@class='pc-offer-price']" +
                    "//span[@class='pc-price']//strong"
            );
            log.info("cout {}", cout);
            tarif.setMontant(cout);
            String screenshotPath = captureScreenshot(tarif.getNom(), false, tarif);
            if (screenshotPath != null) {
                tarif.setCaptureImg(screenshotPath);
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
        // Fermer le bandeau cookie (Axeptio)
        elementLib.randomWait(700, 1300);
        clickIfExists("#axeptio_btn_dismiss");
        elementLib.humanTypeById("username", c.getUsername());
        elementLib.randomWait(1500, 2500);
        elementLib.humanTypeById("password", c.getPassword());
        clickIfExists("[name='login']");
    }

    private void navigation() {
        // Ouvrir la section Santé individuelle
        elementLib.clickByXpath("//span[text()='Santé individuelle']");
        elementLib.randomWait(1500, 2500);
        // "Accéder au comparateur" ouvre un nouvel onglet → on y bascule
        elementLib.clickByXpath("//span[normalize-space()='Accéder au comparateur']");
        elementLib.switchToNewWindow();
        this.page = elementLib.getPage(); // synchronise la référence pour captureScreenshot / scrollDown
        elementLib.randomWait(1500, 2500);
    }

    private void remplirContrat(FluxData flux) {
        boolean aEnfants = !flux.getEnfants().isEmpty()
                && flux.getEnfants().getFirst().getNom() != null
                && !flux.getEnfants().getFirst().getNom().isEmpty();
        boolean aConjoint = flux.getPersonnes().size() >= 2;

        // Sélectionner qui on assure (label for = ID de l'input radio)
        if (aConjoint && aEnfants) {
            elementLib.clickByXpath("//label[@for='who_me_partner_children']");
        } else if (aConjoint) {
            elementLib.clickByXpath("//label[@for='who_me_partner']");
        } else if (aEnfants) {
            elementLib.clickByXpath("//label[@for='who_me_children']");
        } else {
            elementLib.clickByXpath("//label[@for='who_me']");
        }

        // Pas de remplacement d'un contrat chez un autre assureur
        elementLib.clickByXpath("//label[@for='contractReplacement_false']");

        // Date de début du contrat (mois prochain)
        elementLib.humanTypeById("startDate", dateEffet(1));
    }

    private void remplirAdherents(FluxData flux) {
        String civilite = flux.getPersonnes().getFirst().getCivilite();
        if (civilite.equalsIgnoreCase("M") || civilite.equalsIgnoreCase("Monsieur")) {
            elementLib.clickByXpath("//label[@for='insured_title_monsieur']");
        } else {
            elementLib.clickByXpath("//label[@for='insured_title_madame']");
        }

        elementLib.humanTypeById("insured_lastname", flux.getPersonnes().getFirst().getNom());
        elementLib.humanTypeById("insured_firstname", flux.getPersonnes().getFirst().getPrenom());
        elementLib.humanTypeById("birthdate", flux.getPersonnes().getFirst().getDateNaissance());
        choixCategorieSocioPro(flux, 0);
        choixRegime(flux, 0);
        elementLib.randomWait(1500, 2500);
        elementLib.humanTypeById("postalCode", flux.getPersonnes().get(0).getCodePostal());
    }

    private void remplirConjoint(FluxData flux) {
        if (flux.getPersonnes().size() < 2) return;

        elementLib.humanTypeById("partner-birthdate", flux.getPersonnes().get(1).getDateNaissance());
        choixCategorieSocioPro(flux, 1);
        choixRegime(flux, 1);
    }

    private void remplirEnfant(FluxData flux) {
        if (flux.getEnfants().isEmpty()
                || flux.getEnfants().getFirst().getNom() == null
                || flux.getEnfants().getFirst().getNom().isEmpty()) return;

        ajouterEnfant();
        elementLib.humanTypeById("child_0_birthdate", flux.getEnfants().getFirst().getDateNaissance());
        elementLib.randomWait(700, 1300);

        if (flux.getEnfants().size() >= 2
                && flux.getEnfants().get(1).getNom() != null
                && !flux.getEnfants().get(1).getNom().isEmpty()) {
            ajouterEnfant();
            elementLib.humanTypeById("child_1_birthdate", flux.getEnfants().get(1).getDateNaissance());
            elementLib.randomWait(700, 1300);
        }
    }

    private void ajouterEnfant() {
        // Bouton "+" du compteur d'enfants
        elementLib.clickByXpath("//*[@id='children_count']/button[2]");
    }

    private void recherche() {
        elementLib.randomWait(1500, 2500);
        elementLib.clickByXpath("//button[normalize-space(text())='Découvrir les offres']");
    }

    private void choixCategorieSocioPro(FluxData flux, int index) {
        String selectId = (index == 0) ? "insured_category_select" : "partner_category_select";
        String profession = flux.getPersonnes().get(index).getProfessionSpecifique();

        String option = switch (profession.toLowerCase()) {
            case "agriculteur"                          -> "Agriculteurs exploitants";
            case "artisan"                              -> "Artisans";
            case "salarié cadre"                        -> "Cadres";
            case "chef d'entreprise"                    -> "Chefs d'entreprise";
            case "commerçant"                           -> "Commerçants et assimilés";
            case "salarié non cadre : employé"          -> "Employés, agents de maitrise";
            case "ouvrier"                              -> "Ouvriers";
            case "profession libérale",
                 "profession libérale médicale",
                 "profession libérale paramédicale"     -> "Professions libérales et assimilés";
            case "retraité"                             -> "Retraités";
            default                                     -> null;
        };

        if (option != null) {
            elementLib.selectByLabel("#" + selectId, option);
        } else {
            log.warn("CSP non reconnue : '{}'", profession);
        }
    }

    private void choixRegime(FluxData flux, int index) {
        String selectId = (index == 0) ? "insured_regime" : "partner_regime";
        // Valeur par défaut : Sécurité Sociale
        // TODO : mapper flux.getPersonnes().get(index).getRegime() vers les options disponibles
        elementLib.selectByLabel("#" + selectId, "Sécurité Sociale");
    }

    private String dateEffet(int mois) {
        return LocalDate.now()
                .plus(mois, ChronoUnit.MONTHS)
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private boolean verif(FluxData flux, String type) {
        String profession = flux.getPersonnes().getFirst().getProfessionSpecifique();
        if (type.equalsIgnoreCase("Pro")) {
            return profession.equalsIgnoreCase("Chef d'entreprise")
                || profession.equalsIgnoreCase("Artisan")
                || profession.equalsIgnoreCase("Agriculteur")
                || profession.equalsIgnoreCase("Commerçant")
                || profession.equalsIgnoreCase("Profession libérale")
                || profession.equalsIgnoreCase("Profession libérale médicale")
                || profession.equalsIgnoreCase("Profession libérale paramédicale");
        }
        return profession.equalsIgnoreCase("Salarié cadre")
            || profession.equalsIgnoreCase("Salarié non cadre : employé")
            || profession.equalsIgnoreCase("Ouvrier")
            || profession.equalsIgnoreCase("Fonctionnaire classe a")
            || profession.equalsIgnoreCase("Fonctionnaire hors classe a")
            || profession.equalsIgnoreCase("Intermittent")
            || profession.equalsIgnoreCase("Intérimaire");
    }
}
