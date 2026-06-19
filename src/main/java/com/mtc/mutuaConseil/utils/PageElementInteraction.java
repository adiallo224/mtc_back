package com.mtc.mutuaConseil.utils;

import com.mtc.mutuaConseil.models.FluxData;
import com.mtc.mutuaConseil.models.Tarif;
import com.mtc.mutuaConseil.services.servicesImpl.TarifService;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

abstract public class PageElementInteraction {

    private static final Logger log = LoggerFactory.getLogger(PageElementInteraction.class);
    private String namePage;
    private JavascriptExecutor js;
    Actions actions = null;
    @Autowired private TarifService tarifService;

    protected abstract void pageName(String namePage);

    protected WebElement waitForElement(WebDriver driver, By by, int timeoutSeconds, int pollIntervalSeconds) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            wait.pollingEvery(Duration.ofSeconds(pollIntervalSeconds));
            return wait.until(ExpectedConditions.visibilityOfElementLocated(by));
        } catch (TimeoutException e) {
            log.error("Element not found: {}", by);
            return null;
        }
    }

    public WebElement waitForElementIxplicit2(WebDriver driver, By locator, int timeOut, int intervalSeconds) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeOut), Duration.ofSeconds(intervalSeconds));
        return wait.until(
                expectedCondition -> {
                    WebElement element = driver.findElement(locator);
                    if (element.isDisplayed() && element.isEnabled()) {
                        return element;
                    } else {
                        return null;
                    }
                }
        );
    }

    public WebElement waitForElementExplicit(WebDriver driver, By locator, int timeOut) {
        try {
            return LibSelenium.waitForElementExplicit(driver, locator, timeOut);
        } catch (TimeoutException e) {
            log.error("Element not found or not visible/enabled: {}", locator);
            return null;
        }
    }

    public void scrollDown(WebDriver driver, int x, int y) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("window.scrollBy(" + x + "," + y + ")");
    }

    public void selectDate(WebDriver driver, WebElement dateInputField, String date) {
        dateInputField.click();
        dateInputField.sendKeys(date);
        WebElement spanAnnee = LibSelenium.waitForElement(driver, By.xpath("//span[@class='year active']"), 10, 2);
        spanAnnee.click();
        WebElement spanMois = LibSelenium.waitForElement(driver, By.xpath("//span[@class='month active']"), 10, 2);
        spanMois.click();
        WebElement spanJour = LibSelenium.waitForElement(driver, By.xpath("//td[@class='active day']"), 10, 2);
        spanJour.click();
    }

    public static void selectDate1(WebDriver driver, By locator, String date) {
        WebElement dateInputField = driver.findElement(locator);
        dateInputField.click();
        dateInputField.sendKeys(date);
        WebElement spanAnnee = LibSelenium.waitForElement(driver, By.xpath("//span[@class='year active']"), 10, 2);
        spanAnnee.click();
        WebElement spanMois = LibSelenium.waitForElement(driver, By.xpath("//span[@class='month active']"), 10, 2);
        spanMois.click();
        WebElement spanJour = LibSelenium.waitForElement(driver, By.xpath("//td[@class='active day']"), 10, 2);
        spanJour.click();
    }

    public void clicButtonById(WebDriver driver, String locator, Actions actions) {
        WebElement webElement = waitForElement(driver, By.id(locator), 10, 1);
        actions.moveToElement(webElement).click().perform();
    }

    public void clicButtonByName(WebDriver driver, String locator, Actions actions) {
        WebElement webElement = waitForElement(driver, By.name(locator), 10, 1);
        actions.moveToElement(webElement).click().perform();
    }

    public void clicButtonByXpath(WebDriver driver, String locator, Actions actions) {
        WebElement webElement = waitForElement1(driver, By.xpath(locator), 20, 1);
        actions.moveToElement(webElement).click().perform();
    }

    public void clicButtonByAnyLocator(WebDriver driver, By locator, Actions actions) {
        WebElement webElement = waitForElement(driver, locator, 20, 2);
        actions.moveToElement(webElement).click().perform();
    }

    public void clickableButtonByXpath(WebDriver driver, String locator, Actions actions) {
        // Attente explicite pour que l'élément soit cliquable
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement webElement = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(locator)));
        // Effectuer l'action de clic
        actions.moveToElement(webElement).click().perform();
    }

    public void inputById(WebDriver driver, String locator, String data) {
        WebElement webElement = waitForElement(driver, By.id(locator), 10, 1);
        webElement.sendKeys(data);
    }

    public void inputByName(WebDriver driver, String locator, String data) {
        WebElement webElement = waitForElement1(driver, By.name(locator), 10, 1);
        webElement.sendKeys(data);
    }

    public void inputByXpath(WebDriver driver, String locator, String data) {
        WebElement webElement = waitForElement(driver, By.xpath(locator), 10, 1);
        webElement.sendKeys(data);
    }

    /**
     *
     * @param seconds
     */
    public void waitThread(int seconds) {
        try {
           Thread.sleep(seconds * 1000L);
        } catch (InterruptedException ie){
            log.error(ie.getMessage());
        }
    }

    /**
     *
     * @param driver
     * @param locator
     * @param timeOut
     * @param intervalSeconds
     */
     public boolean nextPage(WebDriver driver, By locator, int timeOut, int intervalSeconds) {
        try {
            WebElement nextPage = waitForElement1(driver, locator, timeOut, intervalSeconds);
            if (nextPage != null) {
                waitThread(2);
                nextPage.click();
                return true;
            } else {
                log.error("Next page button not found");
                return false;
            }
        } catch (Exception e) {
            log.error("An error occurred while trying to navigate to next page", e);
            return false;
        }
     }

     /**
     *
     * @param driver
     * @param locator0
     * @param locator1
     */
    public void selectByClickTwoElement(WebDriver driver, By locator0, By locator1) {
        actions = new Actions(driver);
        elementNeutre(driver);
        waitThread(2);
        WebElement element1 = waitForElement(driver, locator0, 15, 3);
        element1.click();
        waitThread(2);
        WebElement element2 = waitForElement(driver, locator1, 15, 3);
        element2.click();
    }

    public void elementNeutre(WebDriver driver) {
        // Cliquer sur un élément neutre pour supprimer le focus
        WebElement neutralElement = driver.findElement(By.tagName("body"));
        neutralElement.click();
    }

    /**
     *
     * @param driver
     * @param actions
     * @param locator0
     * @param locator1
     */
    public void selectByClickTwoElementActions(WebDriver driver, Actions actions, By locator0, By locator1) {
        WebElement element1 = waitForElement(driver, locator0, 25, 3);
        element1.click();
       // actions.moveToElement(element1).click().perform();
        waitThread(1);
        WebElement element2 = waitForElement(driver, locator1, 25, 3);
        actions.moveToElement(element2).click().perform();
        // actions.moveToElement(element2).keyUp(Keys.ENTER).perform();
    }

    /**
     *
     * @param driver
     */
    public void switchPage(WebDriver driver) {
        FluentWait<WebDriver> fluentWait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(30))
                .pollingEvery(Duration.ofMillis(500))
                .ignoring(NoSuchWindowException.class);
        // Attendre jusqu'à ce qu'il y ait plus d'une fenêtre ouverte
        fluentWait.until(driv -> driv.getWindowHandles().size() > 1);
        String currentWindowHandle = driver.getWindowHandle();
        Set<String> handles = driver.getWindowHandles();
        // Basculer vers la nouvelle fenêtre
        log.info("Current window handle: {}", currentWindowHandle);
        for (String handle : handles) {
            log.info("Handle: {}", handle);
            if (!handle.equals(currentWindowHandle)) {
                log.info("Switching to window: {}", handle);
                driver.switchTo().window(handle);
                break;
            }
        }
        log.info(driver.getTitle());
    }

    /**
     *
     * @param driver
     * @param webElement
     * @param value
     */
    public void selectElementByValue(WebDriver driver, WebElement webElement, String value) {
        js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].value = arguments[1];", webElement, value);
    }

    /**
     *
     * @param driver
     * @param element
     */
    public void clickElementJS(WebDriver driver, WebElement element) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].click();", element);
    }

    /**
     *
     * @param driver
     * @param webElement
     */
    public void clickElementByJS(WebDriver driver, WebElement webElement) {
        js = (JavascriptExecutor) driver;
        js.executeScript("arguments[0].value = arguments[1];", webElement);
    }

    /**
     *
     * @param driver
     * @param locator
     * @param typeElement
     * @param indexTr
     * @param indexTd
     * @return
     */
    public WebElement recupererElementTable(WebDriver driver, By locator, String typeElement, int indexTr, int indexTd) {
        // Localiser la table
        WebElement tableElement = waitForElement(driver, locator, 30, 1);
        // Obtenir le tbody de la table
        WebElement tbody = tableElement.findElement(By.tagName("tbody"));
        // Gérer la récupération d'un TR ou d'un TD
        if (typeElement.equalsIgnoreCase("TR")) {
            // Récupérer la ligne TR à l'index spécifié
            return tbody.findElements(By.tagName("tr")).get(indexTr);
        } else if (typeElement.equalsIgnoreCase("TD")) {
            // Récupérer la ligne TR à l'index spécifié
            WebElement ligneTR = tbody.findElements(By.tagName("tr")).get(indexTr);
            // Récupérer la cellule TD à l'index spécifié dans la ligne TR
            return ligneTR.findElements(By.tagName("td")).get(indexTd);
        } else {
            throw new IllegalArgumentException("Type d'élément non valide : " + typeElement);
        }
    }

    /**
     *
     * @param driver
     * @param tableId
     * @param rowOrCellLocator
     * @return
     */
    public WebElement getTableRowOrTableCell(WebDriver driver, String tableId, String rowOrCellLocator) {
        WebElement table = driver.findElement(By.id(tableId));
        List<WebElement> rows = table.findElements(By.xpath(".//tr"));

        for (WebElement row : rows) {
            List<WebElement> cells = row.findElements(By.xpath(".//td"));

            for (WebElement cell : cells) {
                if (cell.getText().equals(rowOrCellLocator)) {
                    return cell;
                }
            }
        }
        return null;
    }

    /**
     *
     * @param driver
     * @param idTable
     * @param typeElement
     * @param index
     * @return
     */
    public WebElement recupererElementTable1(WebDriver driver, String idTable, String typeElement, int index) {

        WebElement tableElement = driver.findElement(By.id(idTable));

        List<WebElement> lignes = tableElement.findElements(By.tagName("tr"));

        if (index < 0 || index >= lignes.size()) {
            throw new IllegalArgumentException("Index invalide : " + index);
        }
        WebElement ligne = lignes.get(index);
        if ("tr".equalsIgnoreCase(typeElement)) {
            return ligne;
        } else if ("td".equalsIgnoreCase(typeElement)) {
            List<WebElement> cellules = ligne.findElements(By.tagName("td"));
            if (index >= cellules.size()) {
                throw new IllegalArgumentException("Index invalide : " + index);
            }

            return cellules.get(index);
        } else {
            throw new IllegalArgumentException("Type d'élément non valide : " + typeElement);
        }
    }

    /**
     *
     * @param firstString
     * @param secondString
     * @return boolean true or false
     */
    public static boolean containsAllElements(String firstString, String secondString) {
        String cleanSecondString = secondString.replaceAll("[^a-zA-Z,]", "");
        String[] words = cleanSecondString.split(",");
        for (String word : words) {
            if (!Pattern.compile("\\b" + word + "\\b", Pattern.CASE_INSENSITIVE).matcher(firstString).find())
                return false;
        }
        return true;
    }

    public void clickEnter(Actions actions) {
        actions.sendKeys(Keys.TAB).perform();
    }

    public void clickKeyEnter(Actions actions) {
        actions.sendKeys(Keys.ENTER).perform();
    }

    public void select(WebDriver driver, By locator, int timeOut, int intervalSeconds, String selection) {
        WebElement dropdown = waitForElement(driver, locator, timeOut, intervalSeconds);
        Select select = new Select(dropdown);
        List<WebElement> options = select.getOptions();
        for (WebElement webElement: options){
            if (webElement.getText().equalsIgnoreCase(selection)) {
                select.selectByVisibleText(selection);
                break;
            }
        }
    }

    public boolean inputByLocator(WebDriver driver, By locator, String data) {
        try {
            WebElement webElement = waitForElement1(driver, locator, 20, 1);
            if (webElement != null) {
                webElement.sendKeys(data);
                return true;
            } else {
                log.info("Element not found: {}", locator);
                return false;
            }
        } catch (NoSuchElementException e) {
            log.info("Element not found: {}", locator);
            return false;
        }
    }

    public boolean clicButton(WebDriver driver, By locator, Actions actions) {
        try {
            WebElement webElement = waitForElement(driver, locator, 20, 2);
            actions.moveToElement(webElement).click().perform();
            return true;
        } catch (NoSuchElementException | TimeoutException e) {
            return false;
        }
    }

    protected WebElement waitForElement1(WebDriver driver, By by, int timeoutSeconds, int pollIntervalSeconds) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        WebElement element = null;
        try {
            String script = "return " + byToJavaScript(by) + ".singleNodeValue;";
            element = (WebElement) js.executeScript(script);

            if (element != null && element.isDisplayed()) {
                return element;
            }
        } catch (Exception e) {
            log.warn("Failed to locate element via JavascriptExecutor, falling back to standard Selenium method.");
        }
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            wait.pollingEvery(Duration.ofSeconds(pollIntervalSeconds));
            return wait.until(ExpectedConditions.visibilityOfElementLocated(by));
        } catch (TimeoutException e) {
            log.error("Element not found using Selenium: {}", by);
            return null;
        }
    }

    private String byToJavaScript(By by) {
        if (by instanceof By.ById) {
            return "document.getElementById('" + extractLocator(by) + "')";
        } else if (by instanceof By.ByName) {
            return "document.getElementsByName('" + extractLocator(by) + "')[0]";
        } else if (by instanceof By.ByXPath) {
            return "document.evaluate(\"" + extractLocator(by) + "\", document, null, XPathResult.FIRST_ORDERED_NODE_TYPE, null)";
        } else if (by instanceof By.ByClassName) {
            return "document.getElementsByClassName('" + extractLocator(by) + "')[0]";
        } else if (by instanceof By.ByCssSelector) {
            return "document.querySelector('" + extractLocator(by) + "')";
        } else {
            throw new UnsupportedOperationException("Type de sélecteur non supporté: " + by);
        }
    }

    private String extractLocator(By by) {
        return by.toString().replaceAll(".*: ", "").replace("By.", "");
    }

    public String convertDate(String inputDate) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
        String outputDate = "";
        try {
            Date date = inputFormat.parse(inputDate);
            outputDate = outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return outputDate;
    }

    public void scrollAndSelect(WebDriver driver, By locator, String itemToSelect) {
        WebElement listbox = waitForElement(driver, locator, 15, 1);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        List<WebElement> items = listbox.findElements(By.xpath(".//div[@role='option']"));
        int previousSize = items.size();
        Long visibleHeight = (Long) js.executeScript("return arguments[0].clientHeight;", listbox);
        boolean found = false;
        int step = visibleHeight.intValue();
        while (!found) {
            js.executeScript("arguments[0].scrollTop += " + step, listbox);
            waitThread(1);
            items = listbox.findElements(By.xpath(".//div[@role='option']"));
            int newSize = items.size();
            if (newSize == previousSize || newSize > previousSize ) {
                previousSize = newSize;
                for (WebElement item : items) {
                    if (item.getText().equalsIgnoreCase(itemToSelect) || item.getText().contains(itemToSelect)) {
                        item.click();
                        found = true;
                        break;
                    }
                }
            } else {
                break;
            }
        }

        if (!found) {
            log.info("L'élément " + itemToSelect + " n'a pas été trouvé.");
        }
    }

    public void scrollAndSelect1_(WebDriver driver, By locator, String itemToSelect, String itemsLocator) {
        WebElement listbox = waitForElement(driver, locator, 15, 1);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        List<WebElement> items = listbox.findElements(By.xpath(itemsLocator));
        js.executeScript("arguments[0].scrollTop += " + listbox);
        waitThread(1);
        items = listbox.findElements(By.xpath(itemsLocator));
        for (WebElement item : items) {
            if (item.getText().equalsIgnoreCase(itemToSelect) || item.getText().contains(itemToSelect)) {
                item.click();
                break;
            }
        }
    }

    public void scrollAndSelect1(WebDriver driver, By locator, String itemToSelect, String itemsLocator) {
        WebElement listbox = waitForElement(driver, locator, 15, 1); // Attendre que le conteneur soit visible
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Récupère la liste des éléments
        List<WebElement> items = listbox.findElements(By.xpath(itemsLocator));

        for (WebElement item : items) {
            // Scroller légèrement (ligne par ligne ou pixel par pixel)
            js.executeScript("arguments[0].scrollIntoView(true);", item);

            // Pause pour permettre au DOM de se mettre à jour
            waitThread(1);

            // Vérifier si l'élément correspond au texte cible
            if (item.getText().equalsIgnoreCase(itemToSelect) || item.getText().contains(itemToSelect)) {
                // Vérifiez si l'élément est visible et cliquable
                if (item.isDisplayed()) {
                    item.click(); // Cliquer sur l'élément trouvé
                    return; // Terminer la méthode après avoir trouvé l'élément
                }
            }
        }
        // Lancer une exception si l'élément n'est pas trouvé
        throw new NoSuchElementException("Item '" + itemToSelect + "' not found in the list.");
    }

    /**
     * Méthode pour sélectionner une option spécifique dans un dropdown avec scrolling dynamique.
     *
     * @param driver
     * @param dropdownId   L'ID du dropdown (ou du champ input associé).
     * @param optionText   Le texte de l'option à sélectionner.
     *
     */
    public void selectDropdownOptionWithScroll(WebDriver driver, String dropdownId, String optionText) {
        // 1. Localiser le dropdown (input associé)
        WebElement dropdownInput = driver.findElement(By.id(dropdownId));

        // 2. Cliquer sur le dropdown pour l'ouvrir
        dropdownInput.click();

        // 3. Attendre que la liste des options soit chargée
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement listbox = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@role='listbox']")));

        // 4. Initialiser les variables pour le scrolling
        boolean optionFound = false;
        int scrollIncrement = 50; // Valeur de défilement en pixels
        int currentScrollPosition = 0;

        while (!optionFound) {
            try {
                // Rechercher l'option cible dans la liste visible
                WebElement targetOption = listbox.findElement(By.xpath(".//span[text()='" + optionText + "']"));

                // Si l'option est trouvée, scroller jusqu'à elle et cliquer
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", targetOption);
                targetOption.click();
                optionFound = true;
            } catch (org.openqa.selenium.NoSuchElementException e) {
                // Si l'option n'est pas encore visible, faire défiler davantage
                if (currentScrollPosition < listbox.getSize().getHeight()) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollTop = arguments[1];", listbox, currentScrollPosition + scrollIncrement);
                    currentScrollPosition += scrollIncrement;
                } else {
                    // Si nous avons atteint la fin de la liste sans trouver l'option
                    throw new RuntimeException("L'option '" + optionText + "' n'a pas été trouvée dans le dropdown.");
                }
            }
        }
    }

    public void selectDropdownOptionWithScroll(WebDriver driver, String optionText) {
        // 3. Attendre que la liste des options soit chargée
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement listbox = wait.until(ExpectedConditions.presenceOfElementLocated(By.xpath("//div[@role='listbox']")));

        // 4. Initialiser les variables pour le scrolling
        boolean optionFound = false;
        int scrollIncrement = 50; // Valeur de défilement en pixels
        int currentScrollPosition = 0;

        while (!optionFound) {
            try {
                // Rechercher l'option cible dans la liste visible
                WebElement targetOption = listbox.findElement(By.xpath(".//span[text()='" + optionText + "']"));

                // Si l'option est trouvée, scroller jusqu'à elle et cliquer
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView(true);", targetOption);
                targetOption.click();
                optionFound = true;
            } catch (org.openqa.selenium.NoSuchElementException e) {
                // Si l'option n'est pas encore visible, faire défiler davantage
                if (currentScrollPosition < listbox.getSize().getHeight()) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].scrollTop = arguments[1];", listbox, currentScrollPosition + scrollIncrement);
                    currentScrollPosition += scrollIncrement;
                } else {
                    // Si nous avons atteint la fin de la liste sans trouver l'option
                    throw new RuntimeException("L'option '" + optionText + "' n'a pas été trouvée dans le dropdown.");
                }
            }
        }
    }

    public String getCotisation(String args) {
        String euroIndex = String.valueOf(args.indexOf("€"));
        String sum = args.substring(0, Integer.parseInt(euroIndex)).trim();
        sum = sum.replace("\u202F", "").trim();
        return sum;
    }

    public String getCotisation1(String args) {
        String sum = null;
        String regex = "([\\d\\s,]+) €";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(args);

        if (matcher.find()) {
            sum = matcher.group(1);
            sum = sum.replace("\u202F", "").trim();
        }
        return sum;
    }

    /**
     * Capture l'écran et retourne l'image sous forme de tableau d'octets.
     *
     * @param driver Le WebDriver utilisé pour naviguer dans la page.
     * @return Un tableau d'octets représentant l'image capturée.
     */
//    public String captureScreenshot(WebDriver driver) {
//        try {
//            if (driver instanceof TakesScreenshot) {
//                TakesScreenshot screenshotDriver = (TakesScreenshot) driver;
//                byte[] screenshotBytes = screenshotDriver.getScreenshotAs(OutputType.BYTES);
//                return Base64.getEncoder().encodeToString(screenshotBytes);
//            } else {
//                throw new UnsupportedOperationException("Le WebDriver fourni ne supporte pas la capture d'écran.");
//            }
//        } catch (Exception e) {
//            log.error("captureScreenshot Erreur_captureScreenshot : {}", e.getMessage());
//            return null;
//        }
//    }

    public String captureScreenshot(WebDriver driver, String nom, boolean isError, Tarif tarif) {
        try {
            if (driver instanceof TakesScreenshot) {
                // Capture de l'écran sous forme de tableau d'octets
                TakesScreenshot screenshotDriver = (TakesScreenshot) driver;
                byte[] screenshotBytes = screenshotDriver.getScreenshotAs(OutputType.BYTES);

                // Convertir le tableau d'octets en BufferedImage
                BufferedImage image = ImageIO.read(new ByteArrayInputStream(screenshotBytes));
                // Utiliser le tempId pour générer le nom du fichier avant la persistance
                String tempId = tarif.getTempId();
                String imagePath = tarifService.saveImage(image, nom, tempId, isError);
                if (isError)
                   tarif.setCaptureImgErreur(imagePath);
                else
                   tarif.setCaptureImgPath(imagePath);

              return imagePath;
            } else {
                throw new UnsupportedOperationException("Le WebDriver fourni ne supporte pas la capture d'écran.");
            }
        } catch (Exception e) {
            log.error("Erreur_captureScreenshot : {}", e.getMessage());
            return null;
        }
    }

    public String gestionDiffere(FluxData flux, int index, boolean inclusOuHors, String except) {
        String dureeFinale = null;
        if (index == 0) {
           if (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Pas de différé")) {
               double dureeDouble = Shared.stringToDouble(flux.getPrets().get(index).getDuree()) + Shared.stringToDouble(flux.getPrets().get(index).getDureeDiffere());
               dureeFinale = Shared.doubleToString(dureeDouble);
           }
           if (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Partiel")) {
               double dureeDouble = Shared.stringToDouble(flux.getPrets().get(index).getDuree()) + Shared.stringToDouble(flux.getPrets().get(index).getDureeDiffere());
               dureeFinale = Shared.doubleToString(dureeDouble);
           }
           if (flux.getPrets().get(index).getDiffere().equalsIgnoreCase("Total")) {
               double dureeDouble = Shared.stringToDouble(flux.getPrets().get(index).getDuree()) + Shared.stringToDouble(flux.getPrets().get(index).getDureeDiffere());
               dureeFinale = Shared.doubleToString(dureeDouble);
           }
        }
      return dureeFinale;
    }

    protected String dateEffet(int i) {
        LocalDate today = LocalDate.now();
        LocalDate nextMonth = today.plus(i, ChronoUnit.MONTHS);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return nextMonth.format(formatter);
    }

    public void recaptchaTest(WebDriver driver, String apiKey) throws Exception {
        com.fast.captcha.model.FastCaptchaResponse response = com.fast.captcha.CaptchaSolver.solve("e7f8f378-7973-4677-9c74-5f5b7aab941c",
                "6Lc0ujEUAAAAALD-jR-eOhj61K2UE8-G5N0z5BN3", "https://thedataextractors.com/");
        if(!"SUCCESS".equals(response.getStatus())) {
            log.error("Response is not successful {}", response);
            throw new RuntimeException("Captcha solution not found.");
        }
        String solution = response.getSolution();
        //enter it in text area
        WebElement captchaSolutionElement = driver.findElement(By.id("g-recaptcha-response"));
        String javaScript = "arguments[0].style.height = 'auto'; arguments[0].style.display = 'block';";
        ((JavascriptExecutor) driver).executeScript(javaScript, captchaSolutionElement);
        captchaSolutionElement.sendKeys(solution);

//        new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.frameToBeAvailableAndSwitchToIt(By.xpath("//iframe[starts-with(@name, 'a-') and starts-with(@src, 'https://www.google.com/recaptcha')]")));
//        new WebDriverWait(driver, Duration.ofSeconds(20)).until(ExpectedConditions.elementToBeClickable(By.cssSelector("div.recaptcha-checkbox-checkmark"))).click();
//        driver.switchTo().frame("a-9wt0e8vkopnm");
//        driver.findElement(By.xpath("//span[@id='recaptcha-anchor']")).click();
//        switchTo().frame($x("//iframe[starts-with(@name, 'a-') and starts-with(@src, 'https://www.google.com/recaptcha')]"));
//        $("div.rc-anchor-content").click();
//        switchTo().defaultContent();
    }

    protected static String getCaracteres(String word, int nombre, String direction) {
        if ((word == null || word.isEmpty()) && nombre <= 0) {
            return word; // Retourne la chaîne inchangée si elle est vide ou null
        }

        if (nombre <= 0) {
            return word;
        }

        // Vérifie que la direction est valide
        if (!direction.equalsIgnoreCase("gauche") && !direction.equalsIgnoreCase("droite")) {
            throw new IllegalArgumentException("La direction doit être 'gauche' ou 'droite'.");
        }

        // Supprime les caractères à gauche
        if (direction.equalsIgnoreCase("gauche")) {
            if (nombre >= word.length()) {
                return ""; // Retourne une chaîne vide si le nombre dépasse la longueur
            }
            return word.substring(nombre);
        }

        // Supprime les caractères à droite
        if (direction.equalsIgnoreCase("droite")) {
            if (nombre >= word.length()) {
                return "";
            }
            return word.substring(0, word.length() - nombre);
        }

        return word; // Par défaut, retourne la chaîne inchangée
    }

    public static String getPrix(String texte) {
        Pattern pattern = Pattern.compile("(\\d+[.,]\\d+)");
        Matcher matcher = pattern.matcher(texte);

        if (matcher.find()) {
            String prixStr = matcher.group(1).replace(",", ".");
            return prixStr;
        }
        return null;
    }

    public static String extractYear(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        // Séparation par "/"
        String[] parts = dateStr.split("/");

        // Vérifie qu'on a bien 3 parties : jour / mois / année
        if (parts.length == 3) {
            String year = parts[2];
            return year;
        }
        // Retourne null si le format est inattendu
        return null;
    }

}
