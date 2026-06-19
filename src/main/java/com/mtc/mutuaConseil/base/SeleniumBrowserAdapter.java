package com.mtc.mutuaConseil.base;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class SeleniumBrowserAdapter implements BrowserAdapter {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private WebDriver driver;
    private SeleniumElementLib elementLib;
    
    /**
     * Obtient le WebDriver sous-jacent
     */
    public WebDriver getDriver() {
        return driver;
    }

    public SeleniumBrowserAdapter(boolean headless, BrowserType type) {
        switch(type){
            case SELENIUM_FIREFOX:
                driver = new FirefoxDriver();
                break;
            case SELENIUM_EDGE:
                driver = new EdgeDriver();
                break;
            case SELENIUM_CHROME:
            default:
                ChromeOptions options = new ChromeOptions();
                if(headless) options.addArguments("--headless=new");
                driver = new ChromeDriver(options);
        }
        driver.manage().window().maximize();
        elementLib = new SeleniumElementLib(driver);
    }

    @Override
    public void navigate(String url) { driver.get(url); }

    @Override
    public void click(String selector) {
        try { driver.findElement(By.xpath(selector)).click(); return; } catch(Exception e){}
        driver.findElement(By.cssSelector(selector)).click();
    }

    @Override
    public void clickXpath(String xpath) { driver.findElement(By.xpath(xpath)).click(); }

    @Override
    public void type(String selector, String text) {
        try {
            WebElement e = driver.findElement(By.xpath(selector));
            e.clear();
            for(char c: text.toCharArray()){
                e.sendKeys(Character.toString(c));
                WaitUtils.humanTypeDelay();
            }
        } catch (Exception e){
            WebElement e2 = driver.findElement(By.cssSelector(selector));
            e2.clear();
            e2.sendKeys(text);
        }
    }

    @Override
    public void typeXpath(String xpath, String text) {
        WebElement e = driver.findElement(By.xpath(xpath));
        e.clear();
        for(char c: text.toCharArray()){
            e.sendKeys(Character.toString(c));
            WaitUtils.humanTypeDelay();
        }
    }

    @Override
    public void select(String selector, String value) {
        WebElement e = driver.findElement(By.xpath(selector));
        new Select(e).selectByValue(value);
    }

    @Override
    public void selectByVisibleText(String selector, String visibleText) {
        WebElement e = driver.findElement(By.xpath(selector));
        new Select(e).selectByVisibleText(visibleText);
    }

    @Override
    public boolean exists(String selector){
        List<WebElement> list = driver.findElements(By.xpath(selector));
        return !list.isEmpty();
    }

    @Override
    public boolean existsXpath(String xpath){ return !driver.findElements(By.xpath(xpath)).isEmpty(); }

    @Override
    public String getText(String selector) {
        try { return driver.findElement(By.xpath(selector)).getText(); }
        catch(Exception e) { return driver.findElement(By.cssSelector(selector)).getText(); }
    }

    @Override
    public String getTextXpath(String xpath) { return driver.findElement(By.xpath(xpath)).getText(); }

    @Override
    public void scrollBy(int pixels) {
        ((JavascriptExecutor)driver).executeScript("window.scrollBy(0, arguments[0])", pixels);
    }

    @Override
    public Map<String, Integer> getViewport() {
        Object w = ((JavascriptExecutor) driver).executeScript("return window.innerWidth");
        Object h = ((JavascriptExecutor) driver).executeScript("return window.innerHeight");
        return Map.of("width", ((Number)w).intValue(), "height", ((Number)h).intValue());
    }

    @Override
    public String takeScreenshot(String fileName, boolean fullPage) {
        try {
            File src = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
            File dest = new File("screenshots/" + fileName + ".png");
            Files.createDirectories(dest.getParentFile().toPath());
            Files.copy(src.toPath(), dest.toPath());
            return dest.getAbsolutePath();
        } catch (Exception e){
            log.error("Erreur screenshot selenium: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public byte[] takeScreenshotAsBytes(boolean fullPage) {
        try {
            if (driver instanceof TakesScreenshot) {
                return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
            } else {
                log.error("Le WebDriver ne supporte pas la capture d'écran");
                return null;
            }
        } catch (Exception e) {
            log.error("Erreur screenshot selenium (bytes): {}", e.getMessage());
            return null;
        }
    }

    @Override
    public ElementLib getElementLib() { return elementLib; }

    @Override
    public java.util.Set<String> getWindowHandles() {
        return driver.getWindowHandles();
    }

    @Override
    public String getWindowHandle() {
        return driver.getWindowHandle();
    }

    @Override
    public void switchToWindow(String handle) {
        driver.switchTo().window(handle);
    }

    @Override
    public String getTitle() {
        return driver.getTitle();
    }

    @Override
    public void waitForNewWindow(int timeoutSeconds) {
        try {
            int initialWindowCount = driver.getWindowHandles().size();
            FluentWait<WebDriver> fluentWait = new FluentWait<>(driver)
                    .withTimeout(Duration.ofSeconds(timeoutSeconds))
                    .pollingEvery(Duration.ofMillis(500))
                    .ignoring(NoSuchWindowException.class);
            
            // Attendre jusqu'à ce qu'il y ait plus de fenêtres qu'au départ
            fluentWait.until(driv -> driv.getWindowHandles().size() > initialWindowCount);
        } catch (Exception e) {
            log.error("Erreur lors de l'attente d'une nouvelle fenêtre: {}", e.getMessage());
        }
    }

    @Override
    public void close() {
        try { driver.quit(); } catch (Exception e) { log.warn("Erreur close selenium: {}", e.getMessage()); }
    }
}
