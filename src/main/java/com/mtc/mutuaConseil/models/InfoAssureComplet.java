package com.mtc.mutuaConseil.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Data
@ToString(exclude = "fluxData")
@Entity
public class InfoAssureComplet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "travail_manuel")
    private Boolean travailManuel;
    @Column(name = "travail_manuel_manu_lourde")
    private Boolean travailManuelManuLourde;
    @Column(name = "travail_hauteur")
    private Boolean travailHauteur;
    @Column(name = "produit_danger")
    private Boolean produitDanger;
    @Column(name = "metier_expose")
    private Boolean metierExpose;
    @Column(name = "sport_risque")
    private Boolean sportRisque;
    private Boolean fumeur;
    @Column(name = "fumeur_elec_nico")
    private Boolean fumeurElecNico;
    @Column(name = "fumeur_sans_nico")
    private Boolean fumeurSansNico;
    @Column(name = "deplacement_pro_20000")
    private Boolean deplacementPro20000;
    @Column(name = "deplacement_etranger_60")
    private Boolean deplacementEtranger60;
    @Column(name = "deplacement_pays_risque")
    private Boolean deplacementPaysRisque;
    @Column(name = "instrument_precis")
    private Boolean instrumentPrecis;
    private String garantie;
    private String quotite;
    @Column(name = "quotite_deces")
    private String quotiteDeces;
    @Column(name = "garantie_chomage")
    private Boolean garantieChomage;
    private String hauteur;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "flux_data_id", nullable = false)
    @JsonBackReference
    private FluxData fluxData;
}
