package com.mtc.mutuaConseil.services.servicesImpl.assuMutuelPro;


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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

import static java.util.Objects.nonNull;

@Service
public class EcaHeomieMProService extends BasePlaywrightService implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(EcaHeomieMProService.class);
    private final TypeAssuranceService typeAssuranceService;

    public EcaHeomieMProService(TypeAssuranceService typeAssuranceService) {
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        log.info("Début de traitement -- Eca_Heomie_Mutuel_Pro (Playwright)");

        Tarif tarif = TarifUtils.createDefaultTarif(c, typeAssuranceService, 3L);
        tarif.setNom(c.getNomFournisseur());
        TypeAssurance typeAssurance = new TypeAssurance();
        typeAssurance.setTypeAssurance(EnumTypeAssurance.MUTUELLE_PRO);
        tarif.setTypeAssurance(typeAssurance);

        try {
            initializeBrowser(false);
            humanLikeNavigate(c.getUrlFournisseur());
            connexion(c);
            choixComplementaire();
            remplirBesoins(flux);
            remplirSituationProfessionnelle(flux);
            remplirSituationPersonnelle(flux);
            suivant();
            page.waitForLoadState(LoadState.NETWORKIDLE);
            elementLib.randomWait(4000, 6000);
            choixFormule(2);
            scrollDown(300);
            String cout = elementLib.getElementTextByXpath("//*[@id='panelsStayOpen-collapseOne']/div/div[2]/div[1]/div[1]/div[3]/strong/span");
            log.info("cout {}", cout);
            tarif.setMontant(cout);
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

    public void choixFormule(int niveau) {
        if (niveau < 1 || niveau > 5) {
            throw new IllegalArgumentException("Le niveau doit être compris entre 1 et 5.");
        }
        String value = "DIRECT_" + (niveau * 100);
        page.locator("#formule_choisie_miltis").selectOption(value);
    }

    private void connexion(Compte c) {
        elementLib.humanTypeById("login-name", c.getUsername());
        elementLib.humanTypeById("login-password", c.getPassword());
        elementLib.clickByXpath("//form//button[@type='submit']");
        elementLib.randomWait(700, 1300);
    }

    private void choixComplementaire() {
        elementLib.clickByXpath("//a[@href='https://partenaire.heomi.fr/nouveau-devis/entreprise']");
        elementLib.randomWait(700, 1300);
        elementLib.clickByXpath("//a[@href='https://partenaire.heomi.fr/entreprise/tns/presentation/SANTE_TNS']");
    }

    private void remplirBesoins(FluxData flux) {
        elementLib.randomWait(700, 1300);
        elementLib.humanTypeById("date_effet_sante", dateEffet(1));
        elementLib.scrollDown(400);
    }

    private void remplirSituationProfessionnelle(FluxData flux) {
        choixProfession(flux);
        choixStatut(flux);
        choixRegime();
        scrollDown(400);
    }

    private void remplirSituationPersonnelle(FluxData flux) {
        elementLib.typeById("dn_assure", flux.getPersonnes().getFirst().getDateNaissance());
        if (flux.getPersonnes().size() == 2) {
            elementLib.clickById("has_conjoint-0");
            elementLib.randomWait(700, 1300);
            elementLib.humanTypeById("dn_conjoint", flux.getPersonnes().get(1).getDateNaissance());
        }
        choixNbEnfants(flux);
        remplirEnfant(flux);
        elementLib.typeById("code_postal", flux.getPersonnes().getFirst().getCodePostal());
        scrollDown(400);
        elementLib.clickById("soins_generaux_faible");
        elementLib.clickById("hospitalisation_faible");
        scrollDown(150);
        elementLib.clickById("optique_faible");
        scrollDown(250);
        elementLib.clickById("dentaire_faible");
        elementLib.clickById("appareil_auditif_tns_faible");
        elementLib.clickById("medecines_douces_tns_non");
    }

    private void remplirEnfant(FluxData flux) {
        if (flux.getEnfants().getFirst().getNom() != null && !flux.getEnfants().getFirst().getNom().isEmpty()) {
            elementLib.humanTypeById("dn_enfant_0", flux.getEnfants().getFirst().getDateNaissance());
        }
        if (flux.getEnfants().size() >= 2) {
            elementLib.humanTypeById("dn_enfant_1", flux.getEnfants().get(1).getDateNaissance());
        }
    }

    private void choixNbEnfants(FluxData flux) {
        boolean sansEnfant = flux.getEnfants().getFirst().getNom() == null || flux.getEnfants().getFirst().getNom().isEmpty();
        int nbEnfants = sansEnfant ? 0 : flux.getEnfants().size();
        elementLib.selectByLabel("#nbr_enfants", String.valueOf(nbEnfants));
    }

    private void suivant() {
        elementLib.clickByTextElement("étape suivante");
    }

    private String dateEffet(int mois) {
        return LocalDate.now()
                .plus(mois, ChronoUnit.MONTHS)
                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    private void choixProfession(FluxData flux) {
        elementLib.clickByXpath("//span[@data-select2-id='1']");
        elementLib.randomWait(700, 1300);
        String profession = flux.getPersonnes().getFirst().getProfession();
        elementLib.humanTypeByXpath("//input[@aria-controls='select2-profession-results']", profession);
        /*
        String professionSite = switch (profession.toLowerCase().trim()) {
            case "agent d'assurances" -> "Agent d'assurance";
            case "agent immobilier" -> "Agent immobilier";
            case "agriculteur" -> "Exploitant agricole et viticole";
            case "ambulancier" -> "Ambulancier";
            case "architecte" -> "Architecte";
            case "assistante sociale" -> "Assistante sociale";
            case "aubergiste" -> "Aubergiste";
            case "audioprothésiste" -> "Audioprothésiste";
            case "aviculteur" -> "Aviculteur";
            case "avocat" -> "Avocat";
            case "boulanger" -> "Boulanger";
            case "brocanteur" -> "Brocanteur";
            case "buraliste" -> "Buraliste / Débitant de tabac";
            case "cafetier" -> "Cafetier";
            case "cariste" -> "Cariste";
            case "carreleur" -> "Carreleur";
            case "carrossier" -> "Carrossier";
            case "cartographe" -> "Cartographe";
            case "caviste" -> "Caviste";
            case "charcutier" -> "Charcutier";
            case "charpentier" -> "Charpentier";
            case "chaudronnier" -> "Chaudronnier";
            case "chauffagiste" -> "Chauffagiste";
            case "chauffeur livreur" -> "Chauffeur-livreur";
            case "chirurgien" -> "Chirurgien";
            case "chirurgien-dentiste" -> "Chirurgien dentiste";
            case "coiffeur" -> "Coiffeur";
            case "coloriste conseil" -> "Coloriste conseil";
            case "commerçant" -> "Commerçant de détail (hors alimentaire)";
            case "commissaire aux comptes" -> "Commissaire aux comptes";
            case "commissaire-priseur" -> "Commissaire-priseur";
            case "concepteur rédacteur" -> "Concepteur-rédacteur";
            case "conseiller artistique" -> "Conseiller artistique";
            case "conseiller commercial" -> "Conseiller commercial";
            case "conseiller conjugal" -> "Conseiller conjugal";
            case "conseiller en gestion" -> "Conseiller en gestion";
            case "conseiller en gestion de patrimoine" -> "Conseiller en gestion de patrimoine";
            case "conseiller financier" -> "Conseiller financier";
            case "conseiller fiscal" -> "Conseiller fiscal";
            case "conseiller littéraire" -> "Conseiller littéraire";
            case "consultant" -> "Autre conseiller - consultant";
            case "consultant en management" -> "Consultant en management";
            case "consultant en stratégie" -> "Consultant en stratégie";
            case "consultant it" -> "Consultant IT";
            case "consultant marketing" -> "Consultant Marketing";
            case "cordonnier" -> "Cordonnier";
            case "courtier d’assurances" -> "Courtier d'assurances";
            case "courtier en travaux" -> "Courtier en travaux";
            case "cuisinier" -> "Cuisinier";
            case "décorateur" -> "Décorateur";
            case "diagnostiqueur immobilier" -> "Diagnostiqueur immobilier";
            case "diététicien" -> "Diététicien";
            case "documentaliste" -> "Documentaliste";
            case "droguiste" -> "Droguiste";
            case "encadreur" -> "Encadreur";
            case "enseignant" -> "Enseignant - Professeur (hors sport)";
            case "ergonome" -> "Ergonome Web";
            case "expert-comptable" -> "Expert comptable";
            case "expert automobile" -> "Expert Auto";
            case "fleuriste" -> "Fleuriste";
            case "formateur" -> "Formateur";
            case "frigoriste" -> "Frigoriste";
            case "garagiste" -> "Garagiste";
            case "graphiste" -> "Graphiste - Illustrateur (CIPAV)";
            case "gynécologue" -> "Gynécologue médical";
            case "horticulteur" -> "Horticulteur (affilié MSA)";
            case "huissier de justice" -> "Huissier de justice";
            case "imprimeur" -> "Imprimeur";
            case "informaticien" -> "Informaticien (autres)";
            case "ingénieur en informatique" -> "Ingénieur informaticien";
            case "journaliste" -> "Journaliste audiovisuel";
            case "juriste" -> "Juriste";
            case "libraire" -> "Libraire";
            case "maçon" -> "Maçon";
            case "marchand de biens" -> "Marchand de biens immobiliers";
            case "maraîcher" -> "Maraîcher";
            case "marbrier" -> "Marbrier";
            case "médecin" -> "Médecin généraliste";
            case "menuisier" -> "Menuisier";
            case "notaire" -> "Notaire";
            case "opticien" -> "Opticien";
            case "orthophoniste" -> "Orthophoniste";
            case "paysagiste" -> "Paysagiste";
            case "pharmacien" -> "Pharmacien";
            case "photographe" -> "Photographe";
            case "plombier" -> "Plombier";
            case "professeur" -> "Enseignant - Professeur (hors sport)";
            case "programmeur" -> "Analyste programmeur";
            case "psychologue" -> "Psychologue";
            default -> "Agent d'assurance";
        };
        */
        page.locator("#select2-profession-results li").first().click();

    }

    private void choixStatut(FluxData flux) {
        elementLib.clickByXpath("//select[@id='statut']");
        elementLib.randomWait(700, 1300);
        String statutSource = flux.getPersonnes().getFirst().getProfessionSpecifique();

        String value = switch (statutSource.toUpperCase()) {
            case "ARTISAN" -> "INDEPENDANT_ARTISAN";
            case "CHEF_ENTREPRISE" -> "MANDATAIRE_SOCIAL";
            case "COMMERCANT",
                 "PROF_LIB",
                 "MEDICAL_PROF",
                 "PARAMEDICAL_PROF",
                 "AGRICULTEUR" -> "AUTO_ENTREPRENEUR";
            default -> "ARTISAN";
        };
        page.locator("#statut").selectOption(value);
    }

    private void choixRegime() {
        page.locator("#regime").selectOption(
                new SelectOption().setLabel("SSI / Régime général")
        );
    }

}
