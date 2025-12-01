# Getting Started

You can start using the YAJCo parser generator tool by following the steps described below.  
If you prefer to explore more complex examples, see
the [yajco-examples project](https://github.com/kpi-tuke/yajco-examples).

---

## Maven Builds

**The recommended way to use YAJCo is within a Maven project.**  
YAJCo consists of multiple modules and dependencies, which are managed through Maven.  
If you’re new to Maven, you can [download and install it here](http://maven.apache.org/) and read its basic
documentation.

---

## Creating a Java Maven Project

Next, create a simple Maven project. You can do this from your IDE or using the following command:

```bash
mvn archetype:generate \
  -DgroupId=sk.tuke.yajco.example \
  -DartifactId=yajco-example \
  -DarchetypeArtifactId=maven-archetype-quickstart \
  -DinteractiveMode=false
```

This command creates a basic Java Maven project using the `maven-archetype-quickstart` template.


---

## Configuring Dependencies

Ensure your project uses at least **JDK 11** by including the following properties in your `pom.xml`:

```xml
<properties>
  <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
  <maven.compiler.release>11</maven.compiler.release>
  <yajco.version>0.6.0</yajco.version>
</properties>
```

This also sets the YAJCo version.

Add the following **YAJCo** dependencies to your project’s `pom.xml`:

```xml
<dependencies>
  <dependency>
    <groupId>sk.tuke.yajco</groupId>
    <artifactId>yajco-annotations</artifactId>
    <version>${yajco.version}</version>
  </dependency>
  <dependency>
    <groupId>sk.tuke.yajco</groupId>
    <artifactId>yajco-beaver-parser-generator-module</artifactId>
    <version>${yajco.version}</version>
  </dependency>
  <dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.13.1</version>
    <scope>test</scope>
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
            <artifactId>yajco-beaver-parser-generator-module</artifactId>
            <version>${yajco.version}</version>
          </path>
        </annotationProcessorPaths>
      </configuration>
    </plugin>
  </plugins>
</build>
```

---

## Creating a Language Model

Let’s define a very simple YAJCo language.

Our language will recognize the keyword `id` followed by an identifier made up of lowercase Latin letters.

A valid input sentence would look like this:

```
id superman
```

This language has one concept, `SimpleIdentifier`, represented as a Java class.
The root concept is marked with the `@Parser` annotation. Each constructor defines a syntax rule,
and annotations such as `@Before` and `@TokenDef` describe keywords and tokens.

```java
package mylang;

import yajco.annotation.*;
import yajco.annotation.config.*;

@Parser(tokens = @TokenDef(name = "ident", regexp = "[a-z]+"))
public class SimpleIdentifier {

    private String identifier;

    @Before("id")
    public SimpleIdentifier(String ident) {
        identifier = ident;
    }

    public String getIdentifier() {
        return identifier;
    }
}
```

The `getIdentifier()` method provides access to the parsed identifier name.

After creating the project with this class, build it using Maven:

```bash
mvn package
```

This will generate your parser automatically.
You can find the generated sources in `target/generated-sources/annotations`.

---

## Running the Parser

Now let’s create a small program that uses the generated parser to read an input string.

```java
import mylang.SimpleIdentifier;
import mylang.parser.*;

public class Main {
    public static void main(String[] args) throws ParseException {
        String input = "id superman";
        System.out.println("Going to parse: '" + input + "'");

        SimpleIdentifier identifier = new LALRSimpleIdentifierParser().parse(input);
        System.out.println("Parsed identifier: " + identifier.getIdentifier());
    }
}
```

Run the example using Maven:

```bash
mvn exec:java -Dexec.mainClass="Main"
```

You should see the following output:

```
Going to parse: 'id superman'
Parsed identifier: superman
```

---

### Congratulations! 

You have now created your first simple language using only plain Java classes and YAJCo.

You can download
the [complete source code of this example](https://github.com/kpi-tuke/yajco-examples/archive/master.zip)
(see the `getting-started` directory inside the ZIP)
or explore [more complex examples](https://github.com/kpi-tuke/yajco-examples).
