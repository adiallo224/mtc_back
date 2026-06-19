package com.mtc.mutuaConseil.repositories;

import com.mtc.mutuaConseil.models.TypeAssurance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TypeAssuranceRepository extends JpaRepository<TypeAssurance, Long> {
}
