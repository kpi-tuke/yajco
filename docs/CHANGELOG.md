# Changes from Version 0.5.9 to 0.6.0

## Overview

This release includes 149 non-merge commits and represents a significant evolution of YAJCo, focusing on language pattern expansion, Java modernization, improved multi-parser capabilities, and better type system support. It also adds support for ANTLR4 parser generator. 

## Major Changes

### New Language Patterns

- **Flag Pattern** - Initial implementation for Beaver and Antlr4 (d727693)
  - Allows boolean flags in language syntax
  - Represented by the `@Flag` annotation

- **Mixed Repetition Pattern** - Initial implementation (4c1ebb0)
  - Supports complex repetition patterns in grammar rules

- **Unordered Group Pattern** - Represented by `@UnorderedParameters` annotation
  - Allows parameters to appear in any order in the syntax

- **Quoted String Pattern** - Represented by `@StringToken` annotation
  - Handles string literals with proper quoting

- **Shared Pattern** - Represented by `@Shared` annotation
  - Enables sharing of language elements across different contexts

- **Whitespace and Comment Patterns** - Implemented as parameters to existing `@Skip` annotation (39c52ad)
  - Simplified definition of whitespace and comment handling

### Other New Features

- **Automatic IDENTIFIER Token** - Now automatically added and used for properties marked with `@Identifier` and `@References` (bb8ab1c)
  - Reduces boilerplate in language definitions

- **`@Keyword`** - Added as an alias for `@Before`, can be placed on parameters (experimental) (4e02f01, d612759)
  - More intuitive annotation name for keyword tokens

- **`@UniqueValues`** - Ensures uniqueness constraints on language elements

### Type System Enhancements

- **Optional Type Support** - Added comprehensive support for `java.util.Optional<T>` throughout the codebase
  - Visitor and Printer properly handle optional types
  - No need to return `Optional<T>` from getter methods (method overloading resolves it)

### Java & Build System Updates

- **Upgraded to Java 11** - Annotation processor and annotations updated to support Java 11 (fe8272e, 973fda4)
- **Code Formatter Change** - Replaced Jalopy with google-java-format (c914dcf)
  - Improved formatting of generated Java files

- **Dependency Updates**:
  - Updated Velocity to version 2.2
  - Updated Maven dependencies

- **Security Fixes**:
  - Fixed xstream compatibility with latest Java
  - Bumped junit from 4.12 to 4.13.1

### New Parser Generator Backend: ANTLR4

- **ANTLR4 Support Added** - Complete implementation of ANTLR4 parser generator backend (8391ced and subsequent commits)
  - Previously YAJCo supported Beaver and JavaCC backends
  - ANTLR4 grammar generation with semantic actions
  - Custom regex-matching lexer for unified lexical analysis
  - Support for references, arrays, lists, sets, enums, and factory methods
  - Improved error reporting for lexer and parser errors
  - Note: Not all language patterns are supported yet (work in progress)

### Experimental Xtext Support

- **Xtext Integration** - Experimental support for Xtext framework (e086621)
  - Added integration of YAJCo with Xtext
  - Xtext module moved into yajco-modules (2eb6d4d)
  - Removed dependency of annotation processor on Xtext module (c9edc84)

### Multi-Parser Support

- **Multiple Parsers** - Now allows multiple parsers in the same project (a5b9bcc)
  - Can use different parser backends (Beaver, JavaCC, ANTLR4) simultaneously
- **Common Parser Interface** - All generated parsers now implement a common interface (085d91d)
  - Added common `ParseException` (20d1925)
  - Better interoperability between different parser implementations
- **Service Provider Interface (SPI)** - Generated parsers registered as service providers in each CompilerGenerator

### License Change

- **Changed from GNU LGPL v3 to MIT License** (f4bc11a)
  - More permissive licensing for broader adoption

## Bug Fixes & Improvements

### Parser & Grammar Generation

- Fixed `@Identifier` recognition in base classes (7af4908)
- Fixed `@FactoryMethod` causing exceptions in ReferenceResolver (49e234e)
- Fixed unordered parameters processing with `@After` tokens (7950b7e)
- Fixed missing separator in sequences with finite `@Range.maxOccurs` (5fd0279)
- More reliable generation of actions for UnorderedParameters grammar rules (e203576)

### Code Generation

- Fixed Printer optional unboxing issues (829bf51, 0997b24)
- Improved indentation in generated Visitor (6b5634b)
- Fixed compile errors in generated visitor by unboxing Optional values (97acc60)
- Optimized imports in Utilities.java (6b76997)

### Reference Resolution

- Fixed ReferenceResolver hash issues (bded7b3)
  - Changed HashMap to IdentityHashMap to handle objects with custom hashCode()
  - Use IdentityHashSet in generated Visitor to ensure all objects are visited

- Resolve identifier fields from parent classes recursively (ff68f45)

### ANTLR4 Module

- Removed SecurityManager hack for running Antlr4 (2284628)
- Unified lexical analysis across parser generators
- Multiple bug fixes and improvements for ANTLR4 grammar generation


## Project Infrastructure

- **CI/CD Migration** - Migrated from Travis CI to GitHub Actions (ea984bb)
- **Build Scripts** - Created Maven workflow for GitHub Actions
- **Project Organization**:
  - Reorganized Xtext module into yajco-modules
  - Simplified pom.xml files across modules
  - Updated project metadata and URLs
  - Cleaned up examples and removed unnecessary documentation files (45a02e8)

## Refactoring & Code Quality

- **Symbol Management**:
  - Symbol is now Cloneable (483bdf4)
  - Changed `withVarName()` to return a clone (5b23f26)

- **Type Safety**:
  - Return patterns cast to requested type (0997b24)
  - Improved generic type handling (964eb9a)

- **Code Cleanup**:
  - Moved types not used in language model to yajco-grammar-module (896804e)
  - Consistent indentation using spaces throughout project (b2cf7b8)
  - Removed backwards-compatibility code and unnecessary abstractions

## Breaking Changes

- Minimum Java version is now 11
- License changed from LGPL v3 to MIT
- Some internal APIs refactored for better type safety

## Contributors

Special thanks to all contributors who made this release possible, including pull requests from:
- **Miroslav Remák** - Complete ANTLR4 backend implementation, visitor enhancements, and numerous bug fixes
- **Michal Fecenko** - Multiple feature additions including new annotations and patterns
- **Ján Halama** - Integration with Xtext 
- **Adrián Szegedi** - Bug fixes and refactoring improvements
