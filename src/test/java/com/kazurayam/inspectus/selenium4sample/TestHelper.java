package com.kazurayam.inspectus.selenium4sample;


import com.kazurayam.ashotwrapper.AShotWrapper;
import com.kazurayam.materialstore.core.filesystem.FileType;
import com.kazurayam.materialstore.core.filesystem.JobName;
import com.kazurayam.materialstore.core.filesystem.JobTimestamp;
import com.kazurayam.materialstore.core.filesystem.Material;
import com.kazurayam.materialstore.core.filesystem.MaterialstoreException;
import com.kazurayam.materialstore.core.filesystem.Metadata;
import com.kazurayam.materialstore.core.filesystem.Store;
import com.kazurayam.materialstore.core.util.CopyDir;
import com.kazurayam.materialstore.core.util.DeleteDir;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestHelper {

    private static final Logger logger = LoggerFactory.getLogger(TestHelper.class);
    private static final Path currentWorkingDir;
    private static final Path testOutputDir;
    private static final Path fixturesDir;

    static {
        currentWorkingDir = Paths.get(System.getProperty("user.dir"));
        testOutputDir = currentWorkingDir.resolve("build/tmp/testOutput");
        fixturesDir = currentWorkingDir.resolve("src/test/fixtures");
    }

    public static Path getCurrentWorkingDirectory() {
        return currentWorkingDir;
    }

    public static Path getCWD() {
        return getCurrentWorkingDirectory();
    }

    public static Path getFixturesDirectory() {
        return fixturesDir;
    }

    public static Path getTestOutputDir() {
        return testOutputDir;
    }

    /**
     * Create dir if not exits.
     * Delete dir if already exits, and recreate it.
     * @param dir
     * @return
     * @throws IOException
     */
    public static Path initializeDirectory(Path dir) throws IOException {
        if (Files.exists(dir)) {
            DeleteDir.deleteDirectoryRecursively(dir);
        }
        Files.createDirectories(dir);
        return dir;
    }

    public static void copyDirectory(Path sourceDir, Path targetDir) throws IOException {
        Objects.requireNonNull(sourceDir);
        Objects.requireNonNull(targetDir);
        if (!Files.exists(sourceDir)) {
            throw new IOException(String.format("%s does not exist", sourceDir));
        }
        if (!Files.isDirectory(sourceDir)) {
            throw new IOException(String.format("%s is not a directory", sourceDir));
        }
        if (!Files.exists(targetDir.getParent())) {
            Files.createDirectories(targetDir.getParent());
        }
        Files.walkFileTree(sourceDir, new CopyDir(sourceDir, targetDir));
    }

    /**
     * create the out directory for the testCase object to write output files
     */
    public static Path createTestClassOutputDir(Object testCase) {
        Path output = getTestOutputDir()
                .resolve(testCase.getClass().getName());
        try {
            if (!Files.exists(output)) {
                Files.createDirectories(output);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return output;
    }

    public static URL makeURL(String urlStr) {
        try {
            return new URL(urlStr);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static Material takePageScreenshotSaveIntoStore(
            WebDriver driver,
            Store store, JobName jobName, JobTimestamp jobTimestamp, Metadata md) {
        try {
            /*
            BufferedImage bi = AShotWrapper.takeEntirePageImage(driver,
                    new AShotWrapper.Options.Builder().build());
            assertNotNull(bi);
            Material mt = store.write(jobName, jobTimestamp, FileType.PNG, md, bi);
            return mt;
             */
            TakesScreenshot shooter = (TakesScreenshot)driver;
            byte[] bytes = shooter.getScreenshotAs(OutputType.BYTES);
            Material mt = store.write(jobName, jobTimestamp, FileType.PNG, md, bytes);
            return mt;
        } catch (MaterialstoreException e) {
            throw new RuntimeException(e);
        }
    }

}