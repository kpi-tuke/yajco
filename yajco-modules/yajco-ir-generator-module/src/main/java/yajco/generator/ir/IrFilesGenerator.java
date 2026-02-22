package yajco.generator.ir;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import yajco.generator.FilesGenerator;
import yajco.generator.GeneratorException;
import yajco.model.BindingNotationPart;
import yajco.model.CompoundNotationPart;
import yajco.model.Concept;
import yajco.model.Language;
import yajco.model.LocalVariablePart;
import yajco.model.MixedRepetitionPart;
import yajco.model.Notation;
import yajco.model.NotationPart;
import yajco.model.OptionalPart;
import yajco.model.Property;
import yajco.model.PropertyReferencePart;
import yajco.model.SkipDef;
import yajco.model.TokenDef;
import yajco.model.TokenPart;
import yajco.model.UnorderedParamPart;
import yajco.model.pattern.NotationPartPattern;
import yajco.model.pattern.impl.Identifier;
import yajco.model.pattern.impl.Operator;
import yajco.model.pattern.impl.References;
import yajco.model.pattern.impl.Separator;
import yajco.model.pattern.impl.Token;
import yajco.model.type.ArrayType;
import yajco.model.type.ComponentType;
import yajco.model.type.ListType;
import yajco.model.type.ListTypeWithShared;
import yajco.model.type.OptionalType;
import yajco.model.type.OrderedSetType;
import yajco.model.type.PrimitiveType;
import yajco.model.type.ReferenceType;
import yajco.model.type.SetType;
import yajco.model.type.Type;

import javax.annotation.processing.Filer;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.InputStream;
import java.io.IOException;
import java.io.Writer;
import java.util.Locale;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
    }

    private String defaultFileName(Language language, Properties properties) {
        String resolvedLanguageName = resolveLanguageName(language, properties);
        String languageName = resolvedLanguageName == null || resolvedLanguageName.isEmpty()
                ? "language"
                : resolvedLanguageName;
        return languageName + DEFAULT_OUTPUT_SUFFIX;
    }

    private Map<String, Object> buildIr(Language language, Properties properties) {
        Map<String, Object> root = new LinkedHashMap<String, Object>();
        root.put("irVersion", IR_VERSION);

        Map<String, Object> producer = new LinkedHashMap<String, Object>();
        producer.put("name", "yajco");
        producer.put("version", PRODUCER_VERSION);
        root.put("producer", producer);

        Map<String, Object> languageInfo = new LinkedHashMap<String, Object>();
        languageInfo.put("name", resolveLanguageName(language, properties));
        languageInfo.put("entryConcept", getEntryConcept(language));
        languageInfo.put("fileExtensions", resolveFileExtensions(properties));
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
            return new ArrayList<String>();
        }

        List<String> extensions = new ArrayList<String>();
        String[] parts = configured.split(",");
        for (String part : parts) {
            String value = part.trim();
            if (!value.isEmpty()) {
                extensions.add(value);
            }
        }
        return extensions;
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

    private List<Map<String, Object>> toTokens(List<TokenDef> tokens, List<SkipDef> skips) {
        List<Map<String, Object>> serialized = new ArrayList<Map<String, Object>>();
        for (TokenDef token : tokens) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("name", token.getName());
            item.put("pattern", token.getRegexp());
            item.put("channel", "default");
            item.put("role", classifyTokenRole(token.getName(), token.getRegexp(), "default"));
            serialized.add(item);
        }
        for (SkipDef skip : skips) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("name", "SKIP_" + serialized.size());
            item.put("pattern", skip.getRegexp());
            item.put("channel", "skip");
            item.put("role", "whitespace");
            serialized.add(item);
        }
        return serialized;
    }

    private List<Map<String, Object>> toConcepts(List<Concept> concepts) {
        List<Map<String, Object>> serialized = new ArrayList<Map<String, Object>>();
        for (Concept concept : concepts) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("name", concept.getName());
            item.put("abstract", concept.getConcreteSyntax().isEmpty());
            item.put("parent", concept.getParent() == null ? null : concept.getParent().getName());

            Operator operator = concept.getPattern(Operator.class);
            if (operator != null) {
                Map<String, Object> operatorMap = new LinkedHashMap<String, Object>();
                operatorMap.put("precedence", operator.getPriority());
                operatorMap.put("associativity", operator.getAssociativity() == null ? null : operator.getAssociativity().name());
                item.put("operator", operatorMap);
            }

            item.put("properties", toProperties(concept));
            item.put("syntax", toSyntax(concept));
            serialized.add(item);
        }
        return serialized;
    }

    private List<Map<String, Object>> toProperties(Concept concept) {
        List<Map<String, Object>> serialized = new ArrayList<Map<String, Object>>();
        for (Property property : concept.getAbstractSyntax()) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("name", property.getName());
            item.put("type", toType(property.getType()));
            item.put("identifier", property.getPattern(Identifier.class) != null);

            BindingInfo bindingInfo = findBindingInfo(concept, property.getName());
            item.put("reference", bindingInfo.hasReferences);

            Map<String, Object> syntax = new LinkedHashMap<String, Object>();
            syntax.put("token", bindingInfo.tokenName);
            syntax.put("separator", bindingInfo.separator);
            syntax.put("references", bindingInfo.references);
            syntax.put("symbolRole", bindingInfo.symbolRole);
            item.put("syntax", syntax);

            serialized.add(item);
        }
        return serialized;
    }

    private List<Map<String, Object>> toSyntax(Concept concept) {
        List<Map<String, Object>> serialized = new ArrayList<Map<String, Object>>();
        for (Notation notation : concept.getConcreteSyntax()) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("parts", toNotationParts(notation.getParts()));
            serialized.add(item);
        }
        return serialized;
    }

    private List<Map<String, Object>> toNotationParts(List<NotationPart> parts) {
        List<Map<String, Object>> serialized = new ArrayList<Map<String, Object>>();
        for (NotationPart part : parts) {
            serialized.add(toNotationPart(part));
        }
        return serialized;
    }

    private Map<String, Object> toNotationPart(NotationPart part) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
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

    private List<Map<String, Object>> toNotationPatterns(List<NotationPartPattern> patterns) {
        List<Map<String, Object>> serialized = new ArrayList<Map<String, Object>>();
        for (NotationPartPattern pattern : patterns) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("name", pattern.getClass().getSimpleName());
            if (pattern instanceof Token) {
                item.put("token", ((Token) pattern).getName());
            } else if (pattern instanceof Separator) {
                item.put("separator", ((Separator) pattern).getValue());
            } else if (pattern instanceof References) {
                References references = (References) pattern;
                Map<String, Object> refs = new LinkedHashMap<String, Object>();
                refs.put("concept", references.getConcept() == null ? null : references.getConcept().getName());
                refs.put("property", references.getProperty() == null ? null : references.getProperty().getName());
                item.put("references", refs);
            }
            serialized.add(item);
        }
        return serialized;
    }

    private Map<String, Object> toType(Type type) {
        Map<String, Object> serialized = new LinkedHashMap<String, Object>();
        if (type instanceof PrimitiveType) {
            serialized.put("kind", "primitive");
            serialized.put("name", ((PrimitiveType) type).getPrimitiveTypeConst().name());
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
                Map<String, Object> referencesMap = new LinkedHashMap<String, Object>();
                referencesMap.put("concept", references.getConcept() == null ? null : references.getConcept().getName());
                referencesMap.put("property", references.getProperty() == null ? null : references.getProperty().getName());
                info.references = referencesMap;
            }
        }
        return info;
    }

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

    private String classifyTokenRole(String name, String pattern, String channel) {
        if ("skip".equals(channel)) {
            return "whitespace";
        }

        String lowerName = name == null ? "" : name.toLowerCase(Locale.ROOT);
        String lowerPattern = pattern == null ? "" : pattern.toLowerCase(Locale.ROOT);

        if (lowerName.contains("ident") || lowerName.contains("name") || lowerPattern.contains("cname")) {
            return "identifier";
        }
        if (lowerName.contains("string") || lowerPattern.contains("string") || lowerPattern.contains("escaped")) {
            return "string";
        }
        if (lowerName.contains("number") || lowerName.contains("value") || lowerPattern.contains("[0-9") || lowerPattern.contains("digit")) {
            return "number";
        }
        if (isPunctuationPattern(pattern)) {
            return "operator";
        }

        return "keyword";
    }

    private boolean isPunctuationPattern(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return false;
        }

        if (pattern.startsWith("[") && pattern.endsWith("]") && pattern.length() <= 4) {
            return true;
        }

        return pattern.matches("^[^a-zA-Z0-9\\s]+$");
    }

    private static final class BindingInfo {
        private String tokenName;
        private String separator;
        private boolean hasReferences;
        private Map<String, Object> references;
        private String symbolRole;

        private static BindingInfo empty() {
            BindingInfo info = new BindingInfo();
            info.references = new LinkedHashMap<String, Object>();
            info.symbolRole = "plain";
            return info;
        }
    }
}
