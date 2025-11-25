# @Skip Annotation Usage Guide

The `@Skip` annotation in YAJCo allows you to define patterns that the lexical analyzer should skip during tokenization, such as whitespace and comments.

## Parameters

- `value`: Direct regular expression (highest priority)
- `whitespace`: Boolean flag for standard whitespace skipping
- `start`: Starting characters for comments
- `end`: Optional ending characters for multi-line comments

## Usage Examples

### 1. Using Direct Regular Expression (Original Behavior)

```java
@Parser(
    mainNode = "com.example.MyLanguage",
    skips = {
        @Skip("\\s+"),           // Skip whitespace
        @Skip("//.*"),           // Skip single-line comments
        @Skip("/\\*[\\s\\S]*?\\*/")  // Skip multi-line comments
    }
)
```

### 2. Using Whitespace Flag (Syntactic Sugar)

```java
@Parser(
    mainNode = "com.example.MyLanguage",
    skips = {
        @Skip(whitespace = true)  // Equivalent to: @Skip("\\s")
    }
)
```

### 3. Using Start for Single-Line Comments

```java
@Parser(
    mainNode = "com.example.MyLanguage",
    skips = {
        @Skip(start = "//"),      // Equivalent to: @Skip("//.*")
        @Skip(start = "#"),       // Equivalent to: @Skip("#.*")
        @Skip(start = "--")       // Equivalent to: @Skip("--.*")
    }
)
```

### 4. Using Start and End for Multi-Line Comments

```java
@Parser(
    mainNode = "com.example.MyLanguage",
    skips = {
        @Skip(start = "/*", end = "*/"),           // C-style comments
        @Skip(start = "<!--", end = "-->"),        // XML comments
        @Skip(start = "{-", end = "-}")            // Haskell-style comments
    }
)
```

### 5. Combined Example

```java
@Parser(
    mainNode = "com.example.MyLanguage",
    skips = {
        @Skip(whitespace = true),          // Skip all whitespace
        @Skip(start = "//"),               // Skip C++ style single-line comments
        @Skip(start = "/*", end = "*/"),   // Skip C-style multi-line comments
        @Skip(value = "@rem.*")            // Custom pattern for batch file comments
    }
)
```

## Priority Rules

The annotation processor applies the following priority when converting `@Skip` to regular expressions:

1. **`value`** - If set, this regex is used directly (all other parameters ignored)
2. **`whitespace`** - If true, generates `\s` pattern
3. **`start` + `end`** - If both set, generates multi-line comment pattern
4. **`start`** only - If set without `end`, generates single-line comment pattern

## Generated Regular Expressions

The syntactic sugar parameters are converted to regular expressions as follows:

| Parameters | Generated Regex | Description |
|------------|----------------|-------------|
| `whitespace = true` | `\s` | Matches any whitespace character |
| `start = "//"` | `//.*` | Matches from `//` to end of line |
| `start = "/*", end = "*/"` | `/\*(?:(?!\*/)[\\s\\S])*\*/` | Matches from `/*` to `*/` (including newlines) |

## Special Character Escaping

When using `start` and `end` parameters, special regex characters are automatically escaped. For example:

```java
@Skip(start = "/*", end = "*/")
```

The `*` characters are automatically escaped in the generated regex, so you don't need to worry about regex metacharacters.

## Notes

- If no `@Skip` annotations are provided, YAJCo automatically adds a default whitespace skip pattern (`\s`)
- The `value` parameter gives you full control with raw regex for complex patterns
- The `start`/`end` parameters provide a simpler syntax for common comment styles
- You can mix both approaches in the same `@Parser` annotation
