package com.mtc.mutuaConseil.base;

import java.util.Map;
import java.util.Set;

public interface BrowserAdapter {
    void navigate(String url);
    void click(String selector);
    void clickXpath(String xpath);
    void type(String selector, String text);
    void typeXpath(String xpath, String text);
    void select(String selector, String value);
    void selectByVisibleText(String selector, String visibleText);
    boolean exists(String selector);
    boolean existsXpath(String xpath);
    String getText(String selector);
    String getTextXpath(String xpath);
    void scrollBy(int pixels);
    Map<String, Integer> getViewport();
    String takeScreenshot(String fileName, boolean fullPage);
    byte[] takeScreenshotAsBytes(boolean fullPage);
    ElementLib getElementLib();
    void close();
    
    // Méthodes pour la gestion des fenêtres/onglets
    Set<String> getWindowHandles();
    String getWindowHandle();
    void switchToWindow(String handle);
    String getTitle();
    void waitForNewWindow(int timeoutSeconds);
}
