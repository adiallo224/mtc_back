package com.mtc.mutuaConseil.controllers;

import com.mtc.mutuaConseil.dtos.requests.CompteRequestDto;
import com.mtc.mutuaConseil.dtos.responses.CompteResponseDto;
import com.mtc.mutuaConseil.services.servicesImpl.parametrage.CompteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/comptes")
public class CompteController {

    private final CompteService compteService;

    public CompteController(CompteService compteService) {
       this.compteService = compteService;
    }

    @PostMapping
    public ResponseEntity<CompteResponseDto> addCompte(@RequestBody CompteRequestDto compteRequestDto) {
        CompteResponseDto compteResponseDto = compteService.createCompte(compteRequestDto);
      return ResponseEntity.ok(compteResponseDto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CompteResponseDto> updateCompte(@PathVariable("id") Long id, @RequestBody CompteRequestDto compteRequestDto) {
        CompteResponseDto compteResponseDto = compteService.updateCompte(id, compteRequestDto);
        return ResponseEntity.ok(compteResponseDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CompteResponseDto> getCompteById(@PathVariable("id") Long id) {
        CompteResponseDto compteResponseDto = compteService.getCompteById(id);
        return ResponseEntity.ok(compteResponseDto);
    }

    @GetMapping
    public ResponseEntity<List<CompteResponseDto>> getAllComptes() {
        List<CompteResponseDto> compteResponseDtos = compteService.getAllComptes();
        return ResponseEntity.ok(compteResponseDtos);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCompte(@PathVariable Long id) {
        compteService.deleteCompte(id);
        return ResponseEntity.ok().body("{\"message\": \"Compte supprimé avec succès\"}");
    }

}
