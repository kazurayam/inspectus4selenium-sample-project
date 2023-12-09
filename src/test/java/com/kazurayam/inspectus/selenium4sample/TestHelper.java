package com.kazurayam.inspectus.selenium4sample;

import com.kazurayam.inspectus.core.UncheckedInspectusException;
import com.kazurayam.materialstore.util.CopyDir;
import com.kazurayam.materialstore.util.DeleteDir;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;

public class TestHelper {

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
    public static Path createTestClassOutputDir(Class clazz) {
        Objects.requireNonNull(clazz);
        Path output = getTestOutputDir().resolve(clazz.getName());
        try {
            if (!Files.exists(output)) {
                Files.createDirectories(output);
            }
        } catch (IOException e) {
            throw new UncheckedInspectusException(e);
        }
        return output;
    }

    public static URL makeURL(String urlStr) {
        try {
            return new URL(urlStr);
        } catch (MalformedURLException e) {
            throw new UncheckedInspectusException(e);
        }
    }

}