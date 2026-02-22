# YAJCO IR Generator Module

This module exports YAJCO language metadata to shared DSL IR JSON.

Enable generation with parser option:

```java
@Parser(options = {
  @Option(name = "yajco.generateTools", value = "ir")
})
```

Optional output filename:

- `yajco.ir.file=my-language.ir.json`
- `yajco.ir.languageName=my-language`
- `yajco.ir.fileExtensions=karel,kr`

Default output: `<resolved-language-name>.ir.json` where language name uses `yajco.ir.languageName` when provided, otherwise YAJCO language name.
