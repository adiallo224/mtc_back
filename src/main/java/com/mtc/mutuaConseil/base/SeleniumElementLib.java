package com.mtc.mutuaConseil.base;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class SeleniumElementLib implements ElementLib {

    private final Logger log = LoggerFactory.getLogger(SeleniumElementLib.class);

    private final WebDriver driver;

    private JavascriptExecutor js;

    public SeleniumElementLib(WebDriver driver) { this.driver = driver; }

    @Override
    public void clickByXpath(String xpath) {
        WebElement e = driver.findElement(By.xpath(xpath));
        e.click();
    }

    @Override
    public void clickByActions(String selector) {
        WebElement element = new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(findBy(selector)));

        new Actions(driver)
                .moveToElement(element)
                .click()
                .perform();
    }

    private By findBy(String selector) {

        if (selector.startsWith("//") || selector.startsWith("(//")) {
            return By.xpath(selector);
        }
        if (selector.startsWith("#") || selector.startsWith(".") || selector.contains(">")) {
            return By.cssSelector(selector);
        }
        if (selector.startsWith("id=")) {
            return By.id(selector.substring(3));
        }
        if (selector.startsWith("name=")) {
            return By.name(selector.substring(5));
        }
        if (selector.startsWith("class=")) {
            return By.className(selector.substring(6));
        }

        return By.id(selector); // fallback
    }


    @Override
    public void clickJSExecutorByXpath(String xpath) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        WebElement element = driver.findElement(By.xpath(xpath));
        js.executeScript("arguments[0].click();", element);

    }

    @Override
    public void clickById(String id) {
        WebElement e = driver.findElement(By.id(id));
        e.click();
    }

    @Override
    public void clickByName(String name) {
        WebElement e = driver.findElement(By.name(name));
        e.click();
    }

    @Override
    public void clickByClassName(String className) {
        WebElement e = driver.findElement(By.className(className));
        e.click();
    }

    @Override
    public void typeByXpath(String xpath, String text) {
        WebElement e = driver.findElement(By.xpath(xpath));
        e.clear();
        for(char c: text.toCharArray()){
            e.sendKeys(Character.toString(c));
            WaitUtils.humanTypeDelay();
        }
    }

    @Override
    public void typeById(String id, String text) {
        WebElement e = driver.findElement(By.id(id));
        e.clear();
        for(char c: text.toCharArray()){
            e.sendKeys(Character.toString(c));
            WaitUtils.humanTypeDelay();
        }
    }

    @Override
    public void typeByClassName(String className, String text) {
        WebElement e = driver.findElement(By.className(className));
        e.clear();
        for(char c: text.toCharArray()){
            e.sendKeys(Character.toString(c));
            WaitUtils.humanTypeDelay();
        }
    }

    @Override
    public void typeByName(String name, String text) {
        WebElement e = driver.findElement(By.name(name));
        e.clear();
        for(char c: text.toCharArray()){
            e.sendKeys(Character.toString(c));
            WaitUtils.humanTypeDelay();
        }
    }

    @Override
    public void click(String cssOrSelector) {
        try { driver.findElement(By.xpath(cssOrSelector)).click(); return; } catch(Exception ex){}
        driver.findElement(By.cssSelector(cssOrSelector)).click();
    }

    @Override
    public void type(String cssOrSelector, String text) {
        try {
            WebElement e = driver.findElement(By.xpath(cssOrSelector));
            e.clear();
            e.sendKeys(text);
        } catch(Exception ex) {
            WebElement e = driver.findElement(By.cssSelector(cssOrSelector));
            e.clear();
            e.sendKeys(text);
        }
    }

    @Override
    public String getTextByXpath(String xpath) {
        return driver.findElement(By.xpath(xpath)).getText();
    }

    @Override
    public boolean isVisibleXpath(String xpath) {
        try {
            return driver.findElement(By.xpath(xpath)).isDisplayed();
        } catch (Exception e) { return false; }
    }

    @Override
    public void hover(String selector) {
        // Optional: use Actions if required
    }

    @Override
    public void scrollIntoView(String selector) {
        WebElement e = driver.findElement(By.xpath(selector));
        ((org.openqa.selenium.JavascriptExecutor)driver).executeScript("arguments[0].scrollIntoView(true);", e);
    }

    @Override
    public void pressEnterOnBody() {
        try {
            WebElement body = driver.findElement(By.tagName("body"));
            body.click();
            Actions actions = new Actions(driver);
            actions.sendKeys(Keys.ENTER).perform();
        } catch (Exception e) {
            log.error("Erreur lors de l'appui sur Entrée sur le body", e);
        }
    }

    @Override
    public void pressEnter() {
        Actions actions = new Actions(driver);
        actions.sendKeys(Keys.ENTER).click().perform();
    }

    @Override
    public boolean isElementPresent(String selector) {
        try {
            By by;

            // Si c'est un XPath
            if (selector.startsWith("/") || selector.startsWith("(")) {
                by = By.xpath(selector);
            }
            // Si c'est un ID au format #myId
            else if (selector.startsWith("#")) {
                by = By.id(selector.substring(1));
            }
            // Si c'est une recherche par name : name=firstName
            else if (selector.startsWith("name=")) {
                by = By.name(selector.substring(5));
            }
            // Sinon, on considère que c'est un CSS
            else {
                by = By.cssSelector(selector);
            }

            return !driver.findElements(by).isEmpty();

        } catch (Exception e) {
            return false;
        }
    }

}
