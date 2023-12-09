package com.kazurayam.inspectus.selenium4sample;

import com.kazurayam.unittest.TestOutputOrganizer;

public class TestOutputOrganizerFactory {

    public static TestOutputOrganizer create(Class<?> clazz) {
        return new TestOutputOrganizer.Builder(clazz)
                .outputDirPath("build/tmp/testOutput")
                .subDirPath(clazz)
                .build();
    }
}
