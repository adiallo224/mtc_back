package com.mtc.mutuaConseil.dtos.responses;

import lombok.Data;

import java.util.List;

@Data
public class CompteResponseDto {
    private Long id;
    private String username;
    private String password;
    private String urlFournisseur;
    private String nomFournisseur;
    private String typeAssurance;
    private String source;
    private Boolean actif;
    private int niveau;

}
