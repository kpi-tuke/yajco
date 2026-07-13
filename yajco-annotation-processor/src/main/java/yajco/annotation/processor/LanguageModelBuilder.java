package yajco.annotation.processor;

import org.checkerframework.checker.nullness.qual.NonNull;
import yajco.annotation.*;
import yajco.annotation.config.Option;
import yajco.annotation.config.Parser;
import yajco.annotation.config.Skip;
import yajco.annotation.reference.References;
import yajco.generator.GeneratorException;
import yajco.model.*;
import yajco.model.pattern.Pattern;
import yajco.model.pattern.impl.Factory;
import yajco.model.type.ListType;
import yajco.model.type.OptionalType;
import yajco.model.type.PrimitiveTypeConst;
import yajco.model.type.Type;
import yajco.model.utilities.XMLLanguageFormatHelper;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;
import javax.lang.model.type.TypeMirror;
import java.util.*;
import java.util.Optional;

public class LanguageModelBuilder {
    private Properties properties;
    private ProcessingEnvironment processingEnv;
    private final Set<? extends Element> rootElements;
    private final Set<String> excludes;
    private final TypeResolver typeResolver;
    private ConceptRegistry conceptRegistry;
    private PatternMapper patternMapper;

    /**
     * Builded language.
     */
    private Language language;

    /**
     * Used for creation of string tokens defined by @StringToken annotation.
     */
    private int stringTokenId = 1;
    private static final String DEFAULT_STRING_TOKEN_NAME = "STRING_TOKEN";
    private static final String IDENTIFIER_TOKEN_NAME = "IDENTIFIER";
    private static final String IDENTIFIER_TOKEN_REGEXP = "[a-zA-Z_][a-zA-Z0-9_]*";

    public LanguageModelBuilder(Properties properties, ProcessingEnvironment processingEnv, Set<? extends Element> rootElements, Set<String> excludes) {
        this.properties = properties;
        this.processingEnv = processingEnv;
        this.rootElements = rootElements;
        this.excludes = excludes;
        this.typeResolver = new TypeResolver(processingEnv.getTypeUtils(), this::resolveReferenceConcept);
        this.patternMapper = new PatternMapper(processingEnv);
    }

    public Language createLanguageModel(Element parserAnnotationElement, Parser parserAnnotation) {
        this.extractOptionsFromParserAnnotation(parserAnnotation);
        this.extractLanguageAnnotation(parserAnnotationElement);

        // Extract the main element, package or type can be annotated with @Parser.
        TypeElement mainElement = extractMainElement(parserAnnotationElement, parserAnnotation);

        // Create language.
        language = new Language(mainElement);
        conceptRegistry = new ConceptRegistry(language, rootElements, excludes, processingEnv.getTypeUtils());
        // Add main package name == language name.
        String mainElementName = mainElement.getQualifiedName().toString();
        int lastDotIndex = mainElementName.lastIndexOf('.');
        String languageName = lastDotIndex >= 0 ? mainElementName.substring(0, lastDotIndex) : "";
        System.out.println("---- mainElementName: " + mainElementName + " == languageName: " + languageName);
        if (!languageName.isEmpty()) {
            language.setName(languageName);
        }

        // Add language concepts from included JARs.
        addLanguageConceptsFromIncludedJARs();

        // Start processing with the main element.
        System.out.println(" ? mainElement ? : " + mainElement);
        processTypeElement(mainElement);

        // Add tokens and skips into language.
        addTokensAndSkipsIntoLanguage(parserAnnotation, parserAnnotationElement);

        // Convert properties to language settings.
        language.setSettings(LanguageSetting.convertToLanguageSetting(properties));

        return language;
    }

    public Concept resolveReferenceConcept(TypeElement typeElement) {
        if (typeElement != null && conceptRegistry.isKnownClass(typeElement)) {
            return processTypeElement(typeElement);
        } else {
            return null;
        }
    }

    private @NonNull TypeElement extractMainElement(Element parserAnnotationElement, Parser parserAnnotation) {
        ElementKind parserAnnotationElemKind = parserAnnotationElement.getKind();
        TypeElement mainElement;
        String mainElementName;
        if (parserAnnotationElemKind == ElementKind.PACKAGE) {
            mainElementName = parserAnnotation.mainNode();
            if (mainElementName.indexOf('.') == -1) {
                //TODO: Chyba v javac - ak nexistovala trieda tak ju vytvorilo ale nehlasilo null
                // ak som zavolal pre "Expression" vratilo sice null ale potom hlasilo duplicate class
                //mainElement = processingEnv.getElementUtils().getTypeElement(mainElementName);
                //mozno treba niekde v tools nastavit aby negenerovalo po otazke triedu ak neexistuje,
                //zda sa mi ze som to niekde videl v helpe
                mainElementName = ((PackageElement) parserAnnotationElement).getQualifiedName() + "." + mainElementName;
            }
            mainElement = processingEnv.getElementUtils().getTypeElement(mainElementName);
        } else if (parserAnnotationElemKind == ElementKind.CLASS || parserAnnotationElemKind == ElementKind.INTERFACE || parserAnnotationElemKind == ElementKind.ENUM) {
            mainElement = (TypeElement) parserAnnotationElement;
            mainElementName = mainElement.asType().toString();
            properties.setProperty("yajco.mainNode", mainElementName);
        } else {
            System.err.println("@Parser annotation can't be used on [" + parserAnnotationElemKind.toString() + "]\n");
            throw new GeneratorException("Annotation @Parser should annotate only package, class, interface or enum.");
        }

        if (mainElement == null) {
            throw new GeneratorException("Main language concept not found '" + parserAnnotation.mainNode() + "'");
        }
        return mainElement;
    }

    /**
     * Collects all options from @Parser annotation and store them to Properties object. Sets className and mainNode
     * if they are defined in @Parser annotation.
     *
     * @param parserAnnotation @Parser annotation object.
     */
    private void extractOptionsFromParserAnnotation(Parser parserAnnotation) {
        for (Option option : parserAnnotation.options()) {
            properties.setProperty(option.name(), option.value());
        }

        if (!parserAnnotation.className().isEmpty()) {
            properties.setProperty("yajco.className", parserAnnotation.className());
        }
        if (!parserAnnotation.mainNode().isEmpty()) {
            properties.setProperty("yajco.mainNode", parserAnnotation.mainNode());
        }
    }

    /**
     * Extracts metadata from {@code @Language} annotation and stores it in properties.
     * When {@code @Language} is present, IR generation is automatically enabled.
     *
     * @param element The element annotated with both @Parser and potentially @Language.
     */
    private void extractLanguageAnnotation(Element element) {
        yajco.annotation.config.Language lang = element.getAnnotation(yajco.annotation.config.Language.class);
        if (lang == null) {
            return;
        }

        // Auto-enable IR generation when @Language is present
        String tools = properties.getProperty("yajco.generateTools", "");
        if (!tools.toLowerCase().contains("ir") && !tools.toLowerCase().contains("all")) {
            properties.setProperty("yajco.generateTools",
                tools.isEmpty() ? "ir" : tools + ",ir");
        }

        // Language name
        if (!lang.name().isEmpty()) {
            properties.setProperty("yajco.ir.languageName", lang.name());
        }

        // Description
        if (!lang.description().isEmpty()) {
            properties.setProperty("yajco.ir.description", lang.description());
        }

        // Version
        if (!lang.version().isEmpty()) {
            properties.setProperty("yajco.ir.version", lang.version());
        }

        // File extensions (joined as comma-separated string)
        if (lang.fileExtensions().length > 0) {
            properties.setProperty("yajco.ir.fileExtensions",
                String.join(",", lang.fileExtensions()));
        }

        // IR file name defaults to {languageName}.ir.json if not already set
        if (!properties.containsKey("yajco.ir.file") && !lang.name().isEmpty()) {
            properties.setProperty("yajco.ir.file", lang.name() + ".ir.json");
        }
    }

    /**
     * Loads language concepts, skips and tokens from languages included as JAR files.
     */
    private void addLanguageConceptsFromIncludedJARs() {
        List<Language> includedLanguages = XMLLanguageFormatHelper.getAllLanguagesFromXML();
        System.out.println("Loaded external language specifications: " + includedLanguages.size());
        for (Language incLang : includedLanguages) {
            System.out.print("Loaded from JAR: ");
            for (Concept concept : incLang.getConcepts()) {
                System.out.print(concept.getName() + ", ");
            }
            System.out.println();

            addToListAsSet(language.getSkips(), incLang.getSkips(), true);
            addToListAsSet(language.getTokens(), incLang.getTokens(), true);
            conceptRegistry.registerImportedConcepts(incLang.getConcepts());
        }
    }

    /**
     * Adds tokens and skips defined in @Parser annotation into language.
     * Also adds comment skip rules from @Language if present.
     * Extracts comment metadata from {@code @Skip(lineComment=...)} and
     * {@code @Skip(blockComment=...)} entries in {@code @Parser.skips()} and stores
     * them as IR properties.
     *
     * @param parserAnnotation @Parser annotation object
     * @param parserAnnotationElement The annotated element (for reading @Language)
     */
    private void addTokensAndSkipsIntoLanguage(Parser parserAnnotation, Element parserAnnotationElement) {
        List<TokenDef> tokens = new ArrayList<>();
        List<SkipDef> skips = new ArrayList<>();
        for (yajco.annotation.config.TokenDef tokenDef : parserAnnotation.tokens()) {
            tokens.add(new TokenDef(tokenDef.name(), tokenDef.regexp(), tokenDef));
        }
        for (Skip skip : parserAnnotation.skips()) {
            SkipConversionResult conversion = convertSkip(skip, parserAnnotationElement);
            if (conversion.regexp.isEmpty()) {
                continue;
            }
            skips.add(new SkipDef(conversion.regexp, skip));
            if (conversion.lineComment != null) {
                properties.setProperty("yajco.ir.lineComment", conversion.lineComment);
            }
            if (conversion.blockCommentStart != null) {
                properties.setProperty("yajco.ir.blockComment.start", conversion.blockCommentStart);
                properties.setProperty("yajco.ir.blockComment.end", conversion.blockCommentEnd);
            }
        }

        // Add default white space for skips if empty.
        if (skips.isEmpty()) {
            skips.add(new SkipDef("\\s"));
        }

        addToListAsSet(language.getSkips(), skips, true);
        addToListAsSet(language.getTokens(), tokens, true);
    }

    private static final class SkipConversionResult {
        private final String regexp;
        private final String lineComment;
        private final String blockCommentStart;
        private final String blockCommentEnd;

        private SkipConversionResult(String regexp, String lineComment, String blockCommentStart, String blockCommentEnd) {
            this.regexp = regexp;
            this.lineComment = lineComment;
            this.blockCommentStart = blockCommentStart;
            this.blockCommentEnd = blockCommentEnd;
        }
    }

    /**
     * Converts @Skip annotation parameters into parser skip regexp and optional
     * IR comment metadata.
     *
     * @param skip @Skip annotation object
     * @param element The annotated element, used for error reporting
     * @return Skip conversion output containing regexp and optional comment metadata
     */
    private SkipConversionResult convertSkip(Skip skip, Element element) {
        // If value is explicitly set, use it directly (highest priority)
        if (!skip.value().isEmpty()) {
            return new SkipConversionResult(skip.value(), null, null, null);
        }

        // Handle whitespace flag
        if (skip.whitespace()) {
            return new SkipConversionResult("\\s", null, null, null);
        }

        // Handle line comment: prefix → "prefix.*"
        if (!skip.lineComment().isEmpty()) {
            return new SkipConversionResult(escapeRegex(skip.lineComment()) + ".*", skip.lineComment(), null, null);
        }

        // Handle block comment: [start, end] → "start(?:(?!end)[\s\S])*end"
        if (skip.blockComment().length != 0) {
            if (skip.blockComment().length != 2) {
                error("@Skip blockComment must have exactly 2 elements: {start, end}, e.g. {\"/*\", \"*/\"}.", element);
                return new SkipConversionResult("", null, null, null);
            }
            String start = escapeRegex(skip.blockComment()[0]);
            String end = escapeRegex(skip.blockComment()[1]);
            return new SkipConversionResult(
                start + "(?:(?!" + end + ")[\\s\\S])*" + end,
                null,
                skip.blockComment()[0],
                skip.blockComment()[1]);
        }

        // Handle deprecated comment patterns with start and optional end
        if (!skip.start().isEmpty()) {
            String start = escapeRegex(skip.start());
            if (!skip.end().isEmpty()) {
                String end = escapeRegex(skip.end());
                return new SkipConversionResult(start + "(?:(?!" + end + ")[\\s\\S])*" + end, null, skip.start(), skip.end());
            } else {
                return new SkipConversionResult(start + ".*", skip.start(), null, null);
            }
        }

        // If nothing is specified, return empty string (should not happen in practice)
        return new SkipConversionResult("", null, null, null);
    }

    /**
     * Escapes special regex characters in a literal string.
     *
     * @param literal String to escape
     * @return Escaped string safe for use in regex
     */
    private String escapeRegex(String literal) {
        return literal.replaceAll("([\\\\.*+?^${}()|\\[\\]])", "\\\\$1");
    }


    /**
     * Reports a compilation error via the annotation processing {@link javax.annotation.processing.Messager}.
     * The error is attached to the given element, so IDEs and javac can point to the exact location.
     *
     * @param message Error message to display
     * @param element The annotated element where the error occurred
     */
    private void error(String message, Element element) {
        processingEnv.getMessager().printMessage(javax.tools.Diagnostic.Kind.ERROR, message, element);
    }

    /**
     * Processes language model elements.
     *
     * @param typeElement Language model element to process.
     * @return Processed language concept.
     */
    private Concept processTypeElement(TypeElement typeElement) {
        return processTypeElement(typeElement, null);
    }

    /**
     * Processes language model elements and creates unique language concepts for all of them.
     *
     * @param typeElement Language model element to process.
     * @param superConcept Parent concept.
     * @return Processed language concept.
     */
    private Concept processTypeElement(TypeElement typeElement, Concept superConcept) {
        Concept existing = conceptRegistry.resolveKnown(typeElement);
        Concept concept = conceptRegistry.getOrCreate(typeElement, superConcept);
        if (concept == null) {
            return null;
        }
        if (existing != null && !conceptRegistry.consumeImportedConcept(concept)) {
            return concept;
        }

        processTypeElementAccordingToKind(typeElement, concept);
        patternMapper.addPatternsFromAnnotations(typeElement, concept);
        processDirectSubclasses(typeElement, concept);
        return concept;
    }

    /**
     * Processes language model element according to its kind.
     *
     * @param typeElement Language model element.
     * @param concept Language concept for representing language model element.
     */
    private void processTypeElementAccordingToKind(TypeElement typeElement, Concept concept) {
        if (typeElement.getKind() == ElementKind.ENUM) { // Enum type
            processEnum(concept, typeElement);
        } else if (typeElement.getKind() == ElementKind.CLASS) { // Class
            System.out.println(" modifiers: " + typeElement.getModifiers());
            if (typeElement.getModifiers().contains(Modifier.ABSTRACT)) { // Abstract class
                processAbstractClass(concept, typeElement);
            } else {  //Concrete class
                processConcreteClass(concept, typeElement);
            }
        } else if (typeElement.getKind() == ElementKind.INTERFACE) { // Interface
            processInterface(concept, typeElement);
        } else {
            throw new GeneratorException("Not supported type in model '" + typeElement + "'");
        }
    }

    /**
     * Processes Enum language model elements.
     *
     * @param concept Language concept representing language model element.
     * @param typeElement Language model element.
     */
    private void processEnum(Concept concept, TypeElement typeElement) {
        concept.addPattern(new yajco.model.pattern.impl.Enum());
        for (Element element : typeElement.getEnclosedElements()) {
            if (element.getKind() != ElementKind.ENUM_CONSTANT) {
                continue; // Skip non enum constants.
            }

            Token tokenAnnotation = element.getAnnotation(Token.class);
            Notation notation = new Notation(typeElement);
            TokenPart tokenPart;
            if (tokenAnnotation != null) {
                tokenPart = new TokenPart(tokenAnnotation.value(), tokenAnnotation);
            } else {
                tokenPart = new TokenPart(element.getSimpleName().toString(), element);
            }
            notation.addPart(tokenPart);
            concept.addNotation(notation);
        }
    }

    /**
     * Processes concrete classes of language model.
     *
     * @param concept Language concept representing language model element.
     * @param classElement Language model element.
     */
    private void processConcreteClass(Concept concept, TypeElement classElement) {
        defineAbstractSytax(concept, classElement);
        defineConcreteSyntax(concept, classElement);
    }

    /**
     * Parses concepts concrete syntax.
     *
     * @param concept Language concept representing language model element.
     * @param classElement Language model element.
     */
    private void defineConcreteSyntax(Concept concept, TypeElement classElement) {
        Set<ExecutableElement> constructors = getConstructorsAndFactoryMethods(classElement);

        // Check for class-level @Before, @Keyword, and @After annotations
        Before beforeClassAnnotation = classElement.getAnnotation(Before.class);
        Keyword keywordClassAnnotation = classElement.getAnnotation(Keyword.class);
        After afterClassAnnotation = classElement.getAnnotation(After.class);

        // If there are no constructors but class has @Before, @Keyword, or @After annotations, create a default constructor notation
        if (constructors.isEmpty() && (beforeClassAnnotation != null || keywordClassAnnotation != null || afterClassAnnotation != null)) {
            Notation defaultNotation = new Notation(classElement);

            // Apply class-level @Before annotation
            if (beforeClassAnnotation != null) {
                addTokenParts(defaultNotation, beforeClassAnnotation.value());
            }

            // Apply class-level @Keyword annotation
            if (keywordClassAnnotation != null) {
                addTokenParts(defaultNotation, keywordClassAnnotation.value());
            }

            // Apply class-level @After annotation
            if (afterClassAnnotation != null) {
                addTokenParts(defaultNotation, afterClassAnnotation.value());
            }

            concept.addNotation(defaultNotation);
        }

        for (ExecutableElement constructor : constructors) {
            Notation notation = new Notation(constructor);

            // Apply class-level @Before annotation first, if present
            if (beforeClassAnnotation != null) {
                addTokenParts(notation, beforeClassAnnotation.value());
            }

            // Apply class-level @Keyword annotation, if present
            if (keywordClassAnnotation != null) {
                addTokenParts(notation, keywordClassAnnotation.value());
            }

            // Then apply constructor-level @Before annotation
            if (constructor.getAnnotation(Before.class) != null) {
                addTokenParts(notation, constructor.getAnnotation(Before.class).value());
            }

            // Then apply constructor-level @Keyword annotation
            if (constructor.getAnnotation(Keyword.class) != null) {
                addTokenParts(notation, constructor.getAnnotation(Keyword.class).value());
            }

            // @UnorderedParameters annotation.
            UnorderedParameters unorderedParametersAnnotation = constructor.getAnnotation(UnorderedParameters.class);
            MixedRepetition mixedRepetitionAnnotation = constructor.getAnnotation(MixedRepetition.class);

            if (unorderedParametersAnnotation != null) {
                processUnorderedParamsConstructor(concept, constructor, notation, unorderedParametersAnnotation);
            } else if (mixedRepetitionAnnotation != null) {
                processMixedRepetitionConstructor(concept, constructor, notation, mixedRepetitionAnnotation);
            } else {
                for (VariableElement paramElement : constructor.getParameters()) {
                    processParameter(concept, notation, paramElement);
                }
            }

            // Apply constructor-level @After annotation
            if (constructor.getAnnotation(After.class) != null) {
                addTokenParts(notation, constructor.getAnnotation(After.class).value());
            }

            // Then apply class-level @After annotation, if present
            if (afterClassAnnotation != null) {
                addTokenParts(notation, afterClassAnnotation.value());
            }

            concept.addNotation(notation);
            if (constructor.getKind() == ElementKind.METHOD) {
                notation.addPattern(new Factory(constructor.getSimpleName().toString()));
            }

            //TODO: odstranit pri prenesesni @Operator na triedu
            // Add concept pattern from annotations (Type).
            patternMapper.addPatternsFromAnnotations(constructor, concept);
        }
    }

    /**
     * Processes constructor annotated with @UnorderedParameters annotation.
     *
     * @param concept Language concept.
     * @param notation Language concept notation.
     * @param unorderedParametersAnnotation UnorderedParameters annotation
     */
    private void processUnorderedParamsConstructor(Concept concept, ExecutableElement constructor, Notation notation, UnorderedParameters unorderedParametersAnnotation) {
        int excludedCount = 0;
        int unorderedCount = 0;
        boolean cantBeExcluded = false;
        boolean cantBeUnordered = false;

        for (VariableElement paramElement : constructor.getParameters()) {
            if (Arrays.asList(unorderedParametersAnnotation.exclude()).contains(paramElement.getSimpleName().toString())) {
                processParameter(concept, notation, paramElement);
                excludedCount++;
                cantBeUnordered = unorderedCount > 0 && excludedCount > 0;
            } else if (typeResolver.getType(paramElement.asType()) instanceof OptionalType)  {
                UnorderedParamPart unorderedParamPart = new UnorderedParamPart(null);
                OptionalPart optionalPart = (OptionalPart) processCompoundParameter(concept, paramElement, new OptionalPart(null));
                unorderedParamPart.addPart(optionalPart);
                notation.addPart(unorderedParamPart);
                unorderedCount++;
                cantBeExcluded = unorderedCount > 0 && excludedCount > 0;
            } else {
                UnorderedParamPart unorderedParamPart = new UnorderedParamPart(null);
                processCompoundParameter(concept, paramElement, unorderedParamPart);
                notation.addPart(unorderedParamPart);
                unorderedCount++;
                cantBeExcluded = unorderedCount > 0 && excludedCount > 0;
            }

            if (cantBeExcluded && cantBeUnordered) {
                throw new GeneratorException("Excluded parameter can't be in the middle of unordered parameters.");
            }
        }
    }

    /**
     * Processes constructor annotated with @MixedRepetition annotation.
     *
     * @param concept Language concept.
     * @param constructor Constructor or factory method element.
     * @param notation Language concept notation.
     * @param mixedRepetitionAnnotation MixedRepetition annotation
     */
    private void processMixedRepetitionConstructor(Concept concept, ExecutableElement constructor, Notation notation, MixedRepetition mixedRepetitionAnnotation) {
        MixedRepetitionPart mixedRepetitionPart = new MixedRepetitionPart(null);

        for (VariableElement paramElement : constructor.getParameters()) {
            String paramName = paramElement.getSimpleName().toString();

            // Check if parameter is excluded
            if (Arrays.asList(mixedRepetitionAnnotation.exclude()).contains(paramName)) {
                processParameter(concept, notation, paramElement);
            } else {
                // Parameter should be a collection type (List or Array)
                Type paramType = typeResolver.getType(paramElement.asType());

                if (!(paramType instanceof ListType || paramType instanceof ArrayType)) {
                    throw new GeneratorException(
                        "Parameter '" + paramName + "' in @MixedRepetition must be a List or Array type. Found: " + paramType.getClass().getSimpleName()
                    );
                }

                // Create property reference part for this collection parameter
                Property property = concept.getProperty(paramName);
                if (property == null) {
                    property = new Property(paramName, paramType, null);
                    concept.addProperty(property);
                }

                PropertyReferencePart propertyRefPart = new PropertyReferencePart(property, paramElement);

                // Add patterns from annotations (e.g., @Before, @After on individual parameters)
                patternMapper.addPatternsFromAnnotations(paramElement, propertyRefPart);

                // Add this property reference to the mixed repetition part
                mixedRepetitionPart.addPart(propertyRefPart);
            }
        }

        // Add the mixed repetition part to the notation
        if (mixedRepetitionPart.getParts().size() > 0) {
            notation.addPart(mixedRepetitionPart);
        }
    }

    /**
     * Parses concepts abstract syntax.
     *
     * @param concept Language concept representing language model element.
     * @param classElement Language model element.
     */
    private void defineAbstractSytax(Concept concept, TypeElement classElement) {
        System.out.println("starting ProcessConcreteClass method");
        for (Element element : classElement.getEnclosedElements()) {
            System.out.println("- enclosedElement: " + element.getSimpleName().toString() + "[" + element.getKind() + "]");
            if (element.getKind().isField()) {
                System.out.println("+++ " + classElement.toString() + "> " + element.toString());
                VariableElement fieldElement = (VariableElement) element;

                // Add only fields with property patterns (PropertyPattern).
                if (patternMapper.hasPatternAnnotations(fieldElement)) {
                    Property property = new Property(
                        fieldElement.getSimpleName().toString(),
                        typeResolver.getType(fieldElement.asType()),
                        fieldElement);
                    patternMapper.addPatternsFromAnnotations(fieldElement, property);
                    concept.addProperty(property);
                }
            }
        }
    }

    /**
     * Processes abstract classes of language model elements
     *
     * @param concept Language concept representing language model element.
     * @param typeElement Language model element.
     */
    private void processAbstractClass(Concept concept, TypeElement typeElement) {
        //TODO: toto je len docasne pre testovanie a pokial sa ujasni ako to chceme
        //processConcreteClass(concept, typeElement);
    }

    /**
     * Processes interfaces of language model elements
     *
     * @param concept Language concept representing language model element.
     * @param typeElement Language model element.
     */
    private void processInterface(Concept concept, TypeElement typeElement) {
    }

    /**
     * Processes parameters of constructor or factory method in language model.
     *
     * @param concept Language concept.
     * @param notation Notation of language concept.
     * @param paramElement Parameter of constructor or factory method.
     */
    private void processParameter(Concept concept, Notation notation, VariableElement paramElement) {
        Type type = typeResolver.getType(paramElement.asType());
        yajco.annotation.Flag flagAnnotation = paramElement.getAnnotation(yajco.annotation.Flag.class);

        // Handle @Flag annotation on boolean parameters
        if (flagAnnotation != null) {
            if (!(type instanceof yajco.model.type.PrimitiveType
                  && ((yajco.model.type.PrimitiveType) type).getPrimitiveTypeConst() == PrimitiveTypeConst.BOOLEAN)) {
                throw new GeneratorException("@Flag annotation can only be used on boolean parameters");
            }

            String paramName = paramElement.getSimpleName().toString();
            Property property = concept.getProperty(paramName);
            if (property == null) {
                property = new Property(paramName, type, null);
                concept.addProperty(property);
            }

            // Create an optional part containing the flag token
            OptionalPart optionalPart = new OptionalPart(null);
            optionalPart.addPart(new TokenPart(flagAnnotation.value()));

            // Add property reference with Flag pattern
            PropertyReferencePart propertyRefPart = new PropertyReferencePart(property, paramElement);
            propertyRefPart.addPattern(new yajco.model.pattern.impl.Flag(flagAnnotation.value(), flagAnnotation));
            optionalPart.addPart(propertyRefPart);

            notation.addPart(optionalPart);
        } else if (type instanceof OptionalType) {
            OptionalPart optionalPart = (OptionalPart) processCompoundParameter(concept, paramElement, new OptionalPart(null));
            notation.addPart(optionalPart);
        } else {
            // @Keyword annotation.
            if (paramElement.getAnnotation(Keyword.class) != null) {
                addTokenParts(notation, paramElement.getAnnotation(Keyword.class).value());
            }

            // @Before annotation.
            if (paramElement.getAnnotation(Before.class) != null) {
                addTokenParts(notation, paramElement.getAnnotation(Before.class).value());
            }

            String paramName = paramElement.getSimpleName().toString();
            TypeMirror typeMirror = paramElement.asType();
            References references = paramElement.getAnnotation(References.class);
            Token tokenAnnotation = paramElement.getAnnotation(Token.class);
            StringToken stringTokenAnnotation = paramElement.getAnnotation(StringToken.class);
            BindingNotationPart part = null;

            if (references != null) { // @References annotation.
                //TODO: zatial nie je podpora pre polia referencii, treba to vsak doriesit
                type = typeResolver.getSimpleType(typeMirror);
                LocalVariablePart localVariablePart = new LocalVariablePart(paramName, type, paramElement);
                notation.addPart(localVariablePart);

                part = processReferencedConcept(concept, paramElement, references, localVariablePart);
            } else { // Property reference.
                Property property = concept.getProperty(paramName);
                if (property == null) {
                    property = new Property(paramName, typeResolver.getType(typeMirror), null);
                    concept.addProperty(property);
                }

                part = new PropertyReferencePart(property, paramElement);
                notation.addPart(part);
            }

            if (tokenAnnotation != null) {
                part.addPattern(new yajco.model.pattern.impl.Token(tokenAnnotation.value(), tokenAnnotation));
            } else if (stringTokenAnnotation != null) {
                TokenDef tokenDef = createStringTokenDef(stringTokenAnnotation);
                part.addPattern(new yajco.model.pattern.impl.Token(tokenDef.getName(), tokenAnnotation));
            } else if (references != null) {
                // Automatically add IDENTIFIER token for @References parameters
                ensureIdentifierTokenExists();
                part.addPattern(new yajco.model.pattern.impl.Token(IDENTIFIER_TOKEN_NAME, references));
            } else if (part instanceof PropertyReferencePart) {
                // Check if the referenced property has @Identifier pattern
                PropertyReferencePart propRefPart = (PropertyReferencePart) part;
                if (propertyHasIdentifierPattern(propRefPart.getProperty())) {
                    // Automatically add IDENTIFIER token for properties with @Identifier
                    ensureIdentifierTokenExists();
                    part.addPattern(new yajco.model.pattern.impl.Token(IDENTIFIER_TOKEN_NAME, paramElement));
                }
            }

            // Add notation part pattern from annotations (NotationPartPattern).
            patternMapper.addPatternsFromAnnotations(paramElement, part);

            // @After annotation.
            if (paramElement.getAnnotation(After.class) != null) {
                addTokenParts(notation, paramElement.getAnnotation(After.class).value());
            }
        }
    }

    /**
     * Processes compound parameters (Parameters which know their whole concrete syntax).
     *
     * @param concept Language concept.
     * @param paramElement Parameter of constructor or factory method.
     * @param notationPart Compound notation part.
     *
     * @return Compound notation part.
     */
    private CompoundNotationPart processCompoundParameter(Concept concept, VariableElement paramElement, CompoundNotationPart notationPart) {
        // @Keyword annotation.
        if (paramElement.getAnnotation(Keyword.class) != null) {
            for (String value : paramElement.getAnnotation(Keyword.class).value()) {
                notationPart.addPart(new TokenPart(value));
            }
        }

        // @Before annotation.
        if (paramElement.getAnnotation(Before.class) != null) {
            for (String value : paramElement.getAnnotation(Before.class).value()) {
                notationPart.addPart(new TokenPart(value));
            }
        }

        String paramName = paramElement.getSimpleName().toString();
        TypeMirror typeMirror = paramElement.asType();
        References references = paramElement.getAnnotation(References.class);
        Token tokenAnnotation = paramElement.getAnnotation(Token.class);
        StringToken stringTokenAnnotation = paramElement.getAnnotation(StringToken.class);
        BindingNotationPart part;

        if (references != null) { // @References annotation.
            Type type;
            List<? extends TypeMirror> types = ((DeclaredType) typeMirror).getTypeArguments();
            if (processingEnv.getTypeUtils().asElement(typeMirror).toString().equals(Optional.class.getName())) {
                type = typeResolver.getSimpleType(types.get(types.size() - 1));
            } else {
                type = typeResolver.getSimpleType(typeMirror);
            }
            LocalVariablePart localVariablePart = new LocalVariablePart(paramName, type, paramElement);
            notationPart.addPart(localVariablePart);
            part = processReferencedConcept(concept, paramElement, references, localVariablePart);
        } else { // Property reference.
            Property property = concept.getProperty(paramName);
            if (property == null) {
                property = new Property(paramName, typeResolver.getType(typeMirror), null);
                concept.addProperty(property);
            }

            part = new PropertyReferencePart(property, paramElement);
            notationPart.addPart(part);
        }

        if (tokenAnnotation != null) {
            part.addPattern(new yajco.model.pattern.impl.Token(tokenAnnotation.value(), tokenAnnotation));
        } else if (stringTokenAnnotation != null) {
            TokenDef tokenDef = createStringTokenDef(stringTokenAnnotation);
            part.addPattern(new yajco.model.pattern.impl.Token(tokenDef.getName(), tokenAnnotation));
        }

        // Add notation part pattern from annotations (NotationPartPattern).
        patternMapper.addPatternsFromAnnotations(paramElement, part);

        // @After annotation.
        if (paramElement.getAnnotation(After.class) != null) {
            for (String value : paramElement.getAnnotation(After.class).value()) {
                notationPart.addPart(new TokenPart(value));
            }
        }

        return notationPart;
    }

    /**
     * Creates token for string token and adds it to language.
     *
     * @param stringTokenAnnotation StringToken annotation.
     * @return TokenDef
     */
    private TokenDef createStringTokenDef(StringToken stringTokenAnnotation) {
        String regex = formatStringTokenRegex(stringTokenAnnotation);
        TokenDef tokenDef = null;

        for (TokenDef token: language.getTokens()) {
            if (token.getName().contains(DEFAULT_STRING_TOKEN_NAME) && token.getRegexp().equals(regex)) {
                // Use existing string token with the same regex.
                tokenDef = token;
            }
        }

        if (tokenDef == null) {
            tokenDef = new TokenDef(DEFAULT_STRING_TOKEN_NAME + "_" + stringTokenId++, regex);
        }

        // Add string token to language.
        addToListAsSet(language.getTokens(), Collections.singletonList(tokenDef),false);

        return tokenDef;
    }

    /**
     * Ensures IDENTIFIER token exists in the language definition.
     * If the token doesn't exist, it creates and adds it automatically.
     *
     * @return TokenDef for IDENTIFIER token
     */
    private TokenDef ensureIdentifierTokenExists() {
        // Check if IDENTIFIER token already exists
        for (TokenDef token : language.getTokens()) {
            if (token.getName().equals(IDENTIFIER_TOKEN_NAME)) {
                return token;
            }
        }

        // Create new IDENTIFIER token
        TokenDef tokenDef = new TokenDef(IDENTIFIER_TOKEN_NAME, IDENTIFIER_TOKEN_REGEXP);

        // Add IDENTIFIER token to language
        addToListAsSet(language.getTokens(), Collections.singletonList(tokenDef), false);

        return tokenDef;
    }

    /**
     * Checks if a property has an @Identifier pattern.
     *
     * @param property Property to check
     * @return true if property has @Identifier pattern, false otherwise
     */
    private boolean propertyHasIdentifierPattern(Property property) {
        if (property == null) {
            return false;
        }
        for (Pattern pattern : property.getPatterns()) {
            if (pattern instanceof yajco.model.pattern.impl.Identifier) {
                return true;
            }
        }
        return false;
    }

    /**
     * Formats regex from @StringToken annotation. Regex is used to match strings.
     *
     * @param stringTokenAnnotation StringToken annotation
     * @return Regex for matching strings.
     */
    private String formatStringTokenRegex(StringToken stringTokenAnnotation) {
        String delimiter = stringTokenAnnotation.delimiter();

        if (delimiter.equals("\"") || delimiter.equals("'") || delimiter.equals("\\")) {
            delimiter = "\\" + stringTokenAnnotation.delimiter();
        }

        return delimiter + "(?:\\\\" + delimiter + "|[^" + delimiter + "])*?" + delimiter;
    }

    /**
     * Processes referenced concept.
     *
     * @param concept Language concept.
     * @param paramElement Parameter of constructor or factory method.
     * @param references References annotation.
     * @param part  Local variable notation part.
     * @return Binding notation part.
     */
    private BindingNotationPart processReferencedConcept(Concept concept, VariableElement paramElement, References references, LocalVariablePart part) {
        String paramName = paramElement.getSimpleName().toString();
        try {
            references.value();
        } catch (MirroredTypeException e) {
            TypeElement referencedTypeElement = (TypeElement) processingEnv.getTypeUtils().asElement(e.getTypeMirror());
            Concept referencedConcept = processTypeElement(referencedTypeElement);
            Property property = null;
            if (!references.field().isEmpty()) {
                property = concept.getProperty(references.field());
            }
            if (property == null) {
                property = findReferencedProperty(paramElement, referencedConcept, references.field());
                if (property == null) {
                    String propertyName;
                    if (references.field().isEmpty()) {
                        propertyName = paramName;
                    } else {
                        propertyName = references.field();
                    }
                    property = new Property(propertyName, new yajco.model.type.ReferenceType(referencedConcept, referencedTypeElement), e);
                }
                concept.addProperty(property);
            }
            // If names of notationPart and referenced property are identical, no need to fill property data to References pattern.
            if (property.getName().equals(paramName)) {
                property = null;
            }
            part.addPattern(new yajco.model.pattern.impl.References(referencedConcept, property, references));
        }
        return part;
    }

    /**
     * Finds referenced property.
     *
     * @param paramElement Parameter of constructor or factory method in language model.
     * @param referencedConcept Referenced concept.
     * @param proposedName Referenced field name.
     * @return Referenced property.
     */
    private Property findReferencedProperty(VariableElement paramElement, Concept referencedConcept, String proposedName) {
        Element element = paramElement;

        // Go up on tree until you find class element.
        while (element != null && !element.getKind().isClass()) {
            element = element.getEnclosingElement();
        }

        // Class element found.
        if (element != null) {
            for (Element elem : element.getEnclosedElements()) {
                if (elem.getKind().isField()) {
                    VariableElement fieldElement = (VariableElement) elem;
                    Type fieldType = typeResolver.getType(fieldElement.asType());
                    if (fieldType instanceof yajco.model.type.ReferenceType) {
                        yajco.model.type.ReferenceType referenceType = (yajco.model.type.ReferenceType) fieldType;
                        if (referenceType.getConcept().equals(referencedConcept)) {
                            if (!proposedName.isEmpty() && !fieldElement.getSimpleName().toString().equals(proposedName)) {
                                continue;
                            }
                            return new Property(fieldElement.getSimpleName().toString(), referenceType, fieldElement);
                        }
                    }
                }
            }
        }
        return null;
    }

//    private yajco.model.pattern.impl.Range getRange(VariableElement paramElement, TypeMirror type) {
//        yajco.annotation.Range range = paramElement.getAnnotation(yajco.annotation.Range.class);
//        if (type.getKind() == TypeKind.ARRAY) {
//            if (range != null) {
//                return new yajco.model.pattern.impl.Range(range.minOccurs(), range.maxOccurs());
//            }
//        } else {
//            if (range != null) {
//                throw new GeneratorException("@Range should be applied only on array or list  type " + paramElement);
//            }
//        }
//        return null;
//    }

    /**
     * Finds all constructors nad factory methods in language model class.
     *
     * @param classElement Language model class.
     * @return Set of executable elements (constructors and factory methods).
     */
    private Set<ExecutableElement> getConstructorsAndFactoryMethods(TypeElement classElement) {
        Set<ExecutableElement> constructors = new HashSet<>();
        for (Element element : processingEnv.getElementUtils().getAllMembers(classElement)) {
            boolean isConstructor = isConstructor(element);
            boolean isFactoryMethod = isFactoryMethod(element);

            if (isConstructor || isFactoryMethod) {
                constructors.add((ExecutableElement) element);
            }
        }

        return constructors;
    }

    /**
     * Checks if language model element is constructor.
     *
     * @param element Language model element.
     * @return If element is constructor.
     */
    private boolean isConstructor(Element element) {
        return element.getKind() == ElementKind.CONSTRUCTOR && element.getModifiers().contains(Modifier.PUBLIC)
               && element.getAnnotation(Exclude.class) == null;
    }

    /**
     * Checks if language model element is factory method.
     *
     * @param element Language model element.
     * @return If element is factory method.
     */
    private boolean isFactoryMethod(Element element) {
        return element.getKind() == ElementKind.METHOD && element.getModifiers().contains(Modifier.PUBLIC)
               && element.getAnnotation(FactoryMethod.class) != null && element.getAnnotation(Exclude.class) == null;
    }

    private void addTokenParts(Notation notation, String[] values) {
        for (String value : values) {
            notation.addPart(new TokenPart(value));
        }
    }

    /**
     * Processes direct subclasses of language model element.
     *
     * @param typeElement Language model element.
     * @param concept Language concept representing a language model element.
     */
    private void processDirectSubclasses(TypeElement typeElement, Concept concept) {
        for (TypeElement subtypeElement : conceptRegistry.directSubtypesOf(typeElement)) {
            processTypeElement(subtypeElement, concept);
        }
    }

    /**
     * Adds new items to original list according to overwrite flag.
     *
     * @param originalList Original list.
     * @param newItems List of new items.
     * @param overwrite Overwrite flag.
     * @param <T>
     */
    private <T> void addToListAsSet(List<T> originalList, List<T> newItems, boolean overwrite) {
        for (T item : newItems) {
            if (originalList.contains(item)) {
                if (overwrite) {
                    // dolezite je to, aby item.equals fungoval spravne
                    originalList.remove(item); // odstrani povodny
                    originalList.add(item); // vlozi novy
                }
            } else {
                originalList.add(item);
            }
        }
    }
}
