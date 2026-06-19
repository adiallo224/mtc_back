package com.mtc.mutuaConseil.utils;

import com.mtc.mutuaConseil.models.Compte;
import com.mtc.mutuaConseil.models.Tarif;
import com.mtc.mutuaConseil.models.TypeAssurance;
import com.mtc.mutuaConseil.models.enums.EnumTypeAssurance;
import com.mtc.mutuaConseil.services.servicesImpl.TypeAssuranceService;

import java.util.UUID;

public class TarifUtils {

    public static Tarif createDefaultTarif(Compte c, TypeAssuranceService typeAssuranceService, Long idAssu) {
        Tarif tarif = new Tarif();
        tarif.setTempId(UUID.randomUUID().toString());
        tarif.setExecution(false);
        tarif.setNom(c.getNomFournisseur());
        TypeAssurance typeAssurance = null;
        if (idAssu == 1) {
            typeAssurance = typeAssuranceService.findById(idAssu);
            typeAssurance.setTypeAssurance(EnumTypeAssurance.PRET);
        }
        if (idAssu == 2) {
            typeAssurance = typeAssuranceService.findById(idAssu);
            typeAssurance.setTypeAssurance(EnumTypeAssurance.MUTUELLE_INDIV);
        }
        if (idAssu == 3) {
            typeAssurance = typeAssuranceService.findById(idAssu);
            typeAssurance.setTypeAssurance(EnumTypeAssurance.MUTUELLE_PRO);
        }
        if (idAssu == 4) {
            typeAssurance = typeAssuranceService.findById(idAssu);
            typeAssurance.setTypeAssurance(EnumTypeAssurance.AUTO);
        }
        assert typeAssurance != null;
        typeAssurance.setAssuPret(true);
        tarif.setTypeAssurance(typeAssurance);
      return tarif;
    }
}
