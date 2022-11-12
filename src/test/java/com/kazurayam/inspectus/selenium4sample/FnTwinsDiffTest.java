package com.kazurayam.inspectus.selenium4sample;

import com.kazurayam.inspectus.fn.FnTwinsDiff;
import com.kazurayam.materialstore.core.filesystem.metadata.IgnoreMetadataKeys;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.BeforeAll;
import org.openqa.selenium.WebDriver;

import java.nio.file.Path;
import com.kazurayam.inspectus.core.Inspectus;
import com.kazurayam.inspectus.core.InspectusException;
import com.kazurayam.inspectus.core.Intermediates;
import com.kazurayam.inspectus.core.Parameters;
import com.kazurayam.materialstore.core.filesystem.JobName;
import com.kazurayam.materialstore.core.filesystem.JobTimestamp;
import com.kazurayam.materialstore.core.filesystem.Material;
import com.kazurayam.materialstore.core.filesystem.Metadata;
import com.kazurayam.materialstore.core.filesystem.SortKeys;
import com.kazurayam.materialstore.core.filesystem.Stores;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Using Selenium open a browser to visit 2 URL;
 * one is a psuedo Projection environment,
 * another is a psuedo Development environment.
 * Take screenshots of 2 URL and compare the images
 * to compile HTML report.
 */
public class FnTwinsDiffTest {

    private Path testClassOutputDir;
    private WebDriver driver;

    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    public void setup() {
        testClassOutputDir = TestHelper.createTestClassOutputDir(this);
        //
        ChromeOptions opt = new ChromeOptions();
        opt.addArguments("headless");
        driver = new ChromeDriver(opt);
    }

    @AfterEach
    public void tearDown() {
        driver.quit();
    }

    @Test
    void testMaterialize() throws InspectusException {
        Parameters parameters = new Parameters.Builder()
                .store(Stores.newInstance(testClassOutputDir.resolve("store")))
                .jobName(new JobName("testMaterialize"))
                .jobTimestamp(JobTimestamp.now())
                .profileLeft("ProductionEnv")
                .profileRight("DevelopmentEnv")
                .ignoreMetadataKeys(
                        new IgnoreMetadataKeys.Builder()
                                .ignoreKey("URL.host")
                                .build()
                )
                .sortKeys(new SortKeys("step"))
                .build();
        Inspectus twinsDiff = new FnTwinsDiff(fn);
        twinsDiff.execute(parameters);
    }

    private final Function<Parameters, Intermediates> fn = (p) -> {
        JobTimestamp jt1 = p.getJobTimestamp();
        // visit the 1st target
        processURL(p, jt1,
                "http://myadmin.kazurayam.com/",
                "//img[@alt='umineko']",
                p.getProfileLeft(),
                "01");

        // visit the 2nd target
        JobTimestamp jt2 = JobTimestamp.laterThan(jt1);
        processURL(p, jt2,
                "http://devadmin.kazurayam.com/",
                "//img[@alt='umineko']",
                p.getProfileRight(),
                "01");
        return new Intermediates.Builder()
                .jobTimestampLeft(jt1)
                .jobTimestampRight(jt2)
                .build();
    };

    private void processURL(Parameters p, JobTimestamp jt,
                            String urlStr, String handle, String profile, String step) {
        URL url1 = TestHelper.makeURL(urlStr);
        driver.get(urlStr);
        driver.manage().window().setSize(new Dimension(1024, 1000));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(3));
        w.until(ExpectedConditions.presenceOfElementLocated(
                By.xpath(handle)));
        Metadata md = Metadata.builder(url1)
                .put("profile", profile)
                .put("step", step)
                .build();
        Material mt1 = TestHelper.takePageScreenshotSaveIntoStore(driver,
                p.getStore(), p.getJobName(), jt, md);
        assertNotNull(mt1);
        assertNotEquals(Material.NULL_OBJECT, mt1);
    }
}
