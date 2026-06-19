package com.mtc.mutuaConseil.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Shared {

    private static final Logger log = LoggerFactory.getLogger(Shared.class);

    public static String doubleToString(double value) {
        return String.valueOf(value);
    }

    public static String intToString(int value) {
        return String.valueOf(value);
    }

    public static Double stringToDouble(String value) {
        return Double.valueOf(value);
    }

    public static int convertToInt(String str) {
        // Utilise Integer.parseInt() pour convertir la chaîne en entier
        int number = Integer.parseInt(str);
        return number;
    }

    public static int extractNumber(String input) {
        // Créez un motif regex pour correspondre à tous les chiffres dans la chaîne
        Pattern pattern = Pattern.compile("\\d+");
        Matcher matcher = pattern.matcher(input);
        // Parcourez les correspondances pour trouver le premier nombre
        if (matcher.find()) {
            // Convertissez la correspondance en entier
            return Integer.parseInt(matcher.group());
        } else {
            // Si aucun nombre n'est trouvé, renvoyez une valeur par défaut (0 ou une autre valeur selon vos besoins)
            return 0;
        }
    }

    public static String getDepartementFromCodePostal(String codePostal) {
        if (codePostal == null || codePostal.length() < 2) {
            throw new IllegalArgumentException("Code postal invalide");
        }
        String numeroDepartement = codePostal.substring(0, 2);
        // Gestion des cas particuliers pour les départements de 3 chiffres
        if (numeroDepartement.equals("97") || numeroDepartement.equals("98")) {
            if (codePostal.length() >= 3) {
                numeroDepartement = codePostal.substring(0, 3);
            }
        }
        return numeroDepartement;
    }

}
