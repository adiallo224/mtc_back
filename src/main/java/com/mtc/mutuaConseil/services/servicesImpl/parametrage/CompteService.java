package com.mtc.mutuaConseil.services.servicesImpl.parametrage;

import com.mtc.mutuaConseil.dtos.requests.CompteRequestDto;
import com.mtc.mutuaConseil.dtos.responses.CompteResponseDto;
import com.mtc.mutuaConseil.models.Compte;
import com.mtc.mutuaConseil.repositories.CompteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CompteService {

    @Autowired
    private CompteRepository compteRepository;

    public CompteResponseDto createCompte(CompteRequestDto compteRequestDto) {
        Compte c = new Compte();
        c.setUsername(compteRequestDto.getUsername());
        c.setPassword(compteRequestDto.getPassword());
        c.setNomFournisseur(compteRequestDto.getNomFournisseur());
        c.setUrlFournisseur(compteRequestDto.getUrlFournisseur());
        c.setTypeAssurance(compteRequestDto.getTypeAssurance());
        c.setActif(compteRequestDto.getActif());
        c.setSource(compteRequestDto.getSource());
        c.setNiveau(compteRequestDto.getNiveau());
        Compte createdCompte = compteRepository.save(c);
        CompteResponseDto compteResponseDto = new CompteResponseDto();
        compteResponseDto.setId(createdCompte.getId());
        compteResponseDto.setUsername(createdCompte.getUsername());
        compteResponseDto.setPassword(createdCompte.getPassword());
        compteResponseDto.setNomFournisseur(createdCompte.getNomFournisseur());
        compteResponseDto.setUrlFournisseur(createdCompte.getUrlFournisseur());
        compteResponseDto.setTypeAssurance(createdCompte.getTypeAssurance());
        compteResponseDto.setActif(createdCompte.getActif());
        compteResponseDto.setSource(createdCompte.getSource());
        compteResponseDto.setNiveau(createdCompte.getNiveau());
        return compteResponseDto;
    }

    public CompteResponseDto updateCompte(Long id, CompteRequestDto compteRequestDto) {
        Compte updatedCompte = compteRepository.getCompteById(id).orElseThrow();
        updatedCompte.setUsername(compteRequestDto.getUsername());
        updatedCompte.setPassword(compteRequestDto.getPassword());
//        updatedCompte.setNomFournisseur(compteRequestDto.getNomFournisseur());
        updatedCompte.setUrlFournisseur(compteRequestDto.getUrlFournisseur());
        updatedCompte.setTypeAssurance(compteRequestDto.getTypeAssurance());
        updatedCompte.setActif(compteRequestDto.getActif());
        updatedCompte.setNiveau(compteRequestDto.getNiveau());
        Compte compte = compteRepository.save(updatedCompte);
        CompteResponseDto compteResponseDto = new CompteResponseDto();
        compteResponseDto.setId(compte.getId());
        compteResponseDto.setUsername(compte.getUsername());
        compteResponseDto.setNomFournisseur(compte.getNomFournisseur());
        compteResponseDto.setUrlFournisseur(compte.getUrlFournisseur());
        compteResponseDto.setTypeAssurance(compte.getTypeAssurance());
        compteResponseDto.setActif(compte.getActif());
        compteResponseDto.setNiveau(compte.getNiveau());
        return compteResponseDto;
    }

    public void updateCompte(Long id, String elt) {
        Compte updatedCompte = compteRepository.getCompteById(id).orElseThrow();
        updatedCompte.setAuthKeyQrCode(elt);
        compteRepository.save(updatedCompte);
    }

    public CompteResponseDto getCompteById(Long id) {
        Compte compte = compteRepository.getCompteById(id).orElseThrow();
        CompteResponseDto compteResponseDto = new CompteResponseDto();
        compteResponseDto.setId(compte.getId());
        compteResponseDto.setUsername(compte.getUsername());
        compteResponseDto.setNomFournisseur(compte.getNomFournisseur());
        compteResponseDto.setUrlFournisseur(compte.getUrlFournisseur());
        compteResponseDto.setTypeAssurance(compte.getTypeAssurance());
        compteResponseDto.setActif(compte.getActif());
        compteResponseDto.setNiveau(compte.getNiveau());
        return compteResponseDto;
    }

    public Compte getCompteFromId(Long id) {
        return compteRepository.findById(id).orElseThrow();
    }

    public List<CompteResponseDto> getAllComptes() {
        List<CompteResponseDto> compteResponseDtos = new ArrayList<>();
        List<Compte> comptes = compteRepository.findAll();
        for (Compte c : comptes) {
            CompteResponseDto compteResponseDto = new CompteResponseDto();
            compteResponseDto.setId(c.getId());
            compteResponseDto.setUsername(c.getUsername());
            compteResponseDto.setPassword(c.getPassword());
            compteResponseDto.setNomFournisseur(c.getNomFournisseur());
            compteResponseDto.setUrlFournisseur(c.getUrlFournisseur());
            compteResponseDto.setTypeAssurance(c.getTypeAssurance());
            compteResponseDto.setSource(c.getSource());
            compteResponseDto.setActif(c.getActif());
            compteResponseDto.setNiveau(c.getNiveau());
            compteResponseDtos.add(compteResponseDto);
        }
        return compteResponseDtos;
    }

    public List<Compte> getComptes() {
        List<Compte> comptesActifs = new ArrayList<>();
        List<Compte> comptes = compteRepository.findAll();
        for(Compte c : comptes){
            if(c.getActif()){
               comptesActifs.add(c);
            }
        }
        return comptesActifs;
    }

    public List<Compte> getComptes(String typeAssurance) {
        List<Compte> comptesActifs = new ArrayList<>();
        List<Compte> comptes = compteRepository.findAll();
        for (Compte c : comptes){
            if (c.getActif() != null && c.getActif() && c.getTypeAssurance() != null && c.getTypeAssurance().equalsIgnoreCase(typeAssurance)){
                comptesActifs.add(c);
            }
        }
        return comptesActifs;
    }

    public void deleteCompte(Long id) {
        Compte c = compteRepository.getCompteById(id).orElseThrow();
        compteRepository.delete(c);
    }
}
