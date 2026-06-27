package com.mtc.mutuaConseil.services.servicesImpl.parametrage;

import com.mtc.mutuaConseil.dtos.requests.CompteRequestDto;
import com.mtc.mutuaConseil.dtos.responses.CompteResponseDto;
import com.mtc.mutuaConseil.models.Compte;
import com.mtc.mutuaConseil.repositories.CompteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.util.Objects.isNull;

@Service
public class CompteService {

    @Autowired
    private CompteRepository compteRepository;

    private void validateOrdre(String typeAssurance, Integer ordre, Long excludeId) {
        if (isNull(ordre)) return;
        List<Compte> comptes = compteRepository.findAll();
        for (Compte c : comptes) {
            if (!isNull(excludeId) && c.getId().equals(excludeId)) continue;
            if (typeAssurance != null && typeAssurance.equalsIgnoreCase(c.getTypeAssurance())
                    && ordre.equals(c.getOrdre())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "L'ordre " + ordre + " est déjà utilisé par un autre service de type " + typeAssurance);
            }
        }
    }

    private CompteResponseDto toResponseDto(Compte c) {
        CompteResponseDto dto = new CompteResponseDto();
        dto.setId(c.getId());
        dto.setUsername(c.getUsername());
        dto.setPassword(c.getPassword());
        dto.setNomFournisseur(c.getNomFournisseur());
        dto.setUrlFournisseur(c.getUrlFournisseur());
        dto.setTypeAssurance(c.getTypeAssurance());
        dto.setActif(c.getActif());
        dto.setSource(c.getSource());
        dto.setNiveau(c.getNiveau());
        dto.setOrdre(c.getOrdre());
        return dto;
    }

    public CompteResponseDto createCompte(CompteRequestDto compteRequestDto) {
        validateOrdre(compteRequestDto.getTypeAssurance(), compteRequestDto.getOrdre(), null);
        Compte c = new Compte();
        c.setUsername(compteRequestDto.getUsername());
        c.setPassword(compteRequestDto.getPassword());
        c.setNomFournisseur(compteRequestDto.getNomFournisseur());
        c.setUrlFournisseur(compteRequestDto.getUrlFournisseur());
        c.setTypeAssurance(compteRequestDto.getTypeAssurance());
        c.setActif(compteRequestDto.getActif());
        c.setSource(compteRequestDto.getSource());
        c.setNiveau(compteRequestDto.getNiveau());
        c.setOrdre(compteRequestDto.getOrdre());
        return toResponseDto(compteRepository.save(c));
    }

    public CompteResponseDto updateCompte(Long id, CompteRequestDto compteRequestDto) {
        validateOrdre(compteRequestDto.getTypeAssurance(), compteRequestDto.getOrdre(), id);
        Compte updatedCompte = compteRepository.getCompteById(id).orElseThrow();
        updatedCompte.setUsername(compteRequestDto.getUsername());
        updatedCompte.setPassword(compteRequestDto.getPassword());
        updatedCompte.setUrlFournisseur(compteRequestDto.getUrlFournisseur());
        updatedCompte.setTypeAssurance(compteRequestDto.getTypeAssurance());
        updatedCompte.setActif(compteRequestDto.getActif());
        updatedCompte.setNiveau(compteRequestDto.getNiveau());
        updatedCompte.setOrdre(compteRequestDto.getOrdre());
        return toResponseDto(compteRepository.save(updatedCompte));
    }

    public void updateCompte(Long id, String elt) {
        Compte updatedCompte = compteRepository.getCompteById(id).orElseThrow();
        updatedCompte.setAuthKeyQrCode(elt);
        compteRepository.save(updatedCompte);
    }

    public CompteResponseDto getCompteById(Long id) {
        return toResponseDto(compteRepository.getCompteById(id).orElseThrow());
    }

    public Compte getCompteFromId(Long id) {
        return compteRepository.findById(id).orElseThrow();
    }

    public List<CompteResponseDto> getAllComptes() {
        List<CompteResponseDto> dtos = new ArrayList<>();
        for (Compte c : compteRepository.findAll()) {
            dtos.add(toResponseDto(c));
        }
        return dtos;
    }

    public List<Compte> getComptes() {
        List<Compte> comptesActifs = new ArrayList<>();
        for (Compte c : compteRepository.findAll()) {
            if (c.getActif() != null && c.getActif()) {
                comptesActifs.add(c);
            }
        }
        return comptesActifs;
    }

    public List<Compte> getComptes(String typeAssurance) {
        List<Compte> comptesActifs = new ArrayList<>();
        for (Compte c : compteRepository.findAll()) {
            if (c.getActif() != null && c.getActif()
                    && c.getTypeAssurance() != null
                    && c.getTypeAssurance().equalsIgnoreCase(typeAssurance)) {
                comptesActifs.add(c);
            }
        }
        comptesActifs.sort(Comparator.comparing(Compte::getOrdre,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return comptesActifs;
    }

    public void deleteCompte(Long id) {
        Compte c = compteRepository.getCompteById(id).orElseThrow();
        compteRepository.delete(c);
    }
}
