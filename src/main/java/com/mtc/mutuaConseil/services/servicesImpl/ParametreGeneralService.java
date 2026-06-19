package com.mtc.mutuaConseil.services.servicesImpl;

import com.mtc.mutuaConseil.dtos.requests.ParametreGeneralRequestDto;
import com.mtc.mutuaConseil.dtos.responses.ParametreGeneralResponseDto;
import com.mtc.mutuaConseil.models.ParametreGeneral;
import com.mtc.mutuaConseil.repositories.ParametreGeneralRepository;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ParametreGeneralService {

    private final ParametreGeneralRepository parametreGeneralRepository;

    public ParametreGeneralService(ParametreGeneralRepository parametreGeneralRepository){
        this.parametreGeneralRepository = parametreGeneralRepository;
    }

    // A faire et preciser que table doit avoir une seule ligne

    public ParametreGeneral findById(long id) {
        return parametreGeneralRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("ParametreGeneral non trouvé"));
    }

    // Méthode pour récupérer tous les ParametreGeneral
    public List<ParametreGeneral> findAll() {
        return parametreGeneralRepository.findAll();
    }

    // Méthode pour créer un nouveau ParametreGeneral
    public ParametreGeneralResponseDto create(ParametreGeneralRequestDto parametreGeneralRequestDto) {
        ParametreGeneral parametreGeneral = new ParametreGeneral();
        parametreGeneral.setName("unique_parametre_general");
        parametreGeneral.setDossierImage(parametreGeneralRequestDto.getDossierImage());
        parametreGeneral.setModeRecherche(parametreGeneralRequestDto.getModeRecherche());
        parametreGeneral.setHeurePurge(parametreGeneralRequestDto.getHeurePurge());
        parametreGeneral.setNbJours(parametreGeneralRequestDto.getNbJours());
        ParametreGeneral createdParametreGeneral = parametreGeneralRepository.save(parametreGeneral);
        return getParametreGeneralResponseDto(createdParametreGeneral);
    }

    protected @NotNull ParametreGeneralResponseDto getParametreGeneralResponseDto(ParametreGeneral createdParametreGeneral) {
        ParametreGeneralResponseDto parametreGeneralResponseDto = new ParametreGeneralResponseDto();
        parametreGeneralResponseDto.setId(createdParametreGeneral.getId());
        parametreGeneralResponseDto.setName(createdParametreGeneral.getName());
        parametreGeneralResponseDto.setDossierImage(createdParametreGeneral.getDossierImage());
        parametreGeneralResponseDto.setModeRecherche(createdParametreGeneral.getDossierImage());
        parametreGeneralResponseDto.setHeurePurge(createdParametreGeneral.getHeurePurge());
        parametreGeneralResponseDto.setNbJours(createdParametreGeneral.getNbJours());
        return parametreGeneralResponseDto;
    }

    // Méthode pour mettre à jour un ParametreGeneral existant
    public ParametreGeneral update(ParametreGeneral parametreGeneralDetails) {
        ParametreGeneral parametreGeneral = findById(parametreGeneralDetails.getId());
        parametreGeneral.setDossierImage(parametreGeneralDetails.getDossierImage());
        parametreGeneral.setModeRecherche(parametreGeneralDetails.getModeRecherche());
        parametreGeneral.setHeurePurge(parametreGeneralDetails.getHeurePurge());
        parametreGeneral.setNbJours(parametreGeneralDetails.getNbJours());
        return parametreGeneralRepository.save(parametreGeneral);
    }

    // Méthode pour supprimer un ParametreGeneral par son ID
    public void delete(long id) {
        ParametreGeneral parametreGeneral = findById(id);
        parametreGeneralRepository.delete(parametreGeneral);
    }

}
