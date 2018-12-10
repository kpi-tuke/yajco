package yajco.generator.parsergen.antlr4.translator;

import yajco.ReferenceResolver;
import yajco.generator.parsergen.antlr4.model.*;
import yajco.generator.util.RegexUtil;
import yajco.generator.util.Utilities;
import yajco.grammar.NonterminalSymbol;
import yajco.grammar.Symbol;
import yajco.grammar.TerminalSymbol;
import yajco.grammar.bnf.Alternative;
import yajco.grammar.bnf.Production;
import yajco.grammar.semlang.*;
import yajco.grammar.translator.YajcoModelToBNFGrammarTranslator;
import yajco.model.Language;
import yajco.model.type.*;

import java.util.*;

/**
 * Translates YAJCo model to ANTLR4 grammar model
 * (yajco.model.Language -> yajco.generator.parsergen.antlr4.model.Grammar)
 */
public class ModelTranslatorOld {
    private final Language language;
    private final yajco.grammar.bnf.Grammar bnfGrammar;
    private final String parserClassName;
    private final String parserPackageName;

    public static final String RETURN_VAR_NAME = "_retval";
    private final static String REFERENCE_RESOLVER_CLASS_NAME = ReferenceResolver.class.getCanonicalName();

    public ModelTranslatorOld(Language language, String parserClassName, String parserPackageName) {
        this.language = language;
        this.bnfGrammar = YajcoModelToBNFGrammarTranslator.getInstance().translate(language);
        this.parserClassName = parserClassName;
        this.parserPackageName = parserPackageName;
    }

    public Grammar translate() {
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

    private String convertProductionName(String name) {
        // ANTLR parser rule names must begin with a lowercase letter
        // Also use a prefix to avoid clashes with Java keywords that
        // occur due to lowercasing.
        return "nt_" + name.toLowerCase();
    }

    private String makeReturnRule(NonterminalSymbol nonterminal) {
        return typeToString(nonterminal.getReturnType()) + " " + RETURN_VAR_NAME;
    }

    private ParserRule makeMainRule() {
        String name = convertProductionName(this.bnfGrammar.getStartSymbol().getName());
        SequencePart part = new SequencePart(new ArrayList<>(Arrays.asList(
            new RulePart(name),
            // Include explicit EOF so the parser is not allowed to match a subset of the input
            // without reporting an error.
            new RulePart("EOF")
        )));
        part.setCodeAfter("$" + RETURN_VAR_NAME + " = $" + name + ".ctx." + RETURN_VAR_NAME + ";");
        return new ParserRule("main",
                makeReturnRule(this.bnfGrammar.getStartSymbol()),
                part);
    }

    private List<ParserRule> translateProductions() {
        List<ParserRule> parserRules = new ArrayList<>();
        parserRules.add(makeMainRule());

        for (Map.Entry<NonterminalSymbol, Production> entry : this.bnfGrammar.getProductions().entrySet()) {
            NonterminalSymbol nonterminal = entry.getKey();
            Production production = entry.getValue();

            List<Part> altChildren = new ArrayList<>();
            int i = 0;
            for (Alternative alt : production.getRhs()) {
                List<Part> seqChildren = new ArrayList<>();
                for (Symbol sym : alt.getSymbols()) {
                    if (sym.getVarName() != null) {
                        // We need to suffix labels/varNames because ANTLR requires that they
                        // be unique across all alternatives and YajcoModelToBNFGrammarTranslator
                        // does not respect this.
                        sym.setVarName(sym.getVarName() + "_alt" + i);
                    }
                    seqChildren.add(new RulePart(translateSymbol(sym), sym.getVarName()));
                }
                SequencePart seqPart = new SequencePart(seqChildren);
                seqPart.setCodeAfter(translateActions(alt.getActions()));
                altChildren.add(seqPart);
                i++;
            }
            AlternativePart altPart = new AlternativePart(altChildren);

            parserRules.add(
                new ParserRule(convertProductionName(nonterminal.getName()),
                makeReturnRule(nonterminal),
                altPart
            ));
        }

        return parserRules;
    }

    private String translateSymbol(Symbol sym) {
        if (sym instanceof TerminalSymbol) {
            TerminalSymbol termSym = (TerminalSymbol) sym;
            return termSym.getName();
        } else {
            NonterminalSymbol nontermSym = (NonterminalSymbol) sym;
            return convertProductionName(nontermSym.getName());
        }
    }

    private String translateActions(List<Action> actions) {
        StringBuilder sb = new StringBuilder();
        for (Action action : actions) {
            sb.append(translateAction(action)).append("\n");
        }
        return sb.toString();
    }

    private String translateAction(Action action) {
        switch (action.getActionType()) {
            case ASSIGN:
                return translateAssignAction((AssignAction) action);
            case DEFINE_VAR:
                return translateDefineVariableAction((DefineVariableAction) action);
            case RETURN:
                return translateReturnAction((ReturnAction) action);
            case CONVERT_STRING_TO_PRIMITIVE:
                return translateConvertStringToPrimitiveTypeAction((ConvertStringToPrimitiveTypeAction) action);
            case CONVERT_COLLECTION_TO_ARRAY:
                return translateConvertCollectionToArrayAction((ConvertCollectionToArrayAction) action);
            case CONVERT_LIST_TO_COLLECTION:
                return translateConvertListToCollectionAction((ConvertListToCollectionAction) action);
            case CREATE_COLLECTION_INST:
                return translateCreateCollectionInstanceAction((CreateCollectionInstanceAction) action);
            case ADD_ELEMENT_TO_COLLECTION:
                return translateAddElementToCollectionAction((AddElementToCollectionAction) action);
            case CREATE_CLASS_INST:
                return translateCreateClassInstanceAction((CreateClassInstanceAction) action);
            case CREATE_ENUM_INST:
                return translateCreateEnumInstanceAction((CreateEnumInstanceAction) action);
            case REF_RESOLVER_REGISTER:
                return translateReferenceResolverRegisterAction((ReferenceResolverRegisterAction) action);
            default:
                throw new IllegalArgumentException("Unknown SemLang action detected: '" + action.getClass().getCanonicalName() + "'!");
        }
    }

    private String translateAssignAction(AssignAction action) {
        return translateLValue(action.getLValue()) + " = " + translateRValue(action.getRValue()) + ";";
    }

    private String translateDefineVariableAction(DefineVariableAction action) {
        return typeToString(action.getVarType()) + " " + action.getVarName() + " = null;";
    }

    private String translateReturnAction(ReturnAction action) {
        return "$" + RETURN_VAR_NAME + " = " + translateRValue(action.getRValue()) + ";";
    }

    private String translateConvertStringToPrimitiveTypeAction(ConvertStringToPrimitiveTypeAction action) {
        if (action.getType().getPrimitiveTypeConst() == PrimitiveTypeConst.STRING) {
            return "";
        }
        return primitiveTypeToString(action.getType()) + ".valueOf(" + translateRValue(action.getRValue()) + ")";
    }

    private String translateConvertCollectionToArrayAction(ConvertCollectionToArrayAction action) {
        if (action.getCollectionType() instanceof ArrayType) {
            return "";
        }
        return translateRValue(action.getRValue()) + ".toArray(new " + typeToString(action.getInnerType()) + "[]{})";
    }

    private String translateConvertListToCollectionAction(ConvertListToCollectionAction action) {
        if (action.getResultCollectionType() instanceof ArrayType) {
            return translateRValue(action.getRValue()) + ".toArray(new "
                    + typeToString(action.getResultCollectionInnerType()) + "[]{})";
        } else if (action.getResultCollectionType() instanceof ListType) {
            return "new java.util.ArrayList<" + typeToString(action.getResultCollectionInnerType())
                    + ">(" + translateRValue(action.getRValue()) + ")";
        } else if (action.getResultCollectionType() instanceof SetType) {
            return "new java.util.HashSet<" + typeToString(action.getResultCollectionInnerType())
                    + ">(" + translateRValue(action.getRValue()) + ")";
        } else {
            throw new IllegalArgumentException("Unknown component type detected: '" + action.getResultCollectionType().getClass().getCanonicalName() + "'!");
        }
    }

    private String translateCreateCollectionInstanceAction(CreateCollectionInstanceAction action) {
        if (action.getComponentType() instanceof ArrayType) {
            return "new " + typeToString(action.getInnerType()) + "[0]";
        } else if (action.getComponentType() instanceof ListType) {
            return "new java.util.ArrayList<" + typeToString(action.getInnerType()) + ">()";
        } else if (action.getComponentType() instanceof SetType) {
            return "new java.util.HashSet<" + typeToString(action.getInnerType()) + ">()";
        } else {
            throw new IllegalArgumentException("Unknown component type detected: '" + action.getComponentType().getClass().getCanonicalName() + "'!");
        }
    }

    private String translateAddElementToCollectionAction(AddElementToCollectionAction action) {
        return translateLValue(action.getLValue()) + ".add(" + translateRValue(action.getRValue()) + ");";
    }

    private String translateCreateClassInstanceAction(CreateClassInstanceAction action) {
        StringBuilder sb = new StringBuilder();
        if (action.getFactoryMethodName() == null || action.getFactoryMethodName().equals("")) {
            sb.append("new ").append(action.getClassType()).append("(");
        } else {
            sb.append(action.getClassType()).append(".").append(action.getFactoryMethodName()).append("(");
        }

        for (int i = 0; i < action.getParameters().size(); i++) {
            sb.append(translateRValue(action.getParameters().get(i)));
            if (i != (action.getParameters().size() - 1)) {
                sb.append(", ");
            }
        }
        sb.append(")");

        return sb.toString();
    }

    private String translateCreateEnumInstanceAction(CreateEnumInstanceAction action) {
        return action.getEnumType() + "." + action.getEnumConstant();
    }

    private String translateReferenceResolverRegisterAction(ReferenceResolverRegisterAction action) {
        StringBuilder sb = new StringBuilder();
        sb.append(REFERENCE_RESOLVER_CLASS_NAME + ".getInstance().register(")
                .append(translateCreateClassInstanceAction(action));
        String factoryMethodName = action.getFactoryMethodName();
        if (factoryMethodName != null && !factoryMethodName.isEmpty()) {
            sb.append(", \""+factoryMethodName+"\"");
        }
        if (action.getParameters().size() > 0) {
            sb.append(", (Object)");
            for (int i = 0; i < action.getParameters().size(); i++) {
                sb.append(translateRValue(action.getParameters().get(i)));
                if (i != (action.getParameters().size() - 1)) {
                    sb.append(", ");
                }
            }
        }
        sb.append(")");
        return sb.toString();
    }

    private String translateLValue(LValue lv) {
        if (lv.getSymbol() != null) {
            Symbol sym = lv.getSymbol();

            StringBuilder sb = new StringBuilder();
            sb.append("$ctx.").append(sym.getVarName());
            boolean labelled = new RulePart(sym.getName(), sym.getVarName()).shouldGenerateLabel();
            if (!labelled) {
                // labels don't generate methods
                sb.append("()");
            }
            if (sym instanceof NonterminalSymbol) {
                sb.append("." + RETURN_VAR_NAME);
            } else {
                if (!labelled) {
                    // labels generate member of type Token directly so this intermediate call must not be here
                    sb.append(".getSymbol()");
                }
                sb.append(".getText()");
            }
            return sb.toString();
        } else {
            return lv.getVarName();
        }
    }

    private String translateRValue(RValue rv) {
        if (rv.getSymbol() != null || rv.getVarName() != null) {
            return translateLValue(rv);
        } else {
            return translateAction(rv.getAction());
        }
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

    public Map<String, String> getOrderedTokens() {
        Map<String, String> acyclicTerminals = new LinkedHashMap<>();
        Map<String, String> cyclicTerminals = new LinkedHashMap<>();

        for (Map.Entry<String, TerminalSymbol> entry : this.bnfGrammar.getTerminals().entrySet()) {
            String symbolName = entry.getKey();
            String regexOrLiteral = this.bnfGrammar.getTerminalPool().get(entry.getValue());
            String regex;
            if (symbolName.startsWith(YajcoModelToBNFGrammarTranslator.DEFAULT_SYMBOL_NAME)) {
                // literal
                regex = Utilities.encodeStringIntoRegex(regexOrLiteral);
            } else {
                // already regex
                regex = regexOrLiteral;
            }

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
}
