package com.kazurayam.inspectus.selenium4sample;

import com.kazurayam.inspectus.core.Environment;
import com.kazurayam.inspectus.core.Inspectus;
import com.kazurayam.inspectus.core.InspectusException;
import com.kazurayam.inspectus.core.Intermediates;
import com.kazurayam.inspectus.core.Parameters;
import com.kazurayam.inspectus.core.UncheckedInspectusException;
import com.kazurayam.inspectus.fn.FnTwinsDiff;
import com.kazurayam.inspectus.materialize.discovery.SitemapLoader;
import com.kazurayam.inspectus.materialize.discovery.Target;
import com.kazurayam.inspectus.materialize.selenium.WebDriverFormulas;
import com.kazurayam.inspectus.materialize.selenium.WebPageMaterializingFunctions;
import com.kazurayam.materialstore.core.JobName;
import com.kazurayam.materialstore.core.JobTimestamp;
import com.kazurayam.materialstore.core.Material;
import com.kazurayam.materialstore.core.MaterialstoreException;
import com.kazurayam.materialstore.core.SortKeys;
import com.kazurayam.materialstore.core.Store;
import com.kazurayam.materialstore.core.Stores;
import com.kazurayam.materialstore.core.metadata.IgnoreMetadataKeys;
import com.kazurayam.unittest.TestOutputOrganizer;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Using Selenium WebDriver, open a browser to visit a pair of
 * website (Environments). Each website contains
 * multiple URLs.
 * Take screenshots of 2 websites and compare the images
 * to compile a HTML report.
 */
public class SeleniumTwinsDiffTest extends AbstractMaterializingTest {

    private static TestOutputOrganizer too =
            TestOutputOrganizerFactory.create(SeleniumTwinsDiffTest.class);
    private static Path classOutputDir;
    private static Path fixturesDir;
    private WebDriver driver;
    private WebDriverFormulas wdf;

    @BeforeAll
    static void setupClass() throws IOException {
        classOutputDir = too.cleanClassOutputDirectory();
        fixturesDir = too.getProjectDirectory().resolve("src/test/fixtures");
        WebDriverManager.chromedriver().clearDriverCache().setup();
    }

    @BeforeEach
    public void setup() {
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
    public void test_performTwinsDiff() throws InspectusException {
        Parameters parameters = new Parameters.Builder()
                .store(Stores.newInstance(classOutputDir.resolve("store")))
                .jobName(new JobName("MyAdmin"))
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
    private final BiFunction<Parameters, Intermediates, Intermediates> fn =
            (parameters, intermediates) -> {
        try {
            Store store = parameters.getStore();
            JobName jobName = parameters.getJobName();
            JobTimestamp jobTimestamp = parameters.getJobTimestamp();
            WebPageMaterializingFunctions functions =
                    new WebPageMaterializingFunctions(store, jobName, jobTimestamp);

            assert parameters.getEnvironment() != Environment.NULL_OBJECT :
                    "parameters.getEnvironment() must not return null";
            Environment env = parameters.getEnvironment();

            Path dataDir = fixturesDir.resolve("FnTwinsDiffTest");
            List<Target> targetList;
            Map<String, String> bindings;
            switch (env.toString()) {
                case "ProductionEnv":
                    bindings = Collections.singletonMap("URL_PREFIX", "http://myadmin.kazurayam.com");
                    targetList =
                            SitemapLoader.loadSitemapJson(dataDir.resolve("sitemap.json"), bindings)
                                    .getTargetList();
                    assert !targetList.isEmpty() : "targetList is empty";
                    break;
                case "DevelopmentEnv":
                    bindings = Collections.singletonMap("URL_PREFIX", "http://devadmin.kazurayam.com");
                    targetList =
                            SitemapLoader.loadSitemapJson(dataDir.resolve("sitemap.json"), bindings)
                                    .getTargetList();
                    assert !targetList.isEmpty() : "targetList is empty";
                    break;
                default:
                    throw new UncheckedInspectusException(
                            String.format("unknown Environment env=%s", env));
            }
            // process the targets
            for (int i = 0; i < targetList.size(); i++) {
                Target target = targetList.get(i);
                wdf.navigateTo(driver, target.getUrl(), target.getHandle().getBy(), 10);
                Map<String, String> attributes = new HashMap<>();
                attributes.put("environment", env.toString());
                attributes.put("step", String.format("%02d", i + 1));
                Material mt = storeEntirePageScreenshot(functions, driver, target, attributes);
                storeHTMLSource(functions, driver, target, attributes);
                assertNotNull(mt);
            }
        } catch (MaterialstoreException | InspectusException e) {
            throw new UncheckedInspectusException(e);
        }
        return new Intermediates.Builder(intermediates).build();
    };


}
