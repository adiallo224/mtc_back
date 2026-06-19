package com.mtc.mutuaConseil.base;

public interface ElementLib {
    void clickByXpath(String xpath);
    void clickByActions(String selector);
    void clickJSExecutorByXpath(String xpath);
    void clickByClassName(String className);
    void clickByName(String className);
    void clickById(String id);
    void typeByXpath(String xpath, String text);
    void typeById(String id, String text);
    void typeByClassName(String className, String text);
    void typeByName(String name, String text);
    void click(String cssOrSelector);
    void type(String cssOrSelector, String text);
    String getTextByXpath(String xpath);
    boolean isVisibleXpath(String xpath);
    void hover(String selector);
    void scrollIntoView(String selector);
    void pressEnterOnBody();
    void pressEnter();
    boolean isElementPresent(String selector);
}
