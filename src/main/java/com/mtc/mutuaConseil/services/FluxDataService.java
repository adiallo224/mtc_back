package com.mtc.mutuaConseil.services;

import com.mtc.mutuaConseil.models.*;
import com.mtc.mutuaConseil.repositories.FluxDataRepository;
import com.mtc.mutuaConseil.services.servicesImpl.TarifService;
import com.mtc.mutuaConseil.services.servicesImpl.TypeAssuranceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FluxDataService {

    private final static Logger log = LoggerFactory.getLogger(FluxDataService.class);
    private final FluxDataRepository fluxDataRepository;
    private final TypeAssuranceService typeAssuranceService;
    private final TarifService tarifService;

    public FluxDataService(FluxDataRepository fluxDataRepository, TypeAssuranceService typeAssuranceService, TarifService tarifService){
        this.fluxDataRepository = fluxDataRepository;
        this.typeAssuranceService = typeAssuranceService;
        this.tarifService = tarifService;
    }

    public void saveFluxData(FluxData fluxData) {
        this.fluxDataRepository.save(fluxData);
    }

    public void saveFluxData(FluxData fluxData, long typeAssuranceId, boolean type) {
        TypeAssurance typeAssurance = this.typeAssuranceService.findById(typeAssuranceId);
        String typeAssuranceStr = typeAssurance.getTypeAssurance().toString().toUpperCase();
        switch (typeAssuranceStr) {
            case "PRET":
                typeAssurance.setAssuPret(type);
                break;
            case "MUTUELLE_INDIV":
                typeAssurance.setAssuMutuelIndiv(type);
                break;
            case "MUTUELLE_PRO":
                typeAssurance.setAssuMutuelPro(type);
                break;
            case "AUTO":
                typeAssurance.setAssuAuto(type);
                break;
            default:
                throw new IllegalArgumentException("TypeAssurance inconnu : " + typeAssuranceStr);
        }

        fluxData.setTypeAssurance(typeAssurance);

        if (fluxData.getPersonnes() != null) {
            for (Personne personne : fluxData.getPersonnes()) {
                personne.setFluxData(fluxData);
            }
        }
        if (fluxData.getEnfants() != null) {
            for (Enfant enfant : fluxData.getEnfants()) {
                enfant.setFluxData(fluxData);
            }
        }
        if (fluxData.getPrets() != null) {
            for (Pret pret : fluxData.getPrets()) {
                pret.setFluxData(fluxData);
            }
        }
        if (fluxData.getInfoAssureComplets() != null) {
            for (InfoAssureComplet info : fluxData.getInfoAssureComplets()) {
                info.setFluxData(fluxData);
            }
        }
        if (fluxData.getTarifs() != null) {
            for (Tarif tarif : fluxData.getTarifs()) {
                tarif.setFluxData(fluxData);
                tarif.setTypeAssurance(typeAssurance);
            }
        }

        this.fluxDataRepository.save(fluxData);

        FluxData lastFluxData = getLastFluxData();
        if (lastFluxData != null && lastFluxData.getTarifs() != null) {
            for (Tarif tarif : lastFluxData.getTarifs()) {
                renameImageFiles(tarif);
            }
        }
    }

    public void renameImageFiles(Tarif tarif) {
        Path currentCapturePath = null;
        if (tarif.getCaptureImgPath() == null) {
            return;
        }
        currentCapturePath = Paths.get(tarif.getCaptureImgPath());
        String currentFileName = currentCapturePath.getFileName().toString();
        String uuidRegex = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";
        Pattern pattern = Pattern.compile(uuidRegex);
        Matcher matcher = pattern.matcher(currentFileName);
        String newFileName;
        if (matcher.find()) {
            newFileName = currentFileName.replaceFirst(uuidRegex, tarif.getId().toString());
        } else {
            throw new IllegalArgumentException("Le nom du fichier ne contient pas d'UUID valide.");
        }
        Path newCapturePath = currentCapturePath.getParent().resolve(newFileName);
        try {
             Files.move(currentCapturePath, newCapturePath);
             tarif.setCaptureImgPath(newCapturePath.toString());
             tarifService.updateTarif(tarif.getId(), tarif);
        } catch (IOException e) {
            log.info("renameImageFiles Erreur {} :", e.getMessage());
        }
    }

    public FluxData getLastFluxData() {
        return this.fluxDataRepository.findFirstByOrderByIdDesc();
    }

}
