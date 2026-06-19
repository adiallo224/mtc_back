package com.mtc.mutuaConseil.dtos.requests;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class InfoAssureCompletRequestDTO {
    private Boolean travailManuel;
    private Boolean travailManuelManuLourde;
    private Boolean travailHauteur;
    private Boolean produitDanger;
    private Boolean metierExpose;
    private Boolean sportRisque;
    private Boolean fumeur;
    private Boolean fumeurElecNico;
    private Boolean fumeurSansNico;
    @JsonProperty("deplacement_pro_20000")
    private Boolean deplacementPro20000;
    @JsonProperty("deplacement_etranger_60")
    private Boolean deplacementEtranger60;
    private Boolean deplacementPaysRisque;
    private Boolean instrumentPrecis;
    private String garantie;
    private String quotiteDeces;
    private String quotite;
    private Boolean garantieChomage;
    private String hauteur;
}
