package com.mtc.mutuaConseil.selectors;

import java.nio.file.Path;

public class SelectorNotFoundException extends RuntimeException {

    public SelectorNotFoundException(String provider, String key, Path file) {
        super("Sélecteur introuvable -- fournisseur='%s', clé='%s', fichier='%s'. Édite ce fichier YAML pour corriger (pas besoin de recompiler)."
                .formatted(provider, key, file));
    }

    public SelectorNotFoundException(String provider, Path file, Throwable cause) {
        super("Impossible de charger les sélecteurs du fournisseur '%s' -- fichier attendu : '%s' (répertoire de travail courant : '%s'). Vérifie la propriété 'selectors.base-path'."
                .formatted(provider, file.toAbsolutePath(), Path.of("").toAbsolutePath()), cause);
    }
}
