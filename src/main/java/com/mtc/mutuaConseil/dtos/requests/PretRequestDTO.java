package com.mtc.mutuaConseil.dtos.requests;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PretRequestDTO {
    private String objet;
    private String banque;
    private String type;
    private String montantPret;
    private String duree;
    private String differe;
    private String dureeDiffere;
    private String typeTaux;
    private String taux;
    private String nouveauOuReprise;
    private String dateEffet;
    private String dureeAmort;
}
