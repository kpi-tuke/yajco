package yajco.generator.parsergen.antlr4.translator;

import yajco.ReferenceResolver;
import yajco.generator.GeneratorException;
import yajco.generator.parsergen.Conversions;
import yajco.generator.parsergen.antlr4.model.*;
import yajco.generator.util.RegexUtil;
import yajco.generator.util.Utilities;
import yajco.model.*;
import yajco.model.pattern.impl.*;
import yajco.model.pattern.impl.Enum;
import yajco.model.type.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Translates YAJCo model to ANTLR4 grammar model
 * (yajco.model.Language -> yajco.generator.parsergen.antlr4.model.Grammar)
 */
public class ModelTranslator {
    public static final String RETURN_VAR_NAME = "_retval";
    private final static String REFERENCE_RESOLVER_CLASS_NAME = ReferenceResolver.class.getCanonicalName();

    private static final Conversions conversions = new Conversions();

    private final Language language;
    private final String parserClassName;
    private final String parserPackageName;

    private static class Production {
        String returns;
        List<Alternative> alternatives;
    }

    private static class Alternative {
        Parentheses par;
        Operator op;
        SequencePart sequence;
    }

    private static class LabelProvider {
        private Map<String, Integer> counters = new HashMap<>();

        public String createLabel(String ruleName) {
            if (counters.containsKey(ruleName)) {
                counters.put(ruleName, counters.get(ruleName) + 1);
            } else {
                counters.put(ruleName, 1);
            }
            return ruleName + "_" + counters.get(ruleName);
        }
    }

    private final Map<String, Production> productions = new LinkedHashMap<>();

    private final Map<String, String> tokens = new LinkedHashMap<>();

    public ModelTranslator(Language language, String parserClassName, String parserPackageName) {
        this.language = language;
        this.parserClassName = parserClassName;
        this.parserPackageName = parserPackageName;
    }

    public Grammar translate() {
        for (TokenDef tokenDef : this.language.getTokens()) {
            this.tokens.put(convertTokenName(tokenDef.getName()), tokenDef.getRegexp());
        }

        for (Concept c : this.language.getConcepts()) {
            for (Notation n : c.getConcreteSyntax()) {
                for (NotationPart part : n.getParts()) {
                    if (part instanceof TokenPart) {
                        String tokenName = ((TokenPart) part).getToken();
                        this.tokens.putIfAbsent(convertTokenName(tokenName), Utilities.encodeStringIntoRegex(tokenName));
                    }
                }
            }
        }

        // Process concepts starting from top-level ones.
        for (Concept c : this.language.getConcepts()) {
            if (c.getParent() == null) {
                processTopLevelConcept(c);
            }
        }

        // In an IS-A relationship, we merge all subconcept rules into top-level rules to prevent potential indirect
        // left recursion. However, sometimes the subconcept is also in a HAS-A relationship with another concept and
        // then we need to create a new production rule for it.
        for (Concept c : this.language.getConcepts()) {
            for (Notation n : c.getConcreteSyntax()) {
                for (NotationPart part : n.getParts()) {
                    if (part instanceof PropertyReferencePart) {
                        Type type = ((PropertyReferencePart) part).getProperty().getType();
                        ReferenceType referenceType = null;

                        if (type instanceof ReferenceType) {
                            referenceType = (ReferenceType) type;
                        } else if (type instanceof ComponentType) {
                            Type innerType = ((ComponentType) type).getComponentType();
                            if (innerType instanceof ReferenceType) {
                                referenceType = (ReferenceType) innerType;
                            }
                        }

                        if (referenceType != null) {
                            if (!this.productions.containsKey(
                                    convertProductionName(referenceType.getConcept().getConceptName()))) {
                                processTopLevelConcept(referenceType.getConcept());
                            }
                        }
                    }
                }
            }
        }

        List<ParserRule> parserRules = translateProductions();
        // Intentionally empty as we will use a custom lexer
        List<LexicalRule> lexicalRules = new ArrayList<>();

        // Forward declaration of tokens to silence ANTLR warnings
        List<String> implicitTokens = new ArrayList<>();
        for (Map.Entry<String, String> entry : getOrderedTokens().entrySet()) {
            implicitTokens.add(entry.getKey());
        }

        return new Grammar(
                this.parserClassName,
                "package " + this.parserPackageName + ";",
                implicitTokens,
                parserRules,
                lexicalRules);
    }

    public Map<String, String> getOrderedTokens() {
        Map<String, String> acyclicTerminals = new LinkedHashMap<>();
        Map<String, String> cyclicTerminals = new LinkedHashMap<>();

        for (Map.Entry<String, String> entry : this.tokens.entrySet()) {
            String symbolName = entry.getKey();
            String regex = entry.getValue();

            if (RegexUtil.isCyclic(regex)) {
                cyclicTerminals.put(symbolName, regex);
            } else {
                acyclicTerminals.put(symbolName, regex);
            }
        }

        // combine maps together, in the order of acyclic to cyclic
        for (Map.Entry<String, String> entry : cyclicTerminals.entrySet()) {
            acyclicTerminals.put(entry.getKey(), entry.getValue());
        }

        return acyclicTerminals;
    }

    private List<ParserRule> translateProductions() {
        List<ParserRule> parserRules = new ArrayList<>();
        parserRules.add(makeMainRule());

        for (Map.Entry<String, Production> entry : this.productions.entrySet()) {
            String name = entry.getKey();
            Production production = entry.getValue();
            AlternativePart altPart = new AlternativePart(
                    production.alternatives.stream()
                            .map(alt -> alt.sequence)
                            .collect(Collectors.toCollection(ArrayList::new)));
            parserRules.add(new ParserRule(name, production.returns, altPart));
        }

        return parserRules;
    }

    private void processTopLevelConcept(Concept concept) {
        List<Alternative> unresolvedAlts = new ArrayList<>();
        boolean isAbstract = (concept.getAbstractSyntax().isEmpty() && concept.getConcreteSyntax().isEmpty());

        if (isAbstract) {
            // Depth-first search for descendant leaves (concrete concepts).
            Stack<Concept> conceptsToVisit = new Stack<>();
            conceptsToVisit.push(concept);
            while (!conceptsToVisit.isEmpty()) {
                Concept c = conceptsToVisit.pop();
                Set<Concept> subConcepts = getDirectSubconcepts(c);
                if (subConcepts.isEmpty()) {
                    // Found a descendant leaf.
                    unresolvedAlts.addAll(processConcreteConcept(c));
                } else {
                    for (Concept subConcept : subConcepts) {
                        conceptsToVisit.push(subConcept);
                    }
                }
            }
        } else {
            unresolvedAlts = processConcreteConcept(concept);
        }

        // Group operator alternatives by priority.
        Map<Integer, List<Alternative>> operatorGroups = unresolvedAlts.stream()
                .filter(alt -> alt.op != null)
                .sorted(Comparator.comparingInt((Alternative alt) -> alt.op.getPriority()).reversed())
                .collect(Collectors.groupingBy(alt -> alt.op.getPriority(), LinkedHashMap::new, Collectors.toList()));

        // Merge alternatives with the same priority.
        List<Alternative> operatorAlts = operatorGroups.entrySet().stream()
                .map(entry -> entry.getValue())
                .map(this::mergeOperatorAlternatives)
                .collect(Collectors.toCollection(ArrayList::new));

        // Prepare final list of alternatives.
        List<Alternative> alts = new ArrayList<>();

        Parentheses par = (Parentheses) concept.getPattern(Parentheses.class);
        if (par != null) {
            List<Part> parts = new ArrayList<>();

            String lparToken;
            if (this.language.getToken(par.getLeft()) != null) {
                lparToken = convertTokenName(par.getLeft());
            } else {
                lparToken = addToken(par.getLeft(), Utilities.encodeStringIntoRegex(par.getLeft()));
            }

            String rparToken;
            if (this.language.getToken(par.getRight()) != null) {
                rparToken = convertTokenName(par.getRight());
            } else {
                rparToken = addToken(par.getRight(), Utilities.encodeStringIntoRegex(par.getRight()));
            }

            String parRuleName = convertProductionName(concept.getConceptName());

            parts.add(new RulePart(lparToken));
            parts.add(new RulePart(parRuleName));
            parts.add(new RulePart(rparToken));

            Alternative parAlt = new Alternative();
            parAlt.par = par;
            parAlt.sequence = new SequencePart(parts);
            parAlt.sequence.setCodeAfter("$" + RETURN_VAR_NAME + " = $" + parRuleName + "." + RETURN_VAR_NAME + ";");
            alts.add(parAlt);
        }

        alts.addAll(operatorAlts);

        List<Alternative> remainingAlts = unresolvedAlts.stream().filter(alt -> alt.op == null)
                .collect(Collectors.toCollection(ArrayList::new));
        alts.addAll(remainingAlts);

        Production p = new Production();
        p.alternatives = alts;
        p.returns = makeReturnsString(concept);
        this.productions.put(
                convertProductionName(concept.getConceptName()), p);
    }

    // Merge alternatives with same priority into one.
    private Alternative mergeOperatorAlternatives(List<Alternative> alts) {
        if (alts.isEmpty())
            throw new IllegalArgumentException("Empty list of alternatives");

        if (alts.size() == 1) {
            return alts.get(0);
        }

        Alternative primaryAlt = alts.get(0); // Alternative to merge others to.

        // Make sure all alternatives have the same number of parts.
        if (!alts.stream().allMatch(alt ->
                alt.sequence.getParts().size() == primaryAlt.sequence.getParts().size())) {
            throw new GeneratorException("Cannot merge alternatives");
        }

        boolean mergedOnce = false;

        for (int i = 0; i < primaryAlt.sequence.getParts().size(); i++) {
            List<Part> parts = new ArrayList<>();

            for (Alternative alt : alts) {
                Part part = alt.sequence.getParts().get(i);
                Part primaryAltPart = primaryAlt.sequence.getParts().get(i);

                if (!part.getClass().equals(primaryAltPart.getClass())) {
                    throw new GeneratorException("Cannot merge alternatives");
                }

                if (part instanceof RulePart) {
                    RulePart rulePart = (RulePart) part;
                    String name = rulePart.getName();

                    if (rulePart.isTerminal()) {
                        parts.add(rulePart);
                    }
                }
            }

            if (!parts.isEmpty()) {
                if (mergedOnce) {
                    throw new GeneratorException("Merging alternatives which differ in more than one terminal is not supported yet.");
                }
                AlternativePart newPart = new AlternativePart(parts);
                newPart.setLabel("op");
                primaryAlt.sequence.setPart(i, newPart);
                mergedOnce = true;

                // Construct merged switch action.
                StringBuilder sb = new StringBuilder("switch ($ctx.op.getType()) {\n");
                for (int j = 0; j < parts.size(); j++) {
                    sb.append("case ").append(((RulePart) parts.get(j)).getName()).append(":\n");
                    sb.append("    ").append(alts.get(j).sequence.getCodeAfter()).append("\n");
                    sb.append("    ").append("break;\n");
                }
                sb.append("}\n");
                primaryAlt.sequence.setCodeAfter(sb.toString());
            }
        }

        return primaryAlt;
    }

    private List<Alternative> processConcreteConcept(Concept concept) {
        List<Alternative> alts = new ArrayList<>();
        Enum enumPattern = (Enum) concept.getPattern(Enum.class);

        for (Notation n : concept.getConcreteSyntax()) {
            List<Part> parts = new ArrayList<>();

            LabelProvider labelProvider = new LabelProvider();
            List<String> params = new ArrayList<>();

            for (NotationPart part : n.getParts()) {
                if (part instanceof TokenPart) {
                    String tokenName = ((TokenPart) part).getToken();
                    parts.add(new RulePart(convertTokenName(tokenName)));
                } else if (part instanceof BindingNotationPart) {
                    BindingNotationPart bindingNotationPart = (BindingNotationPart) part;
                    Type type;
                    if (bindingNotationPart instanceof PropertyReferencePart) {
                        type = ((PropertyReferencePart) bindingNotationPart).getProperty().getType();
                    } else {
                        type = ((LocalVariablePart) bindingNotationPart).getType();
                        if (!(type instanceof PrimitiveType)) {
                            throw new GeneratorException("Referring type must be primitive!");
                        }
                    }
                    String typeString = typeToString(type);

                    if (type instanceof ReferenceType) {
                        ReferenceType referenceType = (ReferenceType) type;
                        String ruleName = convertProductionName(referenceType.getConcept().getConceptName());

                        RulePart rulePart = new RulePart(ruleName);
                        rulePart.setLabel(labelProvider.createLabel(ruleName));
                        params.add("$ctx." + rulePart.getLabel() + "." + RETURN_VAR_NAME);
                        parts.add(rulePart);
                    } else if (type instanceof PrimitiveType) {
                        if (!conversions.containsConversion(typeString)) {
                            throw new GeneratorException("Cannot handle type " + typeString);
                        }

                        String conversionExpr = conversions.getConversion(typeString).trim();

                        String ruleName;
                        Token tokenPattern = (Token) bindingNotationPart.getPattern(Token.class);
                        if (tokenPattern != null) {
                            ruleName = convertTokenName(tokenPattern.getName());
                        } else {
                            if (bindingNotationPart instanceof PropertyReferencePart) {
                                ruleName = convertTokenName(((PropertyReferencePart) bindingNotationPart).getProperty().getName());
                            } else {
                                ruleName = convertTokenName(((LocalVariablePart) bindingNotationPart).getName());
                            }
                        }

                        RulePart rulePart = new RulePart(ruleName);
                        rulePart.setLabel(labelProvider.createLabel(ruleName));
                        params.add(String.format(conversionExpr, "$ctx." + rulePart.getLabel() + ".getText()"));
                        parts.add(rulePart);
                    } else if (type instanceof ComponentType) {
                        ComponentType componentType = (ComponentType) type;
                        Type innerType = componentType.getComponentType();
                        String innerTypeString = typeToString(innerType);

                        String ruleName; // Name of the terminal/non-terminal pertaining to the list's element.
                        String productionName; // Name of the production rule to be created.

                        if (innerType instanceof ReferenceType) {
                            ruleName = convertProductionName(((ReferenceType) innerType).getConcept().getConceptName());
                            productionName = ruleName + "_list";
                        } else if (innerType instanceof PrimitiveType) {
                            if (!conversions.containsConversion(innerTypeString)) {
                                throw new GeneratorException("Cannot handle type " + innerTypeString + " in " + typeString);
                            }

                            Token tokenPattern = (Token) bindingNotationPart.getPattern(Token.class);
                            if (tokenPattern != null) {
                                ruleName = convertTokenName(tokenPattern.getName());
                            } else {
                                if (bindingNotationPart instanceof PropertyReferencePart) {
                                    ruleName = convertTokenName(((PropertyReferencePart) bindingNotationPart).getProperty().getName());
                                } else {
                                    ruleName = convertTokenName(((LocalVariablePart) bindingNotationPart).getName());
                                }
                            }
                            // Token ruleName will be in all caps and a parser rule can't begin with a capital letter
                            // so prefix it with "nt_".
                            productionName = "nt_" + ruleName + "_list";
                        } else if (innerType instanceof ComponentType) {
                            throw new GeneratorException("Component types of component types are not supported.");
                        } else {
                            throw new GeneratorException("Unknown type.");
                        }

                        while (this.productions.containsKey(productionName)) {
                            productionName = productionName + "_";
                        }

                        Production p = new Production();
                        p.returns = typeString + " " + RETURN_VAR_NAME;
                        p.alternatives = new ArrayList<>();

                        Alternative alt = new Alternative();
                        Range range = (Range) bindingNotationPart.getPattern(Range.class);
                        if (range == null) {
                            range = new Range();
                        }

                        Separator separator = (Separator) bindingNotationPart.getPattern(Separator.class);
                        String sepToken = "";
                        if (separator != null) {
                            if (this.language.getToken(separator.getValue()) != null) {
                                sepToken = convertTokenName(separator.getValue());
                            } else {
                                sepToken = addToken(separator.getValue(), Utilities.encodeStringIntoRegex(separator.getValue()));
                            }
                        }

                        alt.sequence = generateListGrammar(ruleName, range, sepToken);

                        StringBuilder action = new StringBuilder();

                        // Streams of primitive types are not possible in Java. :-(
                        boolean useStream =
                                (!(innerType instanceof PrimitiveType)
                                    || ((PrimitiveType) innerType).getPrimitiveTypeConst() == PrimitiveTypeConst.STRING);

                        if (useStream) {
                            action.append("$").append(RETURN_VAR_NAME).append(" = $ctx.").append(ruleName)
                                    .append("().stream().map(elem -> ");
                            if (innerType instanceof PrimitiveType) {
                                String conversionExpr = conversions.getConversion(innerTypeString);
                                action.append(String.format(conversionExpr, "elem.getSymbol().getText()"));
                            } else {
                                action.append("elem.").append(RETURN_VAR_NAME);
                            }
                            action.append(")");
                            if (type instanceof ArrayType) {
                                action.append(".toArray(" + typeString + "::new);");
                            } else if (type instanceof ListType) {
                                action.append(".collect(java.util.stream.Collectors.toList());");
                            } else if (type instanceof SetType) {
                                action.append(".collect(java.util.stream.Collectors.toSet());");
                            } else {
                                throw new GeneratorException("Unknown component type");
                            }
                        } else {
                            assert type instanceof ArrayType;

                            String conversionExpr = conversions.getConversion(innerTypeString);

                            String boxedTypeString = primitiveTypeToBoxedTypeString((PrimitiveType) innerType);
                            action.append("java.util.List<").append(boxedTypeString).append("> boxedList = ")
                                    .append("$ctx.").append(ruleName).append("().stream().map(elem -> ")
                                    .append(String.format(conversionExpr, "elem.getSymbol().getText()"))
                                    .append(").collect(java.util.stream.Collectors.toList());\n");
                            action.append("$").append(RETURN_VAR_NAME).append(" = new ")
                                    .append(innerTypeString).append("[boxedList.size()];\n");
                            action.append("for (int i = 0; i < boxedList.size(); i++) {\n    ")
                                    .append("$").append(RETURN_VAR_NAME).append("[i] = boxedList.get(i);\n}");
                        }

                        alt.sequence.setCodeAfter(action.toString());

                        p.alternatives.add(alt);
                        this.productions.put(productionName, p);

                        RulePart rulePart = new RulePart(productionName);
                        rulePart.setLabel(labelProvider.createLabel(productionName));
                        params.add("$ctx." + rulePart.getLabel() + "." + RETURN_VAR_NAME);
                        parts.add(rulePart);
                    }
                }

            }

            if (parts.isEmpty()) {
                continue;
            }

            Alternative alt = new Alternative();
            alt.sequence = new SequencePart(parts);
            alt.op = (Operator) concept.getPattern(Operator.class);
            if (alt.op != null) {
                switch (alt.op.getAssociativity()) {
                    case AUTO:
                        // TODO: Implement AUTO.
                        // For now it is synonymous with LEFT, so fall-through is intentional.
                    case LEFT:
                        alt.sequence.setAssociativity(Part.Associativity.Left);
                        break;
                    case RIGHT:
                        alt.sequence.setAssociativity(Part.Associativity.Right);
                        break;
                    default:
                        break;
                }
            }

            Factory factoryPattern = (Factory) n.getPattern(Factory.class);

            StringBuilder action = new StringBuilder();
            action.append("$").append(RETURN_VAR_NAME).append(" = ");
            if (enumPattern == null) {
                action.append("yajco.ReferenceResolver.getInstance().register(");
                if (factoryPattern != null) {
                    // Factory method call.
                    action.append(getFullConceptClassName(concept)).append(".").append(factoryPattern.getName());
                } else {
                    // Constructor call.
                    action.append("new ").append(getFullConceptClassName(concept));
                }
                action.append("(").append(
                        params.stream()
                                .collect(Collectors.joining(", ")
                                )).append(")");
                if (factoryPattern != null) {
                    action.append(", \"").append(factoryPattern.getName()).append("\"");
                }
                action.append(
                        params.stream()
                                .map(s -> ", (Object) " + s)
                                .collect(Collectors.joining())
                ).append(")");
            } else {
                // Enum.
                TokenPart part = (TokenPart) n.getParts().get(0);
                if (enumPattern != null) {
                    action.append(getFullConceptClassName(concept)).append(".").append(part.getToken());
                }
            }
            action.append(";");

            alt.sequence.setCodeAfter(action.toString());
            alts.add(alt);
        }

        return alts;
    }

    // Generates a grammar rule for a list of elements, which may be lexical or parser rules.
    // The list is constrained by the given range, with elements separated by the given separator token.
    // Note: No semantic actions are generated, only pure grammar.
    private SequencePart generateListGrammar(String elemRule, Range range, String sepToken) {
        List<Part> parts = new ArrayList<>();

        if (sepToken.isEmpty()) { // No separator.
            if (range.getMaxOccurs() == Range.INFINITY) {
                if (range.getMinOccurs() == 0) {
                    parts.add(new ZeroOrMorePart(new RulePart(elemRule)));
                } else {
                    for (int i = 0; i < (range.getMinOccurs() - 1); i++) {
                        parts.add(new RulePart(elemRule));
                    }
                    parts.add(new OneOrMorePart(new RulePart(elemRule)));
                }
            } else {
                for (int i = 0; i < range.getMinOccurs(); i++) {
                    parts.add(new RulePart(elemRule));
                }
                for (int i = 0; i < (range.getMaxOccurs() - range.getMinOccurs()); i++) {
                    parts.add(new ZeroOrOnePart(new RulePart(elemRule)));
                }
            }
        } else { // With separator.
            if (range.getMaxOccurs() == Range.INFINITY) {
                if (range.getMinOccurs() == 0) {
                    // ( elem ( SEP elem )* )?
                    parts.add(
                        new ZeroOrOnePart(
                            new SequencePart(Arrays.asList(
                                new RulePart(elemRule),
                                new ZeroOrMorePart(
                                    new SequencePart(Arrays.asList(
                                        new RulePart(sepToken),
                                        new RulePart(elemRule)
                                    ))
                                )
                            ))
                        )
                    );
                } else {
                    parts.add(new RulePart(elemRule));
                    for (int i = 0; i < (range.getMinOccurs() - 1); i++) {
                        parts.add(
                            new SequencePart(Arrays.asList(
                                new RulePart(sepToken),
                                new RulePart(elemRule)
                            ))
                        );
                    }
                    parts.add(
                        new ZeroOrMorePart(
                            new SequencePart(Arrays.asList(
                                new RulePart(sepToken),
                                new RulePart(elemRule)
                            ))
                        )
                    );
                }
            } else {
                parts.add(new RulePart(elemRule));
                for (int i = 0; i < (range.getMinOccurs() - 1); i++) {
                    parts.add(
                        new SequencePart(Arrays.asList(
                            new RulePart(sepToken),
                            new RulePart(elemRule)
                        ))
                    );
                }
                for (int i = 0; i < (range.getMaxOccurs() - range.getMinOccurs()); i++) {
                    parts.add(
                        new ZeroOrOnePart(
                            new SequencePart(Arrays.asList(
                                new RulePart(sepToken),
                                new RulePart(elemRule)
                            ))
                        )
                    );
                }

                if (range.getMinOccurs() == 0) {
                    parts = Arrays.asList(new ZeroOrOnePart(new SequencePart(parts)));
                }
            }
        }

        return new SequencePart(parts);
    }

    private String getFullConceptClassName(Concept c) {
        return yajco.model.utilities.Utilities.getLanguagePackageName(this.language) + "." + c.getName();
    }

    // Make ANTLR4 "returns" string for a concept.
    private String makeReturnsString(Concept c) {
        return getFullConceptClassName(c) + " " + RETURN_VAR_NAME;
    }

    private String convertProductionName(String name) {
        // ANTLR parser rule names must begin with a lowercase letter
        // Also use a prefix to avoid clashes with Java keywords that
        // occur due to lowercasing.
        name = name.replace('.', '_');
        return "nt_" + name.toLowerCase();
    }

    private ParserRule makeMainRule() {
        Concept mainConcept = this.language.getConcepts().get(0);
        String name = convertProductionName(mainConcept.getConceptName());
        SequencePart part = new SequencePart(new ArrayList<>(Arrays.asList(
                new RulePart(name),
                // Include explicit EOF so the parser is not allowed to match a subset of the input
                // without reporting an error.
                new RulePart("EOF")
        )));
        part.setCodeAfter("$" + RETURN_VAR_NAME + " = $" + name + ".ctx." + RETURN_VAR_NAME + ";");
        return new ParserRule("main",
                makeReturnsString(mainConcept),
                part);
    }

    private String typeToString(Type type) {
        if (type instanceof PrimitiveType) {
            return primitiveTypeToString((PrimitiveType) type);
        } else if (type instanceof ComponentType) {
            return componentTypeToString((ComponentType) type);
        } else if (type instanceof ReferenceType) {
            return referenceTypeToString((ReferenceType) type);
        } else {
            throw new IllegalArgumentException("Unknown type detected: '" + type.getClass().getCanonicalName() + "'!");
        }
    }

    private String primitiveTypeToString(PrimitiveType primitiveType) {
        switch (primitiveType.getPrimitiveTypeConst()) {
            case BOOLEAN:
                return "boolean";
            case INTEGER:
                return "int";
            case REAL:
                return "float";
            case STRING:
                return "java.lang.String";
            default:
                throw new IllegalArgumentException("Unknown primitive type detected: '" + primitiveType.toString() + "'!");
        }
    }

    private String primitiveTypeToBoxedTypeString(PrimitiveType primitiveType) {
        switch (primitiveType.getPrimitiveTypeConst()) {
            case BOOLEAN:
                return "java.lang.Boolean";
            case INTEGER:
                return "java.lang.Integer";
            case REAL:
                return "java.lang.Float";
            case STRING:
                return "java.lang.String";
            default:
                throw new IllegalArgumentException("Unknown primitive type detected: '" + primitiveType.toString() + "'!");
        }
    }

    private String componentTypeToString(ComponentType componentType) {
        if (componentType instanceof ArrayType) {
            return typeToString(componentType.getComponentType()) + "[]";
        } else if (componentType instanceof ListType) {
            return "java.util.List<" + typeToString(componentType.getComponentType()) + ">";
        } else if (componentType instanceof SetType) {
            return "java.util.Set<" + typeToString(componentType.getComponentType()) + ">";
        } else {
            throw new IllegalArgumentException("Unknown component type detected: '" + componentType.getClass().getCanonicalName() + "'!");
        }
    }

    private String referenceTypeToString(ReferenceType referenceType) {
        return yajco.model.utilities.Utilities.getFullConceptClassName(language, referenceType.getConcept());
    }

    private String convertTokenName(String token) {
        token = "T_" + Utilities.encodeStringIntoTokenName(token);
        return token;
    }

    private String addToken(String token, String regex) {
        String newName = convertTokenName(token);

        for (Map.Entry<String, String> entry : this.tokens.entrySet()) {
            if (regex.equals(entry.getValue())) {
                return entry.getKey();
            }
        }

        // Make sure the token name is unique.
        while (this.tokens.containsKey(newName)) {
            newName += "_";
        }
        this.tokens.put(newName, regex);
        return newName;
    }

    private Set<Concept> getDirectSubconcepts(Concept parent) {
        Set<Concept> subconcepts = new HashSet<Concept>();
        for (Concept concept : this.language.getConcepts()) {
            if (concept.getParent() != null && concept.getParent().equals(parent)) {
                subconcepts.add(concept);
            }
        }
        return subconcepts;
    }
}
