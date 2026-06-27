package com.mtc.mutuaConseil.dtos.requests;

import lombok.Data;

@Data
public class CompteRequestDto {
    private String username;
    private String password;
    private String urlFournisseur;
    private String nomFournisseur;
    private String typeAssurance;
    private Boolean actif;
    private String source;
    private int niveau;
    private Integer ordre;
}

