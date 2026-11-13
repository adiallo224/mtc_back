package com.mtc.mutuaConseil.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Data
@ToString(exclude = "fluxData")
@Entity
@Table
public class Personne {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String civilite;
    private String nom;
    private String prenom;
    private String dateNaissance;
    private String nomVoie;
    private String numeroVoie;
    private String codePostal;
    private String ville;
    private String pays;
    private String telephone;
    private String email;
    private String regime;
    private String nationalite;
    private String statutProfession;
    private String professionSpecifique;
    private String profession;
    private Boolean autoEntrepreneur;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "flux_data_id", nullable = false)
    @JsonBackReference
    private FluxData fluxData;
}
