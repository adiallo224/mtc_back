package com.mtc.mutuaConseil.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Data
@ToString(exclude = "fluxData")
@Entity
public class Pret {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String objet;
    private String banque;
    private String type;
    private String montantPret;
    private String duree;
    private String differe;
    private String dureeDiffere;
    private String typeTaux;
    private String taux;
    private String nouveauOuReprise;
    private String dateEffet;
    private String dureeAmort;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "flux_data_id", nullable = false)
    @JsonBackReference
    private FluxData fluxData;
}
