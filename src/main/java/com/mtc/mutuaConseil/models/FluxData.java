package com.mtc.mutuaConseil.models;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Data
@ToString(exclude = {"tarifs", "personnes", "enfants", "prets", "infoAssureComplets"})
@Entity
public class FluxData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "type_assurance_id", nullable = false)
    private TypeAssurance typeAssurance;
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, fetch = FetchType.EAGER, mappedBy = "fluxData")
    @JsonManagedReference
    private List<Personne> personnes;
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, fetch = FetchType.EAGER, mappedBy = "fluxData")
    @JsonManagedReference
    private List<Enfant> enfants;
    @OneToOne(cascade = {CascadeType.PERSIST, CascadeType.REMOVE})
    private Entreprise entreprise;
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, fetch = FetchType.EAGER, mappedBy = "fluxData")
    @JsonManagedReference
    private List<Pret> prets;
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, fetch = FetchType.EAGER, mappedBy = "fluxData")
    @JsonManagedReference
    private List<InfoAssureComplet> infoAssureComplets;
    @OneToMany(cascade = {CascadeType.PERSIST, CascadeType.REMOVE}, fetch = FetchType.EAGER, mappedBy = "fluxData")
    @JsonManagedReference
    private List<Tarif> tarifs;
    @Column(updatable = false)
    @CreationTimestamp
    private LocalDateTime dateCreation;
    @UpdateTimestamp
    private LocalDateTime dateModification;
}





