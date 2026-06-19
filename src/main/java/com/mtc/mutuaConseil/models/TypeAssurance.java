package com.mtc.mutuaConseil.models;

import com.mtc.mutuaConseil.models.enums.EnumTypeAssurance;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
public class TypeAssurance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, unique = true)
    private EnumTypeAssurance typeAssurance;

    private Boolean assuPret = false;
    private Boolean assuAuto = false;
    private Boolean assuMutuelIndiv = false;
    private Boolean assuMutuelPro= false;
}
