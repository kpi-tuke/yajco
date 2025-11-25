# Automatic IDENTIFIER Token for @Identifier and @References

YAJCo now automatically adds an `IDENTIFIER` token type for properties and parameters annotated with `@Identifier` or `@References`, eliminating the need to manually add `@Token("IDENTIFIER")` annotations.

## Overview

When you mark a field with `@Identifier` or a parameter with `@References`, the annotation processor will:

1. Automatically create an `IDENTIFIER` token in the lexical analyzer (if it doesn't exist)
2. Automatically apply `@Token("IDENTIFIER")` to the notation part that uses this field/parameter
3. Use the default identifier regex pattern: `[a-zA-Z_][a-zA-Z0-9_]*`

## Default IDENTIFIER Token

The automatically created `IDENTIFIER` token has the following definition:

```java
TokenDef("IDENTIFIER", "[a-zA-Z_][a-zA-Z0-9_]*")
```

This matches typical programming language identifiers:
- Starts with a letter (a-z, A-Z) or underscore (_)
- Followed by zero or more letters, digits (0-9), or underscores

## Usage Examples

### Before (Manual Token Assignment)

Previously, you had to explicitly add `@Token("IDENTIFIER")` to parameters:

```java
public class Concept {
    @Identifier
    private String name;
}

public class ConceptUsage {
    public ConceptUsage(
        @References(Concept.class) @Token("IDENTIFIER") String conceptName
    ) {
        this.conceptName = conceptName;
    }
}
```

### After (Automatic Token Assignment)

Now, the `@Token("IDENTIFIER")` annotation is added automatically:

```java
public class Concept {
    @Identifier
    private String name;
}

public class ConceptUsage {
    public ConceptUsage(
        @References(Concept.class) String conceptName  // @Token("IDENTIFIER") added automatically
    ) {
        this.conceptName = conceptName;
    }
}
```

## How It Works

### For @References Parameters

When a parameter is annotated with `@References`:

```java
public MyClass(
    @References(SomeClass.class) String identifier
) { ... }
```

The processor automatically:
1. Ensures the `IDENTIFIER` token exists in the language definition
2. Adds `@Token("IDENTIFIER")` pattern to the notation part

### For @Identifier Properties

When a field is annotated with `@Identifier` and used in a constructor/factory parameter:

```java
public class Concept {
    @Identifier
    private String name;

    public Concept(String name) {  // Parameter 'name' references property 'name'
        this.name = name;
    }
}
```

The processor automatically:
1. Detects that the property `name` has `@Identifier` pattern
2. Ensures the `IDENTIFIER` token exists
3. Adds `@Token("IDENTIFIER")` to the notation part for parameter `name`

## Explicit Token Override

You can still explicitly specify a different token if needed:

```java
public class Concept {
    @Identifier
    private String name;

    public Concept(
        @Token("CUSTOM_NAME_TOKEN") String name  // Explicit token overrides automatic IDENTIFIER
    ) {
        this.name = name;
    }
}
```

The explicit `@Token` annotation takes precedence over the automatic IDENTIFIER token.

## Priority Rules

The annotation processor applies tokens in this priority order:

1. **Explicit `@Token`** - If present, uses the specified token name
2. **`@StringToken`** - If present, creates a STRING_TOKEN_N and uses it
3. **`@References`** - Automatically adds IDENTIFIER token
4. **Property with `@Identifier`** - Automatically adds IDENTIFIER token if the property has @Identifier pattern

## Custom IDENTIFIER Token

If you want to customize the IDENTIFIER token pattern, you can define it explicitly in your `@Parser` annotation:

```java
@Parser(
    mainNode = "com.example.MyLanguage",
    tokens = {
        @TokenDef(name = "IDENTIFIER", regexp = "[a-z][a-zA-Z0-9]*")  // Custom: lowercase first letter
    }
)
```

When the processor detects an existing `IDENTIFIER` token in the language definition, it will use that instead of creating the default one.

## Complete Example

```java
@Parser(
    mainNode = "com.example.expressions.Program",
    skips = {
        @Skip(whitespace = true),
        @Skip(start = "//")
    }
    // No need to define IDENTIFIER token - it's added automatically!
)
package com.example.expressions;

public class Variable {
    @Identifier
    private String name;

    // No @Token needed - automatically added because 'name' has @Identifier
    public Variable(String name) {
        this.name = name;
    }
}

public class VariableReference {
    private String varName;

    // No @Token needed - automatically added because of @References
    public VariableReference(
        @References(Variable.class) String varName
    ) {
        this.varName = varName;
    }
}
```

## Benefits

1. **Less Boilerplate**: No need to repeatedly add `@Token("IDENTIFIER")` annotations
2. **Consistency**: All identifiers use the same token automatically
3. **Automatic Token Creation**: The IDENTIFIER token is created in the lexical analyzer only when needed
4. **Cleaner Code**: Language definitions are more concise and readable
5. **Backward Compatible**: Explicit `@Token` annotations still work and take precedence

## Implementation Details

- **Token Name**: `IDENTIFIER`
- **Default Regex**: `[a-zA-Z_][a-zA-Z0-9_]*`
- **Token Creation**: Only created once, reused for all @Identifier/@References usages
- **Location**: `/yajco-annotation-processor/src/main/java/yajco/annotation/processor/AnnotationProcessor.java`
