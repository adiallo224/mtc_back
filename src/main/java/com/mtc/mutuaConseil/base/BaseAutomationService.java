package com.mtc.mutuaConseil.base;

import com.mtc.mutuaConseil.models.FluxData;
import com.mtc.mutuaConseil.models.Tarif;
import com.mtc.mutuaConseil.services.servicesImpl.TarifService;
import com.mtc.mutuaConseil.utils.Shared;
import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

public abstract class BaseAutomationService {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected BrowserAdapter browser;
    protected ElementLib element;
    @Autowired
    private TarifService tarifService;

    protected void init(BrowserType browserType, boolean headless) {
        this.browser = BrowserFactory.create(browserType, headless);
        this.element = browser.getElementLib();
    }
    
    /**
     * Obtient le WebDriver depuis le BrowserAdapter si c'est un SeleniumBrowserAdapter
     * Retourne null si le BrowserAdapter n'est pas un SeleniumBrowserAdapter
     */
    protected WebDriver getWebDriver() {
        if (browser instanceof SeleniumBrowserAdapter) {
            return ((SeleniumBrowserAdapter) browser).getDriver();
        }
        log.warn("Le BrowserAdapter n'est pas un SeleniumBrowserAdapter, impossible d'obtenir le WebDriver");
        return null;
    }
    
    /**
     * Obtient le JavascriptExecutor depuis le BrowserAdapter si c'est un SeleniumBrowserAdapter
     * Retourne null si le BrowserAdapter n'est pas un SeleniumBrowserAdapter
     */
    protected JavascriptExecutor getJavascriptExecutor() {
        WebDriver driver = getWebDriver();
        if (driver instanceof JavascriptExecutor) {
            return (JavascriptExecutor) driver;
        }
        log.warn("Le WebDriver n'implémente pas JavascriptExecutor");
        return null;
    }

    protected void navigate(String url) { browser.navigate(url); }

    protected void humanNavigate(String url) {
        browser.navigate(url);
        WaitUtils.humanDelay();
    }

    protected void click(String xpath) { element.clickByXpath(xpath); }

    protected void type(String xpath, String text) { element.typeByXpath(xpath, text); }

    protected String getText(String xpath) { return element.getTextByXpath(xpath); }

    protected void cleanup() {
        if (browser != null) {
            browser.close();
        }
    }

    public void scrollDown(int x, int y) {
        if (browser == null) {
            log.error("BrowserAdapter n'est pas initialisé");
            return;
        }
        browser.scrollBy(y);
    }

    public void selectDate(By dateInputLocator, String date) {
        if (browser == null) {
            log.error("BrowserAdapter n'est pas initialisé");
            return;
        }
        String dateInputSelector = byToString(dateInputLocator);
        browser.clickXpath(dateInputSelector);
        browser.typeXpath(dateInputSelector, date);
        waitThread(1);
        browser.clickXpath("//span[@class='year active']");
        waitThread(1);
        browser.clickXpath("//span[@class='month active']");
        waitThread(1);
        browser.clickXpath("//td[@class='active day']");
    }

    public void waitThread(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException ie){
            log.error(ie.getMessage());
        }
    }

    public boolean nextPage(By locator, int timeOut, int intervalSeconds) {
        if (browser == null) {
            log.error("BrowserAdapter n'est pas initialisé");
            return false;
        }
        try {
            String selector = byToString(locator);
            waitForElementExists(selector, timeOut, intervalSeconds);
            
            if (browser.existsXpath(selector) || browser.exists(selector)) {
                waitThread(2);
                browser.clickXpath(selector);
                return true;
            } else {
                log.error("Next page button not found: {}", selector);
                return false;
            }
        } catch (Exception e) {
            log.error("An error occurred while trying to navigate to next page", e);
            return false;
        }
    }

    public void elementNeutre() {
        if (browser == null) {
            log.error("BrowserAdapter n'est pas initialisé");
            return;
        }
        browser.clickXpath("//body");
    }

    public void switchPage() {
        if (browser == null) {
            log.error("BrowserAdapter n'est pas initialisé");
            return;
        }
        
        try {
            // Attendre qu'une nouvelle fenêtre soit ouverte
            browser.waitForNewWindow(7);
            
            // Obtenir les handles de toutes les fenêtres
            Set<String> handles = browser.getWindowHandles();
            String currentWindowHandle = browser.getWindowHandle();
            
            // Basculer vers la nouvelle fenêtre
            log.info("Current window handle: {}", currentWindowHandle);
            for (String handle : handles) {
                log.info("Handle: {}", handle);
                if (!handle.equals(currentWindowHandle)) {
                    log.info("Switching to window: {}", handle);
                    browser.switchToWindow(handle);
                    break;
                }
            }
            log.info("Page title: {}", browser.getTitle());
        } catch (Exception e) {
            log.error("Erreur lors du basculement de fenêtre: {}", e.getMessage());
        }
    }

    public void select(By locator, int timeOut, int intervalSeconds, String selection) {
        if (browser == null) {
            log.error("BrowserAdapter n'est pas initialisé");
            return;
        }
        // Convertir By en String (xpath)
        String selector = byToString(locator);
        
        // Attendre que l'élément soit présent
        waitForElementExists(selector, timeOut, intervalSeconds);
        
        // Sélectionner par texte visible
        browser.selectByVisibleText(selector, selection);
    }

    private String byToString(By by) {
        String byStr = by.toString();
        if (byStr.startsWith("By.xpath:")) {
            return byStr.replace("By.xpath: ", "");
        } else if (byStr.startsWith("By.id:")) {
            String id = byStr.replace("By.id: ", "");
            return "//*[@id='" + id + "']";
        } else if (byStr.startsWith("By.name:")) {
            String name = byStr.replace("By.name: ", "");
            return "//*[@name='" + name + "']";
        } else if (byStr.startsWith("By.className:")) {
            String className = byStr.replace("By.className: ", "");
            return "//*[@class='" + className + "']";
        } else if (byStr.startsWith("By.cssSelector:")) {
            return byStr.replace("By.cssSelector: ", "");
        }
        // Par défaut, retourner tel quel
        return byStr;
    }

    private boolean waitForElementExists(String selector, int timeoutSeconds, int pollIntervalSeconds) {
        if (browser == null) {
            log.error("BrowserAdapter n'est pas initialisé");
            return false;
        }
        
        long startTime = System.currentTimeMillis();
        long timeout = timeoutSeconds * 1000L;
        long pollInterval = pollIntervalSeconds * 1000L;
        
        while (System.currentTimeMillis() - startTime < timeout) {
            if (browser.existsXpath(selector) || browser.exists(selector)) {
                return true;
            }
            try {
                Thread.sleep(pollInterval);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Interruption lors de l'attente de l'élément: {}", selector);
                return false;
            }
        }
        log.warn("Timeout: l'élément '{}' n'a pas été trouvé dans les {} secondes", selector, timeoutSeconds);
        return false;
    }

    public String captureScreenshot(String nom, boolean isError, Tarif tarif) {
        try {
            if (browser == null) {
                log.error("BrowserAdapter n'est pas initialisé");
                return null;
            }

            // Capture de l'écran sous forme de tableau d'octets via BrowserAdapter
            byte[] screenshotBytes = browser.takeScreenshotAsBytes(true);
            
            if (screenshotBytes == null || screenshotBytes.length == 0) {
                log.error("La capture d'écran a retourné des données vides");
                return null;
            }

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

    public void recaptchaTest(String apiKey) throws Exception {
        if (browser == null) {
            log.error("BrowserAdapter n'est pas initialisé");
            return;
        }
        com.fast.captcha.model.FastCaptchaResponse response = com.fast.captcha.CaptchaSolver.solve("e7f8f378-7973-4677-9c74-5f5b7aab941c",
                "6Lc0ujEUAAAAALD-jR-eOhj61K2UE8-G5N0z5BN3", "https://thedataextractors.com/");
        if(!"SUCCESS".equals(response.getStatus())) {
            log.error("Response is not successful {}", response);
            throw new RuntimeException("Captcha solution not found.");
        }
        String solution = response.getSolution();
        // Utiliser ElementLib pour interagir avec le captcha
        // Note: Cette méthode nécessite un accès JavaScript, ce qui peut nécessiter une implémentation spécifique
        // Pour l'instant, on utilise les méthodes de base
        browser.typeXpath("//textarea[@id='g-recaptcha-response']", solution);
    }

    /**
     * Version avec BrowserAdapter - retourne un boolean
     */
    protected boolean waitForElement(By by, int timeoutSeconds, int pollIntervalSeconds) {
        if (browser == null) {
            log.error("BrowserAdapter n'est pas initialisé");
            return false;
        }
        
        String selector = byToString(by);
        return waitForElementExists(selector, timeoutSeconds, pollIntervalSeconds);
    }
    
    /**
     * Version avec WebDriver pour compatibilité avec les classes existantes - retourne un WebElement
     */
    protected WebElement waitForElement(WebDriver driver, By by, int timeoutSeconds, int pollIntervalSeconds) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            wait.pollingEvery(Duration.ofSeconds(pollIntervalSeconds));
            return wait.until(ExpectedConditions.visibilityOfElementLocated(by));
        } catch (org.openqa.selenium.TimeoutException e) {
            log.error("Element not found: {}", by);
            return null;
        }
    }
    
    /**
     * Version avec BrowserAdapter - retourne un boolean
     */
    protected boolean waitForElement1(By by, int timeoutSeconds, int pollIntervalSeconds) {
        // Même implémentation que waitForElement pour l'instant
        return waitForElement(by, timeoutSeconds, pollIntervalSeconds);
    }
    
    /**
     * Version avec WebDriver pour compatibilité avec les classes existantes - retourne un WebElement
     */
    protected WebElement waitForElement1(WebDriver driver, By by, int timeoutSeconds, int pollIntervalSeconds) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeoutSeconds));
            wait.pollingEvery(Duration.ofSeconds(pollIntervalSeconds));
            return wait.until(ExpectedConditions.visibilityOfElementLocated(by));
        } catch (org.openqa.selenium.TimeoutException e) {
            log.error("Element not found using Selenium: {}", by);
            return null;
        }
    }

    public List<WebElement> getElements(String selector) {
        By by;

        // commence par / ou (
        if (selector.startsWith("/") || selector.startsWith("(")) {
            by = By.xpath(selector);
        }
        // commence par #
        else if (selector.startsWith("#")) {
            by = By.id(selector.substring(1));
        }
        // format name=email
        else if (selector.startsWith("name=")) {
            by = By.name(selector.substring(5));
        }
        // Sinon → CSS selector
        else {
            by = By.cssSelector(selector);
        }
        return getWebDriver().findElements(by);
    }

}
