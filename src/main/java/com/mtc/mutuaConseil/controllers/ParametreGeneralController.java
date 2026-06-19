package com.mtc.mutuaConseil.controllers;

import com.mtc.mutuaConseil.dtos.requests.ParametreGeneralRequestDto;
import com.mtc.mutuaConseil.dtos.responses.ParametreGeneralResponseDto;
import com.mtc.mutuaConseil.models.ParametreGeneral;
import com.mtc.mutuaConseil.services.servicesImpl.ParametreGeneralService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/parametreGeneral")
public class ParametreGeneralController {

    private final ParametreGeneralService parametreGeneralService;

    public ParametreGeneralController(ParametreGeneralService parametreGeneralService) {
        this.parametreGeneralService = parametreGeneralService;
    }

    // Endpoint pour mettre à jour un ParametreGeneral existant
    @PutMapping("/updateParametreGeneral")
    public ResponseEntity<ParametreGeneral> updateParametreGeneral(@RequestBody ParametreGeneral parametreGeneralDetails) {
        ParametreGeneral updatedParametreGeneral = parametreGeneralService.update(parametreGeneralDetails);
        return ResponseEntity.ok(updatedParametreGeneral);
    }

    // Endpoint pour créer un nouveau ParametreGeneral
    @PostMapping("/createParametreGeneral")
    public ResponseEntity<ParametreGeneralResponseDto> createParametreGeneral(@RequestBody ParametreGeneralRequestDto parametreGeneralRequestDto) {
        ParametreGeneralResponseDto createdParametreGeneralResponseDto = parametreGeneralService.create(parametreGeneralRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(createdParametreGeneralResponseDto);
    }

    // Endpoint pour récupérer un ParametreGeneral par son ID
    @GetMapping("/getParametreGeneralById/{id}")
    public ResponseEntity<ParametreGeneral> getParametreGeneralById(@PathVariable long id) {
        ParametreGeneral parametreGeneral = parametreGeneralService.findById(id);
        return ResponseEntity.ok(parametreGeneral);
    }

    // Endpoint pour récupérer tous les ParametreGeneral
    @GetMapping("/getAllParametreGenerals")
    public ResponseEntity<List<ParametreGeneral>> getAllParametreGenerals() {
        List<ParametreGeneral> parametreGenerals = parametreGeneralService.findAll();
        return ResponseEntity.ok(parametreGenerals);
    }

    // Endpoint pour supprimer un ParametreGeneral par son ID
    @DeleteMapping("/deleteParametreGeneral/{id}")
    public ResponseEntity<Void> deleteParametreGeneral(@PathVariable long id) {
        parametreGeneralService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
