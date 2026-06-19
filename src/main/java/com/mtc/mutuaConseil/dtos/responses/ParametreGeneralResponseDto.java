package com.mtc.mutuaConseil.dtos.responses;

import lombok.Data;

import java.time.LocalTime;

@Data
public class ParametreGeneralResponseDto {
    private Long id;
    private String name = "unique_parametre_general";
    private String dossierImage;
    private String modeRecherche;
    private LocalTime heurePurge;
    private int nbJours;
}
