package com.kazurayam.inspectus.selenium4sample;

import com.kazurayam.inspectus.core.Inspectus;
import com.kazurayam.inspectus.core.InspectusException;
import com.kazurayam.inspectus.core.Intermediates;
import com.kazurayam.inspectus.core.Parameters;
import com.kazurayam.inspectus.core.UncheckedInspectusException;
import com.kazurayam.inspectus.fn.FnShootings;
import com.kazurayam.inspectus.materialize.selenium.WebDriverFormulas;
import com.kazurayam.materialstore.core.JobName;
import com.kazurayam.materialstore.core.JobTimestamp;
import com.kazurayam.materialstore.core.Material;
import com.kazurayam.materialstore.core.Metadata;
import com.kazurayam.materialstore.core.SortKeys;
import com.kazurayam.materialstore.core.Store;
import com.kazurayam.materialstore.core.Stores;
import com.kazurayam.unittest.TestOutputOrganizer;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Using Selenium, open a browser to visit the DuckDuckGo site.
 * Take 3 screenshots to store images into the store.
 * Will compile a Shootings report in HTML.
 */
public class SeleniumShootingsTest {

    private static TestOutputOrganizer too =
            TestOutputOrganizerFactory.create(SeleniumShootingsTest.class);
    private static Path classOutputDirectory;
    private WebDriver driver;
    private WebDriverFormulas wdf;

    @BeforeAll
    static void setupClass() throws IOException {
        too.cleanClassOutputDirectory();
        classOutputDirectory = too.getClassOutputDirectory();
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    public void setup() {
        ChromeOptions opt = new ChromeOptions();
        opt.addArguments("headless");
        opt.addArguments("--remote-allow-origins=*");
        driver = new ChromeDriver(opt);
        driver.manage().window().setSize(new Dimension(1024, 1000));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));

        wdf = new WebDriverFormulas();
    }

    @AfterEach
    public void tearDown() {
        driver.quit();
    }


    @Test
    void test_performShootings() throws InspectusException {
        Parameters parameters = new Parameters.Builder()
                .store(Stores.newInstance(classOutputDirectory.resolve("store")))
                .jobName(new JobName("testMaterialize"))
                .jobTimestamp(JobTimestamp.now())
                .sortKeys(new SortKeys("step"))
                .build();
        Inspectus shootings = new FnShootings(fn);
        shootings.execute(parameters);
    }

    /**
     * We will visit the search engine https://duckduckgo.co/ ,
     * make a query for keyword "selenium".
     * We will take full page screenshots and turn them into PNG images,
     * then write 3 material objects into the store.
     * We will put some metadata on the material objects.
     */
    private final BiFunction<Parameters, Intermediates, Intermediates> fn =
            (parameters, intermediates) -> {
        // pick up the parameter values
        Store store = parameters.getStore();
        JobName jobName = parameters.getJobName();
        JobTimestamp jobTimestamp = parameters.getJobTimestamp();
        // visit the target
        String urlStr = "https://duckduckgo.com/";
        URL url = makeURL(urlStr);
        driver.get(urlStr);
        String title = driver.getTitle();
        assertTrue(title.contains("DuckDuckGo"));

        // explicitly wait for <input name="q">
        By inputQ = By.xpath("//input[@name='q']");
        wdf.waitForElementPresent(driver,inputQ, 3);
        // take the 1st screenshot of the blank search page
        Metadata md1 = Metadata.builder(url).put("step", "01").build();
        Material mt1 = MaterializeUtils.takePageScreenshotSaveIntoStore(driver,
                store, jobName, jobTimestamp, md1);
        assertNotNull(mt1);
        assertNotEquals(Material.NULL_OBJECT, mt1);

        // type a keyword "selenium" in the <input> element, then
        // take the 2nd screenshot
        driver.findElement(inputQ).sendKeys("selenium");
        Metadata md2 = Metadata.builder(url).put("step", "02").build();
        Material mt2 = MaterializeUtils.takePageScreenshotSaveIntoStore(driver,
                store, jobName, jobTimestamp, md2);
        assertNotNull(mt2);
        assertNotEquals(Material.NULL_OBJECT, mt2);

        // send ENTER, wait for the search result page to be loaded,
        driver.findElement(inputQ).sendKeys(Keys.RETURN);
        By inputQSelenium = By.xpath("//input[@name='q' and @value='selenium']");
        wdf.waitForElementPresent(driver, inputQSelenium, 3);

        // take the 3rd screenshot
        Metadata md3 = Metadata.builder(url).put("step", "03").build();
        Material mt3 = MaterializeUtils.takePageScreenshotSaveIntoStore(driver,
                store, jobName, jobTimestamp, md3);
        assertNotNull(mt3);
        assertNotEquals(Material.NULL_OBJECT, mt3);

        // done all, exit the Function returning a Intermediate object
        return new Intermediates.Builder(intermediates).build();
    };

    private static URL makeURL(String urlStr) {
        try {
            return new URL(urlStr);
        } catch (MalformedURLException e) {
            throw new UncheckedInspectusException(e);
        }
    }
}
