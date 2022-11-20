package com.kazurayam.inspectus.selenium4sample;

import com.kazurayam.inspectus.core.Environment;
import com.kazurayam.inspectus.core.Inspectus;
import com.kazurayam.inspectus.core.InspectusException;
import com.kazurayam.inspectus.core.Intermediates;
import com.kazurayam.inspectus.core.Parameters;
import com.kazurayam.inspectus.core.UncheckedInspectusException;
import com.kazurayam.inspectus.fn.FnTwinsDiff;
import com.kazurayam.materialstore.base.materialize.Target;
import com.kazurayam.materialstore.base.materialize.TargetCSVParser;
import com.kazurayam.materialstore.core.filesystem.JobName;
import com.kazurayam.materialstore.core.filesystem.JobTimestamp;
import com.kazurayam.materialstore.core.filesystem.Material;
import com.kazurayam.materialstore.core.filesystem.MaterialstoreException;
import com.kazurayam.materialstore.core.filesystem.Metadata;
import com.kazurayam.materialstore.core.filesystem.SortKeys;
import com.kazurayam.materialstore.core.filesystem.Stores;
import com.kazurayam.materialstore.core.filesystem.metadata.IgnoreMetadataKeys;
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

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Using Selenium open a browser to visit 2 URL;
 * one is a pseudo Projection environment,
 * another is a pseudo Development environment.
 * Take screenshots of 2 URL and compare the images
 * to compile HTML report.
 */
public class FnTwinsDiffTest {

    private Path testClassOutputDir;
    private WebDriver driver;
    private Path fixturesDir;

    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    public void setup() {
        testClassOutputDir = TestHelper.createTestClassOutputDir(this);
        fixturesDir = Paths.get(".").resolve("src/test/fixtures");
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
    void performTwinsDiff() throws InspectusException {
        Parameters parameters = new Parameters.Builder()
                .store(Stores.newInstance(testClassOutputDir.resolve("store")))
                .jobName(new JobName("testMaterialize"))
                .jobTimestamp(JobTimestamp.now())
                .ignoreMetadataKeys(
                        new IgnoreMetadataKeys.Builder()
                                .ignoreKey("URL.host")
                                .build()
                )
                .sortKeys(new SortKeys("step"))
                .build();
        Inspectus twinsDiff =
                new FnTwinsDiff(fn,
                        new Environment("ProductionEnv"),
                        new Environment("DevelopmentEnv"));
        twinsDiff.execute(parameters);
    }


    /*
     * Internally invoked by the FnTwinsDiff#execute().
     *
     */
    private final Function<Parameters, Intermediates> fn =
            (p) -> {
        assert p.getEnvironment() != Environment.NULL_OBJECT : "p.Environment() must not be null";
        Environment env = p.getEnvironment();
        try {
            List<Target> targetList;
            switch (env.toString()) {
                case "ProductionEnv":
                    targetList = getTargetList("targetList.Prod.csv", env);
                    break;
                case "DevelopmentEnv":
                    targetList = getTargetList("targetList.Dev.csv", env);
                    break;
                default:
                    throw new UncheckedInspectusException(
                            String.format("unknown Environment env=%s", env));
            }
            JobTimestamp jobTimestamp = p.getJobTimestamp();
            // process the targets
            for (int i = 0; i < targetList.size(); i++) {
                Target target = targetList.get(i);
                processTarget(p, jobTimestamp, target.getUrl(), target.getBy(),
                        env, String.format("%02d", i + 1));
            }
        } catch (InspectusException e) {
            throw new UncheckedInspectusException(e);
        }
        return Intermediates.NULL_OBJECT;
    };

    private void processTarget(Parameters p, JobTimestamp jt,
                            URL url, By handle,
                            Environment environment, String step) {
        driver.get(url.toExternalForm());
        driver.manage().window().setSize(new Dimension(1024, 1000));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        WebDriverWait w = new WebDriverWait(driver, Duration.ofSeconds(3));
        w.until(ExpectedConditions.presenceOfElementLocated(handle));
        Metadata md = Metadata.builder(url)
                .put("environment", environment.toString())
                .put("step", step)
                .build();
        Material mt = TestHelper.takePageScreenshotSaveIntoStore(driver,
                p.getStore(), p.getJobName(), jt, md);
        assertNotNull(mt);
        assertNotEquals(Material.NULL_OBJECT, mt);
    }

    /*
     * read a CSV file located in the `src/test/fixtures/FnTwinsDiffTest` directory,
     * construct a list of Target objects which contains URLs to materialize.
     */
    private List<Target> getTargetList(String csvName, Environment environment)
            throws InspectusException {
        Path dataDir = fixturesDir.resolve("FnTwinsDiffTest");
        Path targetFile = dataDir.resolve(csvName);
        assert Files.exists(targetFile);
        //
        TargetCSVParser parser = TargetCSVParser.newSimpleParser();
        try {
            return parser.parse(targetFile).stream()
                            .map(t -> t.copyWith("profile", environment.toString()))
                            .collect(Collectors.toList());
        } catch (MaterialstoreException e) {
            throw new InspectusException(e);
        }
    }

}
