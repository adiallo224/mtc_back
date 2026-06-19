package com.mtc.mutuaConseil.models;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class Entreprise {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String siret;
    private String nomEntreprise;
    private String dateCreationEntreprise;
    @Column(name = "code_ape")
    private String codeAPE;
    private String telephone;
    private String numeroVoieEntreprise;
    private String nomVoieEntreprise;
    private String codePostalEntreprise;
    private String villeEntreprise;
    private String paysEntreprise;
    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "flux_data_id")
    private FluxData fluxData;
}
