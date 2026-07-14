package com.mtc.mutuaConseil.services.servicesImpl;

import com.mtc.mutuaConseil.models.Compte;
import com.mtc.mutuaConseil.models.FluxData;
import com.mtc.mutuaConseil.models.Tarif;
import com.mtc.mutuaConseil.services.LaunchedService;
import com.mtc.mutuaConseil.services.servicesImpl.assuMutuelIndiv.*;
import com.mtc.mutuaConseil.services.servicesImpl.assuMutuelPro.*;
import com.mtc.mutuaConseil.services.servicesImpl.assuPret.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.nonNull;

@Service
public class TarifFournisseurService {

    private final Logger logger = LoggerFactory.getLogger(TarifFournisseurService.class);
    private final Map<String, LaunchedService> serviceMap;

    @Autowired
    public TarifFournisseurService(
            AfiescaService afiescaService,
            AlptisService alptisService,
            ApicilService apicilService,
            ApiviaPretPlayWrightService apiviaPretPlayWrightService,
            AprilService aprilService,
            AssureaPretService assureaPretService,
            DigitalInsurePretService digitalInsurePretService,
            HarmonieADPPretService harmonieADPPretService,
            LifeSquarePretService  lifeSquarePretService,
            MetLifeService metLifeService,
            RepamService repamService,
            SimulassurService  simulassurService,
            SWLService swlService,
            UtwinPretService utwinPretService,
            // Mutuelle pro
            AlptisMProService alptisMProService,
            Ami3fMProService ami3FMProService,
            AprilMutuelProService aprilMProService,
            ApiviaMProService apiviaMProService,
            ApicilMutuelProService apicilMProService,
            EcaHeomieMProService ecaMProService,
            EntoriaMutuelProService entoriaMProService,
            FFAMutuelProService ffaMProService,
            HarmonieMutuelProService harmonieMProService,
            HennerMProService hennerMProService,
            LoomaMProService loomaMProService,
            QuatremMProService quatremMProService,
            RepamMProService repamMProService,
            SmisoMProService smisoMProService,
            // Mutuelle indiv
            AprilMIService aprilMIService,
            ApicilMIService apicilMIService,
            AlptisMIService alptisMIService,
            Ami3fMIService ami3fMIService,
            ApiviaMIService apiviaMIService,
            EcaHeomieMlService ecaHeomieMIService,
            HennerMlService hennerMIService,
            QuatremMIService quatremMIService,
            RepamMIService repamMIService,
            SmisoMIService smisoMIService
    ) {
        // Initialisation du map avec les services disposables
        // Mutuelle prêt
        this.serviceMap = new HashMap<>();
        this.serviceMap.put("Afiesca_Prêt", afiescaService);
        this.serviceMap.put("Alptis_Prêt", alptisService);
        this.serviceMap.put("Apicil_Prêt", apicilService);
        this.serviceMap.put("Apivia_Prêt", apiviaPretPlayWrightService);
        this.serviceMap.put("April_Prêt", aprilService);
        this.serviceMap.put("Assurea_Prêt", assureaPretService);
        this.serviceMap.put("DigitalInsure_Prêt", digitalInsurePretService);
        this.serviceMap.put("Harmonie_Prêt", harmonieADPPretService);
        this.serviceMap.put("LifeSquare_Prêt", lifeSquarePretService);
        this.serviceMap.put("MetLife_Prêt", metLifeService);
        this.serviceMap.put("Repam_Prêt", repamService);
        this.serviceMap.put("Simulassur_Prêt", simulassurService);
        this.serviceMap.put("Swlife_Prêt", swlService);
        this.serviceMap.put("Utwin_Prêt", utwinPretService);
        // Mutuelle pro
        this.serviceMap.put("Alptis_Mutuelle_Pro", alptisMProService);
        this.serviceMap.put("Ami3f_Mutuelle_Pro", ami3FMProService);
        this.serviceMap.put("Apicil_Mutuelle_Pro", apicilMProService);
        this.serviceMap.put("Apivia_Mutuelle_Pro", apiviaMProService);
        this.serviceMap.put("April_Mutuelle_Pro", aprilMProService);
        this.serviceMap.put("Eca_Mutuelle_Pro", ecaMProService);
        this.serviceMap.put("Entoria_Mutuelle_Pro", entoriaMProService);
        this.serviceMap.put("Ffa_Mutuelle_Pro", ffaMProService);
        this.serviceMap.put("Harmonie_Mutuelle_Pro", harmonieMProService);
        this.serviceMap.put("Henner_Mutuelle_Pro", hennerMProService);
        this.serviceMap.put("Looma_Mutuelle_Pro", loomaMProService); //à revoir
        this.serviceMap.put("Quatrem_Mutuelle_Pro", quatremMProService);
        this.serviceMap.put("Repam_Mutuelle_Pro", repamMProService);
        this.serviceMap.put("Smiso_Mutuelle_Pro", smisoMProService);
        // Mutuelle Indiv
        this.serviceMap.put("Alptis_Mutuelle_Indiv", alptisMIService);
        this.serviceMap.put("Ami3f_Mutuelle_Indiv", ami3fMIService);
        this.serviceMap.put("Apicil_Mutuelle_Indiv", apicilMIService);
        this.serviceMap.put("Apivia_Mutuelle_Indiv", apiviaMIService);
        this.serviceMap.put("April_Mutuelle_Indiv", aprilMIService);
        this.serviceMap.put("Eca_Mutuelle_Indiv", ecaHeomieMIService);
        this.serviceMap.put("Henner_Mutuelle_Indiv", hennerMIService);
        this.serviceMap.put("Quatrem_Mutuelle_Indiv", quatremMIService);
        this.serviceMap.put("Repam_Mutuelle_Indiv", repamMIService);
        this.serviceMap.put("Smiso_Mutuelle_Indiv", smisoMIService);
    }

    public List<Tarif> getTarifsForComptes(List<Compte> comptes, FluxData flux) {
        List<Tarif> tarifs = new ArrayList<>();
        for (Compte compte : comptes) {
             LaunchedService service = serviceMap.get(compte.getNomFournisseur());
             if (nonNull(service)) {
                 try {
                     tarifs.add(service.getResultFrom(compte, flux));
                 } catch (Exception e) {
                     logger.error("Échec de la recherche pour le fournisseur: {}", compte.getNomFournisseur(), e);
                     Tarif tarifErreur = new Tarif();
                     tarifErreur.setNom(compte.getNomFournisseur());
                     tarifErreur.setExecution(false);
                     tarifErreur.setErreur(e.getMessage());
                     tarifs.add(tarifErreur);
                 }
             } else {
                 logger.warn("Aucun service trouvé pour le fournisseur: {}", compte.getNomFournisseur());
             }
        }
        return tarifs;
    }

}
