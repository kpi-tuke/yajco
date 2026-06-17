# YAJCo IR Generator Module

Generates an IR JSON file during annotation processing. The IR captures the
language model (concepts, tokens, operators, syntax) for consumption by
dsl-tooling.

## Setup

Add the module to `annotationProcessorPaths` in `maven-compiler-plugin`:

```xml
<path>
  <groupId>sk.tuke.yajco</groupId>
  <artifactId>yajco-ir-generator-module</artifactId>
  <version>${yajco.version}</version>
</path>
```

Then enable IR generation via compiler arguments:

```xml
<compilerArgs>
  <arg>-Ayajco.generateTools=ir</arg>
  <arg>-Ayajco.ir.languageName=my-language</arg>
  <arg>-Ayajco.ir.fileExtensions=.ml</arg>
</compilerArgs>
```

See `yajco-examples` for complete `pom.xml` configurations.

## Compiler Arguments

| Argument | Required | Description |
|---|---|---|
| `yajco.generateTools=ir` | Yes | Enables IR generation. Use `all` to also run other generators. |
| `yajco.ir.languageName=<name>` | No | Output filename. Defaults to the YAJCo language name. |
| `yajco.ir.fileExtensions=<exts>` | No | Comma-separated file extensions (e.g. `.karel,.kr`). |
| `yajco.ir.file=<filename>` | No | Explicit output filename (e.g. `my-language.ir.json`). |

These can also be set via `@Parser` annotation options:

```java
@Parser(options = {
  @Option(name = "yajco.generateTools", value = "ir"),
  @Option(name = "yajco.ir.languageName", value = "my-language"),
  @Option(name = "yajco.ir.fileExtensions", value = ".ml")
})
```

## Output

After `mvn compile`, the IR file appears at:

```
target/generated-sources/annotations/<language-name>.ir.json
```
