package com.mtc.mutuaConseil.dtos.requests;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class EntrepriseRequestDTO {
    private String siret;
    private String nomEntreprise;
    private String dateCreationEntreprise;
    private String codeAPE;
    private String telephone;
    private String numeroVoieEntreprise;
    private String nomVoieEntreprise;
    private String codePostalEntreprise;
    private String villeEntreprise;
    private String paysEntreprise;
}
