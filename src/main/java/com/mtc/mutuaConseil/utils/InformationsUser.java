package com.mtc.mutuaConseil.utils;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InformationsUser {

    private static final Logger log = LoggerFactory.getLogger(InformationsUser.class);

    /**
     *
     * @param driver
     * @param locator
     * @param timeOut
     * @param intervalSeconds
     * @param login
     */
     public static boolean infoLogin(WebDriver driver, By locator, int timeOut, int intervalSeconds, String login) {
        if (login == null) {
            log.error("Login is null");
            return false;
        }

        try {
            WebElement inputLogin = LibSelenium.waitForElement(driver, locator, timeOut, intervalSeconds);
            if (inputLogin != null) {
                inputLogin.sendKeys(login);
                return true;
            } else {
                log.error("Login input element not found");
                return false;
            }
        } catch (Exception e) {
            log.error("An error occurred while trying to enter login", e);
            return false;
        }
     }


    /**
     *
     * @param driver
     * @param locator
     * @param timeOut
     * @param intervalSeconds
     * @param password
     */
     public static boolean infoPassword(WebDriver driver, By locator, int timeOut, int intervalSeconds, String password) {
        if (password == null) {
            log.error("Password is null");
            return false;
        }

        try {
            WebElement inputPassword = LibSelenium.waitForElement(driver, locator, timeOut, intervalSeconds);
            if (inputPassword != null) {
                inputPassword.sendKeys(password);
                return true;
            } else {
                log.error("Password input element not found");
                return false;
            }
        } catch (Exception e) {
            log.error("An error occurred while trying to enter login", e);
            return false;
        }
     }

}
