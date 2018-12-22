package yajco.generator.parsergen.antlr4.translator;

import yajco.ReferenceResolver;
import yajco.generator.GeneratorException;
import yajco.generator.parsergen.Conversions;
import yajco.generator.parsergen.antlr4.model.*;
import yajco.generator.util.RegexUtil;
import yajco.generator.util.Utilities;
import yajco.model.*;
import yajco.model.pattern.impl.Factory;
import yajco.model.pattern.impl.Operator;
import yajco.model.pattern.impl.Parentheses;
import yajco.model.pattern.impl.Token;
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

    private class Production {
        Concept concept;
        List<Alternative> alternatives;
    }

    private class Alternative {
        Parentheses par;
        Operator op;
        SequencePart sequence;
    }

    private final Map<String, Production> productions = new LinkedHashMap<>();

    private final Map<String, String> tokens = new LinkedHashMap<>();

    public ModelTranslator(Language language, String parserClassName, String parserPackageName) {
        this.language = language;
        this.parserClassName = parserClassName;
        this.parserPackageName = parserPackageName;
    }

    public Grammar translate() {
        for (TokenDef tokenDef : this.language.getUsedTokens()) {
            this.tokens.put(tokenDef.getName(), tokenDef.getRegexp());
        }

        for (Concept c : this.language.getConcepts()) {
            for (Notation n : c.getConcreteSyntax()) {
                for (NotationPart part : n.getParts()) {
                    if (part instanceof TokenPart) {
                        String tokenName = ((TokenPart) part).getToken();
                        this.tokens.putIfAbsent(tokenName, Utilities.encodeStringIntoRegex(tokenName));
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
            parserRules.add(new ParserRule(name,
                    makeReturnsString(production.concept), altPart));
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
            parts.add(new RulePart(addToken("LPAR",
                    Utilities.encodeStringIntoRegex(par.getLeft()))));
            parts.add(new RulePart(convertProductionName(concept.getName())));
            parts.add(new RulePart(addToken("RPAR",
                    Utilities.encodeStringIntoRegex(par.getRight()))));

            Alternative parAlt = new Alternative();
            parAlt.par = par;
            parAlt.sequence = new SequencePart(parts);
            alts.add(parAlt);
        }

        alts.addAll(operatorAlts);

        List<Alternative> remainingAlts = unresolvedAlts.stream().filter(alt -> alt.op == null)
                .collect(Collectors.toCollection(ArrayList::new));
        alts.addAll(remainingAlts);

        Production p = new Production();
        p.alternatives = alts;
        p.concept = concept;
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

        for (Notation n : concept.getConcreteSyntax()) {
            List<Part> parts = new ArrayList<>();

            Map<String, Integer> counters = new HashMap<>();
            List<String> params = new ArrayList<>();

            for (NotationPart part : n.getParts()) {
                if (part instanceof TokenPart) {
                    String tokenName = ((TokenPart) part).getToken();
                    parts.add(new RulePart(convertTokenName(tokenName)));
                } else if (part instanceof LocalVariablePart) {
                    /*LocalVariablePart localVariablePart = (LocalVariablePart) part;
                    if (localVariablePart.getType() instanceof ReferenceType) {
                        ReferenceType referenceType = (ReferenceType) localVariablePart.getType();
                        parts.add(new RulePart(convertProductionName(referenceType.getConcept().getName())));
                    }*/
                    throw new GeneratorException("References are not supported yet!");
                } else if (part instanceof PropertyReferencePart) {
                    // TODO
                    PropertyReferencePart propertyReferencePart = (PropertyReferencePart) part;
                    Type type = propertyReferencePart.getProperty().getType();
                    String typeString = typeToString(type);

                    if (type instanceof ReferenceType) {
                        ReferenceType referenceType = (ReferenceType) propertyReferencePart.getProperty().getType();
                        String ruleName = convertProductionName(referenceType.getConcept().getName());

                        if (counters.containsKey(ruleName)) {
                            counters.put(ruleName, counters.get(ruleName) + 1);
                        } else {
                            counters.put(ruleName, 1);
                        }

                        RulePart rulePart = new RulePart(ruleName);
                        rulePart.setLabel(ruleName + "_" + counters.get(ruleName));
                        params.add("$ctx." + rulePart.getLabel() + "." + RETURN_VAR_NAME);
                        parts.add(rulePart);
                    } else if (type instanceof PrimitiveType) {
                        if (conversions.containsConversion(typeString)) {
                            String conversionExpr = conversions.getConversion(typeString).trim();

                            Token tokenPattern = (Token) propertyReferencePart.getPattern(Token.class);
                            if (tokenPattern != null) {
                                // TODO
                            } else {
                                String ruleName = propertyReferencePart.getProperty().getName().toUpperCase();

                                if (counters.containsKey(ruleName)) {
                                    counters.put(ruleName, counters.get(ruleName) + 1);
                                } else {
                                    counters.put(ruleName, 1);
                                }

                                RulePart rulePart = new RulePart(ruleName);
                                rulePart.setLabel(ruleName + "_" + counters.get(ruleName));
                                params.add(String.format(conversionExpr, "$ctx." + rulePart.getLabel() + ".getText()"));
                                parts.add(rulePart);
                            }
                        }
                    } else if (type instanceof ComponentType) {
                        // TODO
                        throw new GeneratorException("Component types not supported yet!");
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

            String action = "";
            Factory factory = (Factory) n.getPattern(Factory.class);
            if (factory == null) {
                // Constructor.
                action = "$" + RETURN_VAR_NAME + " = yajco.ReferenceResolver.getInstance().register(new "
                    + getFullConceptClassName(concept) + "(" +
                        params.stream()
                                .collect(Collectors.joining(", ")) +
                        ")" +
                        params.stream()
                                .map(s -> ", (Object) " + s)
                                .collect(Collectors.joining()) +
                        ");";
            } else {
                // Factory method.
                // TODO
                throw new GeneratorException("Factory methods are not supported yet!");
            }

            alt.sequence.setCodeAfter(action);
            alts.add(alt);
        }

        return alts;
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
        token = Utilities.encodeStringIntoTokenName(token);
        return token;
    }

    private String addToken(String token, String regex) {
        String newName = convertTokenName(token);

        if (regex.equals(this.tokens.get(newName))) {
            return newName;
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
