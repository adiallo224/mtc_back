package com.mtc.mutuaConseil.models.fournisseurs.AssPret;

import com.mtc.mutuaConseil.models.Pret;
import lombok.Data;

import java.util.List;

@Data
public class MetLifeAssPret {
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
    private String regime;
    private double quotite;
    private boolean fumeur;

    // Informations sur le conjoint
    private String civiliteConjoint;
    private String nomConjoint;
    private String prenomConjoint;
    private String dateNaissanceConjoint;
    private String telephoneConjoint;
    private String mailConjoint;
    private String regimeConjoint;
    private boolean fumeurConjoint;
    private double quotiteConjoint;

    // Nouveau/reprise Pret
    private List<Pret> pret;

    // Informations sur l'assurance
    private List<String> garanties; // Garanties : DC/PTIA IPT ITT IPP
}
