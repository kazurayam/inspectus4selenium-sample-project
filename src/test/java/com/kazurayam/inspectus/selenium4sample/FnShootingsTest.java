package com.kazurayam.inspectus.selenium4sample;

import com.kazurayam.ashotwrapper.AShotWrapper;
import com.kazurayam.inspectus.core.FnShootings;
import com.kazurayam.inspectus.core.Inspectus;
import com.kazurayam.inspectus.core.InspectusException;
import com.kazurayam.inspectus.core.Intermediates;
import com.kazurayam.inspectus.core.Parameters;
import com.kazurayam.materialstore.core.filesystem.FileType;
import com.kazurayam.materialstore.core.filesystem.JobName;
import com.kazurayam.materialstore.core.filesystem.JobTimestamp;
import com.kazurayam.materialstore.core.filesystem.Material;
import com.kazurayam.materialstore.core.filesystem.MaterialstoreException;
import com.kazurayam.materialstore.core.filesystem.Metadata;
import com.kazurayam.materialstore.core.filesystem.SortKeys;
import com.kazurayam.materialstore.core.filesystem.Store;
import com.kazurayam.materialstore.core.filesystem.Stores;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.awt.image.BufferedImage;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FnShootingsTest {

    private Path testClassOutputDir;
    private WebDriver driver;

    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    public void setup() {
        ChromeOptions opt = new ChromeOptions();
        opt.addArguments("headless");
        driver = new ChromeDriver(opt);
        //
        testClassOutputDir = TestHelper.createTestClassOutputDir(this);
    }

    @AfterEach
    public void tearDown() {
        driver.quit();
    }


    @Test
    void testMaterializing() throws InspectusException {
        Parameters parameters = new Parameters.Builder()
                .store(Stores.newInstance(testClassOutputDir.resolve("store")))
                .jobName(new JobName("testMaterializing"))
                .jobTimestamp(JobTimestamp.now())
                .sortKeys(new SortKeys("step"))
                .build();
        Inspectus shootings = new FnShootings(fn);
        shootings.execute(parameters);
    }

    /**
     * We will visit the seach engine https://duckduckgo.co/ ,
     * make a query for keyword "selenium".
     * We will take full page screenshots and turn them into PNG images,
     * then write 3 material objects into the store.
     * We will put some metadata on the material objects.
     */
    private final Function<Parameters, Intermediates> fn = (p) -> {
        // pick up the parameters
        Store store = p.getStore();
        JobName jobName = p.getJobName();
        JobTimestamp jobTimestamp = p.getJobTimestamp();
        // visit the target
        String urlStr = "https://duckduckgo.com/";
        driver.get(urlStr);
        driver.manage().window().setSize(new Dimension(1024, 1000));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        String title = driver.getTitle();
        assertTrue(title.contains("DuckDuckGo"));
        // explicitly wait for <input name="q">
        WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(3));
        w.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath("//input[@name='q']")));
        // take the 1st screenshot
        Material mt = takePageScreenshotWriteIntoStore(driver, urlStr,
                store, jobName, jobTimestamp);
        assertNotNull(mt);
        assertNotEquals(Material.NULL_OBJECT, mt);
        return Intermediates.NULL_OBJECT;
    };


    private Material takePageScreenshotWriteIntoStore(
            WebDriver driver, String urlStr,
            Store store, JobName jobName, JobTimestamp jobTimestamp) {
        try {
            BufferedImage bi = AShotWrapper.takeEntirePageImage(driver,
                    new AShotWrapper.Options.Builder().build());
            assertNotNull(bi);
            URL url;
            url = new URL(urlStr);
            Metadata md = new Metadata.Builder(url).build();
            Material mt = store.write(jobName, jobTimestamp, FileType.PNG, md, bi);
            return mt;
        } catch (MalformedURLException | MaterialstoreException e) {
            throw new RuntimeException(e);
        }
    }
}
