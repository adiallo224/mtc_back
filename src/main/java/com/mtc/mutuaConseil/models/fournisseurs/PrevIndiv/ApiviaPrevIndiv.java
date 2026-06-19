package com.mtc.mutuaConseil.models.fournisseurs.PrevIndiv;

import lombok.Data;

import java.util.List;

@Data
public class ApiviaPrevIndiv {

    private Long id;
    // Informations personnelles
    private String civilite;
    private String nom;
    private String prenom;
    private String dateNaissance;
    private String nomVoie;
    private String numeroVoie;
    private String codePostal;
    private String ville;
    private String telephone;
    private String mail;

    // Informations professionnelles
    private String nomEntreprise;
    private String siret;
    private String adresseProfessionnelle;
    private String codePostalProfessionnel;
    private String villeProfessionnelle;
    private String codeAPE;

    // Informations sur l'assurance
    private boolean fumeur;
    private boolean sportRisque;
    private double revenusAssurer;
    private List<String> garanties;
}
