package com.kazurayam.inspectus.selenium4sample;

import com.kazurayam.materialstore.core.filesystem.FileType;
import com.kazurayam.materialstore.core.filesystem.JobName;
import com.kazurayam.materialstore.core.filesystem.JobTimestamp;
import com.kazurayam.materialstore.core.filesystem.Material;
import com.kazurayam.materialstore.core.filesystem.MaterialstoreException;
import com.kazurayam.materialstore.core.filesystem.Metadata;
import com.kazurayam.materialstore.core.filesystem.Store;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
public class MaterializeUtils {

    /**
     * take screenshot of web pages using selenium API,
     * store the image into the store
     */
    public static Material takePageScreenshotSaveIntoStore(
            WebDriver driver,
            Store store, JobName jobName, JobTimestamp jobTimestamp, Metadata md) {
        try {
            TakesScreenshot shooter = (TakesScreenshot) driver;
            byte[] bytes = shooter.getScreenshotAs(OutputType.BYTES);
            return store.write(jobName, jobTimestamp, FileType.PNG, md, bytes);
        } catch (MaterialstoreException e) {
            throw new RuntimeException(e);
        }
    }
}
