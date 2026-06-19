package com.mtc.mutuaConseil.dtos.requests;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PersonneRequestDTO {
    private String civilite;
    private String nom;
    private String prenom;
    private String dateNaissance;
    private String nomVoie;
    private String numeroVoie;
    private String codePostal;
    private String ville;
    private String pays;
    private String telephone;
    private String email;
    private String regime;
    private String nationalite;
    private String statutProfession;
    private String professionSpecifique;
    private String profession;
}
