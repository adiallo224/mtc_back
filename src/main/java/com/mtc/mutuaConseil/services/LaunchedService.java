package com.mtc.mutuaConseil.services;

import com.mtc.mutuaConseil.models.Compte;
import com.mtc.mutuaConseil.models.FluxData;
import com.mtc.mutuaConseil.models.Tarif;

public interface LaunchedService {
    Tarif getResultFrom(Compte compte, FluxData flux);
}
