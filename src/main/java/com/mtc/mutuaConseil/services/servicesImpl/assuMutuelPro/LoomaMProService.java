package com.mtc.mutuaConseil.services.servicesImpl.assuMutuelPro;


import com.microsoft.playwright.Locator;
import com.mtc.mutuaConseil.base.BasePlaywrightService;
import com.mtc.mutuaConseil.models.Compte;
import com.mtc.mutuaConseil.models.FluxData;
import com.mtc.mutuaConseil.models.Tarif;
import com.mtc.mutuaConseil.services.LaunchedService;
import com.mtc.mutuaConseil.services.servicesImpl.TypeAssuranceService;
import com.mtc.mutuaConseil.utils.TarifUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Service
public class LoomaMProService extends BasePlaywrightService implements LaunchedService {

    private final Logger log = LoggerFactory.getLogger(LoomaMProService.class);
    private final TypeAssuranceService typeAssuranceService;

    private static final Map<String, String> STATUTS = Map.of(
            "Chef d'entreprise", "1",
            "Travailleur indépendant", "2",
            "Profession libérale", "3",
            "Auto Entrepreneur", "4",
            "Artisan", "5",
            "Commercant", "6",
            "Mandataire non rémunéré", "7",
            "Agriculteur", "9",
            "Conjoint collaborateur", "10"
    );

    public LoomaMProService(TypeAssuranceService typeAssuranceService) {
        this.typeAssuranceService = typeAssuranceService;
    }

    @Override
    public Tarif getResultFrom(Compte c, FluxData flux) {
        log.info("Début de traitement -- Looma_Mutuel_Pro (Playwright)");
        Tarif tarif = TarifUtils.createDefaultTarif(c, typeAssuranceService, 3L);

        try {
            initializeBrowser(false);
            connexion(c);
            projet();
            profil(flux);
            garantieTarifs(flux);
            elementLib.clickById("etape-suivante");
            elementLib.randomWait(1500, 2500);
            String cout = getPrixByNiveau(3, flux);
            log.info("cout {}", cout);
            tarif.setMontant(cout);
            scrollDown(-400);
            String screenshotBytes = captureScreenshot(tarif.getNom(), false, tarif);
            if (screenshotBytes != null) {
                tarif.setCaptureImg(screenshotBytes);
            }
            tarif.setExecution(true);
            log.info("Fin de traitement -- Looma_Mutuel_Pro");
        } catch (Exception e) {
            log.error("An error occurred", e);
            tarif.setErreur(e.getMessage());
            String screenshotBytesErreur = captureScreenshot(tarif.getNom(), true, tarif);
            tarif.setCaptureImgErreur(screenshotBytesErreur);
            tarif.setEtape("");
        } finally {
            cleanup();
        }
        return tarif;
    }

    private void connexion(Compte c) {
        humanLikeNavigate(c.getUrlFournisseur());
        elementLib.humanTypeById("identifiant", c.getUsername());
        elementLib.humanTypeById("password", c.getPassword());
        elementLib.clickByXpath("//button[normalize-space()='Se connecter']");
        elementLib.randomWait(1500, 2500);
    }

    private void projet() {
        elementLib.clickByXpath("//span[normalize-space()='Projets']");
        elementLib.randomWait(700, 1300);
        elementLib.clickByXpath("//a[normalize-space()='Nouveau projet']");
        elementLib.randomWait(700, 1300);
        elementLib.clickByXpath("//img[@alt='TNS']");
        elementLib.randomWait(700, 1300);
    }

    private void profil(FluxData flux) {
        elementLib.humanTypeById("nom", flux.getPersonnes().get(0).getNom());
        elementLib.humanTypeById("prenom", flux.getPersonnes().get(0).getPrenom());
        elementLib.humanTypeById("dns", flux.getPersonnes().get(0).getDateNaissance());
//        clickBody();
        elementLib.randomWait(700, 1300);
        elementLib.clickById("profil-client");
    }

    private void garantieTarifs(FluxData flux) {
        // Données projet
        choixRisque();
        elementLib.humanTypeById("date_effet", dateEffet(1));
        // Données personnelles TNS
        choixSituation(flux);
        if (!flux.getEnfants().get(0).getNom().isEmpty()) {
            for (int i = 0; i < flux.getEnfants().size(); i++) {
                elementLib.clickByXpath("//div[@class='block bt plus']");
            }
        }
        elementLib.humanTypeById("ad1", flux.getPersonnes().get(0).getNumeroVoie() + " " + flux.getPersonnes().get(0).getNomVoie());
        elementLib.humanTypeById("ville_nom", flux.getPersonnes().get(0).getVille());
        choisirVilleSuggestion(flux);
        // Données professionnelles TNS
        choixStatut(flux);
        elementLib.humanTypeById("soc_siret", flux.getEntreprise().getSiret());
        elementLib.humanTypeById("soc_naf_code", flux.getEntreprise().getCodeAPE());
        choisirPremiereSuggestionCodeNaf();
    }

    private void choixRisque() {
        elementLib.selectByValue("#risque", "2");
    }

    private void choixSituation(FluxData flux) {
        if (flux.getPersonnes().size() == 1) {
            elementLib.selectByValue("#situation_id", "2");
        }
        if (flux.getPersonnes().size() == 2) {
            elementLib.selectByValue("#situation_id", "1");
        }
    }

    private void choixStatut(FluxData flux) {
        String valeur = STATUTS.get(flux.getPersonnes().get(0).getProfessionSpecifique());
        if (valeur != null) {
            elementLib.selectByValue("#statut_id", valeur);
        } else {
            log.warn("Statut professionnel non reconnu : {}", flux.getPersonnes().get(0).getProfessionSpecifique());
        }
    }

    private void choisirVilleSuggestion(FluxData flux) {
        Locator suggestionsContainer = page.locator("#ui-id-2");
        suggestionsContainer.waitFor(new Locator.WaitForOptions().setTimeout(10000));
        Locator suggestions = suggestionsContainer.locator(".ui-menu-item-wrapper");
        String codePostal = flux.getPersonnes().get(0).getCodePostal();
        String ville = flux.getPersonnes().get(0).getVille().toUpperCase();
        int count = suggestions.count();
        for (int i = 0; i < count; i++) {
            Locator suggestion = suggestions.nth(i);
            String texte = suggestion.textContent();
            if (texte.contains(codePostal) && texte.contains(ville)) {
                suggestion.click();
                break;
            }
        }
    }

    private void choisirPremiereSuggestionCodeNaf() {
        Locator suggestionsContainer = page.locator("#ui-id-3");
        suggestionsContainer.waitFor(new Locator.WaitForOptions().setTimeout(10000));
        suggestionsContainer.locator(".ui-menu-item-wrapper").first().click();
    }

    private String dateEffet(int mois) {
        LocalDate today = LocalDate.now();
        LocalDate nextMonth = today.plusMonths(mois);
        return nextMonth.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    public String getPrixByNiveau(int niveau, FluxData flux) {
        String structure = getStructure(flux);

        Locator ligne = page.locator("tbody tr")
                .filter(new Locator.FilterOptions().setHasText(structure));

        return ligne.locator("td.niveau" + niveau)
                .textContent()
                .trim();
    }

    private String getStructure(FluxData flux) {

        int nbAdultes = flux.getPersonnes() == null
                ? 0
                : flux.getPersonnes().size();

        int nbEnfants = flux.getEnfants() == null
                ? 0
                : (int) flux.getEnfants().stream()
                .filter(e -> e.getNom() != null && !e.getNom().isBlank())
                .count();

        if (nbAdultes == 1 && nbEnfants == 0) {
            return "Isolé";
        }

        if (nbAdultes == 1 && nbEnfants == 1) {
            return "Duo";
        }

        if (nbAdultes == 2 && nbEnfants == 0) {
            return "Couple";
        }

        if ((nbAdultes >= 1 && nbEnfants >= 2)
                || (nbAdultes == 2 && nbEnfants >= 1)) {
            return "Famille";
        }

        throw new IllegalArgumentException(
                "Structure non gérée : adultes=" + nbAdultes
                        + ", enfants=" + nbEnfants);
    }

}
