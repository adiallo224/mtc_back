package com.mtc.mutuaConseil.utils;

import org.openqa.selenium.*;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.*;

import java.time.Duration;

public class LibSelenium {

    public static WebElement waitForElement(WebDriver driver, By locator, int timeOut, int pollingIntervalSeconds) {
        Wait<WebDriver> wait = new FluentWait<>(driver)
                .withTimeout(Duration.ofSeconds(timeOut))
                .pollingEvery(Duration.ofSeconds(pollingIntervalSeconds))
                .ignoring(NoSuchElementException.class);
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    public static WebElement waitForElement(WebDriver driver, By locator, int timeOut) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeOut));
        return wait.until(ExpectedConditions.elementToBeClickable(locator));
    }

    public static WebElement waitForElementExplicit(WebDriver driver, By locator, int timeOut) {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(timeOut));
        return wait.until(
                expectedCondition -> {
                    WebElement element = driver.findElement(locator);
                    if (element.isDisplayed() || element.isEnabled()) {
                        return element;
                    } else {
                        return null;
                    }
                }
        );
    }

    public static void selectDate(WebDriver driver, WebElement dateInputField, String date) {
        dateInputField.click();
        dateInputField.sendKeys(date);
        WebElement spanAnnee = LibSelenium.waitForElement(driver, By.xpath("//span[@class='year active']"), 10, 2);
        spanAnnee.click();
        WebElement spanMois = LibSelenium.waitForElement(driver, By.xpath("//span[@class='month active']"), 10, 2);
        spanMois.click();
        WebElement spanJour = LibSelenium.waitForElement(driver, By.xpath("//td[@class='active day']"), 10, 2);
        spanJour.click();
    }

    public static void clicButtonByName(WebDriver driver, String locator, Actions actions) {
        WebElement webElement = LibSelenium.waitForElement(driver, By.name(locator), 10, 1);
        actions.moveToElement(webElement).click().perform();
    }

    public static void clicButtonByXpath(WebDriver driver, String locator, Actions actions) {
        WebElement webElement = LibSelenium.waitForElement(driver, By.xpath(locator), 20, 2);
        actions.moveToElement(webElement).click().perform();
    }

}
