package com.mtc.mutuaConseil.base;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.ScreenshotType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class PlaywrightBrowserAdapter implements BrowserAdapter {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;
    private PlaywrightElementLib elementLib;

    public PlaywrightBrowserAdapter(boolean headless, BrowserType browserType) {
        playwright = Playwright.create();
        switch(browserType){
            case PLAYWRIGHT_CHROME: 
                browser = playwright.chromium().launch(
                    new com.microsoft.playwright.BrowserType.LaunchOptions()
                        .setHeadless(headless)
                        .setChannel("chrome")); 
                break;
            case PLAYWRIGHT_FIREFOX: 
                browser = playwright.firefox().launch(
                    new com.microsoft.playwright.BrowserType.LaunchOptions()
                        .setHeadless(headless)); 
                break;
            default: 
                browser = playwright.chromium().launch(
                    new com.microsoft.playwright.BrowserType.LaunchOptions()
                        .setHeadless(headless)); 
                break;
        }
        context = browser.newContext(new Browser.NewContextOptions().setViewportSize(1920,1080));
        page = context.newPage();
        elementLib = new PlaywrightElementLib(page);
    }

    @Override
    public void navigate(String url) {
        page.navigate(url);
        page.waitForLoadState();
    }

    @Override
    public void click(String selector) {
        page.locator(selector).first().click();
    }

    @Override
    public void clickXpath(String xpath) {
        page.locator("xpath=" + xpath).first().click();
    }

    @Override
    public void type(String selector, String text) {
        Locator l = page.locator(selector).first();
        l.click();
        for(char c : text.toCharArray()){
            l.press(Character.toString(c));
            WaitUtils.humanTypeDelay();
        }
    }

    @Override
    public void typeXpath(String xpath, String text) {
        type("xpath=" + xpath, text);
    }

    @Override
    public void select(String selector, String value) {
        page.selectOption(selector, value);
    }

    @Override
    public void selectByVisibleText(String selector, String visibleText) {
        page.selectOption(selector, new com.microsoft.playwright.options.SelectOption().setLabel(visibleText));
    }

    @Override
    public boolean exists(String selector) {
        return page.locator(selector).count() > 0;
    }

    @Override
    public boolean existsXpath(String xpath) {
        return page.locator("xpath=" + xpath).count() > 0;
    }

    @Override
    public String getText(String selector) {
        return page.locator(selector).first().textContent();
    }

    @Override
    public String getTextXpath(String xpath) {
        return getText("xpath=" + xpath);
    }

    @Override
    public void scrollBy(int pixels) {
        page.evaluate("window.scrollBy(0, " + pixels + ")");
    }

    @Override
    public Map<String, Integer> getViewport() {
        return Map.of("width", page.viewportSize().width, "height", page.viewportSize().height);
    }

    @Override
    public String takeScreenshot(String fileName, boolean fullPage) {
        String path = "screenshots/" + fileName + ".png";
        page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get(path)).setFullPage(fullPage).setType(ScreenshotType.PNG));
        return path;
    }

    @Override
    public byte[] takeScreenshotAsBytes(boolean fullPage) {
        return page.screenshot(new Page.ScreenshotOptions()
                .setFullPage(fullPage)
                .setType(ScreenshotType.PNG));
    }

    @Override
    public ElementLib getElementLib() {
        return elementLib;
    }

    @Override
    public Set<String> getWindowHandles() {
        // Playwright utilise des pages, pas des handles comme Selenium
        // On retourne les URLs des pages comme identifiants
        return context.pages().stream()
                .map(Page::url)
                .collect(Collectors.toSet());
    }

    @Override
    public String getWindowHandle() {
        // Retourne l'URL de la page courante comme handle
        return page.url();
    }

    @Override
    public void switchToWindow(String handle) {
        // Pour Playwright, on cherche la page par son URL
        Optional<Page> targetPage = context.pages().stream()
                .filter(p -> p.url().equals(handle))
                .findFirst();
        
        if (targetPage.isPresent()) {
            page = targetPage.get();
            elementLib = new PlaywrightElementLib(page);
        } else {
            log.warn("Page avec l'URL '{}' non trouvée", handle);
        }
    }

    @Override
    public String getTitle() {
        return page.title();
    }

    @Override
    public void waitForNewWindow(int timeoutSeconds) {
        // Pour Playwright, on attend qu'une nouvelle page soit créée dans le contexte
        try {
            int initialPageCount = context.pages().size();
            long startTime = System.currentTimeMillis();
            long timeout = timeoutSeconds * 1000L;
            
            while (System.currentTimeMillis() - startTime < timeout) {
                if (context.pages().size() > initialPageCount) {
                    // Une nouvelle page a été créée, on la définit comme page courante
                    List<Page> pages = context.pages();
                    page = pages.get(pages.size() - 1); // Dernière page créée
                    elementLib = new PlaywrightElementLib(page);
                    page.waitForLoadState();
                    return;
                }
                Thread.sleep(100);
            }
            log.warn("Timeout: aucune nouvelle fenêtre n'a été ouverte dans les {} secondes", timeoutSeconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interruption lors de l'attente d'une nouvelle fenêtre", e);
        }
    }

    @Override
    public void close() {
        try {
            if(context != null) context.close();
            if(browser != null) browser.close();
            if(playwright != null) playwright.close();
        } catch (Exception e){
            log.warn("Erreur fermeture Playwright: {}", e.getMessage());
        }
    }
}
