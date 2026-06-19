package com.mtc.mutuaConseil.controllers;

import com.mtc.mutuaConseil.dtos.requests.FluxDataRequestDTO;
import com.mtc.mutuaConseil.models.Compte;
import com.mtc.mutuaConseil.models.FluxData;
import com.mtc.mutuaConseil.models.Tarif;
import com.mtc.mutuaConseil.models.enums.EnumTypeAssurance;
import com.mtc.mutuaConseil.services.FluxDataService;
import com.mtc.mutuaConseil.services.servicesImpl.TarifFournisseurService;
import com.mtc.mutuaConseil.services.servicesImpl.assuPret.*;
import com.mtc.mutuaConseil.services.servicesImpl.parametrage.CompteService;
import com.mtc.mutuaConseil.utils.mapper.MapperFlux;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/search")
public class TarifController {

    private final Logger logger = LoggerFactory.getLogger(TarifController.class);
    @Autowired private CompteService compteService;
    @Autowired private TarifFournisseurService tarifFournisseurService;
    @Autowired private FluxDataService fluxDataService;
    @Autowired private ApiviaService apiviaService;
    @Autowired private AfiescaService afiescaService;
    @Autowired  private AlptisService alptisService;
    @Autowired private ApicilService apicilService;
    @Autowired private AprilService aprilService;
    @Autowired private AssureaPretService assureaPretService;
    @Autowired private DigitalInsurePretService digitalInsurePretService;
    @Autowired private HarmonieADPPretService harmonieADPPretService;
    @Autowired private MetLifeService metLifeService;
    @Autowired private RepamService repamService;
    @Autowired private SimulassurService simulassurService;
    @Autowired private SWLService swlService;
    @Autowired private UtwinPretService utwinPretService;

    @PostMapping("/getTarifs")
    public ResponseEntity<List<Tarif>> getTarifs(@RequestBody FluxDataRequestDTO fluxDataRequestDTO) {
        logger.info("Flux : {}", fluxDataRequestDTO);
        MapperFlux mapperFlux = new MapperFlux();
        FluxData fluxData = mapperFlux.mapperFluxDataRequestDTOToFluxData(fluxDataRequestDTO);
            List<Tarif> tarifs = new ArrayList<>();
            if (fluxDataRequestDTO.getTypeAssurance().getAssuPret() != null && fluxDataRequestDTO.getTypeAssurance().getAssuPret()) {
                List<Compte> comptes = compteService.getComptes(String.valueOf(EnumTypeAssurance.PRET));
                tarifs.addAll(tarifFournisseurService.getTarifsForComptes(comptes, fluxData));
            }
            if (fluxData.getTypeAssurance().getAssuAuto() != null && fluxData.getTypeAssurance().getAssuAuto()) {
                List<Compte> comptes = compteService.getComptes(String.valueOf(EnumTypeAssurance.AUTO));
                tarifs.addAll(tarifFournisseurService.getTarifsForComptes(comptes, fluxData));
            }
            if (fluxData.getTypeAssurance().getAssuMutuelIndiv() != null && fluxData.getTypeAssurance().getAssuMutuelIndiv()) {
                List<Compte> comptes = compteService.getComptes(String.valueOf(EnumTypeAssurance.MUTUELLE_INDIV));
                tarifs.addAll(tarifFournisseurService.getTarifsForComptes(comptes, fluxData));
            }
            if (fluxData.getTypeAssurance().getAssuMutuelPro() != null && fluxData.getTypeAssurance().getAssuMutuelPro()) {
                List<Compte> comptes = compteService.getComptes(String.valueOf(EnumTypeAssurance.MUTUELLE_PRO));
                tarifs.addAll(tarifFournisseurService.getTarifsForComptes(comptes, fluxData));
            }

            // insertion du flux en base
            fluxData.setTarifs(tarifs);
            if (fluxData.getTypeAssurance().getAssuPret()) {
                fluxDataService.saveFluxData(fluxData, 1, true);
            } else if (fluxData.getTypeAssurance().getAssuMutuelIndiv()) {
                fluxDataService.saveFluxData(fluxData, 2, true);
            } else if (fluxData.getTypeAssurance().getAssuMutuelPro()) {
                fluxDataService.saveFluxData(fluxData, 3, true);
            } else if (fluxData.getTypeAssurance().getAssuAuto()) {
                fluxDataService.saveFluxData(fluxData, 4, true);
            }
            FluxData lastFluxData = fluxDataService.getTarifsWithBase64(fluxDataService.getTarifsWithBase64(fluxData));
        return ResponseEntity.ok(lastFluxData.getTarifs());
    }
}
