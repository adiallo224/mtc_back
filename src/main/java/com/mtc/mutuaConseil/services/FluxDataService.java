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
import java.util.Base64;
import java.util.List;
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

//    public void saveFluxData(FluxData fluxData, long typeAssuranceId, boolean type) {
//        // Trouver le TypeAssurance
//        TypeAssurance typeAssurance = this.typeAssuranceService.findById(typeAssuranceId);
//        if (typeAssurance.getTypeAssurance().toString().equalsIgnoreCase("PRET")) {
//            typeAssurance.setAssuPret(type);
//        }
//        if (typeAssurance.getTypeAssurance().toString().equalsIgnoreCase("MUTUELLE_INDIV")) {
//            typeAssurance.setAssuMutuelIndiv(type);
//        }
//        if (typeAssurance.getTypeAssurance().toString().equalsIgnoreCase("MUTUELLE_PRO")) {
//            typeAssurance.setAssuMutuelPro(type);
//        }
//        if (typeAssurance.getTypeAssurance().toString().equalsIgnoreCase("AUTO")) {
//            typeAssurance.setAssuAuto(type);
//        }
//        fluxData.setTypeAssurance(typeAssurance);
//
//        // ------Ajout de chaque entité au fluxData-------
//        if (fluxData.getPersonnes() != null) {
//            for (Personne personne : fluxData.getPersonnes()) {
//                personne.setFluxData(fluxData);
//            }
//        }
//        if (fluxData.getEnfants() != null) {
//            for (Enfant enfant : fluxData.getEnfants()) {
//                enfant.setFluxData(fluxData);
//            }
//        }
//        if (fluxData.getPrets() != null) {
//            for (Pret pret : fluxData.getPrets()) {
//                pret.setFluxData(fluxData);
//            }
//        }
//        if (fluxData.getInfoAssureComplets() != null) {
//            for (InfoAssureComplet info : fluxData.getInfoAssureComplets()) {
//                info.setFluxData(fluxData);
//            }
//        }
//        if (fluxData.getTarifs() != null) {
//            for (Tarif tarif : fluxData.getTarifs()) {
//                tarif.setFluxData(fluxData);
//                tarif.setTypeAssurance(typeAssurance);
//            }
//        }
//        // Sauvegarde du FluxData
//        this.fluxDataRepository.save(fluxData);
//
//        // Récupérer le dernier FluxData enregistré
//        FluxData lastFluxData = getLastFluxData();
//
//        // Mettre à jour les noms des fichiers avec l'ID réel après la persistance
//        if (lastFluxData != null && lastFluxData.getTarifs() != null) {
//            for (Tarif tarif : lastFluxData.getTarifs()) {
//                 renameImageFiles(tarif); // Renommer les fichiers avec l'ID réel
//            }
//        }
//    }

    public void saveFluxData(FluxData fluxData, long typeAssuranceId, boolean type) {
        // Trouver le TypeAssurance existant en base
        TypeAssurance typeAssurance = this.typeAssuranceService.findById(typeAssuranceId);

        // Mettre à jour les champs en fonction du type
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
        // Lier le TypeAssurance au FluxData
        fluxData.setTypeAssurance(typeAssurance);
        // Associer chaque sous-élément au FluxData
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
                // Ajouter explicitement le TypeAssurance existant
                tarif.setTypeAssurance(typeAssurance);
            }
        }
        // Sauvegarder tout le FluxData (et ses relations)
        this.fluxDataRepository.save(fluxData);
        // Renommer les fichiers d'image après persistence
        FluxData lastFluxData = getLastFluxData();
        if (lastFluxData != null && lastFluxData.getTarifs() != null) {
            for (Tarif tarif : lastFluxData.getTarifs()) {
                renameImageFiles(tarif);
            }
        }
    }

    public void renameImageFiles(Tarif tarif) {
        // Chemins actuels des fichiers basés sur l'UUID temporaire
        Path currentCapturePath = null;
        if (tarif.getCaptureImgPath() == null) {
            return;
        }
        currentCapturePath = Paths.get(tarif.getCaptureImgPath());
        // Récupérer le nom du fichier actuel
        String currentFileName = currentCapturePath.getFileName().toString();
        // Regex pour identifier l'UUID dans le nom du fichier
        String uuidRegex = "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}";
        Pattern pattern = Pattern.compile(uuidRegex);
        Matcher matcher = pattern.matcher(currentFileName);
        // Remplacer l'UUID par l'ID de Tarif
        String newFileName;
        if (matcher.find()) {
            newFileName = currentFileName.replaceFirst(uuidRegex, tarif.getId().toString());
        } else {
            throw new IllegalArgumentException("Le nom du fichier ne contient pas d'UUID valide.");
        }
        // Nouveau chemin basé sur le nouveau nom de fichier
        Path newCapturePath = currentCapturePath.getParent().resolve(newFileName);
        try {
             // Renommer le fichier
             Files.move(currentCapturePath, newCapturePath);
             // Mettre à jour le champ captureImg dans Tarif
            tarif.setCaptureImgPath(newCapturePath.toString());
            tarifService.updateTarif(tarif.getId(), tarif);
        } catch (IOException e) {
            log.info("renameImageFiles Erreur {} :", e.getMessage());
        }
    }

    public FluxData getLastFluxData() {
        // à revoir pour rajouter la remonté par rapport au jour et l'heure s'il faut'
        return this.fluxDataRepository.findFirstByOrderByIdDesc();
    }

    public FluxData getTarifsWithBase64(FluxData fluxData) {
        List<Tarif> tarifications = fluxData.getTarifs();
        if (tarifications != null) {
            for (Tarif tarif : tarifications) {
                String path = tarif.getCaptureImgPath();
                if (path != null && !path.isEmpty()) {
                    if (Files.exists(Path.of(path))) {
                        try {
                            byte[] imageBytes = Files.readAllBytes(Path.of(path));
                            String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                            tarif.setCaptureImg(base64Image);
                        } catch (IOException e) {
                            // Gestion d'erreur ou mettre un message d'erreur en base64
                            tarif.setCaptureImg(null); // ou un message d'erreur
                        }
                    }
                    fluxData.setTarifs(tarifications);
                }
            }
        }
        return fluxData;
    }

}
