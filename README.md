# inspectus4selenium-sample-project

## What is this?

This project demonstrates how to use a set of Java libraries that I developed.

- [materialstore](https://github.com/kazurayam/materialstore) downloadable at [](https://mvnrepository.com/artifact/com.kazurayam/materialstore)
- [inspectus](https://github.com/kazurayam/inspectus) downloadable at [Maven Central](https://mvnrepository.com/artifact/com.kazurayam/inspectus)

and more. These libraries require you to have the following installed on you machine.

- Java 8 or newer
- Gradle v7.6 or newer

This has a lot of external dependencies (Selenium WebDriver 4, etc) that will be automatically resolved by Gradle as specified in the [build.gradle](https://github.com/kazurayam/inspectus4selenium-sample-project/blob/develop/build.gradle) file.

The sample codes in this project shows you how to:

1. take screenshot of web pages and HTML elements.
2. save the images into an object-oriented database named *materialstore*.
3. retrieve the images from the materialstore database, compare 2 to generate a diff image.
4. compile a HTML report that shows the bulk of screenshot images and diff images associated with metadata in a well-organized format.

### Apple Twins Diff

Here I would show you how to take screenshots of HTML elements and compare to find differences.

I will use the following 2 websites as testbed.

I would use the following 2 URLs as testbed:

- https://kazurayam.github.io/myApple/
- https://kazurayam.github.io/myApple-alt/

Please visit these 2 sites and have a look. You would find them quite similar. The pages show a variations of an apple image, like this:

![Apple I bit](https://kazurayam.github.io/inspectus4selenium-sample-project/images/Apple_I_bit.png)


There are small differences in the pages --- the apple is transformed: resized, rotated. I want to compare these silightly different apple images in this pair of web sites programatically.



You want to execute:

```
$ cd <inspectus4selenium-sample-project>
$ ./gradlew test --tests=*AppleTwinsDiff*
```

then the test will create the following output:

```
$ tree -L 3 ./build/tmp/testOutput/
./build/tmp/testOutput/
└── com.kazurayam.inspectus.selenium4sample.AppleTwinsDiffTest
    └── store
        ├── index.html
        ├── myApple
        └── myApple-20231210_213728.html
```

You can open the `myApple-yyyyMMdd_hhmmss.html` with any browser you like.

Here I stored a shot of test output for demonstration.

- [demo output](https://kazurayam.github.io/inspectus4selenium-sample-project/demo/store/myApple-20231210_213728.html)

At first, you would see the list of comparisons in an accordion format.

![Top](https://kazurayam.github.io/inspectus4selenium-sample-project/images/AppleTwinsDiff_top.png)

Please click the button labelled **Show Diff in Modal**. Then you will see the following Diff page:

![Diff](https://kazurayam.github.io/inspectus4selenium-sample-project/images/AppleTwinsDiff_diff.png)

This page shows a "carousel". By clicking the left side or the right side of the page you can slide to the Left and the Right:

![Left](https://kazurayam.github.io/inspectus4selenium-sample-project/images/AppleTwinsDiff_left.png)

![Right](https://kazurayam.github.io/inspectus4katalon-sample-project/images/AppleTwinsDiff_right.png)

The left apple and the right apple look similar but different. Can you see how? Yes, it's rotated. diff image shows the different pixels painted in red.
