package com.mtc.mutuaConseil.utils;

import com.mtc.mutuaConseil.models.FluxData;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;

import static com.mtc.mutuaConseil.utils.Shared.stringToDouble;

public class InformationsPret extends PageElementInteraction{

    /**
     *
     * @param driver
     * @param locator
     * @param timeOut
     * @param intervalSeconds
     * @param flux
     * @param index
     */
    public static boolean infoMontant(WebDriver driver, By locator, int timeOut, int intervalSeconds, FluxData flux, int index) {
        try {
            double montant = stringToDouble(flux.getPrets().get(index).getMontantPret());
            if (montant > 0) {
                WebElement inputMontant = LibSelenium.waitForElement(driver, locator, timeOut, intervalSeconds);
                inputMontant.clear();
                inputMontant.sendKeys(flux.getPrets().get(index).getMontantPret());
            }
            return true;  // L'élément a été trouvé et l'opération réussie
        } catch (Exception e) {
            return false;  // Une exception a été levée, donc l'élément n'a pas été trouvé ou une erreur est survenue
        }
    }

    /**
     *
     * @param driver
     * @param locator
     * @param timeOut
     * @param intervalSeconds
     * @param flux
     * @param index
     */
    public static boolean infoDuree(WebDriver driver, By locator, int timeOut, int intervalSeconds, FluxData flux, int index) {
        try {
            String dureeString = null;
            if (!flux.getPrets().get(index).getDiffere().isEmpty() && (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Partiel") || flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Total"))) {
                dureeString = flux.getPrets().get(index).getDureeAmort();
            } else {
                dureeString = flux.getPrets().get(index).getDuree();
            }
            double duree = stringToDouble(dureeString);
            if (duree > 0) {
                WebElement inputDuree = LibSelenium.waitForElement(driver, locator, timeOut, intervalSeconds);
                inputDuree.clear();
                inputDuree.sendKeys(dureeString);
            }
            return true;  // L'élément a été trouvé et l'opération réussie
        } catch (Exception e) {
            return false;  // Une exception a été levée, donc l'élément n'a pas été trouvé ou une erreur est survenue
        }
    }

    /**
     *
     * @param driver
     * @param locator
     * @param timeOut
     * @param intervalSeconds
     * @param flux
     * @param index
     */
    public static boolean infoTaux(WebDriver driver, By locator, int timeOut, int intervalSeconds, FluxData flux, int index) {
        try {
            String taux = flux.getPrets().get(index).getTaux();
            if (!taux.isEmpty()) {
                WebElement inputTaux = LibSelenium.waitForElement(driver, locator, timeOut, intervalSeconds);
                inputTaux.clear();
                inputTaux.sendKeys(taux);
            }
            return true;  // L'élément a été trouvé et l'opération réussie
        } catch (Exception e) {
            return false;  // Une exception a été levée, donc l'élément n'a pas été trouvé ou une erreur est survenue
        }
    }

    /**
     *
     * @param driver
     * @param locator
     * @param timeOut
     * @param intervalSeconds
     * @param flux
     * @param index
     */
    public static boolean infoDiffere(WebDriver driver, By locator, int timeOut, int intervalSeconds, FluxData flux, int index) {
        try {
            String dureeDiffereString = flux.getPrets().get(index).getDureeDiffere();
            if (dureeDiffereString.isEmpty()) {
                return false;
            }
            double dureeDiffere = stringToDouble(dureeDiffereString);
            if (dureeDiffere > 0) {
                WebElement inputDureeDiffere = LibSelenium.waitForElement(driver, locator, timeOut, intervalSeconds);
                inputDureeDiffere.clear();
                inputDureeDiffere.sendKeys(dureeDiffereString);
            }
            return true;  // L'élément a été trouvé et l'opération réussie
        } catch (Exception e) {
            return false;  // Une exception a été levée, donc l'élément n'a pas été trouvé ou une erreur est survenue
        }
    }

    /**
     *
     * @param driver
     * @param locator
     * @param timeOut
     * @param intervalSeconds
     * @param flux
     * @param index
     */
    public static boolean infoProjet(WebDriver driver, By locator, int timeOut, int intervalSeconds, FluxData flux, int index) {
        try {
            String objet = flux.getPrets().get(index).getObjet();
            if (objet != null) {
                WebElement dropdownProjet = LibSelenium.waitForElement(driver, locator, timeOut, intervalSeconds);
                Select selectProjet = new Select(dropdownProjet);
                selectProjet.selectByVisibleText(objet);
            }
            return true;  // L'élément a été trouvé et l'opération réussie
        } catch (Exception e) {
            return false;  // Une exception a été levée, donc l'élément n'a pas été trouvé ou une erreur est survenue
        }
    }

    /**
     *
     * @param driver
     * @param locator
     * @param timeOut
     * @param intervalSeconds
     * @param flux
     * @param index
     */
    public static boolean infoDateEffet(WebDriver driver, By locator, int timeOut, int intervalSeconds, FluxData flux, int index) {
        try {
            String dateEffet = flux.getPrets().get(index).getDateEffet();
            if (dateEffet != null) {
                WebElement inputDateEffet = LibSelenium.waitForElement(driver, locator, timeOut, intervalSeconds);
                inputDateEffet.clear();  // Optionnel : nettoyer le champ avant d'envoyer la nouvelle valeur
                inputDateEffet.sendKeys(dateEffet);
            }
            return true;  // L'élément a été trouvé et l'opération réussie
        } catch (Exception e) {
            return false;  // Une exception a été levée, donc l'élément n'a pas été trouvé ou une erreur est survenue
        }
    }

    /**
     *
     * @param driver
     * @param locator
     * @param timeOut
     * @param intervalSeconds
     * @param flux
     * @param index
     */
    public static void infoDateEffetStepByStep(WebDriver driver, By locator, int timeOut, int intervalSeconds, FluxData flux, int index) {
        if(flux.getPrets().get(index).getDateEffet() != null) {
            WebElement inputObjet = LibSelenium.waitForElement(driver, locator, timeOut, intervalSeconds);
           // inputObjet.sendKeys(flux.getPrets().get(index).getDateEffet());
            selectDate1(driver, locator, flux.getPrets().get(index).getDateEffet());
        }
    }

    @Override
    protected void pageName(String namePage) {

    }
}
