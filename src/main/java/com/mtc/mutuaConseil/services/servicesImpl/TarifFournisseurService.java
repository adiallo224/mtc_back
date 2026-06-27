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

@Service
public class TarifFournisseurService {

    private final Logger logger = LoggerFactory.getLogger(TarifFournisseurService.class);
    private final Map<String, LaunchedService> serviceMap;

    // Injection des services via le constructeur
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
            AlptisMutuelProService alptisMutuelProService,
            AMI3FMutuelProService ami3FMutuelProService,
            AprilMutuelProService aprilMutuelProService,
            ApiviaMutuelProService apiviaMutuelProService,
            ApicilMutuelProService apicilMutuelProService,
            ECAMutuelProService ecaMutuelProService,
            EntoriaMutuelProService entoriaMutuelProService,
            FFAMutuelProService ffaMutuelProService,
            HarmonieMutuelProService harmonieMutuelProService,
            HennerMutuelProService hennerMutuelProService,
            LoomaMutuelProService loomaMutuelProService,
            QuatremTNSMutuelProService quatremTNSMutuelProService,
            RepamMutuelProService repamMutuelProService,
            SmisoMutuelProService smisoMutuelProService,
            // Mutuelle indiv
            AlptisMutuelIndivPlayWrightService alptisMutuelIndivService,
            AprilMutuelIndivService aprilMutuelIndivService,
            AMI3FMutuelIndivService ami3FMutuelIndivService,
            ApicilMutuelIndivService ApicilMutuelIndivService,
            ApiviaMutuelIndivService apiviaMutuelIndivService,
            EcaHeomieMutuelIndivPlayWrightService ecaMutuelIndivService,
            HennerMutuelIndivPlayWrightService hennerMutuelIndivService,
            QuatremIndivMutuelIndivPlayWrightService quatremIndivMutuelIndivService,
            RepamMutuelIndivPlayWrightService repamMutuelIndivService,
            SmisoMutuelIndivPlyWrightService smisoMutuelIndivService
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
        this.serviceMap.put("Alptis_Mutuelle_Pro", alptisMutuelProService);
        this.serviceMap.put("Ami3f_Mutuelle_Pro", ami3FMutuelProService);
        this.serviceMap.put("Apicil_Mutuelle_Pro", apicilMutuelProService);
        this.serviceMap.put("Apivia_Mutuelle_Pro", apiviaMutuelProService);
        this.serviceMap.put("April_Mutuelle_Pro", aprilMutuelProService);
        this.serviceMap.put("Eca_Mutuelle_Pro", ecaMutuelProService);
        this.serviceMap.put("Entoria_Mutuelle_Pro", entoriaMutuelProService);
        this.serviceMap.put("Ffa_Mutuelle_Pro", ffaMutuelProService);
        this.serviceMap.put("Harmonie_Mutuelle_Pro", harmonieMutuelProService);
        this.serviceMap.put("Henner_Mutuelle_Pro", hennerMutuelProService);
        this.serviceMap.put("Looma_Mutuelle_Pro", loomaMutuelProService); //à revoir
        this.serviceMap.put("Quatrem_Mutuelle_Pro", quatremTNSMutuelProService);
        this.serviceMap.put("Repam_Mutuelle_Pro", repamMutuelProService);
        this.serviceMap.put("Smiso_Mutuelle_Pro", smisoMutuelProService);
        // Mutuelle Indiv
        this.serviceMap.put("Alptis_Mutuelle_Indiv", alptisMutuelIndivService);
        this.serviceMap.put("Ami3f_Mutuelle_Indiv", ami3FMutuelIndivService);
        this.serviceMap.put("Apicil_Mutuelle_Indiv", ApicilMutuelIndivService);
        this.serviceMap.put("Apivia_Mutuelle_Indiv", apiviaMutuelIndivService);
        this.serviceMap.put("April_Mutuelle_Indiv", aprilMutuelIndivService);
        this.serviceMap.put("Eca_Mutuelle_Indiv", ecaMutuelIndivService);
        this.serviceMap.put("Henner_Mutuelle_Indiv", hennerMutuelIndivService);
        this.serviceMap.put("Quatrem_Mutuelle_Indiv", quatremIndivMutuelIndivService);
        this.serviceMap.put("Repam_Mutuelle_Indiv", repamMutuelIndivService);
        this.serviceMap.put("Smiso_Mutuelle_Indiv", smisoMutuelIndivService);
    }

    // Méthode pour récupérer les tarifs pour chaque compte
    public List<Tarif> getTarifsForComptes(List<Compte> comptes, FluxData flux) {
        List<Tarif> tarifs = new ArrayList<>();
        for (Compte compte : comptes) {
             // Récupération du service correspondant au fournisseur du compte
             LaunchedService service = serviceMap.get(compte.getNomFournisseur());
             if (service != null) {
                 tarifs.add(service.getResultFrom(compte, flux));
             } else {
                 logger.warn("Aucun service trouvé pour le fournisseur: {}", compte.getNomFournisseur());
             }
        }
        return tarifs;
    }

}
