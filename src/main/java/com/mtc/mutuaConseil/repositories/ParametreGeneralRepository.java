package com.mtc.mutuaConseil.repositories;

import com.mtc.mutuaConseil.models.ParametreGeneral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ParametreGeneralRepository extends JpaRepository<ParametreGeneral, Long> {
    Optional<ParametreGeneral> getParametreGeneralById(Long id);
}
