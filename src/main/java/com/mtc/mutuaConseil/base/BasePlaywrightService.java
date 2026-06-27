package com.mtc.mutuaConseil.base;

import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.ScreenshotType;
import com.microsoft.playwright.options.SelectOption;
import com.microsoft.playwright.options.WaitForSelectorState;
import com.mtc.mutuaConseil.models.Tarif;
import com.mtc.mutuaConseil.repositories.ParametreGeneralRepository;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import com.mtc.mutuaConseil.services.servicesImpl.TarifService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Paths;

import static java.util.Objects.isNull;

public abstract class BasePlaywrightService {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    protected Playwright playwright;
    protected Browser browser;
    protected Page page;
    protected PlaywrightElementLibrary elementLib;

    @Autowired
    private TarifService tarifService;

    @Autowired
    private ParametreGeneralRepository parametreGeneralRepository;

    private BrowserType getConfiguredBrowserType() {
        return parametreGeneralRepository.findAll().stream()
                .findFirst()
                .map(pg -> {
                    String nav = pg.getNavigateurPlaywright();
                    if (isNull(nav) || nav.isBlank()) return BrowserType.PLAYWRIGHT_CHROMIUM;
                    try { return BrowserType.valueOf(nav); }
                    catch (IllegalArgumentException e) { return BrowserType.PLAYWRIGHT_CHROMIUM; }
                })
                .orElse(BrowserType.PLAYWRIGHT_CHROMIUM);
    }

    protected void initializeBrowser() {
        initializeBrowser(false);
    }

    protected void initializeBrowser(boolean headless) {
        playwright = Playwright.create();
        BrowserType browserType = getConfiguredBrowserType();
        switch (browserType) {
            case PLAYWRIGHT_CHROME:
                browser = playwright.chromium().launch(new com.microsoft.playwright.BrowserType.LaunchOptions()
                        .setHeadless(headless)
                        .setChannel("chrome")
                        .setArgs(java.util.List.of("--start-maximized")));
                break;
            case PLAYWRIGHT_EDGE:
                browser = playwright.chromium().launch(new com.microsoft.playwright.BrowserType.LaunchOptions()
                        .setHeadless(headless)
                        .setChannel("msedge")
                        .setArgs(java.util.List.of("--start-maximized")));
                break;
            case PLAYWRIGHT_FIREFOX:
                browser = playwright.firefox().launch(new com.microsoft.playwright.BrowserType.LaunchOptions()
                        .setHeadless(headless));
                break;
            default:
                browser = playwright.chromium().launch(new com.microsoft.playwright.BrowserType.LaunchOptions()
                        .setHeadless(headless)
                        .setArgs(java.util.List.of("--start-maximized")));
                break;
        }
        BrowserContext context = browser.newContext(new Browser.NewContextOptions().setViewportSize(null));
        page = context.newPage();
        elementLib = new PlaywrightElementLibrary(page);
    }

    // Méthodes de navigation et attente
    protected void humanLikeNavigate(String url) {
        page.navigate(url);
        humanLikeWait();
    }

    protected void closeBrowser() {
        if (browser != null) browser.close();
        if (playwright != null) playwright.close();
    }

    protected void humanLikeWait() {
        try {
            Thread.sleep(1000 + (long)(Math.random() * 1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    protected void waitThread(int seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Attendre qu'un élément soit visible (équivalent waitForElement)
     */
    protected Locator waitForElement(String selector, int timeoutSeconds) {
        Locator locator = page.locator(selector).first();
        locator.waitFor(new Locator.WaitForOptions().setTimeout(timeoutSeconds * 1000));
        return locator;
    }

    protected Locator waitForElementByXpath(String xpath, int timeoutSeconds) {
        Locator locator = page.locator("xpath=" + xpath).first();
        locator.waitFor(new Locator.WaitForOptions().setTimeout(timeoutSeconds * 1000));
        return locator;
    }

    /**
     * Cliquer sur un bouton (équivalent clicButton)
     */
    protected void clickButton(String xpath) {
        humanLikeWait();
        Locator button = page.locator("xpath=" + xpath);
        button.hover();
        humanLikeWait();
        button.click();
    }

    protected void clickButton(String xpath, int timeoutSeconds) {
        Locator button = waitForElementByXpath(xpath, timeoutSeconds);
        button.hover();
        humanLikeWait();
        button.click();
    }

    /**
     * Remplir un champ (équivalent des méthodes infoXXX)
     */
    protected void fillField(String selector, String value) {
        if (value != null && !value.isEmpty()) {
            humanLikeWait();
            Locator field = page.locator(selector);
            field.click();
            field.clear();

            // Typage humain caractère par caractère
            for (char c : value.toCharArray()) {
                field.press(String.valueOf(c));
                try {
                    Thread.sleep(50 + (long)(Math.random() * 150));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    protected void fillFieldByXpath(String xpath, String value) {
        fillField("xpath=" + xpath, value);
    }

    /**
     * Sélectionner dans une dropdown (équivalent Select)
     */
    protected void selectOption(String selector, String visibleText) {
        humanLikeWait();
        page.selectOption(selector, new SelectOption().setLabel(visibleText));
    }

    protected void selectOptionByValue(String selector, String value) {
        humanLikeWait();
        page.selectOption(selector, new SelectOption().setValue(value));
    }

    protected void selectOptionByXpath(String xpath, String visibleText) {
        String selector = "xpath=" + xpath;
        try {
            // Attendre que l'élément soit attaché au DOM
            page.waitForSelector(selector, new Page.WaitForSelectorOptions()
                    .setState(WaitForSelectorState.ATTACHED)
                    .setTimeout(15000));
            // Sélectionner l'option
            selectOption(selector, visibleText);
        } catch (Exception e) {
            log.error("Erreur lors de la sélection par XPath '{}': {}", xpath, e.getMessage());
            throw e;
        }
    }

    /**
     * Vérifier si un élément est présent
     */
    protected boolean isElementPresent(String selector) {
        return page.locator(selector).count() > 0;
    }

    protected boolean isElementPresentByXpath(String xpath) {
        return page.locator("xpath=" + xpath).count() > 0;
    }

    /**
     * Capturer une screenshot (équivalent captureScreenshot)
     */
    protected String captureScreenshot(String fileName, boolean isError) {
        try {
            String suffix = isError ? "_error" : "_success";
            String fullFileName = fileName + suffix + "_" + System.currentTimeMillis() + ".png";
            String path = "screenshots/" + fullFileName;

            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(Paths.get(path))
                    .setFullPage(true));

            return path;
        } catch (Exception e) {
            log.error("Erreur lors de la capture d'écran", e);
            return null;
        }
    }

    public String captureScreenshot(String nom, boolean isError, Tarif tarif) {
        try {
            // Capture de l'écran sous forme de tableau d'octets
            byte[] screenshotBytes = page.screenshot(new Page.ScreenshotOptions()
                    .setFullPage(true) // ou false pour capturer seulement la zone visible
                    .setType(ScreenshotType.PNG));

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

    /**
     * Scroller (équivalent scrollDown)
     */
    protected void scrollDown(int pixels) {
        page.evaluate("window.scrollBy(0, " + pixels + ")");
        humanLikeWait();
    }

    protected void scrollToElement(String selector) {
        Locator element = page.locator(selector);
        element.scrollIntoViewIfNeeded();
        humanLikeWait();
    }

    /**
     * Gérer les fenêtres popup/alertes
     */
    protected void dismissAlerts() {
        page.onDialog(dialog -> {
            log.info("Dialog fermé: {}", dialog.message());
            dialog.dismiss();
        });
    }

    /**
     * Attendre qu'un élément disparaisse
     */
    protected void waitForElementToDisappear(String selector, int timeoutSeconds) {
        page.waitForSelector(selector,
                new Page.WaitForSelectorOptions()
                        .setState(WaitForSelectorState.HIDDEN)
                        .setTimeout(timeoutSeconds * 1000)
        );
    }

    /**
     * Obtenir le texte d'un élément (équivalent getText)
     */
    protected String getElementText(String selector) {
        Locator element = page.locator(selector).first();
        return element.textContent().trim();
    }

    protected String getElementTextByXpath(String xpath) {
        return getElementText("xpath=" + xpath);
    }

    /**
     * Vérifier si un élément est visible
     */
    protected boolean isElementVisible(String selector) {
        Locator element = page.locator(selector).first();
        return element.isVisible();
    }

    /**
     * Méthodes pour les interactions clavier
     */
    protected void pressKey(String key) {
        page.keyboard().press(key);
        humanLikeWait();
    }

    protected void pressEnter() {
        pressKey("Enter");
    }

    protected void pressTab() {
        pressKey("Tab");
    }

    /**
     * Méthodes pour les frames
     */
    protected FrameLocator switchToFrame(String selector) {
        return page.frameLocator(selector);
    }

    /**
     * Nettoyage des ressources
     */
    protected void cleanup() {
        if (browser != null) {
            browser.close();
        }
    }

    // Méthodes de comportement humain avancées
    protected void humanLikeClick(String selector) {
        humanLikeWait();
        Locator element = page.locator(selector);
        element.hover();
        humanLikeWait();
        element.click(new Locator.ClickOptions().setForce(true));
    }

    protected void humanLikeType(String selector, String text) {
        humanLikeWait();
        Locator field = page.locator(selector);
        field.click();
        field.clear();

        for (char c : text.toCharArray()) {
            field.press(String.valueOf(c));
            try {
                Thread.sleep(100 + (long)(Math.random() * 200));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Méthode pour la navigation entre les pages (équivalent nextPage)
     */
    protected void nextPage(String xpath) {
        clickButton(xpath);
        page.waitForLoadState(LoadState.NETWORKIDLE);
        humanLikeWait();
    }

    public void clickIfExists(String selector) {
        try {
            Locator element;
            // Détection automatique du type de sélecteur
            if (selector.startsWith("xpath=") || selector.startsWith("//") || selector.startsWith("(//")) {
                // XPath
                element = page.locator("xpath=" + (selector.startsWith("xpath=") ? selector.substring(6) : selector));
            } else if (selector.startsWith("#")) {
                // ID CSS
                element = page.locator(selector);
            } else if (selector.startsWith("name=")) {
                // Name
                element = page.locator("[name='" + selector.substring(5) + "']");
            } else {
                // CSS par défaut
                element = page.locator(selector);
            }

            element = element.first();

            if (element.count() > 0 && element.isVisible()) {
                element.waitFor(new Locator.WaitForOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(5000));
                element.click();
            }
        } catch (Exception e) {
            log.error("Erreur lors de la tentative de clic sur '{}': {}", selector, e.getMessage());
        }
    }

}