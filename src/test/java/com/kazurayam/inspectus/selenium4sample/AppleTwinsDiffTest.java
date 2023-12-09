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
import com.kazurayam.inspectus.materialize.selenium.WebElementMaterializingFunctions;
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
import org.openqa.selenium.By;
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

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Using Selenium WebDriver 4, visite 2 URLs
 * - https://kazurayam.github.io/myApple
 * - https://kazurayam.github.io/myApple-alt
 *
 * Will take screenshot of the img element and compare these 2 sites.
 */
public class AppleTwinsDiffTest extends AbstractMaterializingTest {

    private static TestOutputOrganizer too =
            TestOutputOrganizerFactory.create(AppleTwinsDiffTest.class);
    private static Path classOutputDir;
    private static Path fixturesDir;
    private WebDriver driver;
    private WebDriverFormulas wdf;

    @BeforeAll
    static void setupClass() throws IOException {
        too.cleanClassOutputDirectory();
        classOutputDir = too.getClassOutputDirectory();
        fixturesDir = too.getProjectDir().resolve("src/test/fixtures");
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
        //
        wdf = new WebDriverFormulas();
    }

    @AfterEach
    public void tearDown() { driver.quit(); }

    @Test
    public void test_performTwinsDiff() throws InspectusException {
        Parameters parameters = new Parameters.Builder()
                .store(Stores.newInstance(classOutputDir.resolve("store")))
                .jobName(new JobName("myApple"))
                .jobTimestamp(JobTimestamp.now())
                .ignoreMetadataKeys(
                        new IgnoreMetadataKeys.Builder()
                                .ignoreKeys("URL.host", "URL.path",
                                        "URL.protocol", "URL.port",
                                        "image-width", "image-height").build()
                )
                .sortKeys(new SortKeys("step"))
                .build();
        Inspectus twinsDiff =
                new FnTwinsDiff(fn,
                        new Environment("Production"),
                        new Environment("Development"));
        twinsDiff.execute(parameters);
    }

    private final BiFunction<Parameters, Intermediates, Intermediates> fn =
            (parameters, intermediates) -> {
        try {
            Store store = parameters.getStore();
            JobName jobName = parameters.getJobName();
            JobTimestamp jobTimestamp = parameters.getJobTimestamp();
            WebElementMaterializingFunctions functions =
                    new WebElementMaterializingFunctions(store, jobName, jobTimestamp);

            assert parameters.getEnvironment() != Environment.NULL_OBJECT :
                    "parameters.getEnvironment() must not return null";
            Environment env = parameters.getEnvironment();

            Path dataDir = fixturesDir.resolve("AppleTwinsDiffTest");
            List<Target> targetList;
            Map<String, String> bindings;
            switch (env.toString()) {
                case "Production":
                    bindings = Collections.singletonMap("URL_PREFIX",
                            "https://kazurayam.github.io/myApple");
                    targetList =
                            SitemapLoader.loadSitemapJson(dataDir.resolve("sitemap.json"), bindings)
                                    .getTargetList();
                    assert !targetList.isEmpty() : "targetList is empty";
                    break;

                case "Development":
                    bindings = Collections.singletonMap("URL_PREFIX",
                            "https://kazurayam.github.io/myApple-alt");
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
                Material mt = storeElementScreenshot(functions, driver, target, By.cssSelector("img#apple"), attributes);
                assertNotNull(mt);
            }
        } catch (MaterialstoreException | InspectusException e) {
            throw new UncheckedInspectusException(e);
        }
        return new Intermediates.Builder(intermediates).build();
    };
}
