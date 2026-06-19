package com.mtc.mutuaConseil.utils;

import lombok.Getter;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SeleniumDriver {

    private final Logger log = LoggerFactory.getLogger(SeleniumDriver.class);
    private static WebDriver driver;

    public SeleniumDriver(String browser) {
        switch (browser.toLowerCase()) {
            case "chrome":
                driver = new ChromeDriver();
                break;
            case "firefox":
                driver = new FirefoxDriver();
                break;
            case "ie":
            case "internetexplorer":
                driver = new InternetExplorerDriver();
                break;
            default:
                throw new IllegalArgumentException("Le navigateur spécifié n'est pas pris en charge.");
        }
    }

    public static WebDriver getDriver(String browser) {
        if (driver == null) {
            new SeleniumDriver(browser);
        }
        return driver;
    }

}


