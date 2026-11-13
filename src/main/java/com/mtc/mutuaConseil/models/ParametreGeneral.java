package com.mtc.mutuaConseil.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalTime;

@Data
@Entity
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ParametreGeneral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true)
    private String name = "unique_parametre_general";

    private String dossierImage;

    private String modeRecherche;

    private @JsonFormat(pattern = "HH:mm:ss") LocalTime heurePurge;

    private int nbJours;

    private String navigateurPlaywright;

}
