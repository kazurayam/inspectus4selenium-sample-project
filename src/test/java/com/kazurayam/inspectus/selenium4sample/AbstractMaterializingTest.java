package com.kazurayam.inspectus.selenium4sample;

import com.kazurayam.inspectus.materialize.discovery.Target;
import com.kazurayam.inspectus.materialize.selenium.WebElementMaterializingFunctions;
import com.kazurayam.inspectus.materialize.selenium.WebPageMaterializingFunctions;
import com.kazurayam.materialstore.core.Material;
import com.kazurayam.materialstore.core.MaterialstoreException;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

import java.util.Map;

abstract class AbstractMaterializingTest {

    Material storeEntirePageScreenshot(WebPageMaterializingFunctions functions,
                                       WebDriver driver,
                                       Target target,
                                       Map<String, String> attributes)
            throws MaterialstoreException {
        // take screenshot, save the image into the store
        return functions.storeEntirePageScreenshot.accept(driver, target, attributes);
    }

    Material storeHTMLSource(WebPageMaterializingFunctions functions,
                             WebDriver driver,
                             Target target,
                             Map<String, String> attributes)
            throws MaterialstoreException {
        // take HTML source, save the text into the store
        return functions.storeHTMLSource.accept(driver, target, attributes);
    }

    Material storeElementScreenshot(WebElementMaterializingFunctions functions,
                                    WebDriver driver,
                                    Target target,
                                    Map<String, String> attributes,
                                    By by) throws MaterialstoreException {
        return functions.storeElementScreenshot.accept(driver, target, attributes, by);
    }

}
