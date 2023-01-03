package com.kazurayam.inspectus.selenium4sample;

import com.kazurayam.inspectus.core.Inspectus;
import com.kazurayam.inspectus.core.InspectusException;
import com.kazurayam.inspectus.core.Intermediates;
import com.kazurayam.inspectus.core.Parameters;
import com.kazurayam.inspectus.core.UncheckedInspectusException;
import com.kazurayam.inspectus.fn.FnChronosDiff;
import com.kazurayam.inspectus.materialize.discovery.Handle;
import com.kazurayam.inspectus.materialize.discovery.Target;
import com.kazurayam.inspectus.materialize.selenium.WebDriverFormulas;
import com.kazurayam.inspectus.materialize.selenium.WebPageMaterializingFunctions;
import com.kazurayam.materialstore.core.filesystem.JobName;
import com.kazurayam.materialstore.core.filesystem.JobTimestamp;
import com.kazurayam.materialstore.core.filesystem.MaterialstoreException;
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
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Using Selenium open a browser to visit a web site,
 * visit multiple URLs of the site and take screenshot.
 * You are supposed to execute this "FunChronosDiffTest"
 * at least twice (2 times). This test will automatically
 * find the products(screenshot images) of the previous run. This test will
 * compare the products of the previous run and the current run,
 * generates a diff information, and compile a HTML report.
 */
public class SeleniumChronosDiffTest {

    private final Logger logger = LoggerFactory.getLogger(SeleniumChronosDiffTest.class);
    private Path testClassOutputDir;
    private WebDriver driver;
    private WebDriverFormulas wdf;

    @BeforeAll
    static void setupClass() { WebDriverManager.chromedriver().setup(); }

    @BeforeEach
    public void setup() {
        testClassOutputDir = TestHelper.createTestClassOutputDir(SeleniumChronosDiffTest.class);
        //
        ChromeOptions opt = new ChromeOptions();
        opt.addArguments("headless");
        driver = new ChromeDriver(opt);
        driver.manage().window().setSize(new Dimension(1024, 1000));
        // we will implicitly wait 5 seconds for the new page to load
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        //
        wdf = new WebDriverFormulas();
    }

    @AfterEach
    public void tearDown() { driver.quit(); }

    @Test
    public void performChronosDiff() throws InspectusException {
        try {
            Parameters parameters = new Parameters.Builder()
                    .store(Stores.newInstance(testClassOutputDir.resolve("store")))
                    .backup(Stores.newInstance(testClassOutputDir.resolve("store-backup")))
                    .jobName(new JobName("performChronosDiff"))
                    .jobTimestamp(JobTimestamp.now())
                    .sortKeys(new SortKeys("step"))
                    .threshold(1.0)
                    .build();
            Inspectus chronosDiff = new FnChronosDiff(fn);
            chronosDiff.execute(parameters);
        } catch (InspectusException e) {
            if (e.getMessage().contains("previousMaterialList of size == 0")) {
                logger.warn("chronosDiff.execute() failed because the backup-store was empty. Will try again.");
                // do it once more with new JobTimestamp
                Parameters parameters = new Parameters.Builder()
                        .store(Stores.newInstance(testClassOutputDir.resolve("store")))
                        .backup(Stores.newInstance(testClassOutputDir.resolve("store-backup")))
                        .jobName(new JobName("performChronosDiff"))
                        .jobTimestamp(JobTimestamp.now())
                        .sortKeys(new SortKeys("step"))
                        .threshold(1.0)
                        .build();
                Inspectus chronosDiff = new FnChronosDiff(fn);
                chronosDiff.execute(parameters);
            }
        }
    }

    /*
     * visit the pages, take screenshot and HTML sources, save the materials into the store.
     * invoked by FnChronosDiff.execute() internally.
     */
    private final BiFunction<Parameters, Intermediates, Intermediates> fn = (parameters, intermediates) -> {
        try {
            Store store = parameters.getStore();
            JobName jobName = parameters.getJobName();
            JobTimestamp jobTimestamp = parameters.getJobTimestamp();
            WebPageMaterializingFunctions functions =
                    new WebPageMaterializingFunctions(store, jobName, jobTimestamp);

            // step1: Top page
            Handle anchorMakeAppointment =
                    new Handle(By.xpath("//a[@id='btn-make-appointment']"));
            Target topPage =
                    Target.builder("http://demoaut-mimic.kazurayam.com")
                            .handle(anchorMakeAppointment)
                            .build();
            wdf.navigateTo(driver, topPage.getUrl(), topPage.getHandle().getBy(), 10);
            // take a screenshot, save the HTML source
            takeScreenshotAndHTMLSource(functions, driver, topPage,
                    Collections.singletonMap("step", "01"));
            // we navigate to the next page (login) with wait for the next page to load
            wdf.navigateByClick(driver, anchorMakeAppointment.getBy(),
                    By.xpath("//input[@id='txt-username']"), 10);

            // step2: Login page
            By inputUsername = By.xpath("//input[@id='txt-username']");
            By inputPassword = By.xpath("//input[@id='txt-password']");
            By buttonLogin = By.xpath("//button[@id='btn-login']");
            Target loginPage =
                    Target.builder(driver.getCurrentUrl())
                            .handle(new Handle(buttonLogin)).build();
            String username = "John Doe";
            String password = "ThisIsNotAPassword";
            driver.findElement(inputUsername).sendKeys(username);
            driver.findElement(inputPassword).sendKeys(password);
            takeScreenshotAndHTMLSource(functions, driver, loginPage,
                    Collections.singletonMap("step", "02"));
            // we will navigate to the appointment page by
            wdf.navigateByClick(driver, buttonLogin,
                    By.xpath("//select[@id='combo_facility']"), 10);

            // step3: Appointment page
            By comboFacility = By.xpath("//select[@id='combo_facility']");
            By inputChk = By.xpath("//input[@id='chk_hospotal_readmission']");
            By inputRadio = By.xpath("//input[@id='radio_program_medicaid']");
            By inputVisitDate = By.xpath("//input[@id = 'txt_visit_date']");
            By textareaComment = By.xpath("//textarea[@id='txt_comment']");
            By buttonBookAppointment = By.xpath("//button[@id='btn-book-appointment']");
            Target appointmentPage =
                    Target.builder(driver.getCurrentUrl())
                            .handle(new Handle(buttonBookAppointment)).build();
            Select selectFacility = new Select(driver.findElement(comboFacility));
            selectFacility.selectByValue("Hongkong CURA Healthcare Center");
            driver.findElement(inputChk).click();
            driver.findElement(inputRadio).click();
            // set the same day in the next week
            LocalDateTime visitDate = LocalDateTime.now().plusWeeks(1);
            String visitDateStr = DateTimeFormatter.ofPattern("dd/MM/yyyy").format(visitDate);
            driver.findElement(inputVisitDate).sendKeys(visitDateStr);
            driver.findElement(inputVisitDate).sendKeys(Keys.chord(Keys.ENTER));
            driver.findElement(textareaComment).sendKeys("this is a comment");
            takeScreenshotAndHTMLSource(functions, driver, appointmentPage,
                    Collections.singletonMap("step", "03"));
            // we navigate to the summary page by
            wdf.navigateByClick(driver, buttonBookAppointment,
                    By.xpath("//a[text()='Go to Homepage']"), 10);

            // Step4: Summary page
            Handle anchorGoHome = new Handle(By.xpath("//a[text()='Go to Homepage']"));
            Target summaryPage =
                    Target.builder(driver.getCurrentUrl()).handle(anchorGoHome).build();
            takeScreenshotAndHTMLSource(functions, driver, summaryPage,
                    Collections.singletonMap("step", "04"));
            // we navigate to the Home
            wdf.navigateByClick(driver, anchorGoHome.getBy(),
                    By.xpath("//a[@id='btn-make-appointment']"), 10);

        } catch (Exception e) {
            throw new UncheckedInspectusException(e);
        }
        return new Intermediates.Builder(intermediates).build();
    };

    private void takeScreenshotAndHTMLSource(WebPageMaterializingFunctions functions,
                                             WebDriver driver,
                                             Target target,
                                             Map<String, String> attributes)
            throws MaterialstoreException {
        // take screenshot, save the image into the store
        functions.storeEntirePageScreenshot.accept(driver, target, attributes);
        // take HTML source, save the text into the store
        functions.storeHTMLSource.accept(driver, target, attributes);
    }
}
