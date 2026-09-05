package com.mtc.mutuaConseil.services.servicesImpl.assuMutuelIndiv;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.AriaRole;
import com.microsoft.playwright.options.LoadState;
import com.mtc.mutuaConseil.base.BasePlaywrightService;
import com.mtc.mutuaConseil.models.Compte;
import com.mtc.mutuaConseil.models.FluxData;
import com.mtc.mutuaConseil.models.Tarif;
import com.mtc.mutuaConseil.models.TypeAssurance;
import com.mtc.mutuaConseil.models.enums.EnumTypeAssurance;
import com.mtc.mutuaConseil.selectors.SelectorStore;
import com.mtc.mutuaConseil.services.LaunchedService;
import com.mtc.mutuaConseil.services.servicesImpl.TypeAssuranceService;
import com.mtc.mutuaConseil.utils.TarifUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
public class AlptisMIService extends BasePlaywrightService implements LaunchedService {

    private static final String PROVIDER = "mutuelIndiv/alptis-mi";

    private final Logger log = LoggerFactory.getLogger(AlptisMIService.class);
    private final TypeAssuranceService typeAssuranceService;
    private final SelectorStore selectors;
    private String currentStep = "démarrage";

    public AlptisMIService(TypeAssuranceService typeAssuranceService, SelectorStore selectors) {
        this.typeAssuranceService = typeAssuranceService;
        this.selectors = selectors;
    }

    private String sel(String key) {
        return selectors.get(PROVIDER, key);
    }

    private void step(String name, Runnable action) {
        currentStep = name;
        log.debug("Étape: {}", name);
        action.run();
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        log.info("Début de traitement -- Alptis_Mutuel_Indiv (Playwright)");
        Tarif tarif = TarifUtils.createDefaultTarif(c, typeAssuranceService, 2L);
        tarif.setNom(c.getNomFournisseur());
        TypeAssurance typeAssurance = new TypeAssurance();
        typeAssurance.setTypeAssurance(EnumTypeAssurance.MUTUELLE_INDIV);
        tarif.setTypeAssurance(typeAssurance);

        try {
            initializeBrowser(false);
            humanLikeNavigate(c.getUrlFournisseur());
            step("connexion", () -> connexion(c));
            step("navigation", this::navigation);
            step("contrat", () -> remplirContrat(flux));
            step("adherents", () -> remplirAdherents(flux));
            step("conjoint", () -> remplirConjoint(flux));
            step("enfant", () -> remplirEnfant(flux));
            step("recherche", this::recherche);
            // Attendre les résultats
            scrollDown(500);
            page.waitForLoadState(LoadState.NETWORKIDLE);
            elementLib.randomWait(4000, 6000);
            step("voir_plus", () -> voirPlus(2));
            currentStep = "lecture_resultats";
            List<String> couts = getPrixTtcParFormule(c.getNiveau());
            log.info("couts (formules) {}", couts);
            tarif.setMontant(couts);
            String screenshotPath = captureScreenshot(tarif.getNom(), false, tarif);
            if (screenshotPath != null) {
                tarif.setCaptureImg(screenshotPath);
            }
            tarif.setExecution(true);
        } catch (Exception e) {
            log.error("Échec à l'étape '{}'", currentStep, e);
            tarif.setErreur(e.getMessage());
            String screenshotPath = captureScreenshot(tarif.getNom(), true, tarif);
            tarif.setCaptureImgErreur(screenshotPath);
            tarif.setEtape(currentStep);
        } finally {
            cleanup();
        }
        return tarif;
    }

    private void voirPlus(int nbr) {
        Locator voirPlus = page.getByRole(
                AriaRole.BUTTON,
                new Page.GetByRoleOptions().setName(sel("recherche.voir_plus_label"))
        );

        if (voirPlus.count() > 0 && voirPlus.first().isVisible()) {
            for (int i=0; i<nbr; i++) {
                 voirPlus.first().click();
                 elementLib.randomWait(4000, 7000);
            }
        }
    }

    private void connexion(Compte c) {
        elementLib.randomWait(2500, 4500);
        clickIfExists("#" + sel("connexion.cookie_dismiss_id"));
        elementLib.humanTypeById(sel("connexion.username_id"), c.getUsername());
        elementLib.randomWait(1500, 2500);
        elementLib.humanTypeById(sel("connexion.password_id"), c.getPassword());
        clickIfExists(sel("connexion.login_button_selector"));
    }

    private void navigation() {
        elementLib.clickByXpath(sel("navigation.sante_individuelle_xpath"));
        elementLib.randomWait(1500, 2500);
        elementLib.clickByXpath(sel("navigation.acceder_comparateur_xpath"));
        elementLib.switchToNewWindow();
        this.page = elementLib.getPage();
        elementLib.randomWait(1500, 2500);
    }

    private void remplirContrat(FluxData flux) {
        boolean aEnfants = !flux.getEnfants().isEmpty()
                && flux.getEnfants().getFirst().getNom() != null
                && !flux.getEnfants().getFirst().getNom().isEmpty();
        boolean aConjoint = flux.getPersonnes().size() >= 2;
        elementLib.randomWait(1500, 2500);

        if (aConjoint && aEnfants) {
            elementLib.clickByXpath(sel("contrat.who_me_partner_children_xpath"));
        } else if (aConjoint) {
            elementLib.clickByXpath(sel("contrat.who_me_partner_xpath"));
        } else if (aEnfants) {
            elementLib.clickByXpath(sel("contrat.who_me_children_xpath"));
        } else {
            elementLib.clickByXpath(sel("contrat.who_me_xpath"));
        }

        elementLib.clickByXpath(sel("contrat.contract_replacement_false_xpath"));

        elementLib.humanTypeById(sel("contrat.start_date_id"), dateEffet(1));
    }

    private void remplirAdherents(FluxData flux) {
        String civilite = flux.getPersonnes().getFirst().getCivilite();
        elementLib.randomWait(1500, 2500);
        if (civilite.equalsIgnoreCase("M") || civilite.equalsIgnoreCase("Monsieur")) {
            elementLib.clickByXpath(sel("adherent.title_monsieur_xpath"));
        } else {
            elementLib.clickByXpath(sel("adherent.title_madame_xpath"));
        }

        elementLib.humanTypeById(sel("adherent.lastname_id"), flux.getPersonnes().getFirst().getNom());
        elementLib.humanTypeById(sel("adherent.firstname_id"), flux.getPersonnes().getFirst().getPrenom());
        elementLib.humanTypeById(sel("adherent.birthdate_id"), flux.getPersonnes().getFirst().getDateNaissance());
        choixCategorieSocioPro(flux, 0);
        choixRegime(flux, 0);
        elementLib.randomWait(1500, 2500);
        elementLib.humanTypeById(sel("adherent.postal_code_id"), flux.getPersonnes().get(0).getCodePostal());
    }

    private void remplirConjoint(FluxData flux) {
        if (flux.getPersonnes().size() < 2) return;

        elementLib.humanTypeById(sel("conjoint.birthdate_id"), flux.getPersonnes().get(1).getDateNaissance());
        choixCategorieSocioPro(flux, 1);
        choixRegime(flux, 1);
    }

    private void remplirEnfant(FluxData flux) {
        if (flux.getEnfants().isEmpty()
                || flux.getEnfants().getFirst().getNom() == null
                || flux.getEnfants().getFirst().getNom().isEmpty()) return;

        ajouterEnfant();
        elementLib.humanTypeById(sel("enfant.enfant0_birthdate_id"), flux.getEnfants().getFirst().getDateNaissance());
        elementLib.randomWait(1500, 2500);

        if (flux.getEnfants().size() >= 2
                && flux.getEnfants().get(1).getNom() != null
                && !flux.getEnfants().get(1).getNom().isEmpty()) {
            ajouterEnfant();
            elementLib.humanTypeById(sel("enfant.enfant1_birthdate_id"), flux.getEnfants().get(1).getDateNaissance());
        }
    }

    private void ajouterEnfant() {
        elementLib.randomWait(1500, 2500);
        elementLib.clickByXpath(sel("enfant.ajouter_xpath"));
    }

    private void recherche() {
        elementLib.randomWait(1500, 2500);
        elementLib.clickByXpath(sel("recherche.decouvrir_offres_xpath"));
    }

    private void choixCategorieSocioPro(FluxData flux, int index) {
        String selectId = (index == 0) ? sel("adherent.category_select_id") : sel("conjoint.category_select_id");
        String profession = flux.getPersonnes().get(index).getProfessionSpecifique();

        String cle = switch (profession.toLowerCase()) {
            case "agriculteur"                          -> "agriculteur";
            case "artisan"                              -> "artisan";
            case "salarié cadre"                        -> "cadre";
            case "chef d'entreprise"                    -> "chef_entreprise";
            case "commerçant"                           -> "commercant";
            case "salarié non cadre : employé"          -> "employe";
            case "ouvrier"                              -> "ouvrier";
            case "profession libérale",
                 "profession libérale médicale",
                 "profession libérale paramédicale"     -> "liberale";
            case "retraité"                             -> "retraite";
            default                                     -> null;
        };

        if (cle != null) {
            elementLib.selectByLabel("#" + selectId, sel("categorie_socio_pro." + cle));
        } else {
            log.warn("CSP non reconnue : '{}'", profession);
        }
    }

    private void choixRegime(FluxData flux, int index) {
        String selectId = (index == 0) ? sel("adherent.regime_select_id") : sel("conjoint.regime_select_id");
        elementLib.selectByLabel("#" + selectId, sel("regime.valeur"));
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

    private List<String> getPrixTtcParFormule(int formule) {
        String template = sel("resultats.prix_selector_template");
        List<String> prix = new ArrayList<>();
        for (int i = formule; i <= 4; i++) {
            // Carte offre dont le libellé exact du niveau est "Niveau i" (exclut les variantes "+ Pack Bien-être")
            Locator offre = page.locator(String.format(template, i));
            if (offre.count() > 0) {
                String texte = offre.first()
                        .locator(".pc-offer__price .pc-price strong")
                        .innerText()
                        .replace(' ', ' ')
                        .trim();
                prix.add(texte);
            }
        }
        return prix;
    }
}
