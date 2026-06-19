package com.mtc.mutuaConseil.models;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

@Data
@ToString(exclude = "fluxData")
@Entity
public class Enfant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String civilite;
    private String nom;
    private String prenom;
    private String dateNaissance;
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "flux_data_id", nullable = false)
    @JsonBackReference
    private FluxData fluxData;
}
