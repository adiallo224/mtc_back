package com.mtc.mutuaConseil.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Data
@ToString(exclude = "fluxData")
@Entity
public class Tarif {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nom;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<String> montant = new ArrayList<>();
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

    public void setMontant(List<String> montant) {
        this.montant = montant != null ? montant : new ArrayList<>();
    }

    public void setMontant(String montant) {
        this.montant = new ArrayList<>();
        if (montant != null) {
            this.montant.add(montant);
        }
    }

    public void addMontant(String montant) {
        if (montant != null) {
            this.montant.add(montant);
        }
    }
}


