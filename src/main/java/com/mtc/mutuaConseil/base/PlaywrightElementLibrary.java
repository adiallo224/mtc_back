package com.mtc.mutuaConseil.base;


import com.microsoft.playwright.Dialog;
import com.microsoft.playwright.FrameLocator;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.MouseButton;
import com.microsoft.playwright.options.SelectOption;
import com.microsoft.playwright.options.WaitForSelectorState;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Getter
public class PlaywrightElementLibrary {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightElementLibrary.class);

    protected Page page;

    public PlaywrightElementLibrary(Page page) {
        this.page = page;
    }

    // ==================== MÉTHODES D'ATTENTE ====================

    /**
     * Attendre qu'un élément soit visible par sélecteur CSS
     */
//    public Locator waitForElement(String selector, int timeoutSeconds) {
//        try {
//            Locator locator;
//            // Détection automatique du type de sélecteur
//            if (selector.startsWith("xpath=") || selector.startsWith("//") || selector.startsWith("(//")) {
//                // XPath
//                locator = page.locator("xpath=" + (selector.startsWith("xpath=") ? selector.substring(6) : selector));
//            } else if (selector.startsWith("#")) {
//                // ID CSS
//                locator = page.locator(selector);
//            } else if (selector.startsWith("name=")) {
//                // Name
//                locator = page.locator("[name='" + selector.substring(5) + "']");
//            } else {
//                // Par défaut CSS selector
//                locator = page.locator(selector);
//            }
//            locator = locator.first(); // si plusieurs correspondances
//            locator.waitFor(new Locator.WaitForOptions()
//                    .setTimeout(timeoutSeconds * 1000)
//                    .setState(WaitForSelectorState.VISIBLE));
//
//            return locator;
//        } catch (Exception e) {
//            log.error("Élément non trouvé avec le sélecteur: {}", selector, e);
//            throw e;
//        }
//    }

    public Locator waitForElement(String selector, int timeoutSeconds) {
        try {
            Locator locator;

            // Détection XPath
            if (selector.startsWith("//")
                    || selector.startsWith("(//")
                    || selector.startsWith("xpath=")) {

                String xpath = selector.startsWith("xpath=")
                        ? selector.substring(6)
                        : selector;

                locator = page.locator("xpath=" + xpath);

            } else {
                // Tous les autres cas => CSS selector
                locator = page.locator(selector);
            }

            locator.first().waitFor(
                    new Locator.WaitForOptions()
                            .setTimeout(timeoutSeconds * 1000)
                            .setState(WaitForSelectorState.VISIBLE)
            );

            return locator.first();

        } catch (Exception e) {
            log.error("Élément non trouvé avec le sélecteur : {}", selector, e);
            throw new RuntimeException(
                    "Impossible de trouver l'élément : " + selector, e);
        }
    }


    /**
     * Attendre un élément par XPath
     */
    public Locator waitForElementByXpath(String xpath, int timeoutSeconds) {
        return waitForElement("xpath=" + xpath, timeoutSeconds);
    }

    /**
     * Attendre un élément par ID
     */
    public Locator waitForElementById(String id, int timeoutSeconds) {
        return waitForElement("#" + id, timeoutSeconds);
    }

    /**
     * Attendre un élément par classe
     */
    public Locator waitForElementByClass(String className, int timeoutSeconds) {
        return waitForElement("." + className, timeoutSeconds);
    }

    /**
     * Attendre qu'un élément contienne un texte spécifique
     */
    public Locator waitForElementWithText(String selector, String text, int timeoutSeconds) {
        return waitForElement(selector + ":has-text('" + text + "')", timeoutSeconds);
    }

    /**
     * Attendre qu'un élément soit caché
     */
    public void waitForElementHidden(String selector, int timeoutSeconds) {
        page.waitForSelector(selector,
                new Page.WaitForSelectorOptions()
                        .setState(WaitForSelectorState.HIDDEN)
                        .setTimeout(timeoutSeconds * 1000));
    }

    // ==================== MÉTHODES DE CLIC ====================

    /**
     * Cliquer sur un élément par sélecteur CSS
     */
    public void click(String selector) {
        click(selector, 10);
    }

    public void click(String selector, int timeoutSeconds) {
        try {
            Locator element = waitForElement(selector, timeoutSeconds);
            element.click();
        } catch (Exception e) {
            log.error("Impossible de cliquer sur l'élément: {}", selector);
            throw e;
        }
    }

    public void waitThread(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Appuye sur Entrée sur l'élément actuellement focusé
     */
    public void pressEnter() {
        try {
            page.keyboard().press("Enter");
            log.info("Touche Entrée pressée sur l'élément focusé");
        } catch (Exception e) {
            log.error("Erreur lors de l'appui sur Entrée", e);
            throw new RuntimeException("Échec de l'appui sur Entrée", e);
        }
    }

    /**
     * Appuye sur Entrée sur le body (utile pour désélectionner)
     */
    public void pressEnterOnBody() {
        try {
            page.click("body"); // Clique d'abord sur le body
            page.keyboard().press("Enter");
            log.info("Entrée pressée sur le body");
        } catch (Exception e) {
            log.error("Erreur lors de l'appui sur Entrée sur le body", e);
            throw new RuntimeException("Échec de l'appui sur Entrée sur le body", e);
        }
    }

    /**
     * Cliquer par XPath
     */
    public void clickByXpath(String xpath) {
        click("xpath=" + xpath);
    }

    /**
     * Cliquer par ID
     */
    public void clickById(String id) {
        click("#" + id);
    }

    /**
     * Cliquer par texte visible
     */
    public void clickByText(String text) {
        click("text=" + text);
    }

    /**
     * Double-clic sur un élément
     */
    public void doubleClick(String selector) {
        Locator element = waitForElement(selector, 10);
        element.dblclick();
    }

    /**
     * Clic droit sur un élément
     */
    public void rightClick(String selector) {
        Locator element = waitForElement(selector, 10);
        element.click(new Locator.ClickOptions().setButton(MouseButton.RIGHT));
    }

    /**
     * Cliquer sur un élément spécifique dans une liste
     */
    public void clickNthElement(String selector, int index) {
        Locator element = page.locator(selector).nth(index);
        element.click();
    }

    // ==================== MÉTHODES DE SAISIE ====================

    /**
     * Saisir du texte dans un champ
     */
    public void type(String selector, String text) {
        type(selector, text, 10);
    }

    public void type(String selector, String text, int timeoutSeconds) {
        try {
            Locator field = waitForElement(selector, timeoutSeconds);
            field.clear();
            field.fill(text);
        } catch (Exception e) {
            log.error("Impossible de saisir dans l'élément: {}", selector);
            throw e;
        }
    }

    /**
     * Saisir par ID
     */
    public void typeById(String id, String text) {
        type("#" + id, text);
    }

    public void humanTypeById(String id, String text) {
        typeHumanLike("#" + id, text);
    }

    public void humanTypeByXpath(String xpath, String text) {
        typeHumanLike("xpath=" + xpath, text);
    }

    public void humanTypeByName(String name, String text) {
        typeHumanLike("name" + name, text);
    }

    /**
     * Saisir par XPath
     */
    public void typeByXpath(String xpath, String text) {
        type("xpath=" + xpath, text);
    }

    /**
     * Saisie humaine (caractère par caractère)
     */
    public void typeHumanLike(String selector, String text) {
        Locator field = waitForElement(selector, 10);
        field.click();
        field.clear();

        for (char c : text.toCharArray()) {
            field.press(String.valueOf(c));
            try {
                Thread.sleep(50 + (long)(Math.random() * 100));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Effacer le contenu d'un champ
     */
    public void clear(String selector) {
        Locator field = waitForElement(selector, 10);
        field.clear();
    }

    // ==================== MÉTHODES POUR SELECT/DROPDOWN ====================

    /**
     * Sélectionner par texte visible
     */
    public void selectByVisibleText(String selector, String visibleText) {
        page.selectOption(selector, new SelectOption().setLabel(visibleText));
        log.debug("Sélection de l'option '{}' dans: {}", visibleText, selector);
    }

    /**
     * Sélectionner par valeur
     */
    public void selectByValue(String selector, String value) {
        page.selectOption(selector, new SelectOption().setValue(value));
    }

    /**
     * Sélectionner par index
     */
    public void selectByIndex(String selector, int index) {
        page.selectOption(selector, new SelectOption().setIndex(index));
    }

    /**
     * Obtenir toutes les options d'un select
     */
    public List<String> getSelectOptions(String selector) {
        List<String> options = new ArrayList<>();
        Locator optionElements = page.locator(selector + " option");

        for (int i = 0; i < optionElements.count(); i++) {
            options.add(optionElements.nth(i).textContent().trim());
        }

        return options;
    }

    /**
     * Obtenir l'option sélectionnée
     */
    public String getSelectedOption(String selector) {
        return page.locator(selector + " option:checked").first().textContent().trim();
    }

    // ==================== MÉTHODES POUR LES TABLEAUX ====================

    /**
     * Obtenir toutes les lignes d'un tableau
     */
    public List<Locator> getTableRows(String tableSelector) {
        return page.locator(tableSelector + " tr").all();
    }

    /**
     * Obtenir les cellules d'une ligne spécifique
     */
    public List<String> getRowCells(String tableSelector, int rowIndex) {
        List<String> cells = new ArrayList<>();
        Locator rowCells = page.locator(tableSelector + " tr:nth-child(" + (rowIndex + 1) + ") td");

        for (int i = 0; i < rowCells.count(); i++) {
            cells.add(rowCells.nth(i).textContent().trim());
        }

        return cells;
    }

    /**
     * Trouver une ligne par texte
     */
    public Locator findTableRowByText(String tableSelector, String searchText) {
        return page.locator(tableSelector + " tr:has-text('" + searchText + "')").first();
    }

    /**
     * Obtenir la valeur d'une cellule spécifique
     */
    public String getTableCellValue(String tableSelector, int row, int column) {
        Locator cell = page.locator(tableSelector + " tr:nth-child(" + row + ") td:nth-child(" + column + ")");
        return cell.textContent().trim();
    }

    /**
     * Cliquer sur une cellule spécifique
     */
    public void clickTableCell(String tableSelector, int row, int column) {
        Locator cell = page.locator(tableSelector + " tr:nth-child(" + row + ") td:nth-child(" + column + ")");
        cell.click();
    }

    /**
     * Compter le nombre de lignes
     */
    public int getRowCount(String tableSelector) {
        return page.locator(tableSelector + " tr").count();
    }

    /**
     * Compter le nombre de colonnes
     */
    public int getColumnCount(String tableSelector) {
        return page.locator(tableSelector + " tr:first-child td").count();
    }

    // ==================== MÉTHODES DE VÉRIFICATION ====================

    /**
     * Vérifier si un élément est présent
     */
    public boolean isElementPresent(String selector) {
        return page.locator(selector).count() > 0;
    }

    /**
     * Vérifier si un élément est visible
     */
    public boolean isElementVisible(String selector) {
        try {
            return page.locator(selector).first().isVisible();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Vérifier si un élément est activé
     */
    public boolean isElementEnabled(String selector) {
        try {
            return page.locator(selector).first().isEnabled();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Vérifier si une checkbox est cochée
     */
    public boolean isCheckboxChecked(String selector) {
        return page.locator(selector).isChecked();
    }

    /**
     * Vérifier le texte d'un élément
     */
    public boolean isTextPresent(String selector, String expectedText) {
        try {
            String actualText = getText(selector);
            return actualText.contains(expectedText);
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== MÉTHODES PAR TITLE ====================

    public Locator getByTitle(String title) {
        return page.getByTitle(title).first();
    }

    public Locator getByTitleExact(String title) {
        return page.getByTitle(title, new com.microsoft.playwright.Page.GetByTitleOptions().setExact(true)).first();
    }

    public void clickByTitle(String title) {
        getByTitle(title).click();
    }

    public String getTextByTitle(String title) {
        try {
            return getByTitle(title).textContent().trim();
        } catch (Exception e) {
            log.error("Impossible de récupérer le texte de l'élément avec title: {}", title);
            return "";
        }
    }

    // ==================== MÉTHODES D'EXTRACTION ====================

    /**
     * Obtenir le texte d'un élément
     */
    public String getText(String selector) {
        try {
            return page.locator(selector).first().textContent().trim();
        } catch (Exception e) {
            log.error("Impossible de récupérer le texte de l'élément: {}", selector);
            return "";
        }
    }

    /**
     * Obtenir la valeur d'un attribut
     */
    public String getAttribute(String selector, String attributeName) {
        return page.locator(selector).first().getAttribute(attributeName);
    }

    /**
     * Obtenir la valeur d'un champ
     */
    public String getValue(String selector) {
        return getAttribute(selector, "value");
    }

    /**
     * Obtenir le HTML interne
     */
    public String getInnerHTML(String selector) {
        return page.locator(selector).first().innerHTML();
    }

    /**
     * Obtenir le HTML externe
     */
    public String getOuterHTML(String selector) {
        return page.locator(selector).first().innerHTML();
    }

    // ==================== MÉTHODES POUR CHECKBOX/RADIO ====================

    /**
     * Cocher une checkbox
     */
    public void checkCheckbox(String selector) {
        if (!isCheckboxChecked(selector)) {
            click(selector);
        }
    }

    /**
     * Décocher une checkbox
     */
    public void uncheckCheckbox(String selector) {
        if (isCheckboxChecked(selector)) {
            click(selector);
        }
    }

    /**
     * Sélectionner un radio button
     */
    public void selectRadioButton(String selector) {
        click(selector);
    }

    // ==================== MÉTHODES POUR LES FRAMES ====================

    /**
     * Basculer vers un frame
     */
    public FrameLocator switchToFrame(String selector) {
        return page.frameLocator(selector);
    }

    /**
     * Revenir au contenu principal
     */
    public void switchToMainContent() {
        // Playwright revient automatiquement au contenu principal
    }

    // ==================== MÉTHODES POUR LES FENÊTRES ====================
    /**
     * Bascule vers la dernière fenêtre (page) ouverte,
     * même si elle a été ouverte juste avant l'appel.
     */
    public void switchToNewWindow() {
        try {
            var context = page.context();
            List<Page> allPages = context.pages();

            // Si plusieurs pages sont ouvertes, on prend la dernière
            if (allPages.size() > 1) {
                Page newPage = allPages.get(allPages.size() - 1);
                newPage.waitForLoadState(LoadState.DOMCONTENTLOADED);
                this.page = newPage;
                log.info("✅ Basculé vers la nouvelle fenêtre existante : {}", newPage.url());
            } else {
                // Si aucune nouvelle page détectée, on attend un court instant
                log.warn("⚠️ Aucune nouvelle fenêtre détectée, attente courte...");
                Page newPage = context.waitForPage(() -> {});
                newPage.waitForLoadState(LoadState.DOMCONTENTLOADED);
                this.page = newPage;
                log.info("✅ Nouvelle fenêtre détectée après attente : {}", newPage.url());
            }
        } catch (Exception e) {
            log.error("Erreur lors du basculement vers la nouvelle fenêtre", e);
            throw new RuntimeException("Impossible de basculer vers la nouvelle fenêtre", e);
        }
    }



    /**
     * Clique sur un sélecteur qui ouvre une nouvelle page, puis bascule dessus
     */
    public void clickAndSwitchToNewWindow(String selector) {
        var context = page.context();

        Page newPage = context.waitForPage(() -> {
            click(selector);
        });

        newPage.waitForLoadState(LoadState.DOMCONTENTLOADED);
        this.page = newPage;
        log.info("Basculé sur la nouvelle fenêtre ouverte par: {}", selector);
    }


    /**
     * Fermer la fenêtre courante
     */
    public void closeCurrentWindow() {
        page.close();
    }

    // ==================== MÉTHODES POUR LES ALERTES ====================

    /**
     * Accepter une alerte
     */
    public void acceptAlert() {
        page.onDialog(Dialog::accept);
    }

    /**
     * Refuser une alerte
     */
    public void dismissAlert() {
        page.onDialog(Dialog::dismiss);
    }

    /**
     * Obtenir le texte d'une alerte
     */
    public String getAlertText() {
        final String[] alertText = new String[1];
        page.onDialog(dialog -> alertText[0] = dialog.message());
        return alertText[0];
    }

    // ==================== MÉTHODES DE NAVIGATION ====================

    /**
     * Recharger la page
     */
    public void refreshPage() {
        page.reload();
    }

    /**
     * Revenir en arrière
     */
    public void goBack() {
        page.goBack();
    }

    /**
     * Aller en avant
     */
    public void goForward() {
        page.goForward();
    }

    /**
     * Obtenir l'URL courante
     */
    public String getCurrentUrl() {
        return page.url();
    }

    /**
     * Obtenir le titre de la page
     */
    public String getPageTitle() {
        return page.title();
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    /**
     * Exécuter du JavaScript
     */
    public Object executeJavaScript(String script) {
        return page.evaluate(script);
    }

    /**
     * Capturer une screenshot
     */
    public String takeScreenshot(String fileName) {
        try {
            String path = "screenshots/" + fileName + "_" + System.currentTimeMillis() + ".png";
            page.screenshot(new Page.ScreenshotOptions().setPath(Paths.get(path)));
            return path;
        } catch (Exception e) {
            log.error("Erreur lors de la capture d'écran", e);
            return null;
        }
    }

    /**
     * Attendre un temps fixe
     */
    public void wait(int seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Attendre de manière aléatoire (comportement humain)
     */
    public void waitRandom(int minSeconds, int maxSeconds) {
        try {
            int waitTime = minSeconds + (int)(Math.random() * (maxSeconds - minSeconds));
            Thread.sleep(waitTime * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ==================== MÉTHODES MANQUANTES À AJOUTER ====================

    /**
     * Faire défiler la page vers le bas
     */
    public void scrollDown(int pixels) {
        page.evaluate("window.scrollBy(0, " + pixels + ")");
        wait(1); // Petite attente après le scroll
    }

    /**
     * Faire défiler vers le haut
     */
    public void scrollUp(int pixels) {
        page.evaluate("window.scrollBy(0, -" + pixels + ")");
        wait(1);
    }

    /**
     * Faire défiler jusqu'en bas
     */
    public void scrollToBottom() {
        page.evaluate("window.scrollTo(0, document.body.scrollHeight)");
        wait(1);
    }

    /**
     * Faire défiler jusqu'en haut
     */
    public void scrollToTop() {
        page.evaluate("window.scrollTo(0, 0)");
        wait(1);
    }

    /**
     * Faire défiler jusqu'à un élément
     */
    public void scrollToElement(String selector) {
        Locator element = page.locator(selector).first();
        element.scrollIntoViewIfNeeded();
        wait(1);
    }

    /**
     * Obtenir le texte d'un élément par XPath
     */
    public String getElementTextByXpath(String xpath) {
        try {
            return page.locator("xpath=" + xpath).first().textContent().trim();
        } catch (Exception e) {
            log.error("Impossible de récupérer le texte de l'élément XPath: {}", xpath);
            return "";
        }
    }

    /**
     * Obtenir le texte d'un élément par sélecteur CSS
     */
    public String getElementText(String selector) {
        try {
            return page.locator(selector).first().textContent().trim();
        } catch (Exception e) {
            log.error("Impossible de récupérer le texte de l'élément: {}", selector);
            return "";
        }
    }

    /**
     * Attendre le chargement de la page
     */
    public void waitForPageLoad() {
        page.waitForLoadState(LoadState.NETWORKIDLE);
        wait(1);
    }

    // ==================== MÉTHODES UTILITAIRES SUPPLÉMENTAIRES ====================

    /**
     * Attendre qu'un élément soit visible
     */
    public void waitForElementVisible(String selector, int timeoutSeconds) {
        page.waitForSelector(selector,
                new Page.WaitForSelectorOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(timeoutSeconds * 1000));
    }

    /**
     * Méthode générique pour sélectionner une option par valeur
     * @param selector Le sélecteur CSS ou autre du select
     * @param value La valeur de l'option à sélectionner
     */
    public void selectByValue1(String selector, String value) {
        try {
            Locator selectLocator = page.locator(selector);
            selectLocator.selectOption(value);
            log.info("Option sélectionnée par valeur '{}' dans le select '{}'", value, selector);
        } catch (Exception e) {
            log.error("Erreur lors de la sélection par valeur '{}' dans '{}'", value, selector, e);
            throw new RuntimeException("Échec de la sélection par valeur", e);
        }
    }

    /**
     * Méthode générique pour sélectionner une option par texte visible
     * @param selector Le sélecteur CSS ou autre du select
     * @param label Le texte visible de l'option à sélectionner
     */
    public void selectByLabel(String selector, String label) {
        try {
            Locator selectLocator = page.locator(selector);
            selectLocator.selectOption(new SelectOption().setLabel(label));
            log.info("Option sélectionnée par label '{}' dans le select '{}'", label, selector);
        } catch (Exception e) {
            log.error("Erreur lors de la sélection par label '{}' dans '{}'", label, selector, e);
            throw new RuntimeException("Échec de la sélection par label", e);
        }
    }

    /**
     * Méthode générique pour sélectionner une option par index
     * @param selector Le sélecteur CSS ou autre du select
     * @param index L'index de l'option à sélectionner (commence à 0)
     */
    public void selectByIndex1(String selector, int index) {
        try {
            Locator selectLocator = page.locator(selector);
            selectLocator.selectOption(new SelectOption().setIndex(index));
            log.info("Option sélectionnée par index '{}' dans le select '{}'", index, selector);
        } catch (Exception e) {
            log.error("Erreur lors de la sélection par index '{}' dans '{}'", index, selector, e);
            throw new RuntimeException("Échec de la sélection par index", e);
        }
    }

    /**
     * Méthode pour vérifier si une option est sélectionnée
     * @param selector Le sélecteur CSS du select
     * @param value La valeur à vérifier
     * @return true si l'option est sélectionnée
     */
    public boolean isOptionSelected(String selector, String value) {
        try {
            Locator selectLocator = page.locator(selector);
            String selectedValue = selectLocator.evaluate("el => el.value").toString();
            return selectedValue.equals(value);
        } catch (Exception e) {
            log.error("Erreur lors de la vérification de l'option sélectionnée", e);
            return false;
        }
    }

    /**
     * Méthode pour obtenir la valeur sélectionnée
     * @param selector Le sélecteur CSS du select
     * @return La valeur de l'option sélectionnée
     */
    public String getSelectedValue(String selector) {
        try {
            Locator selectLocator = page.locator(selector);
            return selectLocator.evaluate("el => el.value").toString();
        } catch (Exception e) {
            log.error("Erreur lors de la récupération de la valeur sélectionnée", e);
            return "";
        }
    }

    /**
     * Méthode pour obtenir le texte de l'option sélectionnée
     * @param selector Le sélecteur CSS du select
     * @return Le texte de l'option sélectionnée
     */
    public String getSelectedText(String selector) {
        try {
            Locator selectLocator = page.locator(selector);
            return selectLocator.evaluate("el => el.options[el.selectedIndex].text").toString();
        } catch (Exception e) {
            log.error("Erreur lors de la récupération du texte sélectionné", e);
            return "";
        }
    }

    /**
     * Méthode pour sélectionner une option par son texte visible dans un select
     * @param selectSelector Le sélecteur CSS du élément select
     * @param visibleText Le texte visible de l'option à sélectionner
     */
    public void selectByVisibleText1(String selectSelector, String visibleText) {
        try {
            Locator selectLocator = page.locator(selectSelector);
            // Méthode 1: Utiliser selectOption avec le label
            selectLocator.selectOption(new SelectOption().setLabel(visibleText));

            log.info("Option sélectionnée par texte visible '{}' dans le select '{}'", visibleText, selectSelector);

        } catch (Exception e) {
            log.error("Erreur lors de la sélection par texte visible '{}' dans '{}'", visibleText, selectSelector, e);
            throw new RuntimeException("Échec de la sélection par texte visible: " + visibleText, e);
        }
    }

    /**
     * Méthode robuste pour sélectionner par texte visible
     */
    public void selectByVisibleText2(String selectSelector, String visibleText) {
        try {
            // Attendre que le select soit présent et visible
            page.waitForSelector(selectSelector, new Page.WaitForSelectorOptions().setState(WaitForSelectorState.VISIBLE));

            // Méthode JavaScript directe - CORRIGÉE
            page.evaluate("([selector, text]) => {\n" +
                    "    const select = document.querySelector(selector);\n" +
                    "    if (!select) return false;\n" +
                    "    for (let i = 0; i < select.options.length; i++) {\n" +
                    "        if (select.options[i].text.trim() === text) {\n" +
                    "            select.selectedIndex = i;\n" +
                    "            select.dispatchEvent(new Event('change', { bubbles: true }));\n" +
                    "            return true;\n" +
                    "        }\n" +
                    "    }\n" +
                    "    return false;\n" +
                    "}", java.util.Arrays.asList(selectSelector, visibleText));

            log.info("Option '{}' sélectionnée avec succès dans '{}'", visibleText, selectSelector);

        } catch (Exception e) {
            log.error("Erreur lors de la sélection de '{}' dans '{}'", visibleText, selectSelector, e);
            throw new RuntimeException("Échec de la sélection de: " + visibleText, e);
        }
    }

    public String getPrixByNiveau(int niveau, String divGeneral, String locatorPrix) {
        if (niveau < 1 || niveau > 6) {
            throw new IllegalArgumentException("Le niveau doit être compris entre 1 et 6");
        }

        Locator cartesNiveaux = page.locator("div.grid.grid-cols-6 > div");

        return cartesNiveaux
                .nth(niveau - 1)
                .locator("p.font-gotham-book")
                .textContent()
                .replace("/mois", "")
                .trim();
    }

}
