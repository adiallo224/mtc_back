package com.mtc.mutuaConseil.base;

public class BrowserFactory {

    public static BrowserAdapter create(BrowserType type, boolean headless) {
        switch(type){
            case PLAYWRIGHT_CHROME:
            case PLAYWRIGHT_FIREFOX:
                return new PlaywrightBrowserAdapter(headless, type);
            case SELENIUM_CHROME:
            case SELENIUM_FIREFOX:
            case SELENIUM_EDGE:
                return new SeleniumBrowserAdapter(headless, type);
            default:
                throw new IllegalArgumentException("Unsupported browser type: " + type);
        }
    }
}
