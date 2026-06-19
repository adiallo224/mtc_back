package com.mtc.mutuaConseil.dtos.requests;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EnfantRequestDTO {
    private String civilite;
    private String nom;
    private String prenom;
    private String dateNaissance;
}
