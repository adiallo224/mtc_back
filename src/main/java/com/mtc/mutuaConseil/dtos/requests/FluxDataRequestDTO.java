package com.mtc.mutuaConseil.dtos.requests;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class FluxDataRequestDTO {
    private TypeAssuranceRequestDTO typeAssurance;
    private List<PersonneRequestDTO> personnes;
    private List<EnfantRequestDTO> enfants;
    private EntrepriseRequestDTO entreprise;
    private List<PretRequestDTO> prets;
    private List<InfoAssureCompletRequestDTO> infoAssureComplets;
}
