package com.mtc.mutuaConseil.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Base64;

public class ImageStorageService {
    private static final String IMAGE_DIRECTORY = "uploads/";

    public String saveBase64Image(String base64Image, String fileName) throws Exception {
        // Créer le répertoire s'il n'existe pas
        File directory = new File(IMAGE_DIRECTORY);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Convertir le Base64 en tableau de bytes
        byte[] imageBytes = Base64.getDecoder().decode(base64Image);

        // Définir le chemin du fichier
        String filePath = IMAGE_DIRECTORY + fileName + ".png";

        // Sauvegarder le fichier
        try (FileOutputStream fos = new FileOutputStream(filePath)) {
            fos.write(imageBytes);
        }

        return filePath;
    }
}
