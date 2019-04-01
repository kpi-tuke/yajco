package yajco.generator.visitorgen;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import yajco.GeneratorHelper;
import yajco.generator.AbstractFileGenerator;
import yajco.generator.GeneratorException;
import yajco.generator.util.Utilities;
import yajco.model.*;
import yajco.model.pattern.impl.Token;
import yajco.model.type.*;

public class VisitorGenerator extends AbstractFileGenerator {

    private static final String PROPERTY_ENABLER = "visitor";

    protected static final String TEMPLATE_PACKAGE = "templates";
    protected static final String VISITOR_PACKAGE = "visitor";
    protected static final String VISITOR_CLASS_NAME = "Visitor";
    private final String template;
    protected final VelocityEngine velocityEngine = new VelocityEngine();
    /**
     * Maps properties from abstract syntax to token names in concrete syntax.
     */
    private Map<Property, String> propertyToTokenNameMap = new HashMap<>();
    /**
     * Maps each token name to all the (primitive) types used for that token.
     */
    private Map<String, Set<String>> tokenToUsedTypesMap = new HashMap<>();

    public VisitorGenerator() {
        template = "Visitor.java.vm";
    }

    @Override
    protected boolean shouldGenerate() {
        String option = properties.getProperty(GeneratorHelper.GENERATE_TOOLS_KEY, "").toLowerCase();
        if (!option.contains("all") && !option.contains(PROPERTY_ENABLER) && !option.contains("printer")) {
            System.out.println(getClass().getCanonicalName()+": Visitor not generated - property disabled (set "+GeneratorHelper.GENERATE_TOOLS_KEY+" to '"+PROPERTY_ENABLER+"' or 'all')");
            return false;
        }
        return true;
    }

    @Override
    public void generate(Language language, Writer writer) {
        fillMaps(language);
        try {
            InputStreamReader reader = new InputStreamReader(getClass().getResourceAsStream(TEMPLATE_PACKAGE + "/" + template), "utf-8");
            VelocityContext context = new VelocityContext();
            context.put("Utilities", Utilities.class);
            context.put("ModelUtilities", yajco.model.utilities.Utilities.class);
            context.put("language", language);
            context.put("package", getPackageName());
            context.put("className", getClassName());
            context.put("arrayTypeClassName", ArrayType.class.getCanonicalName());
            context.put("listTypeClassName", ListType.class.getCanonicalName());
            context.put("setTypeClassName", SetType.class.getCanonicalName());
            context.put("referenceTypeClassName", ReferenceType.class.getCanonicalName());
            context.put("primitiveTypeClassName", PrimitiveType.class.getCanonicalName());
            context.put("propertyToTokenNameMap", propertyToTokenNameMap);
            context.put("tokenToUsedTypesMap", tokenToUsedTypesMap);
            velocityEngine.evaluate(context, writer, "", reader);
            writer.flush();
        } catch (IOException ex) {
            throw new GeneratorException("Cannot generate visitor class", ex);
        }
    }

    @Override
    public String getPackageName() {
        return VISITOR_PACKAGE;
    }

    @Override
    public String getFileName() {
        return getClassName() + ".java";
    }

    @Override
    public String getClassName() {
        return VISITOR_CLASS_NAME;
    }

    /**
     * Fills propertyToTokenNameMap and tokenToUsedTypesMap from the language model.
     * These are used in the visitor template to generate "visit token type" methods and calls.
     * @param language Language model.
     */
    private void fillMaps(Language language) {
        // Clear the maps just in case there is ever a need to reuse VisitorGenerator.
        propertyToTokenNameMap.clear();
        tokenToUsedTypesMap.clear();

        Map<String, TokenDef> mapTokens = new HashMap<>();
        for (TokenDef tokenDef : language.getTokens()) {
            mapTokens.put(tokenDef.getName().toUpperCase(), tokenDef);
        }

        for (Concept c : language.getConcepts()) {
            for (Notation notation : c.getConcreteSyntax()) {
                for (NotationPart part : notation.getParts()) {
                    if (part instanceof PropertyReferencePart) {
                        PropertyReferencePart propertyReferencePart = (PropertyReferencePart) part;
                        Property property = propertyReferencePart.getProperty();

                        // Only process primitive types or collections of primitives
                        Type type = property.getType();
                        if (type instanceof ReferenceType) {
                            continue;
                        }

                        if (type instanceof ComponentType) {
                            Type innerType = ((ComponentType) type).getComponentType();
                            if (!(innerType instanceof PrimitiveType)) {
                                continue;
                            }
                            type = innerType;
                        }

                        // Find out the token name for the parameter
                        String tokenName = null;
                        Token tokenPattern = (Token) propertyReferencePart.getPattern(Token.class);
                        if (tokenPattern != null) {
                            tokenName = tokenPattern.getName().toUpperCase();
                        } else {
                            String possibleToken = property.getName().toUpperCase();
                            if (mapTokens.containsKey(possibleToken)) {
                                tokenName = possibleToken;
                            } else if (possibleToken.endsWith("S")) {
                                possibleToken = possibleToken.substring(0, possibleToken.length() - 1);
                                if (mapTokens.containsKey(possibleToken)) {
                                    tokenName = possibleToken;
                                }
                            }

                            if (tokenName == null) {
                                throw new RuntimeException("Failure to deduce token from parameter name");
                            }
                        }

                        // Fill the maps
                        propertyToTokenNameMap.put(property, tokenName);

                        Set<String> usedTypes = tokenToUsedTypesMap.computeIfAbsent(tokenName, k -> new HashSet<>());
                        usedTypes.add(Utilities.getTypeName(type));
                    }
                }
            }
        }
    }
}
