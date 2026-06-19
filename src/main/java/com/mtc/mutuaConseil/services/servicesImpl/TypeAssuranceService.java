package com.mtc.mutuaConseil.services.servicesImpl;

import com.mtc.mutuaConseil.models.TypeAssurance;
import com.mtc.mutuaConseil.repositories.TypeAssuranceRepository;
import org.springframework.stereotype.Service;

@Service
public class TypeAssuranceService {

    private final TypeAssuranceRepository typeAssuranceRepository;

    public TypeAssuranceService(TypeAssuranceRepository typeAssuranceRepository){
       this.typeAssuranceRepository = typeAssuranceRepository;
    }

    public TypeAssurance findById(long id) {
        return typeAssuranceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("TypeAssurance non trouvé"));
    }
}
