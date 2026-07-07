package yajco.generator.ir;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import yajco.generator.FilesGenerator;
import yajco.generator.GeneratorException;
import yajco.model.*;
import yajco.model.pattern.NotationPartPattern;
import yajco.model.pattern.impl.*;
import yajco.model.type.*;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.util.*;

/**
 * Generates an IR JSON file from a YAJCo {@link Language} model.
 * Registered as a {@link FilesGenerator} SPI service.
 */
public class IrFilesGenerator implements FilesGenerator {
    private static final String GENERATE_TOOLS_KEY = "yajco.generateTools";
    private static final String PROPERTY_ENABLER = "ir";
    private static final String OUTPUT_FILE_KEY = "yajco.ir.file";
    private static final String LANGUAGE_NAME_KEY = "yajco.ir.languageName";
    private static final String FILE_EXTENSIONS_KEY = "yajco.ir.fileExtensions";
    private static final String DEFAULT_OUTPUT_SUFFIX = ".ir.json";
    private static final String IR_VERSION = "1.0.0";
    private static final String PRODUCER_VERSION = detectProducerVersion();

    @Override
    public void generateFiles(Language language, Filer filer, Properties properties) {
        String option = properties.getProperty(GENERATE_TOOLS_KEY, "").toLowerCase();
        if (!option.contains("all") && !option.contains(PROPERTY_ENABLER)) {
            System.out.println(getClass().getCanonicalName()
                + ": IR not generated - property disabled (set "
                + GENERATE_TOOLS_KEY + " to '" + PROPERTY_ENABLER + "' or 'all')");
            return;
        }

        String fileName = properties.getProperty(OUTPUT_FILE_KEY, defaultFileName(language, properties));
        Map<String, Object> ir = buildIr(language, properties);

        Writer writer = null;
        try {
            FileObject fileObject = filer.createResource(StandardLocation.SOURCE_OUTPUT, "", fileName);
            writer = fileObject.openWriter();
            ObjectMapper mapper = new ObjectMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.writeValue(writer, ir);
        } catch (IOException ex) {
            throw new GeneratorException("Cannot write IR file", ex);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException ex) {
                    throw new GeneratorException("Cannot close IR file writer", ex);
                }
            }
        }

        System.out.println(getClass().getCanonicalName()
            + ": IR file generated successfully: " + fileName);
    }

    private String defaultFileName(Language language, Properties properties) {
        String resolvedLanguageName = resolveLanguageName(language, properties);
        String languageName = resolvedLanguageName == null || resolvedLanguageName.isEmpty()
            ? "language"
            : resolvedLanguageName;
        return languageName + DEFAULT_OUTPUT_SUFFIX;
    }

    private Map<String, Object> buildIr(Language language, Properties properties) {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("irVersion", IR_VERSION);

        Map<String, Object> producer = new LinkedHashMap<>();
        producer.put("name", "yajco");
        producer.put("version", PRODUCER_VERSION);
        root.put("producer", producer);

        Map<String, Object> languageInfo = new LinkedHashMap<>();
        languageInfo.put("name", resolveLanguageName(language, properties));
        languageInfo.put("description", resolveDescription(properties));
        languageInfo.put("version", resolveLanguageVersion(properties));
        languageInfo.put("entryConcept", getEntryConcept(language));
        languageInfo.put("fileExtensions", resolveFileExtensions(properties));
        languageInfo.put("comments", resolveCommentSyntax(language.getSkips(), properties));
        root.put("language", languageInfo);

        root.put("tokens", toTokens(language.getTokens(), language.getSkips()));
        root.put("concepts", toConcepts(language.getConcepts()));
        return root;
    }

    private String resolveLanguageName(Language language, Properties properties) {
        String overridden = properties.getProperty(LANGUAGE_NAME_KEY);
        if (overridden != null && !overridden.trim().isEmpty()) {
            return overridden.trim();
        }
        return language.getName();
    }

    private List<String> resolveFileExtensions(Properties properties) {
        String configured = properties.getProperty(FILE_EXTENSIONS_KEY);
        if (configured == null || configured.trim().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> extensions = new ArrayList<>();
        String[] parts = configured.split(",");
        for (String part : parts) {
            String value = part.trim();
            if (!value.isEmpty()) {
                extensions.add(value);
            }
        }
        return extensions;
    }

    private String resolveDescription(Properties properties) {
        String description = properties.getProperty("yajco.ir.description");
        return (description != null && !description.trim().isEmpty()) ? description.trim() : null;
    }

    private String resolveLanguageVersion(Properties properties) {
        String version = properties.getProperty("yajco.ir.version");
        return (version != null && !version.trim().isEmpty()) ? version.trim() : null;
    }

    /**
     * Resolves comment syntax, preferring explicit properties from @Language
     * over heuristic detection from skip patterns.
     */
    private Map<String, Object> resolveCommentSyntax(List<SkipDef> skips, Properties properties) {
        // Check for explicit comment properties (set by @Language annotation)
        String explicitLine = properties.getProperty("yajco.ir.lineComment");
        String explicitBlockStart = properties.getProperty("yajco.ir.blockComment.start");
        String explicitBlockEnd = properties.getProperty("yajco.ir.blockComment.end");

        boolean hasExplicit = (explicitLine != null && !explicitLine.isEmpty())
            || (explicitBlockStart != null && !explicitBlockStart.isEmpty());

        if (hasExplicit) {
            Map<String, Object> comments = new LinkedHashMap<>();
            comments.put("lineComment",
                (explicitLine != null && !explicitLine.isEmpty()) ? explicitLine : null);

            if (explicitBlockStart != null && !explicitBlockStart.isEmpty()
                && explicitBlockEnd != null && !explicitBlockEnd.isEmpty()) {
                List<String> block = new ArrayList<>();
                block.add(explicitBlockStart);
                block.add(explicitBlockEnd);
                comments.put("blockComment", block);
            } else {
                comments.put("blockComment", null);
            }
            return comments;
        }

        // Fall back to heuristic detection from skip patterns
        return detectCommentSyntax(skips);
    }

    private static String detectProducerVersion() {
        Package pkg = IrFilesGenerator.class.getPackage();
        if (pkg != null && pkg.getImplementationVersion() != null && !pkg.getImplementationVersion().isEmpty()) {
            return pkg.getImplementationVersion();
        }

        try (InputStream stream = IrFilesGenerator.class.getClassLoader().getResourceAsStream(
            "META-INF/maven/sk.tuke.yajco/yajco-ir-generator-module/pom.properties")) {
            if (stream != null) {
                Properties properties = new Properties();
                properties.load(stream);
                String version = properties.getProperty("version");
                if (version != null && !version.isEmpty()) {
                    return version;
                }
            }
        } catch (IOException ignored) {
        }

        return "dev";
    }

    private String getEntryConcept(Language language) {
        if (language.getConcepts().isEmpty()) {
            return null;
        }
        return language.getConcepts().get(0).getName();
    }

    // ── Tokens ──────────────────────────────────────────────────────────

    private List<Map<String, Object>> toTokens(List<TokenDef> tokens, List<SkipDef> skips) {
        List<Map<String, Object>> serialized = new ArrayList<>();
        for (TokenDef token : tokens) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", token.getName());
            item.put("pattern", token.getRegexp());
            item.put("channel", "default");
            item.put("role", classifyTokenRole(token.getName(), token.getRegexp(), "default"));
            serialized.add(item);
        }
        for (SkipDef skip : skips) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", "SKIP_" + serialized.size());
            item.put("pattern", skip.getRegexp());
            item.put("channel", "skip");
            item.put("role", classifySkipRole(skip.getRegexp()));
            serialized.add(item);
        }
        return serialized;
    }

    // ── Concepts ────────────────────────────────────────────────────────

    private List<Map<String, Object>> toConcepts(List<Concept> concepts) {
        List<Map<String, Object>> serialized = new ArrayList<>();
        for (Concept concept : concepts) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", concept.getName());
            item.put("abstract", concept.getConcreteSyntax().isEmpty());
            item.put("enum", concept.getPattern(yajco.model.pattern.impl.Enum.class) != null);
            item.put("parent", concept.getParent() == null ? null : concept.getParent().getName());

            Operator operator = concept.getPattern(Operator.class);
            if (operator != null) {
                Map<String, Object> operatorMap = new LinkedHashMap<>();
                operatorMap.put("precedence", operator.getPriority());
                operatorMap.put("associativity", operator.getAssociativity() == null ? null : operator.getAssociativity().name());
                item.put("operator", operatorMap);
            } else {
                item.put("operator", null);
            }

            Parentheses parentheses = concept.getPattern(Parentheses.class);
            if (parentheses != null) {
                Map<String, Object> parenthesesMap = new LinkedHashMap<>();
                parenthesesMap.put("left", parentheses.getLeft());
                parenthesesMap.put("right", parentheses.getRight());
                item.put("parentheses", parenthesesMap);
            } else {
                item.put("parentheses", null);
            }

            item.put("properties", toProperties(concept));
            item.put("syntax", toSyntax(concept));
            serialized.add(item);
        }
        return serialized;
    }

    // ── Properties ──────────────────────────────────────────────────────

    private List<Map<String, Object>> toProperties(Concept concept) {
        List<Map<String, Object>> serialized = new ArrayList<>();
        for (Property property : concept.getAbstractSyntax()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", property.getName());
            item.put("type", toType(property.getType()));
            item.put("identifier", property.getPattern(Identifier.class) != null);

            BindingInfo bindingInfo = findBindingInfo(concept, property.getName());
            item.put("reference", bindingInfo.hasReferences);
            item.put("default", null);

            List<List<String>> beforeAfter = findBeforeAfterTokens(concept, property.getName());
            Map<String, Object> syntax = new LinkedHashMap<>();
            syntax.put("token", bindingInfo.tokenName);
            syntax.put("before", beforeAfter.get(0));
            syntax.put("after", beforeAfter.get(1));
            syntax.put("separator", bindingInfo.separator);
            syntax.put("keyValueSeparator", null);
            syntax.put("references", bindingInfo.references);
            syntax.put("booleanValue", bindingInfo.booleanValue);

            // Identifier properties are symbol definitions regardless of References patterns
            String symbolRole = bindingInfo.symbolRole;
            if ("plain".equals(symbolRole) && property.getPattern(Identifier.class) != null) {
                symbolRole = "definition";
            }
            syntax.put("symbolRole", symbolRole);
            item.put("syntax", syntax);

            serialized.add(item);
        }
        return serialized;
    }

    // ── Syntax (notations) ──────────────────────────────────────────────

    /** Returns [beforeTokens, afterTokens] — contiguous TokenParts adjacent to the named property. */
    private List<List<String>> findBeforeAfterTokens(Concept concept, String propertyName) {
        for (Notation notation : concept.getConcreteSyntax()) {
            List<List<String>> result = findBeforeAfterInParts(notation.getParts(), propertyName);
            if (result != null) {
                return result;
            }
        }
        return Arrays.asList(new ArrayList<>(), new ArrayList<>());
    }

    private List<List<String>> findBeforeAfterInParts(List<NotationPart> parts, String propertyName) {
        for (int i = 0; i < parts.size(); i++) {
            NotationPart part = parts.get(i);
            if (part instanceof PropertyReferencePart) {
                PropertyReferencePart propPart = (PropertyReferencePart) part;
                if (propPart.getProperty() != null
                    && propertyName.equals(propPart.getProperty().getName())) {
                    List<String> before = new ArrayList<>();
                    List<String> after = new ArrayList<>();

                    for (int j = i - 1; j >= 0; j--) {
                        if (parts.get(j) instanceof TokenPart) {
                            before.add(0, ((TokenPart) parts.get(j)).getToken());
                        } else {
                            break;
                        }
                    }

                    for (int j = i + 1; j < parts.size(); j++) {
                        if (parts.get(j) instanceof TokenPart) {
                            after.add(((TokenPart) parts.get(j)).getToken());
                        } else {
                            break;
                        }
                    }

                    return Arrays.asList(before, after);
                }
            }
            // Recurse into compound parts
            if (part instanceof CompoundNotationPart) {
                List<List<String>> nested = findBeforeAfterInParts(
                    ((CompoundNotationPart) part).getParts(), propertyName);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private List<Map<String, Object>> toSyntax(Concept concept) {
        List<Map<String, Object>> serialized = new ArrayList<>();
        for (Notation notation : concept.getConcreteSyntax()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("parts", toNotationParts(notation.getParts()));

            Factory factory = notation.getPattern(Factory.class);
            item.put("factory", factory == null ? null : factory.getName());

            serialized.add(item);
        }
        return serialized;
    }

    private List<Map<String, Object>> toNotationParts(List<NotationPart> parts) {
        List<Map<String, Object>> serialized = new ArrayList<>();
        for (NotationPart part : parts) {
            serialized.add(toNotationPart(part));
        }
        return serialized;
    }

    private Map<String, Object> toNotationPart(NotationPart part) {
        Map<String, Object> item = new LinkedHashMap<>();
        if (part instanceof TokenPart) {
            item.put("kind", "token");
            item.put("token", ((TokenPart) part).getToken());
            return item;
        }
        if (part instanceof PropertyReferencePart) {
            PropertyReferencePart propertyPart = (PropertyReferencePart) part;
            item.put("kind", "property");
            item.put("name", propertyPart.getProperty() == null ? null : propertyPart.getProperty().getName());
            item.put("patterns", toNotationPatterns(propertyPart.getPatterns()));
            item.put("symbolRole", resolvePropertyPartSymbolRole(propertyPart));
            return item;
        }
        if (part instanceof LocalVariablePart) {
            LocalVariablePart localVariablePart = (LocalVariablePart) part;
            item.put("kind", "local");
            item.put("name", localVariablePart.getName());
            item.put("type", toType(localVariablePart.getType()));
            item.put("patterns", toNotationPatterns(localVariablePart.getPatterns()));
            item.put("symbolRole", resolveBindingSymbolRole(localVariablePart));
            return item;
        }
        if (part instanceof OptionalPart || part instanceof UnorderedParamPart || part instanceof MixedRepetitionPart) {
            CompoundNotationPart compound = (CompoundNotationPart) part;
            item.put("kind", compoundKind(compound));
            item.put("parts", toNotationParts(compound.getParts()));
            return item;
        }

        item.put("kind", "unknown");
        item.put("className", part.getClass().getName());
        return item;
    }

    private String compoundKind(CompoundNotationPart part) {
        if (part instanceof OptionalPart) {
            return "optional";
        }
        if (part instanceof UnorderedParamPart) {
            return "unordered";
        }
        return "mixedRepetition";
    }

    // ── Notation patterns (kind-discriminated) ──────────────────────────

    private List<Map<String, Object>> toNotationPatterns(List<NotationPartPattern> patterns) {
        List<Map<String, Object>> serialized = new ArrayList<>();
        for (NotationPartPattern pattern : patterns) {
            Map<String, Object> item = new LinkedHashMap<>();
            if (pattern instanceof Token) {
                item.put("kind", "token");
                item.put("token", ((Token) pattern).getName());
            } else if (pattern instanceof Separator) {
                item.put("kind", "separator");
                item.put("separator", ((Separator) pattern).getValue());
            } else if (pattern instanceof References) {
                References references = (References) pattern;
                item.put("kind", "references");
                item.put("concept", references.getConcept() == null ? null : references.getConcept().getName());
                item.put("property", references.getProperty() == null ? null : references.getProperty().getName());
            } else if (pattern instanceof yajco.model.pattern.impl.BooleanValue) {
                yajco.model.pattern.impl.BooleanValue booleanValue = (yajco.model.pattern.impl.BooleanValue) pattern;
                item.put("kind", "booleanValue");
                item.put("trueToken", booleanValue.getTrueToken());
                item.put("falseToken", booleanValue.getFalseToken());
            } else if (pattern instanceof Range) {
                Range range = (Range) pattern;
                item.put("kind", "range");
                item.put("minOccurs", range.getMinOccurs());
                item.put("maxOccurs", range.getMaxOccurs() == Range.INFINITY ? null : range.getMaxOccurs());
            } else if (pattern instanceof Shared) {
                Shared shared = (Shared) pattern;
                item.put("kind", "shared");
                item.put("value", shared.getValue());
                item.put("separator", shared.getSeparator());
            } else if (pattern instanceof UniqueValues) {
                item.put("kind", "uniqueValues");
            } else {
                item.put("kind", pattern.getClass().getSimpleName().toLowerCase(Locale.ROOT));
            }
            serialized.add(item);
        }
        return serialized;
    }

    // ── Type serialization (normalized primitive names) ─────────────────

    private Map<String, Object> toType(Type type) {
        Map<String, Object> serialized = new LinkedHashMap<>();
        if (type instanceof PrimitiveType) {
            serialized.put("kind", "primitive");
            serialized.put("name", normalizePrimitiveName(((PrimitiveType) type).getPrimitiveTypeConst()));
            return serialized;
        }
        if (type instanceof ReferenceType) {
            serialized.put("kind", "concept");
            ReferenceType referenceType = (ReferenceType) type;
            serialized.put("name", referenceType.getConcept() == null ? null : referenceType.getConcept().getName());
            return serialized;
        }
        if (type instanceof ListType || type instanceof ListTypeWithShared) {
            serialized.put("kind", "list");
            serialized.put("itemType", toType(((ComponentType) type).getComponentType()));
            return serialized;
        }
        if (type instanceof SetType || type instanceof OrderedSetType) {
            serialized.put("kind", "set");
            serialized.put("itemType", toType(((ComponentType) type).getComponentType()));
            return serialized;
        }
        if (type instanceof OptionalType) {
            serialized.put("kind", "optional");
            serialized.put("itemType", toType(((ComponentType) type).getComponentType()));
            return serialized;
        }
        if (type instanceof ArrayType) {
            serialized.put("kind", "array");
            serialized.put("itemType", toType(((ComponentType) type).getComponentType()));
            return serialized;
        }

        serialized.put("kind", "unknown");
        serialized.put("className", type.getClass().getName());
        return serialized;
    }

    private static String normalizePrimitiveName(PrimitiveTypeConst primitiveType) {
        switch (primitiveType) {
            case STRING:
                return "string";
            case INTEGER:
                return "integer";
            case REAL:
                return "float";
            case BOOLEAN:
                return "boolean";
            default:
                return primitiveType.name().toLowerCase(Locale.ROOT);
        }
    }

    // ── Binding info extraction ─────────────────────────────────────────

    private BindingInfo findBindingInfo(Concept concept, String propertyName) {
        for (Notation notation : concept.getConcreteSyntax()) {
            BindingInfo info = findBindingInfoInParts(notation.getParts(), propertyName);
            if (info != null) {
                return info;
            }
        }
        return BindingInfo.empty();
    }

    private BindingInfo findBindingInfoInParts(List<NotationPart> parts, String propertyName) {
        for (NotationPart part : parts) {
            if (part instanceof PropertyReferencePart) {
                PropertyReferencePart propertyPart = (PropertyReferencePart) part;
                if (propertyPart.getProperty() != null && propertyName.equals(propertyPart.getProperty().getName())) {
                    return extractBindingInfo(propertyPart);
                }
            }
            if (part instanceof CompoundNotationPart) {
                BindingInfo nested = findBindingInfoInParts(((CompoundNotationPart) part).getParts(), propertyName);
                if (nested != null) {
                    return nested;
                }
            }
        }
        return null;
    }

    private BindingInfo extractBindingInfo(BindingNotationPart bindingPart) {
        BindingInfo info = BindingInfo.empty();
        info.symbolRole = resolveBindingSymbolRole(bindingPart);
        for (NotationPartPattern pattern : bindingPart.getPatterns()) {
            if (pattern instanceof Token) {
                info.tokenName = ((Token) pattern).getName();
            } else if (pattern instanceof Separator) {
                info.separator = ((Separator) pattern).getValue();
            } else if (pattern instanceof References) {
                References references = (References) pattern;
                info.hasReferences = true;
                Map<String, Object> referencesMap = new LinkedHashMap<>();
                referencesMap.put("concept", references.getConcept() == null ? null : references.getConcept().getName());
                referencesMap.put("property", references.getProperty() == null ? null : references.getProperty().getName());
                info.references = referencesMap;
            } else if (pattern instanceof yajco.model.pattern.impl.BooleanValue) {
                yajco.model.pattern.impl.BooleanValue booleanValue = (yajco.model.pattern.impl.BooleanValue) pattern;
                Map<String, Object> booleanValueMap = new LinkedHashMap<>();
                booleanValueMap.put("trueToken", booleanValue.getTrueToken());
                booleanValueMap.put("falseToken", booleanValue.getFalseToken());
                info.booleanValue = booleanValueMap;
            }
        }
        return info;
    }

    // ── Symbol role resolution ──────────────────────────────────────────

    private String resolvePropertyPartSymbolRole(PropertyReferencePart propertyPart) {
        if (propertyPart.getProperty() == null) {
            return "plain";
        }

        String role = resolveBindingSymbolRole(propertyPart);
        if (!"plain".equals(role)) {
            return role;
        }

        return propertyPart.getProperty().getPattern(Identifier.class) != null ? "definition" : "plain";
    }

    private String resolveBindingSymbolRole(BindingNotationPart bindingPart) {
        for (NotationPartPattern pattern : bindingPart.getPatterns()) {
            if (pattern instanceof References) {
                return "reference";
            }
        }
        return "plain";
    }

    // ── Token role classification ───────────────────────────────────────

    private String classifyTokenRole(String name, String pattern, String channel) {
        if ("skip".equals(channel)) {
            return "whitespace";
        }

        String lowerName = name == null ? "" : name.toLowerCase(Locale.ROOT);
        String lowerPattern = pattern == null ? "" : pattern.toLowerCase(Locale.ROOT);

        if (lowerName.contains("comment") || lowerPattern.contains("comment")
            || lowerPattern.contains("//") || lowerPattern.contains("/*")
            || (lowerPattern.contains("--") && lowerPattern.length() > 2)) {
            return "comment";
        }
        // Shell-style comment
        if (pattern != null && pattern.startsWith("#") && pattern.length() > 1
            && !Character.isLetterOrDigit(pattern.charAt(1))) {
            return "comment";
        }

        if (lowerName.contains("ident") || lowerName.contains("name")
            || lowerName.equals("id")) {
            return "identifier";
        }
        if (lowerName.contains("string") || lowerPattern.contains("string") || lowerPattern.contains("escaped")) {
            return "string";
        }
        if (lowerName.contains("number") || lowerName.equals("value") || lowerPattern.contains("[0-9") || lowerPattern.contains("digit")) {
            return "number";
        }
        if (isBracketPattern(pattern)) {
            return "bracket";
        }
        if (isDelimiterPattern(pattern)) {
            return "delimiter";
        }
        if (isOperatorPattern(pattern)) {
            return "operator";
        }

        // Fallback: pattern looks like a typical identifier regex
        if (lowerPattern.startsWith("[a-z") || lowerPattern.startsWith("[_a-z")
            || lowerPattern.startsWith("[a-za-z") || lowerPattern.startsWith("[_a-za-z")) {
            return "identifier";
        }

        return "keyword";
    }

    private String classifySkipRole(String pattern) {
        String lowerPattern = pattern == null ? "" : pattern.toLowerCase(Locale.ROOT);
        if (lowerPattern.contains("//") || lowerPattern.contains("/*")
            || (lowerPattern.contains("--") && lowerPattern.length() > 2) || lowerPattern.contains("comment")) {
            return "comment";
        }
        // Shell-style: "#.*", "#[^\n]*"
        if (lowerPattern.startsWith("#") && lowerPattern.length() > 1
            && !Character.isLetterOrDigit(lowerPattern.charAt(1))) {
            return "comment";
        }
        return "whitespace";
    }

    private boolean isBracketPattern(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return false;
        }
        String trimmed = pattern.trim();
        return trimmed.equals("(") || trimmed.equals(")") || trimmed.equals("{")
            || trimmed.equals("}") || trimmed.equals("[") || trimmed.equals("]")
            || trimmed.equals("\\(") || trimmed.equals("\\)")
            || trimmed.equals("\\{") || trimmed.equals("\\}")
            || trimmed.equals("\\[") || trimmed.equals("\\]")
            // Regex character-class forms: [(], [)], [{], [}], [[], []]
            || trimmed.equals("[(]") || trimmed.equals("[)]")
            || trimmed.equals("[{]") || trimmed.equals("[}]")
            || trimmed.equals("[[]") || trimmed.equals("[]]");
    }

    private boolean isDelimiterPattern(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return false;
        }
        String trimmed = pattern.trim();
        return trimmed.equals(",") || trimmed.equals(";") || trimmed.equals(":") || trimmed.equals(".");
    }

    private boolean isOperatorPattern(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return false;
        }
        if (isBracketPattern(pattern) || isDelimiterPattern(pattern)) {
            return false;
        }
        return pattern.matches("^[^a-zA-Z0-9\\s]+$");
    }

    // ── Comment syntax detection ──────────────────────────────────────────

    /**
     * Scans skip tokens to detect line and block comment syntax.
     * Returns a CommentSyntax map or null when no comments are found.
     */
    private Map<String, Object> detectCommentSyntax(List<SkipDef> skips) {
        String lineComment = null;
        List<String> blockComment = null;

        for (SkipDef skip : skips) {
            String pattern = skip.getRegexp();
            if (pattern == null) {
                continue;
            }

            // Block comment: /\*...\*/ pattern
            if (pattern.contains("/*") && pattern.contains("*/")) {
                blockComment = new ArrayList<>();
                blockComment.add("/*");
                blockComment.add("*/");
                continue;
            }

            // Line comments — extract the prefix from common patterns
            if (lineComment == null) {
                lineComment = extractLineCommentPrefix(pattern);
            }
        }

        if (lineComment == null && blockComment == null) {
            return null;
        }

        Map<String, Object> comments = new LinkedHashMap<>();
        comments.put("lineComment", lineComment);
        comments.put("blockComment", blockComment);
        return comments;
    }

    /**
     * Extracts a line comment prefix from a regex pattern.
     * Recognizes common forms: // .*, # .*, -- .*, etc.
     * Returns null if the pattern is not a recognized line comment pattern.
     */
    private String extractLineCommentPrefix(String pattern) {
        // Patterns like "//.*", "//[^\n]*", etc.
        if (pattern.startsWith("//")) {
            return "//";
        }
        // Shell-style: "#.*", "#[^\n]*", "#.*\n", etc.
        if (pattern.startsWith("#") && pattern.length() > 1
            && !Character.isLetterOrDigit(pattern.charAt(1))) {
            return "#";
        }
        // SQL-style: "--.*", "--[^\n]*", etc.
        if (pattern.startsWith("--")) {
            return "--";
        }
        return null;
    }

    // ── Inner types ─────────────────────────────────────────────────────

    private static final class BindingInfo {
        private String tokenName;
        private String separator;
        private boolean hasReferences;
        private Map<String, Object> references;
        private Map<String, Object> booleanValue;
        private String symbolRole;

        private static BindingInfo empty() {
            BindingInfo info = new BindingInfo();
            info.references = null;
            info.booleanValue = null;
            info.symbolRole = "plain";
            return info;
        }
    }
}
