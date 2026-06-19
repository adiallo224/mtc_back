package com.mtc.mutuaConseil.utils;

import com.mtc.mutuaConseil.models.FluxData;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class InformationsPersonne extends PageElementInteraction {

    private static final Logger log = LoggerFactory.getLogger(InformationsPersonne.class);

    /**
     *
     * @param driver
     * @param locator
     * @param timeOut
     * @param intervalSeconds
     * @param flux
     * @param index
     */
    public static boolean infoNom(WebDriver driver, By locator, int timeOut, int intervalSeconds, FluxData flux, int index, String source) {
        try {
            if (flux.getPersonnes().get(index).getNom() == null) {
                log.error("Nom is null");
                return false;
            }

            WebElement inputNom = LibSelenium.waitForElement(driver, locator, timeOut, intervalSeconds);
            if (inputNom != null) {
                inputNom.sendKeys(flux.getPersonnes().get(index).getNom());
                return true;
            } else {
                log.error("Nom input element not found");
                return false;
            }
        } catch (Exception e) {
            log.error("An error occurred while trying to enter nom", e);
            return false;
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
    public static boolean infoPrenom(WebDriver driver, By locator, int timeOut, int intervalSeconds, FluxData flux, int index, String source) {
        try {
            if (flux.getPersonnes().get(index).getPrenom() == null) {
                log.error("Prenom is null");
                return false;
            }

            WebElement inputPrenom = LibSelenium.waitForElement(driver, locator, timeOut, intervalSeconds);
            if (inputPrenom != null) {
                inputPrenom.sendKeys(flux.getPersonnes().get(index).getPrenom());
                return true;
            } else {
                log.error("Prenom input element not found");
                return false;
            }
        } catch (Exception e) {
            log.error("An error occurred while trying to enter prenom", e);
            return false;
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
    public static boolean infoDateNaissance(WebDriver driver, By locator, int timeOut, int intervalSeconds, FluxData flux, int index, String source) {
        try {
            if (flux.getPersonnes().get(index).getDateNaissance() == null) {
                log.error("Date de naissance is null");
                return false;
            }

            WebElement inputDateNaissance = LibSelenium.waitForElement(driver, locator, timeOut, intervalSeconds);
            if (inputDateNaissance != null) {
                inputDateNaissance.sendKeys(flux.getPersonnes().get(index).getDateNaissance());
                return true;
            } else {
                log.error("Date de naissance input element not found");
                return false;
            }
        } catch (Exception e) {
            log.error("An error occurred while trying to enter date de naissance", e);
            return false;
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
    public static boolean infoEmail(WebDriver driver, By locator, int timeOut, int intervalSeconds, FluxData flux, int index, String source) {
        try {
            if (flux.getPersonnes().get(index).getEmail() == null) {
                log.error("Email is null");
                return false;
            }

            WebElement inputEmail = LibSelenium.waitForElement(driver, locator, timeOut, intervalSeconds);
            if (inputEmail != null) {
                inputEmail.sendKeys(flux.getPersonnes().get(index).getEmail());
                return true;
            } else {
                log.error("Email input element not found");
                return false;
            }
        } catch (Exception e) {
            log.error("An error occurred while trying to enter email", e);
            return false;
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
    public static boolean infoTelephone(WebDriver driver, By locator, int timeOut, int intervalSeconds, FluxData flux, int index, String source) {
        try {
            if (flux.getPersonnes().get(index).getTelephone() == null) {
                log.error("Telephone number is null");
                return false;
            }

            WebElement inputTelephone = LibSelenium.waitForElement(driver, locator, timeOut, intervalSeconds);
            if (inputTelephone != null) {
                inputTelephone.sendKeys(flux.getPersonnes().get(index).getTelephone());
                return true;
            } else {
                log.error("Telephone input element not found");
                return false;
            }
        } catch (Exception e) {
            log.error("An error occurred while trying to enter telephone number", e);
            return false;
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
    public static boolean infoStatutProfession(WebDriver driver, By locator, int timeOut, int intervalSeconds, FluxData flux, int index, String source) {
        try {
            if (flux.getPersonnes().get(index).getStatutProfession() == null) {
                log.error("Statut profession is null");
                return false;
            }

            WebElement dropdownStatutPro = LibSelenium.waitForElement(driver, locator, timeOut, intervalSeconds);
            if (dropdownStatutPro != null) {
                Select selectStatutPro = new Select(dropdownStatutPro);
                List<WebElement> options = selectStatutPro.getOptions();
                boolean foundOption = false;

                for (WebElement option : options) {
                    if (option.getText().contains(flux.getPersonnes().get(index).getStatutProfession())) {
                        selectStatutPro.selectByVisibleText(flux.getPersonnes().get(index).getStatutProfession());
                        foundOption = true;
                        break;
                    }
                }

                if (!foundOption) {
                    if (source.contains("swt") && flux.getPersonnes().get(index).getStatutProfession().equalsIgnoreCase("Aucun secteur d'activité spécifique")) {
                        selectStatutPro.selectByVisibleText("Aucune profession spécifique");
                    } else {
                        log.error("No matching option found for statut profession");
                        return false;
                    }
                }

                return true;
            } else {
                log.error("Dropdown statut profession element not found");
                return false;
            }
        } catch (Exception e) {
            log.error("An error occurred while trying to select statut profession", e);
            return false;
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
    public static boolean infoProfession(WebDriver driver, By locator, int timeOut, int intervalSeconds, FluxData flux, int index, String source) {
        try {
            String emploi = "";
            if (flux.getPersonnes().get(index).getProfession() == null) {
                log.error("Profession is null");
                return false;
            }

            WebElement dropdownProfession = LibSelenium.waitForElement(driver, locator, timeOut, intervalSeconds);
            if (dropdownProfession != null) {
                Select selectProfession = new Select(dropdownProfession);
                List<WebElement> options = selectProfession.getOptions();
                boolean foundOption = false;

                for (WebElement option : options) {
                    if (option.getText().equalsIgnoreCase("Cadres")) {
                        emploi = "Cadre";
                    }
                    if (option.getText().contains(flux.getPersonnes().get(index).getProfession()) || (!emploi.isEmpty() && flux.getPersonnes().get(index).getProfession().contains(emploi))) {
                        selectProfession.selectByVisibleText(option.getText());
                        foundOption = true;
                        break;
                    }
                }

                if (!foundOption) {
                    log.error("No matching option found for profession");
                    return false;
                }

                return true;
            } else {
                log.error("Dropdown profession element not found");
                return false;
            }
        } catch (Exception e) {
            log.error("An error occurred while trying to select profession", e);
            return false;
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
    public static boolean infoDepartement(WebDriver driver, By locator, int timeOut, int intervalSeconds, FluxData flux, int index, String source) {
        try {
            if (flux.getPersonnes().get(index).getCodePostal() == null) {
                log.error("Code postal is null");
                return false;
            }

            WebElement inputDepartement = LibSelenium.waitForElement(driver, locator, timeOut, intervalSeconds);
            if (inputDepartement != null) {
                inputDepartement.sendKeys(flux.getPersonnes().get(index).getCodePostal().substring(0, 2));
                return true;
            } else {
                log.error("Input departement element not found");
                return false;
            }
        } catch (Exception e) {
            log.error("An error occurred while trying to input departement", e);
            return false;
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
    public static boolean infoAdresse(WebDriver driver, By locator, int timeOut, int intervalSeconds, FluxData flux, int index, String source) {
        try {
            if (flux.getPersonnes().get(index).getNumeroVoie() == null || flux.getPersonnes().get(index).getNomVoie() == null) {
                log.error("Numero voie or nom voie is null");
                return false;
            }

            WebElement inputAdresse = LibSelenium.waitForElement(driver, locator, timeOut, intervalSeconds);
            if (inputAdresse != null) {
                inputAdresse.sendKeys(flux.getPersonnes().get(index).getNumeroVoie() + " " + flux.getPersonnes().get(index).getNomVoie());
                return true;
            } else {
                log.error("Input adresse element not found");
                return false;
            }
        } catch (Exception e) {
            log.error("An error occurred while trying to input adresse", e);
            return false;
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
    public static boolean infoCodePostal(WebDriver driver, By locator, int timeOut, int intervalSeconds, FluxData flux, int index, String source) {
        try {
            if (flux.getPersonnes().get(index).getCodePostal() == null) {
                log.error("Code postal is null");
                return false;
            }

            WebElement inputCodePostal = LibSelenium.waitForElement(driver, locator, timeOut, intervalSeconds);
            if (inputCodePostal != null) {
                inputCodePostal.sendKeys(flux.getPersonnes().get(index).getCodePostal());
                return true;
            } else {
                log.error("Input code postal element not found");
                return false;
            }
        } catch (Exception e) {
            log.error("An error occurred while trying to input code postal", e);
            return false;
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
    public static boolean infoVille(WebDriver driver, By locator, int timeOut, int intervalSeconds, FluxData flux, int index, String source) {
        try {
            String ville = flux.getPersonnes().get(index).getVille();
            if (ville == null) {
                log.error("Ville is null");
                return false;
            }

            WebElement inputVille = LibSelenium.waitForElement(driver, locator, timeOut, intervalSeconds);
            if (inputVille != null) {
                inputVille.sendKeys(ville);
                return true;
            } else {
                log.error("Input ville element not found");
                return false;
            }
        } catch (Exception e) {
            log.error("An error occurred while trying to input ville", e);
            return false;
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
    public static boolean infoPays(WebDriver driver, By locator, int timeOut, int intervalSeconds, FluxData flux, int index, String source) {
        try {
            String pays = flux.getPersonnes().get(index).getPays();
            if (pays == null) {
                log.error("Pays is null");
                return false;
            }

            WebElement dropdownPays = LibSelenium.waitForElement(driver, locator, timeOut, intervalSeconds);
            if (dropdownPays != null) {
                Select selectPays = new Select(dropdownPays);
                List<WebElement> options = selectPays.getOptions();
                for (WebElement webElement : options) {
                    if (webElement.getText().contains(pays)) {
                        selectPays.selectByVisibleText(pays);
                        return true;
                    }
                }
                log.error("Pays not found in the dropdown options");
                return false;
            } else {
                log.error("Dropdown pays element not found");
                return false;
            }
        } catch (Exception e) {
            log.error("An error occurred while trying to select pays", e);
            return false;
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
    public static boolean infoQuotite(WebDriver driver, By locator, int timeOut, int intervalSeconds, FluxData flux, int index, String source) {
        try {
            String quotite = flux.getInfoAssureComplets().get(index).getQuotite();
            if (quotite == null) {
                log.error("Quotite is null");
                return false;
            }

            WebElement inputQuotite = LibSelenium.waitForElement(driver, locator, timeOut, intervalSeconds);
            if (inputQuotite != null) {
                inputQuotite.sendKeys(quotite);
                return true;
            } else {
                log.error("Quotite input element not found");
                return false;
            }
        } catch (Exception e) {
            log.error("An error occurred while trying to input quotite", e);
            return false;
        }
    }

    public static boolean infoQuotiteEmploi(WebDriver driver, By locator, int timeOut, int intervalSeconds, FluxData flux, int index, String source) {
        try {
            String quotite = flux.getInfoAssureComplets().get(index).getQuotite();
            if (quotite == null) {
                log.error("Quotite is null");
                return false;
            }

            WebElement inputQuotite = LibSelenium.waitForElement(driver, locator, timeOut, intervalSeconds);
            if (inputQuotite != null) {
                inputQuotite.sendKeys(quotite);
                return true;
            } else {
                log.error("Quotite input element not found");
                return false;
            }
        } catch (Exception e) {
            log.error("An error occurred while trying to input quotite", e);
            return false;
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
    public static boolean infoQuotiteDeces(WebDriver driver, By locator, int timeOut, int intervalSeconds, FluxData flux, int index, String source) {
        try {
            String quotiteDeces = flux.getInfoAssureComplets().get(index).getQuotiteDeces();
            if (quotiteDeces == null) {
                log.error("QuotiteDeces is null");
                return false;
            }

            WebElement inputQuotite = LibSelenium.waitForElement(driver, locator, timeOut, intervalSeconds);
            if (inputQuotite != null) {
                inputQuotite.sendKeys(quotiteDeces);
                return true;
            } else {
                log.error("QuotiteDeces input element not found");
                return false;
            }
        } catch (Exception e) {
            log.error("An error occurred while trying to input quotiteDeces", e);
            return false;
        }
    }

    @Override
    protected void pageName(String namePage) {

    }
}
