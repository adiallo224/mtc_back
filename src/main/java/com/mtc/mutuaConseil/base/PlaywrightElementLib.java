package com.mtc.mutuaConseil.base;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class PlaywrightElementLib implements ElementLib {
    private final Page page;

    public PlaywrightElementLib(Page page) { this.page = page; }

    @Override
    public void clickByXpath(String xpath) {
        Locator l = page.locator("xpath=" + xpath).first();
        l.waitFor();
        l.hover();
        WaitUtils.humanDelay();
        l.click();
    }

    @Override
    public void clickByActions(String selector) {

    }

    @Override
    public void clickJSExecutorByXpath(String xpath) {

    }

    @Override
    public void clickByClassName(String className) {

    }

    @Override
    public void clickByName(String className) {

    }

    @Override
    public void clickById(String className) {

    }

    @Override
    public void typeByXpath(String xpath, String text) {
        Locator l = page.locator("xpath=" + xpath).first();
        l.click();
        for(char c: text.toCharArray()){
            l.press(String.valueOf(c));
            WaitUtils.humanTypeDelay();
        }
    }

    @Override
    public void typeById(String id, String text) {

    }

    @Override
    public void typeByClassName(String className, String text) {

    }

    @Override
    public void typeByName(String name, String text) {

    }

    @Override
    public void click(String cssOrSelector) {
        Locator l = page.locator(cssOrSelector).first();
        l.waitFor();
        l.hover();
        WaitUtils.humanDelay();
        l.click();
    }

    @Override
    public void type(String cssOrSelector, String text) {
        Locator l = page.locator(cssOrSelector).first();
        l.click();
        for(char c: text.toCharArray()){
            l.press(String.valueOf(c));
            WaitUtils.humanTypeDelay();
        }
    }

    @Override
    public String getTextByXpath(String xpath) {
        return page.locator("xpath=" + xpath).first().textContent();
    }

    @Override
    public boolean isVisibleXpath(String xpath) {
        return page.locator("xpath=" + xpath).first().isVisible();
    }

    @Override
    public void hover(String selector) { page.locator(selector).hover(); }

    @Override
    public void scrollIntoView(String selector) { page.locator(selector).scrollIntoViewIfNeeded(); }

    @Override
    public void pressEnterOnBody() {

    }

    @Override
    public void pressEnter() {

    }

    @Override
    public boolean isElementPresent(String selector) {
        return false;
    }

}
