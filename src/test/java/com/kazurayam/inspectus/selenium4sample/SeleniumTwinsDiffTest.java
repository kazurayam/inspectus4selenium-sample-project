package com.kazurayam.inspectus.selenium4sample;

import com.kazurayam.inspectus.core.Environment;
import com.kazurayam.inspectus.core.Inspectus;
import com.kazurayam.inspectus.core.InspectusException;
import com.kazurayam.inspectus.core.Intermediates;
import com.kazurayam.inspectus.core.Parameters;
import com.kazurayam.inspectus.core.UncheckedInspectusException;
import com.kazurayam.inspectus.fn.FnTwinsDiff;
import com.kazurayam.inspectus.materialize.discovery.Sitemap;
import com.kazurayam.inspectus.materialize.discovery.SitemapLoader;
import com.kazurayam.inspectus.materialize.discovery.Target;
import com.kazurayam.inspectus.materialize.selenium.WebDriverFormulas;
import com.kazurayam.materialstore.core.JobName;
import com.kazurayam.materialstore.core.JobTimestamp;
import com.kazurayam.materialstore.core.Material;
import com.kazurayam.materialstore.core.Metadata;
import com.kazurayam.materialstore.core.SortKeys;
import com.kazurayam.materialstore.core.Stores;
import com.kazurayam.materialstore.core.metadata.IgnoreMetadataKeys;
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

import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Using Selenium open a browser to visit a pair of
 * web site (Environments). Each web site contains
 * multiple URLs.
 * Take screenshots of 2 web sites and compare the images
 * to compile a HTML report.
 */
public class SeleniumTwinsDiffTest {

    private Path testClassOutputDir;
    private WebDriver driver;
    private WebDriverFormulas wdf;
    private Path fixturesDir;

    @BeforeAll
    static void setupClass() {
        WebDriverManager.chromedriver().setup();
    }

    @BeforeEach
    public void setup() {
        testClassOutputDir = TestHelper.createTestClassOutputDir(SeleniumTwinsDiffTest.class);
        fixturesDir = Paths.get(".").resolve("src/test/fixtures");
        //
        ChromeOptions opt = new ChromeOptions();
        opt.addArguments("headless");
        opt.addArguments("--remote-allow-origins=*");
        driver = new ChromeDriver(opt);
        driver.manage().window().setSize(new Dimension(1024, 1000));
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        //
        wdf = new WebDriverFormulas();
    }

    @AfterEach
    public void tearDown() {
        driver.quit();
    }

    @Test
    public void performTwinsDiff() throws InspectusException {
        Parameters parameters = new Parameters.Builder()
                .store(Stores.newInstance(testClassOutputDir.resolve("store")))
                .jobName(new JobName("performTwinsDiff"))
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
     * invoked by the FnTwinsDiff#execute() internally.
     */
    private final BiFunction<Parameters, Intermediates, Intermediates> fn = (parameters, intermediates) -> {
        assert parameters.getEnvironment() != Environment.NULL_OBJECT : "parameters.Environment() must not be null";
        Environment env = parameters.getEnvironment();
        Path dataDir = fixturesDir.resolve("FnTwinsDiffTest");
        try {
            List<Target> targetList;
            switch (env.toString()) {
                case "ProductionEnv":
                    targetList =
                            getTargetList(new URL("http://myadmin.kazurayam.com"),
                                    dataDir.resolve("targetList.csv"));
                    assert targetList.size() > 0 : "targetList is empty";
                    break;
                case "DevelopmentEnv":
                    targetList =
                            getTargetList(new URL("http://devadmin.kazurayam.com"),
                                    dataDir.resolve("targetList.csv"));
                    assert targetList.size() > 0 : "targetList is empty";
                    break;
                default:
                    throw new UncheckedInspectusException(
                            String.format("unknown Environment env=%s", env));
            }
            JobTimestamp jobTimestamp = parameters.getJobTimestamp();
            // process the targets
            for (int i = 0; i < targetList.size(); i++) {
                Target target = targetList.get(i);
                processTarget(parameters, jobTimestamp, target.getUrl(),
                        target.getHandle().getBy(),
                        env, String.format("%02d", i + 1));
            }
        } catch (MalformedURLException | InspectusException e) {
            throw new UncheckedInspectusException(e);
        }
        return new Intermediates.Builder(intermediates).build();
    };

    private void processTarget(Parameters p, JobTimestamp jt,
                            URL url, By handle,
                            Environment environment, String step) {
        driver.get(url.toExternalForm());
        wdf.waitForElementPresent(driver, handle, 3);
        Metadata md = Metadata.builder(url)
                .put("environment", environment.toString())
                .put("step", step)
                .build();
        Material mt = MaterializeUtils.takePageScreenshotSaveIntoStore(driver,
                p.getStore(), p.getJobName(), jt, md);
        assertNotNull(mt);
        assertNotEquals(Material.NULL_OBJECT, mt);
    }

    /*
     * read a CSV file located in the `src/test/fixtures/FnTwinsDiffTest` directory,
     * construct a list of Target objects which contains URLs to materialize.
     */
    private List<Target> getTargetList(URL baseTopPageURL, Path targetFile)
            throws InspectusException {
        assert Files.exists(targetFile);
        //
        Target baseTopPage = Target.builder(baseTopPageURL).build();
        SitemapLoader loader = new SitemapLoader(baseTopPage);
        loader.setWithHeaderRecord(false);
        Sitemap sitemap = loader.parseCSV(targetFile);
        System.out.println("sitemap=" + sitemap.toJson(true));
        List<Target> result = sitemap.getBaseTargetList();
        assert result.size() > 0 : "the result is empty";
        return result;
    }

}
