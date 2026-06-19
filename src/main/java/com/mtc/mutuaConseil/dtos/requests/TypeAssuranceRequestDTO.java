package com.mtc.mutuaConseil.dtos.requests;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class TypeAssuranceRequestDTO {
    private String typeAssurance;
    private Boolean assuPret;
    private Boolean assuAuto;
    private Boolean assuMutuelIndiv;
    private Boolean assuMutuelPro;
}
