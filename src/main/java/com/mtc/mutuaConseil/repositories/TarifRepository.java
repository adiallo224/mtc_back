package com.mtc.mutuaConseil.repositories;

import com.mtc.mutuaConseil.models.Tarif;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TarifRepository extends JpaRepository<Tarif, Long> {
    Optional<Tarif> getTarifById(Long id);
}
