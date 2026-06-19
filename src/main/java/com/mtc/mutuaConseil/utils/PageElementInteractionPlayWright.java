package com.mtc.mutuaConseil.utils;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Mouse;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.PlaywrightException;
import com.mtc.mutuaConseil.models.FluxData;
import com.mtc.mutuaConseil.models.Tarif;
import com.mtc.mutuaConseil.services.servicesImpl.TarifService;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@AllArgsConstructor
abstract public class PageElementInteractionPlayWright {

    private static final Logger log = LoggerFactory.getLogger(PageElementInteraction.class);
    private String namePage;

    @Autowired
    private TarifService tarifService;

    // Constructeur sans arguments pour Spring
    public PageElementInteractionPlayWright() {
    }

    protected abstract void pageName(String namePage);

    protected Locator waitForElement(Page page, String selector, int timeoutMs) {
        try {
            Locator locator = page.locator(selector).first();
            locator.waitFor(new Locator.WaitForOptions().setTimeout(timeoutMs));
            return locator;
        } catch (PlaywrightException e) {
            log.error("Element not found: {}", selector);
            return null;
        }
    }

    protected Locator waitForElement(Page page, String selector) {
        return waitForElement(page, selector, 10);
    }

    public void scrollDown(Page page, int x, int y) {
        page.evaluate("window.scrollBy(" + x + "," + y + ")");
    }

    public void selectDate(Page page, String dateInputSelector, String date) {
        Locator dateInput = waitForElement(page, dateInputSelector);
        dateInput.click();
        dateInput.fill(date);

        // Adaptez ces sélecteurs selon votre application
        page.locator("//span[@class='year active']").first().click();
        page.locator("//span[@class='month active']").first().click();
        page.locator("//td[@class='active day']").first().click();
    }

    public void clickButtonById(Page page, String id, Mouse mouse) {
        Locator element = waitForElement(page, "#" + id);
        element.hover();
        element.click();
    }

    public void clickButtonByName(Page page, String name, Mouse mouse) {
        Locator element = waitForElement(page, "[name='" + name + "']");
        element.hover();
        element.click();
    }

    public void clickButtonByXpath(Page page, String xpath, Mouse mouse) {
        Locator element = waitForElement(page, "xpath=" + xpath);
        element.hover();
        element.click();
    }

    public void clickButtonByAnyLocator(Page page, String selector, Mouse mouse) {
        Locator element = waitForElement(page, selector);
        element.hover();
        element.click();
    }

    public void inputById(Page page, String id, String data) {
        Locator element = waitForElement(page, "#" + id);
        element.fill(data);
    }

    public void inputByName(Page page, String name, String data) {
        Locator element = waitForElement(page, "[name='" + name + "']");
        element.fill(data);
    }

    public void inputByXpath(Page page, String xpath, String data) {
        Locator element = waitForElement(page, "xpath=" + xpath);
        element.fill(data);
    }

    public void waitThread(int seconds) {
//        try {
//            waitForTimeout(seconds * 1000);
//        } catch (Exception e) {
//            log.error(e.getMessage());
//        }
    }

    public boolean nextPage(Page page, String selector, int timeoutSeconds) {
        try {
            Locator nextPage = waitForElement(page, selector, timeoutSeconds);
            if (nextPage != null) {
                waitThread(2);
                nextPage.click();
                return true;
            } else {
                log.error("Next page button not found");
                return false;
            }
        } catch (Exception e) {
            log.error("An error occurred while trying to navigate to next page", e);
            return false;
        }
    }

    public void selectByClickTwoElement(Page page, String selector1, String selector2) {
        elementNeutre(page);
        waitThread(2);
        Locator element1 = waitForElement(page, selector1);
        element1.click();
        waitThread(2);
        Locator element2 = waitForElement(page, selector2);
        element2.click();
    }

    public void elementNeutre(Page page) {
        // Cliquer sur le body pour supprimer le focus
        page.locator("body").click();
    }

    public void switchPage(Page page) {
        // Playwright gère automatiquement les nouvelles pages
        // Vous pouvez accéder aux pages via le contexte
        Page newPage = page.context().waitForPage(() -> {
            // Attendre qu'une nouvelle page s'ouvre
        });

        if (newPage != null) {
            page = newPage;
            log.info("Switched to new page: {}", page.title());
        }
    }

    public void selectElementByValue(Page page, String selector, String value) {
        page.evaluate("(selector, value) => { document.querySelector(selector).value = value; }");
    }

    public void clickElementJS(Page page, String selector) {
        page.evaluate("(selector) => { document.querySelector(selector).click(); }", selector);
    }

    public Locator getTableElement(Page page, String tableSelector, String typeElement, int indexTr, int indexTd) {
        Locator table = waitForElement(page, tableSelector);

        if ("TR".equalsIgnoreCase(typeElement)) {
            return table.locator("tbody tr").nth(indexTr);
        } else if ("TD".equalsIgnoreCase(typeElement)) {
            Locator row = table.locator("tbody tr").nth(indexTr);
            return row.locator("td").nth(indexTd);
        } else {
            throw new IllegalArgumentException("Type d'élément non valide : " + typeElement);
        }
    }

    public void select(Page page, String selector, String selection) {
        Locator dropdown = waitForElement(page, selector);
        dropdown.selectOption(selection);
    }

    public boolean inputByLocator(Page page, String selector, String data) {
        try {
            Locator element = waitForElement(page, selector);
            if (element != null) {
                element.fill(data);
                return true;
            } else {
                log.info("Element not found: {}", selector);
                return false;
            }
        } catch (Exception e) {
            log.info("Element not found: {}", selector);
            return false;
        }
    }

    public boolean clickButton(Page page, String selector) {
        try {
            Locator element = waitForElement(page, selector);
            element.click();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String convertDate(String inputDate) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("dd/MM/yyyy");
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
        String outputDate = "";
        try {
            Date date = inputFormat.parse(inputDate);
            outputDate = outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return outputDate;
    }

    public void scrollAndSelect(Page page, String listboxSelector, String itemToSelect) {
        Locator listbox = waitForElement(page, listboxSelector);

        // Rechercher l'élément avec scrolling
        boolean found = false;
        int maxAttempts = 10;
        int attempts = 0;

        while (!found && attempts < maxAttempts) {
            Locator options = listbox.locator("[role='option']");

            for (int i = 0; i < options.count(); i++) {
                Locator option = options.nth(i);
                String text = option.textContent().trim();

                if (text.equalsIgnoreCase(itemToSelect) || text.contains(itemToSelect)) {
                    option.click();
                    found = true;
                    break;
                }
            }

            if (!found) {
                // Faire défiler
                page.evaluate("(selector) => { document.querySelector(selector).scrollTop += 100; }", listboxSelector);
                waitThread(1);
                attempts++;
            }
        }

        if (!found) {
            log.info("L'élément " + itemToSelect + " n'a pas été trouvé.");
        }
    }

    public String getCotisation(String args) {
        String euroIndex = String.valueOf(args.indexOf("€"));
        String sum = args.substring(0, Integer.parseInt(euroIndex)).trim();
        sum = sum.replace("\u202F", "").trim();
        return sum;
    }

    public String getCotisation1(String args) {
        String sum = null;
        String regex = "([\\d\\s,]+) €";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(args);

        if (matcher.find()) {
            sum = matcher.group(1);
            sum = sum.replace("\u202F", "").trim();
        }
        return sum;
    }

    public String captureScreenshot(Page page, String nom, boolean isError, Tarif tarif) {
        try {
            byte[] screenshotBytes = page.screenshot(new Page.ScreenshotOptions()
                    .setFullPage(true));

            BufferedImage image = ImageIO.read(new ByteArrayInputStream(screenshotBytes));
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
        // Implémentation inchangée
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

    // Les autres méthodes utilitaires restent inchangées...
    public static boolean containsAllElements(String firstString, String secondString) {
        String cleanSecondString = secondString.replaceAll("[^a-zA-Z,]", "");
        String[] words = cleanSecondString.split(",");
        for (String word : words) {
            if (!Pattern.compile("\\b" + word + "\\b", Pattern.CASE_INSENSITIVE).matcher(firstString).find())
                return false;
        }
        return true;
    }

    public static String getPrix(String texte) {
        Pattern pattern = Pattern.compile("(\\d+[.,]\\d+)");
        Matcher matcher = pattern.matcher(texte);

        if (matcher.find()) {
            String prixStr = matcher.group(1).replace(",", ".");
            return prixStr;
        }
        return null;
    }

    public static String extractYear(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        String[] parts = dateStr.split("/");
        if (parts.length == 3) {
            return parts[2];
        }
        return null;
    }
}
