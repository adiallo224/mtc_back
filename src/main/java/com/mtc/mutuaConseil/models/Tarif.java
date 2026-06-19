package com.mtc.mutuaConseil.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Data
@ToString(exclude = "fluxData")
@Entity
public class Tarif {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nom;
    private String montant;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "type_assurance_id")
    private TypeAssurance typeAssurance;
    private boolean execution;
    @Transient
    private String captureImg;
    @Column(columnDefinition = "TEXT")
    private String erreur;
    private String etape;
    @Transient
    private String captureImgErreur;
    private String captureImgPath;
    private String captureImgErreurPath;
    private String tempId;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "flux_data_id")
    @JsonBackReference
    private FluxData fluxData;
}


