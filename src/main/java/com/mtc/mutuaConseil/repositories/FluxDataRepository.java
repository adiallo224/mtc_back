package com.mtc.mutuaConseil.repositories;

import com.mtc.mutuaConseil.models.FluxData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FluxDataRepository extends JpaRepository<FluxData, Long> {

    Optional<FluxData> getFluxDataById(Long id);

    // Récupérer le dernier FluxData enregistré
    FluxData findFirstByOrderByIdDesc();

}
