package com.mtc.mutuaConseil.repositories;

import com.mtc.mutuaConseil.models.Compte;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompteRepository extends JpaRepository<Compte, Long> {
    Optional<Compte> getCompteById(Long id);
}
