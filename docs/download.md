---
icon: lucide/download
---

# Download

YAJCo project is split into multiple modules, so it is recommended to use a build tool with automatic dependency management, such as Maven or Gradle.

The main modules needed for using YAJCo are:

1. `yajco-annotations` – definitions of YAJCo annotations that you can use in your langueage definition
2. `yajco-annotation-processor` – language processor generation logic
3. One of the supported parser generator modules:
     * `yajco-beaver-parser-generator-module` for [Beaver LALR Parser Generator](https://beaver.sourceforge.net/)
     * `yajco-antlr4-parser-generator-module` for [ANTLR4](https://www.antlr.org/)
     * `yajco-javacc-parser-generator-module` for [JavaCC](http://javacc.org)

The Beaver backend module has currently the best support for YAJCo features, and as a LALR parser generator, it supports a wide range of languages.

!!! note
    The `yajco-annotation-processor` module is needed only for annotation processing, but Beaver and ANTLR4 parser generator modules are needed for both annotation processing and runtime, because they provide runtime dependencies for the generated parser classes.


## Maven

To include YAJCo in your Maven project, configure dependencies as follows:

```xml
<properties>
  <yajco.version>0.6.0</yajco.version>
</properties>

<dependencies>
  <dependency>
    <groupId>sk.tuke.yajco</groupId>
    <artifactId>yajco-annotations</artifactId>
    <version>${yajco.version}</version>
  </dependency>
  <dependency>
    <groupId>sk.tuke.yajco</groupId>
    <!-- Selected parser generator module -->
    <artifactId>yajco-beaver-parser-generator-module</artifactId>
    <version>${yajco.version}</version>
  </dependency>
</dependencies>

<build>
<plugins>
  <plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.13.0</version>
    <configuration>
      <annotationProcessorPaths>
        <path>
          <groupId>sk.tuke.yajco</groupId>
          <artifactId>yajco-annotation-processor</artifactId>
          <version>${yajco.version}</version>
        </path>
        <path>
          <groupId>sk.tuke.yajco</groupId>
          <!-- Selected parser generator module -->
          <artifactId>yajco-beaver-parser-generator-module</artifactId>
          <version>${yajco.version}</version>
        </path>
      </annotationProcessorPaths>
    </configuration>
  </plugin>
</plugins>
</build>
```

## Gradle

To include YAJCo in your Gradle project, use the following dependencies:

```kotlin
val yajcoVersion = "0.6.0"

repositories {
    mavenCentral()
}

dependencies {
  implementation("sk.tuke.yajco:yajco-annotations:${yajcoVersion}")
  implementation("sk.tuke.yajco:yajco-beaver-parser-generator-module:${yajcoVersion}")
  annotationProcessor("sk.tuke.yajco:yajco-annotation-processor:$yajcoVersion")
  annotationProcessor("sk.tuke.yajco:yajco-beaver-parser-generator-module:${yajcoVersion}")
}
```


## Source code ##
Source code of all projects is accessible on [GitHub](https://github.com/kpi-tuke/yajco).
