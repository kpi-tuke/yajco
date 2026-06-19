# YAJCo – a model-based parser generator for Java

**YAJCo** (Yet Another Java Compiler compiler) is a language parser generator that uses an annotated object-oriented model as a basis for generating parsers.

Language development doesn't have to start with a grammar. YAJCo allows you to define a model of the language using object-oriented techniques and plain Java or Kotlin classes. The model represents the language structure and can be used to further process it. Next, add concrete syntax annotations to the model classes, and YAJCo will generate a parser for you that would turn text into an object graph.

Read the documentation for [getting started](https://kpi-tuke.github.io/yajco/getting-started/) or check our [examples](https://github.com/kpi-tuke/yajco-examples).

---

## YAJCo Development

To build and use development versions of YAJCo, clone the repository and run:

```bash
mvn clean install
```

Then use current `SNAPSHOT` versions in your Maven dependencies.

Documentation is built using [Zensical](https://zensical.org/). We suggest using [uv](https://docs.astral.sh/uv/) for running it locally:

```bash
uvx zensical serve
```
