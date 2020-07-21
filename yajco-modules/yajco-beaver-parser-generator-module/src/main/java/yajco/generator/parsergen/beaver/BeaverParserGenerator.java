package yajco.generator.parsergen.beaver;

import yajco.generator.parsergen.beaver.semlang.translator.SemLangToJavaTranslator;
import yajco.grammar.NonterminalSymbol;
import yajco.grammar.Symbol;
import yajco.grammar.TerminalSymbol;
import yajco.grammar.bnf.Alternative;
import yajco.grammar.bnf.Grammar;
import yajco.grammar.bnf.Production;
import yajco.grammar.translator.YajcoModelToBNFGrammarTranslator;
import yajco.model.Language;
import yajco.model.pattern.impl.Associativity;
import yajco.model.pattern.impl.Operator;
import yajco.model.type.*;
import yajco.model.utilities.Utilities;

import java.io.PrintStream;
import java.util.*;

public class BeaverParserGenerator {

    public static final String DEFAULT_PACKAGE_NAME = "parser.beaver";
    public static final String PARSER_CLASS_NAME_PREFIX = "LALR";
    public static final String PARSER_CLASS_NAME_SUFFIX = "Parser";
    private static final BeaverParserGenerator instance = new BeaverParserGenerator();
    private Language language;
    private Grammar grammar;
    private Set<TerminalSymbol> usedTerminals;
    private Set<TerminalSymbol> operatorTerminalsUsed;
    private Map<Alternative, String> operatorAlternativesMap;
    private String parserPackageName;
    private String parserClassName;

    private BeaverParserGenerator() {
    }

    public void generateFrom(Language language, Grammar grammar, String parserPackageName, String parserClassName, PrintStream writer) {
        this.language = language;
        this.grammar = grammar;
        this.operatorTerminalsUsed = new HashSet<TerminalSymbol>();
        this.operatorAlternativesMap = new HashMap<Alternative, String>();
        this.parserPackageName = parserPackageName;
        this.parserClassName = parserClassName;
        
        usedTerminals = getUsedTerminals();

        writePackage(writer);
        writeClass(writer);
        writeImports(writer);

        writeTerminals(writer);

        writeOperatorsSpecification(writer);

        writeTypes(writer);

        writeGoal(writer);

        writeGrammar(writer);
    }

    private void writePackage(PrintStream writer) {
        //parserPackageName = language.getName() != null ? language.getName() + "." + DEFAULT_PACKAGE_NAME : DEFAULT_PACKAGE_NAME;

        writer.println("%package \"" + parserPackageName + "\";");
    }

    private void writeClass(PrintStream writer) {
        //String className = PARSER_CLASS_NAME_PREFIX + grammar.getStartSymbol().getName() + PARSER_CLASS_NAME_SUFFIX;

        writer.println("%class \"" + parserClassName + "\";");
    }

    private void writeImports(PrintStream writer) {
//		for (Concept concept : language.getConcepts()) {
//			writer.println("%import \"" + Utilities.getFullConceptClassName(language, concept) + "\";");
//		}
        writer.println("%import \"" + parserPackageName + ".SymbolListImpl\";");
        writer.println("%import \"" + parserPackageName + ".SymbolLinkedHashSetImpl\";");
        writer.println("%import \"" + parserPackageName + ".SymbolListImplWithShared\";");
        writer.println("%import \"" + parserPackageName + ".QuotedStringUtils\";");
        //DOMINIK TEST
        writer.println("%import \"" + parserPackageName + ".SymbolWrapper\";");
        // END
        writer.println();
    }

    private void writeTerminals(PrintStream writer) {
        writer.print("%terminals ");

        List<TerminalSymbol> terminals = new ArrayList<TerminalSymbol>(usedTerminals);
        for (int i = 0; i < terminals.size(); i++) {
            writer.print(terminals.get(i).getName());
            if (i != (terminals.size() - 1)) {
                writer.print(", ");
            }
        }
        writer.println(";");
        writer.println();
    }

    private void writeOperatorsSpecification(PrintStream writer) {
        List<Integer> priorities = new ArrayList(grammar.getOperatorPool().keySet());
        Collections.sort(priorities);

        if (priorities.isEmpty()) {
            return;
        }

        List<String> specifications = new ArrayList<String>(priorities.size());
        for (Integer priority : priorities) {
            String specification = operatorSpecificationToString(priority);
            if (specification != null) {
                specifications.add(specification);
            }
        }

        Collections.reverse(specifications);
        for (String specification : specifications) {
            writer.println(specification);
        }
        writer.println();
    }

    private void writeTypes(PrintStream writer) {
        for (TerminalSymbol terminal : usedTerminals) {
            if (terminal.getName().startsWith(YajcoModelToBNFGrammarTranslator.DEFAULT_SYMBOL_NAME)) {
                continue;
            }

            writer.println("%typeof " + terminal.getName() + " = \"java.lang.String\";");
        }

        for (NonterminalSymbol nonterminal : grammar.getNonterminals().values()) {
            //DOMINIK UPRAVA
            writer.println("%typeof " + nonterminal.getName() + " = \"" + parserPackageName + ".SymbolWrapper<" + typeToString(nonterminal.getReturnType()) + ">\";");
            //writer.println("%typeof " + nonterminal.getName() + " = \"" + typeToString(nonterminal.getReturnType()) + "\";");
        }
        writer.println();
    }

    private void writeGoal(PrintStream writer) {
        writer.println("%goal " + grammar.getStartSymbol().getName() + ";"); //povodne
        writer.println();
    }

    private void writeGrammar(PrintStream writer) {
        for (Production production : grammar.getProductions().values()) {
            writeProduction(production, writer);
        }
    }

    private void writeProduction(Production production, PrintStream writer) {
        writer.println(production.getLhs().getName());

        for (int i = 0; i < production.getRhs().size(); i++) {
            writer.print(i == 0 ? "\t= " : "\t| ");
            writeAlternative(production.getRhs().get(i), writer);
            writer.println();
        }
        writer.println("\t;");
        writer.println();
    }

    private void writeAlternative(Alternative alternative, PrintStream writer) {
        for (int i = 0; i < alternative.getSymbols().size(); i++) {
            Symbol symbol = alternative.getSymbols().get(i);
            writer.print(symbol.getName());
            if (symbol.getVarName() != null) {
                writer.print(".");
                writer.print(symbol.getVarName());
            }
            if (i != (alternative.getSymbols().size() - 1)) {
                writer.print(" ");
            }
        }

        if (operatorAlternativesMap.containsKey(alternative)) {
            writer.print(" @ " + operatorAlternativesMap.get(alternative));
        }

        writer.print("\t{: ");
        SemLangToJavaTranslator.getInstance().translateActions(alternative.getActions(), language, writer);
        writer.print(":}");
    }

    private String operatorSpecificationToString(Integer priority) {
        List<Alternative> opAlternatives = grammar.getOperatorPool().get(priority);
        if (opAlternatives.isEmpty()) {
            return null;
        }
        Operator opPattern = (Operator) opAlternatives.get(0).getPattern(Operator.class);

        int counter = 1;
        StringBuilder builder = new StringBuilder();
        builder.append("%").append(associativityToString(opPattern.getAssociativity())).append(" ");
        for (Alternative alternative : opAlternatives) {
            for (Symbol symbol : alternative.getSymbols()) {
                if (symbol instanceof NonterminalSymbol) {
                    continue;
                }

                TerminalSymbol terminal = (TerminalSymbol) symbol;
                if (!operatorTerminalsUsed.contains(terminal)) {
                    operatorTerminalsUsed.add(terminal);
                    builder.append(terminal.getName()).append(", ");
                } else {
                    String precSpec = "PREC_" + priority + "_" + counter++;
                    builder.append(precSpec).append(", ");
                    operatorAlternativesMap.put(alternative, precSpec);
                }
            }
        }

        if (builder.toString().endsWith(", ")) {
            builder.setLength(builder.length() - 2);
        }
        builder.append(";");

        return builder.toString();
    }

    private String associativityToString(Associativity associativity) {
        switch (associativity) {
            case LEFT:
                return "left";
            case NONE:
                return "nonassoc";
            case RIGHT:
                return "right";
            case AUTO:
                System.out.println("AUTO associativity set to LEFT without any analyse!!!");
                return "left";
            default:
                throw new IllegalArgumentException("Cann't resolve associativity!");
        }
    }

    private String typeToString(Type type) {
        if (type instanceof PrimitiveType) {
            return primitiveTypeToString((PrimitiveType) type);
        } else if (type instanceof ReferenceType) {
            ReferenceType refType = (ReferenceType) type;
            return Utilities.getFullConceptClassName(language, refType.getConcept());
        } else if (type instanceof ComponentType) {
            ComponentType innerType = (ComponentType) type;
            //DOMINIK TEST
            String innerTypeString = typeToString(innerType.getComponentType());
            //String innerTypeString = typeToStringWrapped(innerType.getComponentType()); - MOJE

            if (type instanceof ArrayType) {
                return innerTypeString + "[]";
            } else if (type instanceof ListType) {
                // problem v beaver-i sposobuje nutnost tohoto riadku
                return parserPackageName + ".SymbolListImpl" + "<" + innerTypeString + ">";
                //return "java.util.List<" + innerTypeString + ">";
            } else if (type instanceof SetType) {
                return "java.util.Set<" + innerTypeString + ">";
            } else if (type instanceof OptionalType) {
                return "java.util.Optional<" + innerTypeString + ">";
            } else {
                throw new IllegalArgumentException("Unknown component type detected: '" + type.getClass().getCanonicalName() + "'!");
            }
        } else {
            throw new IllegalArgumentException("Unknown type detected: '" + type.getClass().getCanonicalName() + "'!");
        }
    }

    private String primitiveTypeToString(PrimitiveType type) {
        switch (type.getPrimitiveTypeConst()) {
            case BOOLEAN:
                return "java.lang.Boolean";
            case INTEGER:
                return "java.lang.Integer";
            case REAL:
                return "java.lang.Float";
            case STRING:
                return "java.lang.String";

            default:
                throw new IllegalArgumentException("Unknown primitive type '" + type.toString() + "'!");
        }
    }
    private Set<TerminalSymbol> getUsedTerminals() {
        return getUsedTerminals(grammar);
    }

    public static Set<TerminalSymbol> getUsedTerminals(Grammar grammar) {
        Set<TerminalSymbol> terminals = new HashSet<TerminalSymbol>();
        for (Production production : grammar.getProductions().values()) {
            for (Alternative alternative : production.getRhs()) {
                for (Symbol symbol : alternative.getSymbols()) {
                    if (symbol instanceof TerminalSymbol){
                        TerminalSymbol ts = (TerminalSymbol)symbol;
                        if (grammar.getTerminalPool().containsKey(ts)) {
                            terminals.add(ts);
                        }
                    }
                }
            }
        }
        return terminals;
    }

    public static BeaverParserGenerator getInstance() {
        return instance;
    }
}
