# YAJCo #
**YAJCo** (Yet Another Java Compiler compiler) is a **language parser generator based on annotated model**.

## Our approach ##
Our goal was not to introduce new parsing methods. We have integrated existing methods into a tool that allows language developer to work on higher level of abstraction with language definition that is independent on parsing algorithm and based on abstract syntax.

YAJCo relies on Java as a language for definition of language model and its semantics. Language model consists of annotated Java classes and relations between them. The model corresponds to abstract syntax of the language where each class represents a language concept.

## Architecture ##
The tool contains Java annotation processor that collects metadata attached to classes and their elements and constructs an internal model of language definition used to generate a parser. YAJCo is able to extract relations between classes of the language model and infer part of language syntax based on that. Missing information about concrete syntax is determined from annotations.

![https://yajco.googlecode.com/svn/wiki/images/schema.png](https://yajco.googlecode.com/svn/wiki/images/schema.png)

This definition is used by YAJCo to generate parser specification for one of the existing parser generators. Currently JavaCC and Beaver are supported as backends making it possible to choose parsing algorithm depending on the current needs.

Generated parser together with generated YAJCo language processor can be used to parse language sentences. The result of the parsing is an instance of the language model - instances of model classes with interconnected to form an abstract syntax tree corresponding to the input sentence. Moreover, YAJCo supports automatic resolution of references in the language sentences, so the output structure can be actually abstract syntax graph.

Language semantics can be defined using methods of language model classes. Semantics do not need to be defined directly inside language concept classes. It can be expressed in other classes that would traverse language model, or it can be moved into aspects using AspectJ.

YAJCo can optionally generate other tools besides parser. This includes pretty-printer that is able to generate textual representation of provided object model according to the language grammar. This operation is symmetric to the parsing and makes YAJCo capable of both serialization and deserialization of objects in a specified textual form. Another tool is a visitor class that simplifies traversing the object graph.

The fact that described approach to language definition concentrates on abstract syntax also simplifies language composition. The language can be extended by adding concepts from other language and interconnecting them using some relations. This allows to use powerful composition mechanisms based on object-oriented and aspect-oriented programming already provided by Java and AspectJ.