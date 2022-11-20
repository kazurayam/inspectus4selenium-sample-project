package com.kazurayam.inspectus.selenium;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URL;
import java.time.Duration;

public class WebDriverFormulas {

    public WebDriverFormulas() {}

    public final void navigateByClick(WebDriver driver,
                                      By clickThis,
                                      By handleInTheNext,
                                      int timeout) {
        this.waitForElementPresent(driver, clickThis, timeout);
        driver.findElement(clickThis).click();
        this.waitForElementPresent(driver, handleInTheNext, timeout);
    }

    public final void navigateTo(WebDriver driver,
                                 URL url,
                                 By handle,
                                 int timeout) {
        driver.get(url.toExternalForm());
        this.waitForElementPresent(driver, handle, timeout);
    }

    public final void waitForElementPresent(WebDriver driver,
                                            By handle,
                                            int timeout) {
        new WebDriverWait(driver, Duration.ofSeconds(timeout))
                .until(ExpectedConditions.presenceOfElementLocated(handle));
    }

}
