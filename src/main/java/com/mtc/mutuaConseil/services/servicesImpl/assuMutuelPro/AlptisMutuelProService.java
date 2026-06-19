package com.mtc.mutuaConseil.services.servicesImpl.assuMutuelPro;

import com.mtc.mutuaConseil.services.servicesImpl.TypeAssuranceService;
import com.mtc.mutuaConseil.services.servicesImpl.assuMutuelIndiv.AlptisMutuelIndivService;
import org.springframework.stereotype.Service;

@Service
public class AlptisMutuelProService extends AlptisMutuelIndivService {
    public AlptisMutuelProService(TypeAssuranceService typeAssuranceService) {
        super(typeAssuranceService);
    }
}
