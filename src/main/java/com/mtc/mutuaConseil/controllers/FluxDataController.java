package com.mtc.mutuaConseil.controllers;

import com.mtc.mutuaConseil.models.FluxData;
import com.mtc.mutuaConseil.repositories.FluxDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/fluxdata")
public class FluxDataController {

    private final FluxDataRepository fluxDataRepository;
    private static final Logger log = LoggerFactory.getLogger(FluxDataController.class);

    public FluxDataController(FluxDataRepository fluxDataRepository) {
        this.fluxDataRepository = fluxDataRepository;
    }

    @GetMapping
    public ResponseEntity<List<FluxData>> getAllFluxData() {
        List<FluxData> fluxDataList = fluxDataRepository.findAll();
        if (fluxDataList.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(fluxDataList);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FluxData> getFluxDataById(@PathVariable Long id) {
        return fluxDataRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
