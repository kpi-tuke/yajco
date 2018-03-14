# Getting started #

You can start using YAJCo parser generator tool by following the steps descibed below. If you prefer to use existing more complex examples follow instructions in the [examples page](https://github.com/kpi-tuke/yajco/wiki/Examples).

## Maven builds ##

**Recommended way of using YAJCo is within Maven project.** YAJCo tool consists of plenty modules and dependencies, which can be bothersome to include in project otherwise. If you have not used Maven builds in Java, you need to [download and install Maven](http://maven.apache.org/) and we recommend to get to know a little about how Maven works.

### Prepare Java Maven project ###
Create simple Java Maven project. You can use following command or use any of your prefered IDE.
```bash
mvn archetype:create 
  -DgroupId=sk.tuke.yajco.example
  -DartifactId=yourExampleName
  -DarchetypeArtifactId=maven-archetype-quickstart
```
Add following maven dependencies to project `pom.xml`
- `sk.tuke.yajco:yajco-annotation-processor:0.5.9`
- `sk.tuke.yajco:yajco-beaver-parser-generator-module:0.5.9`
```xml
<dependencies>
    <dependency>
        <groupId>sk.tuke.yajco</groupId>
        <artifactId>yajco-annotation-processor</artifactId>
        <version>0.5.9</version>
    </dependency>
    <dependency>
        <groupId>sk.tuke.yajco</groupId>
        <artifactId>yajco-beaver-parser-generator-module</artifactId>
        <version>0.5.9</version>
    </dependency>
</dependencies>
```
Make sure project uses at least Java 1.6 for compilation. File `pom.xml` should contain following.
```xml
<properties>
    <maven.compiler.source>1.6</maven.compiler.source>
    <maven.compiler.target>1.6</maven.compiler.target>
</properties>
```

### Create a language model ###
The following class defines a very simple YAJCo language specification. We will create a language, which starts with `id` keyword followed by identifier consisting of small latin characters. This is a valid language sentence:

`id superman`

Language will consists of one language concept called **`SimpleIdentifier`**. We will implement it as a Java class with the same name. In order to specify main (root) concept of language, it is needed to mark it with **`@Parser`** annotation. Each constructor represents concrete syntax of a language. Annotation `@Before` serves for specifying a keyword `id` as a required word before identifier. Identifier consists of small latin characters, which is specified by regular expression inside **`@TokenDef`** annotation defined inside `@Parser` annotation. Parameter name is automatically maped to corresponding `TokenDef` name.
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
We have created **`getIdentifier()`** method for easy access to identifier name, which we will use later. After creating project with **`SimpleIdentifier`** class you have succesfully specified your new language. Just build it with Maven:
```bash
mvn package
```
and you get your parser generated instantly. You can check directory `target/generated-sources/annotations` in your project directory for generated parser.

### Running parser ###
It's not fun having a parser such promtly and not being able to use it. Let's create a main class for actual parsing of some textual input. Generated parser is accessible through class **`LALRSimpleIdentifierParser`**, as it has default name created using root concept name (`SimpleIdentifier`).

```java
import mylang.SimpleIdentifier;
import mylang.parser.*;

public class MainClass {

    public static void main(String[] args) throws ParseException {
        String input = "id superman";
        System.out.println("Going to parse: '"+input+"'");

        LALRSimpleIdentifierParser parser = new LALRSimpleIdentifierParser();
        SimpleIdentifier simpleIdentifier = parser.parse(input);

        System.out.println("Parsed identifier: "+simpleIdentifier.getIdentifier());
    }
}
```

It is possible to finally run the example using maven command:

```bash
mvn exec:java -Dexec.mainClass="MainClass"
```

You should be able to see results in standard output:

```
Going to parse: 'id superman'
Parsed identifier: superman
```

_**Congratulations**, you have created your first simple language using nothing else, just plain Java classes._

You can download a [complete source code of getting started example](https://github.com/kpi-tuke/yajco/wiki/examples/yajco_examples.zip) (look in `yajco-example-gettingStarted` directory inside zip) and you can have a look at a [more complex examples](https://github.com/kpi-tuke/yajco/wiki/Examples).
