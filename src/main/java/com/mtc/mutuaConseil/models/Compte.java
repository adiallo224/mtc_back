package com.mtc.mutuaConseil.models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "compte")
public class Compte {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String username;
    private String password;
    private String nomFournisseur;
    private String urlFournisseur;
    private String typeAssurance;
    private String source;
    private Boolean actif;
    private String authKeyQrCode;
    private int niveau;
    private Integer ordre;
   // private String cheminQrCode;
}
