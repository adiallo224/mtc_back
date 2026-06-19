package com.mtc.mutuaConseil.utils.mapper;

import com.mtc.mutuaConseil.dtos.requests.*;
import com.mtc.mutuaConseil.models.*;
import com.mtc.mutuaConseil.models.enums.EnumTypeAssurance;

import java.util.List;
import java.util.stream.Collectors;

public class MapperFlux {

    // méthode pour mapper FluxDataRequestDTO à FluxData
    public FluxData mapperFluxDataRequestDTOToFluxData(FluxDataRequestDTO fluxDataRequestDTO) {
        if (fluxDataRequestDTO == null) {
            return null;
        }
        FluxData fluxData = new FluxData();
        // Mapper le typeAssurance
        if (fluxDataRequestDTO.getTypeAssurance() != null) {
            TypeAssurance typeAssurance = mapperTypeAssuranceRequestDTOToTypeAssurance(fluxDataRequestDTO.getTypeAssurance());
            fluxData.setTypeAssurance(typeAssurance);
        }

        // Mapper les personnes
        if (fluxDataRequestDTO.getPersonnes() != null) {
            List<Personne> personnes = fluxDataRequestDTO.getPersonnes().stream()
                    .map(this::mapperPersonneRequestDTOToPersonne)
                    .collect(Collectors.toList());
            fluxData.setPersonnes(personnes);
        }

        // Mapper les enfants
        if (fluxDataRequestDTO.getEnfants() != null) {
            List<Enfant> enfants = fluxDataRequestDTO.getEnfants().stream()
                    .map(this::mapperEnfantRequestDTOToEnfant)
                    .collect(Collectors.toList());
            fluxData.setEnfants(enfants);
        }

        // Mapper l'entreprise
        if (fluxDataRequestDTO.getEntreprise() != null) {
            Entreprise entreprise = mapperEntrepriseRequestDTOToEntreprise(fluxDataRequestDTO.getEntreprise());
            fluxData.setEntreprise(entreprise);
        }

        // Mapper les prets
        if (fluxDataRequestDTO.getPrets() != null) {
            List<Pret> prets = fluxDataRequestDTO.getPrets().stream()
                    .map(this::mapperPretRequestDTOToPret)
                    .collect(Collectors.toList());
            fluxData.setPrets(prets);
        }

        // Mapper les infoAssureComplets
        if (fluxDataRequestDTO.getInfoAssureComplets() != null) {
            List<InfoAssureComplet> infoAssureComplets = fluxDataRequestDTO.getInfoAssureComplets().stream()
                    .map(this::mapperInfoAssureCompletRequestDTOToInfoAssureComplet)
                    .collect(Collectors.toList());
            fluxData.setInfoAssureComplets(infoAssureComplets);
        }
      return fluxData;
    }

    // méthode pour mapper TypeAssuranceRequestDTO à TypeAssurance
    public TypeAssurance mapperTypeAssuranceRequestDTOToTypeAssurance(TypeAssuranceRequestDTO typeAssuranceRequestDTO) {
        if (typeAssuranceRequestDTO == null) {
            return null;
        }
        TypeAssurance typeAssurance = new TypeAssurance();

        // Convertir le champ typeAssurance de String à EnumTypeAssurance
        if (typeAssuranceRequestDTO.getTypeAssurance() != null) {
            try {
                EnumTypeAssurance enumTypeAssurance = EnumTypeAssurance.valueOf(typeAssuranceRequestDTO.getTypeAssurance());
                typeAssurance.setTypeAssurance(enumTypeAssurance);
            } catch (IllegalArgumentException e) {
                // Gérer le cas où la valeur de typeAssurance ne correspond à aucune valeur de l'énumération
                throw new IllegalArgumentException("Valeur de typeAssurance non valide : " + typeAssuranceRequestDTO.getTypeAssurance());
            }
        }
        // Mapper les autres champs
        typeAssurance.setAssuPret(typeAssuranceRequestDTO.getAssuPret() != null ? typeAssuranceRequestDTO.getAssuPret() : false);
        typeAssurance.setAssuAuto(typeAssuranceRequestDTO.getAssuAuto() != null ? typeAssuranceRequestDTO.getAssuAuto() : false);
        typeAssurance.setAssuMutuelIndiv(typeAssuranceRequestDTO.getAssuMutuelIndiv() != null ? typeAssuranceRequestDTO.getAssuMutuelIndiv() : false);
        typeAssurance.setAssuMutuelPro(typeAssuranceRequestDTO.getAssuMutuelPro() != null ? typeAssuranceRequestDTO.getAssuMutuelPro() : false);

      return typeAssurance;
    }

    // méthode pour mapper PersonneRequestDTO à Personne
    public Personne mapperPersonneRequestDTOToPersonne(PersonneRequestDTO personneRequestDTO) {
        if (personneRequestDTO == null) {
            return null;
        }
        Personne personne = new Personne();
        // Mapper les champs simples
        personne.setCivilite(personneRequestDTO.getCivilite());
        personne.setNom(personneRequestDTO.getNom());
        personne.setPrenom(personneRequestDTO.getPrenom());
        personne.setDateNaissance(personneRequestDTO.getDateNaissance());
        personne.setNomVoie(personneRequestDTO.getNomVoie());
        personne.setNumeroVoie(personneRequestDTO.getNumeroVoie());
        personne.setCodePostal(personneRequestDTO.getCodePostal());
        personne.setVille(personneRequestDTO.getVille());
        personne.setPays(personneRequestDTO.getPays());
        personne.setTelephone(personneRequestDTO.getTelephone());
        personne.setEmail(personneRequestDTO.getEmail());
        personne.setRegime(personneRequestDTO.getRegime());
        personne.setNationalite(personneRequestDTO.getNationalite());
        personne.setStatutProfession(personneRequestDTO.getStatutProfession());
        personne.setProfessionSpecifique(personneRequestDTO.getProfessionSpecifique());
        personne.setProfession(personneRequestDTO.getProfession());
       return personne;
    }

    // méthode pour mapper EnfantRequestDTO à Enfant
    public Enfant mapperEnfantRequestDTOToEnfant(EnfantRequestDTO enfantRequestDTO) {
        if (enfantRequestDTO == null) {
            return null;
        }
        Enfant enfant = new Enfant();
        // Mapper les champs simples
        enfant.setCivilite(enfantRequestDTO.getCivilite());
        enfant.setNom(enfantRequestDTO.getNom());
        enfant.setPrenom(enfantRequestDTO.getPrenom());
        enfant.setDateNaissance(enfantRequestDTO.getDateNaissance());
      return enfant;
    }

    // méthode pour mapper EntrepriseRequestDTO à Entreprise
    public Entreprise mapperEntrepriseRequestDTOToEntreprise(EntrepriseRequestDTO entrepriseRequestDTO) {
        if (entrepriseRequestDTO == null) {
            return null;
        }
        Entreprise entreprise = new Entreprise();
        // Mapper les champs simples
        entreprise.setSiret(entrepriseRequestDTO.getSiret());
        entreprise.setNomEntreprise(entrepriseRequestDTO.getNomEntreprise());
        entreprise.setDateCreationEntreprise(entrepriseRequestDTO.getDateCreationEntreprise());
        entreprise.setCodeAPE(entrepriseRequestDTO.getCodeAPE());
        entreprise.setTelephone(entrepriseRequestDTO.getTelephone());
        entreprise.setNumeroVoieEntreprise(entrepriseRequestDTO.getNumeroVoieEntreprise());
        entreprise.setNomVoieEntreprise(entrepriseRequestDTO.getNomVoieEntreprise());
        entreprise.setCodePostalEntreprise(entrepriseRequestDTO.getCodePostalEntreprise());
        entreprise.setVilleEntreprise(entrepriseRequestDTO.getVilleEntreprise());
        entreprise.setPaysEntreprise(entrepriseRequestDTO.getPaysEntreprise());
      return entreprise;
    }

    // méthode pour mapper PretRequestDTO à Pret
    public Pret mapperPretRequestDTOToPret(PretRequestDTO pretRequestDTO) {
        if (pretRequestDTO == null) {
            return null;
        }
        Pret pret = new Pret();
        // Mapper les champs simples
        pret.setObjet(pretRequestDTO.getObjet());
        pret.setBanque(pretRequestDTO.getBanque());
        pret.setType(pretRequestDTO.getType());
        pret.setMontantPret(pretRequestDTO.getMontantPret());
        pret.setDuree(pretRequestDTO.getDuree());
        pret.setDiffere(pretRequestDTO.getDiffere());
        pret.setDureeDiffere(pretRequestDTO.getDureeDiffere());
        pret.setTypeTaux(pretRequestDTO.getTypeTaux());
        pret.setTaux(pretRequestDTO.getTaux());
        pret.setNouveauOuReprise(pretRequestDTO.getNouveauOuReprise());
        pret.setDateEffet(pretRequestDTO.getDateEffet());
        pret.setDureeAmort(pretRequestDTO.getDureeAmort());
      return pret;
    }

    // méthode pour mapper InfoAssureCompletRequestDTO à InfoAssureComplet
    public InfoAssureComplet mapperInfoAssureCompletRequestDTOToInfoAssureComplet(InfoAssureCompletRequestDTO infoAssureCompletRequestDTO) {
        if (infoAssureCompletRequestDTO == null) {
            return null;
        }
        InfoAssureComplet infoAssureComplet = new InfoAssureComplet();
        // Mapper les champs booléens
        infoAssureComplet.setTravailManuel(infoAssureCompletRequestDTO.getTravailManuel());
        infoAssureComplet.setTravailManuelManuLourde(infoAssureCompletRequestDTO.getTravailManuelManuLourde());
        infoAssureComplet.setTravailHauteur(infoAssureCompletRequestDTO.getTravailHauteur());
        infoAssureComplet.setProduitDanger(infoAssureCompletRequestDTO.getProduitDanger());
        infoAssureComplet.setMetierExpose(infoAssureCompletRequestDTO.getMetierExpose());
        infoAssureComplet.setSportRisque(infoAssureCompletRequestDTO.getSportRisque());
        infoAssureComplet.setFumeur(infoAssureCompletRequestDTO.getFumeur());
        infoAssureComplet.setFumeurElecNico(infoAssureCompletRequestDTO.getFumeurElecNico());
        infoAssureComplet.setFumeurSansNico(infoAssureCompletRequestDTO.getFumeurSansNico());
        infoAssureComplet.setDeplacementPro20000(infoAssureCompletRequestDTO.getDeplacementPro20000());
        infoAssureComplet.setDeplacementEtranger60(infoAssureCompletRequestDTO.getDeplacementEtranger60());
        infoAssureComplet.setDeplacementPaysRisque(infoAssureCompletRequestDTO.getDeplacementPaysRisque());
        infoAssureComplet.setInstrumentPrecis(infoAssureCompletRequestDTO.getInstrumentPrecis());
        infoAssureComplet.setGarantieChomage(infoAssureCompletRequestDTO.getGarantieChomage());

        // Mapper les champs de type String
        infoAssureComplet.setGarantie(infoAssureCompletRequestDTO.getGarantie());
        infoAssureComplet.setQuotiteDeces(infoAssureCompletRequestDTO.getQuotiteDeces());
        infoAssureComplet.setQuotite(infoAssureCompletRequestDTO.getQuotite());
        infoAssureComplet.setHauteur(infoAssureCompletRequestDTO.getHauteur());
      return infoAssureComplet;
    }
}
