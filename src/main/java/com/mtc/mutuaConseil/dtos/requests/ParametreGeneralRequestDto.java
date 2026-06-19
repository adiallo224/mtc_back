package com.mtc.mutuaConseil.dtos.requests;

import lombok.Data;

import java.time.LocalTime;

@Data
public class ParametreGeneralRequestDto {
    private String name = "unique_parametre_general";
    private String dossierImage;
    private String modeRecherche;
    private LocalTime heurePurge;
    private int nbJours;
}
