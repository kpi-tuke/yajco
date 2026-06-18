---
icon: lucide/download
---

# Download #

## Maven ##
YAJCo tool uses Maven for including into Java projects, so **you don't need to download any archives - Maven does that for you**. You only need to specify correct dependency for your project in `pom.xml`.

These are dependencies needed for YAJCo annotation processor to work and generate Beaver parser.

```xml
<dependencies>
    <dependency>
        <groupId>sk.tuke.yajco</groupId>
        <artifactId>yajco-annotation-processor</artifactId>
        <version>0.6.0</version>
    </dependency>
    <dependency>
        <groupId>sk.tuke.yajco</groupId>
        <artifactId>yajco-beaver-parser-generator-module</artifactId>
        <version>0.6.0</version>
    </dependency>
</dependencies>
```

If you prefer JavaCC parser generator use:
```xml
    <dependency>
        <groupId>sk.tuke.yajco</groupId>
        <artifactId>yajco-javacc-parser-generator-module</artifactId>
        <version>0.6.0</version>
    </dependency>
```
instead of `yajco-beaver-parser-generator-module`.

## Source code ##
Source code of all project is accessible on [GitHub](https://github.com/kpi-tuke/yajco).
