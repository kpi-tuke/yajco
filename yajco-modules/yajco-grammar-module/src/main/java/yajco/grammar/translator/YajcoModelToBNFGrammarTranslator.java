package yajco.grammar.translator;

import yajco.grammar.NonterminalSymbol;
import yajco.grammar.Symbol;
import yajco.grammar.TerminalSymbol;
import yajco.grammar.bnf.Alternative;
import yajco.grammar.bnf.Grammar;
import yajco.grammar.bnf.Production;
import yajco.grammar.semlang.SemLangFactory;
import yajco.grammar.type.HashMapType;
import yajco.grammar.type.ObjectType;
import yajco.grammar.type.UnorderedParamType;
import yajco.model.*;
import yajco.model.pattern.Pattern;
import yajco.model.pattern.impl.*;
import yajco.model.type.*;
import yajco.model.utilities.Utilities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class YajcoModelToBNFGrammarTranslator {

    public static final String DEFAULT_SYMBOL_NAME = "SYMBOL";
    private static final String DEFAULT_VAR_NAME = "val";
    private static final String DEFAULT_LIST_NAME = "list";
    private static final String DEFAULT_SET_NAME = "set";
    private static final String DEFAULT_MAP_NAME = "map";
    private static final String DEFAULT_PARAM_VAR_NAME = "param";
    private static final String DEFAULT_ELEMENT_NAME = "elem";
    private static final String DEFAUL_SYMBOL_WITH_SHARED_SUFFIX = "WithSharedPart";
    private static final String DEFAULT_OPTIONAL_SYMBOL_NAME = "Optional";
    private static final String DEFAULT_ARRAY_SYMBOL_NAME = "Array";
    private static final String DEFAULT_PARAMS_SYMBOL_NAME = "Params";
    private static final String DEFAULT_PARAM_SYMBOL_NAME = "Param";
    private static final String DEFAULT_PARAMS_SYMBOL_VAR_NAME = "params";
    private static final YajcoModelToBNFGrammarTranslator instance = new YajcoModelToBNFGrammarTranslator();
    private Language language;
    private Grammar grammar;
    private int arrayID;
    private int optionalID;
    private String sharedPartName;
    private int unorderedParamID;
    private List<NonterminalSymbol> unorderedParamNonterminals;

    private YajcoModelToBNFGrammarTranslator() {
        language = null;
        grammar = null;
        arrayID = 1;
        optionalID = 1;
        unorderedParamID = 1;
        unorderedParamNonterminals = new ArrayList<NonterminalSymbol>();
    }

    public Grammar translate(Language language) {
        if (language == null) {
            throw new IllegalArgumentException("Parameter 'language' cannot be null!");
        }

        this.language = language;
        arrayID = 1;
        optionalID = 1;
        unorderedParamID = 1;
        unorderedParamNonterminals = new ArrayList<NonterminalSymbol>();

        Concept mainConcept = language.getConcepts().get(0);
        NonterminalSymbol startSymbol = new NonterminalSymbol(mainConcept.getConceptName(), new ReferenceType(Utilities.getTopLevelParent(mainConcept), null), toPatternList(mainConcept.getPatterns()));

        //TODO: toto je Dominikov test, aby boli terminali vlozene v poradi v akom su zadefinovane prv
        // problem ak mame terminal, ktory nie je pouzity !!!
        grammar = new Grammar(startSymbol);
        for (TokenDef tokenDef : language.getTokens()) {
            grammar.addTerminal(new TerminalSymbol(tokenDef.getName(), null), tokenDef.getRegexp());
        }
        //koniec

        grammar.addNonterminal(startSymbol);
        for (int i = 1; i < language.getConcepts().size(); i++) {
            Concept concept = language.getConcepts().get(i);
            if (concept.getPattern(Operator.class) != null) {
                continue;
            }

            NonterminalSymbol conceptNonterminal = new NonterminalSymbol(concept.getConceptName(), new ReferenceType(concept, null), toPatternList(concept.getPatterns()));
            grammar.addNonterminal(conceptNonterminal);
        }

        for (Concept c : language.getConcepts()) {
            if (c.getPattern(Operator.class) == null) {
                grammar.addProduction(translateConcept(c));
            }
        }

        if (grammar.getOperatorPool().containsKey(0)) {
            processParenthesesOperator();
        }

        return grammar;
    }

    private Production translateConcept(Concept concept) {
        if (concept.getConcreteSyntax().isEmpty()) {
            return translateAbstractConcept(concept);
        }
        if (concept.getPattern(yajco.model.pattern.impl.Enum.class) != null) {
            return translateEnumConcept(concept);
        } else {
            return translateNonAbstractConcept(concept);
        }
    }

    private Production translateAbstractConcept(Concept concept) {
        NonterminalSymbol conceptNonterminal = grammar.getNonterminal(concept.getConceptName());
        List<Alternative> alternatives = new ArrayList<Alternative>();

        Parentheses parPattern = (Parentheses) concept.getPattern(Parentheses.class);
        if (parPattern != null) {
            Alternative parAlternative = new Alternative();
            NonterminalSymbol nonterminal = new NonterminalSymbol(conceptNonterminal.getName(), conceptNonterminal.getReturnType(), DEFAULT_VAR_NAME);

            parAlternative.addSymbol(getTerminalFor(parPattern.getLeft()));
            parAlternative.addSymbol(nonterminal);
            parAlternative.addSymbol(getTerminalFor(parPattern.getRight()));
            parAlternative.addActions(SemLangFactory.createReturnSymbolValueActions(nonterminal));

            alternatives.add(parAlternative);
            grammar.addOperatorAlternative(0, parAlternative);
        }

        for (Concept ddc : Utilities.getDirectDescendantConcepts(concept, language)) {
            Operator opPattern = (Operator) ddc.getPattern(Operator.class);
            if (opPattern != null) {
                Alternative opAlternative = translateNotation(ddc.getConcreteSyntax().get(0), ddc);
                opAlternative.addPattern(opPattern);
                alternatives.add(opAlternative);
                grammar.addOperatorAlternative(opPattern.getPriority(), opAlternative);
            } else {
                translateDescendatConcept(alternatives, ddc);
            }
        }

        return new Production(conceptNonterminal, alternatives, toPatternList(concept.getPatterns()));
    }

    private void translateDescendatConcept(List<Alternative> alternatives, Concept ddc) {
        NonterminalSymbol ddcNonterminal = new NonterminalSymbol(ddc.getConceptName(), new ReferenceType(ddc, null));
        ddcNonterminal.setVarName(DEFAULT_VAR_NAME);
        Alternative ddcAlternative = new Alternative();
        ddcAlternative.addSymbol(ddcNonterminal);
        ddcAlternative.addActions(SemLangFactory.createReturnSymbolValueActions(ddcNonterminal));
        alternatives.add(ddcAlternative);
    }

    private Production translateNonAbstractConcept(Concept concept) {
        NonterminalSymbol conceptNonterminal = grammar.getNonterminal(concept.getConceptName());
        List<Alternative> alternatives = new ArrayList<Alternative>();

        for (Notation notation : concept.getConcreteSyntax()) {
            alternatives.add(translateNotation(notation, concept));
        }
        for (Concept ddc : Utilities.getDirectDescendantConcepts(concept, language)) {
            translateDescendatConcept(alternatives, ddc);
        }

        return new Production(conceptNonterminal, alternatives, toPatternList(concept.getPatterns()));
    }

    private Production translateEnumConcept(Concept concept) {
        NonterminalSymbol conceptNonterminal = grammar.getNonterminal(concept.getConceptName());
        String enumType = Utilities.getFullConceptClassName(language, concept);
        List<Alternative> alternatives = new ArrayList<Alternative>(concept.getConcreteSyntax().size());

        for (Notation notation : concept.getConcreteSyntax()) {
            Alternative alternative = new Alternative();
            TokenPart tokenPart = (TokenPart) notation.getParts().get(0);
            alternative.addSymbol(translateTokenNotationPart(tokenPart));
            alternative.addActions(SemLangFactory.createEnumInstanceAndReturnActions(enumType, tokenPart.getToken()));

            alternatives.add(alternative);
        }

        return new Production(conceptNonterminal, alternatives, toPatternList(concept.getPatterns()));
    }

    private Alternative translateNotation(Notation notation, Concept concept) {
        Alternative alternative = new Alternative(null, null, toPatternList(notation.getPatterns()));
        List<Symbol> parameters = new ArrayList<Symbol>(notation.getParts().size());

        for (NotationPart part : notation.getParts()) {
            Symbol symbol;
            if (part instanceof TokenPart) {
                symbol = translateTokenNotationPart((TokenPart) part);
            } else if (part instanceof PropertyReferencePart) {
                symbol = translatePropertyRefNotationPart((PropertyReferencePart) part);
                parameters.add(symbol);
            } else if (part instanceof LocalVariablePart) {
                symbol = translateLocalVarPart((LocalVariablePart) part);
                parameters.add(symbol);
            } else if (part instanceof OptionalPart) {
                symbol = translateOptionalPart(concept, (OptionalPart) part);
                parameters.add(symbol);
            } else if (part instanceof UnorderedParamPart) {
                symbol = translateUnorderedParamPart(concept, (UnorderedParamPart) part);
                parameters.add(symbol);
                this.unorderedParamNonterminals.add(new NonterminalSymbol(symbol.getName(), symbol.getReturnType(), DEFAULT_PARAM_VAR_NAME + unorderedParamID++));
                symbol = null;
            } else {
                throw new IllegalArgumentException("Unknown notation part: '" + part.getClass().getCanonicalName() + "'!");
            }

            if (this.unorderedParamNonterminals.size() > 0 && !(part instanceof UnorderedParamPart)) {
                Symbol nonterminal = this.createNonterminalForUnorderedParams(concept);
                alternative.addSymbol(nonterminal);
                this.unorderedParamNonterminals = new ArrayList<NonterminalSymbol>();
            }

            if (symbol != null) {
                alternative.addSymbol(symbol);
            }
        }

        if (this.unorderedParamNonterminals.size() > 0) {
            Symbol nonterminal = this.createNonterminalForUnorderedParams(concept);
            alternative.addSymbol(nonterminal);
            this.unorderedParamNonterminals = new ArrayList<NonterminalSymbol>();
        }

//        Operator opPattern = (Operator) concept.getPattern(Operator.class);
        Factory factoryPattern = (Factory) notation.getPattern(Factory.class);
        if (factoryPattern != null) {
//            if (opPattern == null) {
            alternative.addActions(SemLangFactory.createRefResolverFactoryClassInstRegisterAndReturnActions(Utilities.getFullConceptClassName(language, concept), factoryPattern.getName(), parameters, this.sharedPartName));
//            } else {
//                alternative.addActions(SemLangFactory.createFactoryClassInstanceAndReturnActions(Utilities.getFullConceptClassName(language, concept), factoryPattern.getName(), parameters));
//            }
        } else {
//            if (opPattern == null) {
            alternative.addActions(SemLangFactory.createRefResolverNewClassInstRegisterAndReturnActions(Utilities.getFullConceptClassName(language, concept), parameters, this.sharedPartName));
//            } else {
//                alternative.addActions(SemLangFactory.createNewClassInstanceAndReturnActions(Utilities.getFullConceptClassName(language, concept), parameters));
//            }
        }
        this.sharedPartName = null;

        return alternative;
    }


    /**
     * Creates nonterminal symbol representing array of unordered parameters.
     */
    private NonterminalSymbol createNonterminalForUnorderedParams(Concept concept) {
        NonterminalSymbol lhs = new NonterminalSymbol(concept.getConceptName() + DEFAULT_PARAMS_SYMBOL_NAME, new HashMapType(new ObjectType()));
        grammar.addNonterminal(lhs);

        Production production = new Production(lhs);
        List<Symbol> symbols = new ArrayList<Symbol>(this.unorderedParamNonterminals);

        Alternative alternative = new Alternative();
        alternative.addSymbols(symbols);
        alternative.addActions(SemLangFactory.createHashMapAndPutElementsAndReturnActions(new ObjectType(), DEFAULT_MAP_NAME, symbols));
        production.addAlternative(alternative);

        grammar.addProduction(production);
        grammar.addSequence(lhs.getName(), this.unorderedParamNonterminals.size(), this.unorderedParamNonterminals.size(), null, false, null, lhs);

        return new NonterminalSymbol(lhs.getName(), new ObjectType(), DEFAULT_PARAMS_SYMBOL_VAR_NAME);
    }

    private Symbol translateUnorderedParamPart(Concept concept, UnorderedParamPart unorderedParamPart) {
        NonterminalSymbol conceptNonterminal;
        Alternative alternative = new Alternative();
        Symbol varSymbol = null;

        Symbol symbol = null;
        for (NotationPart notationPart : unorderedParamPart.getParts()) {
            if (notationPart instanceof TokenPart) {
                symbol = translateTokenNotationPart((TokenPart) notationPart);
            } else if (notationPart instanceof PropertyReferencePart) {
                symbol = translatePropertyRefNotationPart((PropertyReferencePart) notationPart);
                varSymbol = symbol;
            } else if (notationPart instanceof LocalVariablePart) {
                symbol = translateLocalVarPart((LocalVariablePart) notationPart);
                varSymbol = symbol;
            } else if (notationPart instanceof OptionalPart) {
                symbol = translateOptionalPart(concept, (OptionalPart) notationPart);
                varSymbol = symbol;
            } else {
                throw new IllegalArgumentException("Unknown notation part: '" + notationPart.getClass().getCanonicalName() + "'!");
            }
            alternative.addSymbol(symbol);
        }

        String varName = null;
        Type varType = null;
        if (varSymbol != null) {
            varName = varSymbol.getVarName();
            varType = varSymbol.getReturnType();
            alternative.addActions(
                SemLangFactory.createNewUnorderedParamClassInstanceAndReturnActions(
                    Collections.singletonList(varSymbol),
                    varName));
        }

        conceptNonterminal = new NonterminalSymbol(
            concept.getConceptName() + DEFAULT_PARAM_SYMBOL_NAME,
            new UnorderedParamType(varType),
            varName);
        Production production = new Production(conceptNonterminal, Collections.singletonList(alternative), toPatternList(concept.getPatterns()));
        Production existingProduction = grammar.getProduction(conceptNonterminal);

        if (existingProduction != null) {
            // Replace existing production with new one, which contains alternatives from old plus new alternative.
            // All alternatives of general param symbol.
            List<Alternative> newAlternatives = new ArrayList<Alternative>(existingProduction.getRhs());
            newAlternatives.add(alternative);
            production = new Production(conceptNonterminal, newAlternatives, toPatternList(concept.getPatterns()));
            grammar.getProductions().remove(conceptNonterminal);
            grammar.addProduction(production);
        } else {
            grammar.addProduction(production);
            grammar.addNonterminal(conceptNonterminal);
        }
        return conceptNonterminal;
    }

    private Symbol translateOptionalPart(Concept concept, OptionalPart optionalPart) {
        for (NotationPart notationPart : optionalPart.getParts()) {
            if (notationPart instanceof LocalVariablePart) {
                return translateOptionalLocalVariablePart(concept, (LocalVariablePart) notationPart);
            } else if (notationPart instanceof PropertyReferencePart) {
                Symbol conceptNonterminal = translateOptionalPropertyReferencePart(concept, optionalPart, (PropertyReferencePart) notationPart);
                if (conceptNonterminal != null) {
                    return conceptNonterminal;
                }
            }
        }
        throw new RuntimeException("Cannot read Optional type!");
    }

    private Symbol translateOptionalPropertyReferencePart(Concept concept, OptionalPart optionalPart, PropertyReferencePart notationPart) {
        Type type = notationPart.getProperty().getType();
        if (type instanceof ComponentType) {
            ComponentType cmpType = (ComponentType) notationPart.getProperty().getType();

            Type innerType = cmpType.getComponentType();
            String name;
            String varName = null;
            if (innerType instanceof ReferenceType) {
                ReferenceType refType = (ReferenceType) innerType;
                name = refType.getConcept().getConceptName();
            } else if (innerType instanceof ComponentType) {
                name = notationPart.getProperty().getName();
            } else {
                TokenDef token = getDefinedToken(notationPart.getProperty().getName());
                name = token != null ? token.getName() : null;
            }

            List<Alternative> alternatives = new ArrayList<Alternative>();

            Alternative alternative1 = new Alternative();
            List<Symbol> symbols = new ArrayList<Symbol>(1);
            for (NotationPart part : optionalPart.getParts()) {
                Symbol symbol;
                if (part instanceof TokenPart) {
                    symbol = translateTokenNotationPart((TokenPart) part);
                } else if (part instanceof PropertyReferencePart) {
                    symbol = translatePropertyRefNotationPart((PropertyReferencePart) part);
                    varName = symbol.getVarName();
                    alternative1.addActions(SemLangFactory.createNewOptionalClassInstanceAndReturnActions(Collections.singletonList(symbol)));
                } else if (part instanceof LocalVariablePart) {
                    symbol = translateLocalVarPart((LocalVariablePart) part);
                    varName = symbol.getVarName();
                } else {
                    throw new IllegalArgumentException("Unknown notation part: '" + optionalPart.getClass().getCanonicalName() + "'!");
                }
                symbols.add(symbol);
            }

            alternative1.addSymbols(symbols);
            alternatives.add(alternative1);

            symbols = new ArrayList<Symbol>(1);
            Alternative alternative2 = new Alternative();
            alternative2.addSymbols(symbols);
            alternative2.addActions(SemLangFactory.createNewOptionalClassInstanceAndReturnActions(symbols));
            alternatives.add(alternative2);

            NonterminalSymbol conceptNonterminal = new NonterminalSymbol(DEFAULT_OPTIONAL_SYMBOL_NAME + name + "_" +optionalID++,
                    new OptionalType(cmpType.getComponentType()), varName);

            Production production = new Production(conceptNonterminal, alternatives, toPatternList(concept.getPatterns()));
            Production existingProduction = grammar.getExistingProductionForOptionalNonterminal(conceptNonterminal.getName(), production);

            return addProductionAndGetNonterminal(conceptNonterminal, production, existingProduction);
        }
        return null;
    }

    private Symbol translateOptionalLocalVariablePart(Concept concept, LocalVariablePart notationPart) {
        if (!(notationPart.getType() instanceof PrimitiveType)) {
            throw new IllegalArgumentException("Type " + notationPart.getType() + " is not primitive!");
        }

        Token tokenPattern = (Token) notationPart.getPattern(Token.class);
        TokenDef token = getDefinedToken(tokenPattern != null ? tokenPattern.getName() : notationPart.getName());
        TerminalSymbol terminal = null;

        if (token != null) {
            terminal = new TerminalSymbol(token.getName(), notationPart.getType(), notationPart.getName(), toPatternList(notationPart.getPatterns()));
        }

        if (token != null && !grammar.getTerminals().containsKey(token.getName())) {
            grammar.addTerminal(terminal, token.getRegexp());
        }

        List<Alternative> alternatives = new ArrayList<Alternative>();

        Alternative alternative1 = new Alternative();
        List<Symbol> symbols = new ArrayList<Symbol>(1);

        symbols.add(terminal);

        alternative1.addSymbols(symbols);
        alternative1.addActions(SemLangFactory.createNewOptionalClassInstanceAndReturnActions(symbols));
        alternatives.add(alternative1);

        symbols = new ArrayList<Symbol>(1);
        Alternative alternative2 = new Alternative();
        alternative2.addSymbols(symbols);
        alternative2.addActions(SemLangFactory.createNewOptionalClassInstanceAndReturnActions(symbols));
        alternatives.add(alternative2);

        NonterminalSymbol conceptNonterminal = new NonterminalSymbol(DEFAULT_OPTIONAL_SYMBOL_NAME + notationPart.getName() + "_" +optionalID++,
                new OptionalType(notationPart.getType()), terminal != null ? terminal.getVarName() : DEFAULT_ELEMENT_NAME+optionalID);

        Production production = new Production(conceptNonterminal, alternatives, toPatternList(concept.getPatterns()));
        Production existingProduction = grammar.getExistingProductionForOptionalNonterminal(conceptNonterminal.getName(), production);

        return addProductionAndGetNonterminal(conceptNonterminal, production, existingProduction);
    }

    private Symbol addProductionAndGetNonterminal(NonterminalSymbol conceptNonterminal, Production production, Production existingProduction) {
        if (existingProduction != null) {
            grammar.addNonterminal(existingProduction.getLhs());
            grammar.addProduction(existingProduction);
            optionalID--;
            return existingProduction.getLhs();
        } else {
            grammar.addProduction(production);
            grammar.addNonterminal(conceptNonterminal);
            return conceptNonterminal;
        }
    }



    private TerminalSymbol translateTokenNotationPart(TokenPart part) {
        return getTerminalFor(part.getToken());
    }

    private TerminalSymbol getTerminalFor(String terminal) {
        if (terminal == null || terminal.isEmpty()) {
            return null;
        }
        TokenDef token = getDefinedToken(terminal);
        TerminalSymbol terminalSymbol;
        if (token != null) {
            terminalSymbol = new TerminalSymbol(token.getName(), null);
            grammar.addTerminal(terminalSymbol, token.getRegexp());
        } else {
            terminalSymbol = createTerminalFor(terminal);
        }

        return terminalSymbol;
    }

    private Symbol translatePropertyRefNotationPart(PropertyReferencePart part) {
        Type type = part.getProperty().getType();
        Symbol symbol;

        if (type instanceof ReferenceType) {
            ReferenceType refType = (ReferenceType) type;
            symbol = new NonterminalSymbol(refType.getConcept().getConceptName(), refType);
        } else if (type instanceof ComponentType) {
            symbol = translateComponentTypePropertyRef(part);
        } else {
            PrimitiveType primType = (PrimitiveType) type;
            Token tokenPattern = (Token) part.getPattern(Token.class);
            String tokenName = tokenPattern != null ? tokenPattern.getName() : part.getProperty().getName();
            TokenDef token = getDefinedToken(tokenName);
            TerminalSymbol terminal = null;
            if (token != null) {
                terminal = new TerminalSymbol(token.getName(), primType);
            }
            if (token != null && !grammar.getTerminals().containsKey(token.getName())) {
                grammar.addTerminal(terminal, token.getRegexp());
            }
            symbol = terminal;
        }

        if (symbol != null) {
            symbol.setVarName(part.getProperty().getName());
            for (Pattern pattern : part.getPatterns())
                symbol.addPattern(pattern);
        }
        return symbol;
    }

    private Symbol translateLocalVarPart(LocalVariablePart part) {
        if (!(part.getType() instanceof PrimitiveType)) {
            throw new IllegalArgumentException("Type " + part.getType() + " is not primitive!");
        }

        Token tokenPattern = (Token) part.getPattern(Token.class);
        TokenDef token = getDefinedToken(tokenPattern != null ? tokenPattern.getName() : part.getName());
        TerminalSymbol terminal = null;
        if (token != null) {
            terminal = new TerminalSymbol(token.getName(), part.getType(), part.getName(), toPatternList(part.getPatterns()));
        }
        if (token != null && !grammar.getTerminals().containsKey(token.getName())) {
            grammar.addTerminal(terminal, token.getRegexp());
        }

        return terminal;
    }

    private Symbol translateComponentTypePropertyRef(PropertyReferencePart part) {
        final ComponentType cmpType = (ComponentType) part.getProperty().getType();
        final Type innerType = cmpType.getComponentType();
        final Symbol symbol;
        final String separator, sharedPartName;
        final int min, max;
        final boolean unique;

        if (innerType instanceof ReferenceType) {
            ReferenceType refType = (ReferenceType) innerType;
            symbol = new NonterminalSymbol(refType.getConcept().getConceptName(), refType, DEFAULT_ELEMENT_NAME);
        } else if (innerType instanceof ComponentType) {
            symbol = translateOptionalComponentTypePropertyRef((ComponentType) innerType, part);
            symbol.setVarName("Optional" + part.getProperty().getName());
        } else {
            TokenDef token = getDefinedToken(part.getProperty().getName());
            symbol = new TerminalSymbol(token != null ? token.getName() : null, innerType, DEFAULT_ELEMENT_NAME);
            if (token != null && !grammar.getTerminals().containsKey(token.getName())) {
                grammar.addTerminal((TerminalSymbol) symbol, token.getRegexp());
            }
        }

        Separator sepPattern = (Separator) part.getPattern(Separator.class);
        Range rangePattern = (Range) part.getPattern(Range.class);
        UniqueValues uniqueValuesPattern = (UniqueValues) part.getPattern(UniqueValues.class);
        Shared sharedPattern = (Shared) part.getPattern(Shared.class);

        unique = uniqueValuesPattern != null;
        sharedPartName = sharedPattern != null ? sharedPattern.getValue() : "";
        separator = sepPattern != null ? sepPattern.getValue() : "";
        min = rangePattern != null ? rangePattern.getMinOccurs() : 0;
        max = rangePattern != null ? rangePattern.getMaxOccurs() : Range.INFINITY;

        if (cmpType instanceof OptionalType) {
            return symbol;
        }
        return getOrCreateSequenceProductionFor(symbol, min, max, separator, cmpType, unique, sharedPattern, sharedPartName);
    }

    private NonterminalSymbol translateOptionalComponentTypePropertyRef(ComponentType cmpType, PropertyReferencePart part) {
        final Type innerType = cmpType.getComponentType();
        final Symbol symbol;
        final String separator, sharedPartName;
        final int min, max;
        final boolean unique;

        if (innerType instanceof ReferenceType) {
            ReferenceType refType = (ReferenceType) innerType;
            symbol = new NonterminalSymbol(refType.getConcept().getConceptName(), refType, DEFAULT_ELEMENT_NAME);
        } else {
            TokenDef token = getDefinedToken(part.getProperty().getName());
            symbol = new TerminalSymbol(token != null ? token.getName() : null, innerType, DEFAULT_ELEMENT_NAME);
            if (token != null && !grammar.getTerminals().containsKey(token.getName())) {
                grammar.addTerminal((TerminalSymbol) symbol, token.getRegexp());
            }
        }

        Separator sepPattern = (Separator) part.getPattern(Separator.class);
        Range rangePattern = (Range) part.getPattern(Range.class);
        UniqueValues uniqueValuesPattern = (UniqueValues) part.getPattern(UniqueValues.class);
        Shared sharedPattern = (Shared) part.getPattern(Shared.class);

        unique = uniqueValuesPattern != null;
        sharedPartName = sharedPattern != null ? sharedPattern.getValue() : "";
        separator = sepPattern != null ? sepPattern.getValue() : "";
        min = rangePattern != null ? rangePattern.getMinOccurs() : 1;
        max = rangePattern != null ? rangePattern.getMaxOccurs() : Range.INFINITY;

        return getOrCreateSequenceProductionFor(symbol, min, max, separator, cmpType, unique, sharedPattern, sharedPartName);
    }

    private NonterminalSymbol getOrCreateSequenceProductionFor(Symbol symbol, int minOccurs, int maxOccurs, String separator, ComponentType cmpType, boolean unique, Shared sharedPattern, String sharedPartName) {
        NonterminalSymbol nonterminal = grammar.getSequenceNonterminalFor(symbol.toString(), minOccurs, maxOccurs, separator, unique, sharedPartName);
        if (nonterminal != null) {
            return new NonterminalSymbol(nonterminal.getName(), cmpType, nonterminal.getVarName());
        }
        if (sharedPattern != null) {
            return createSequenceProductionWithSharedFor(symbol, minOccurs, maxOccurs, separator, cmpType, sharedPattern);
        } else {
            return createSequenceProductionFor(symbol, minOccurs, maxOccurs, separator, cmpType, unique);
        }
    }

    private NonterminalSymbol createSequenceProductionFor(Symbol symbol, int minOccurs, int maxOccurs, String separator, ComponentType cmpType, boolean unique) {
        String name = unique ? DEFAULT_SET_NAME : DEFAULT_LIST_NAME;
        NonterminalSymbol lhs = unique
                ? new NonterminalSymbol(symbol.getName() + DEFAULT_ARRAY_SYMBOL_NAME + arrayID++, new OrderedSetType(cmpType.getComponentType()))
                : new NonterminalSymbol(symbol.getName() + DEFAULT_ARRAY_SYMBOL_NAME + arrayID++, new ListType(cmpType.getComponentType()));
        grammar.addNonterminal(lhs);

        TerminalSymbol sepTerminal = getTerminalFor(separator);

        Production production = new Production(lhs);

        NonterminalSymbol rhsNonterminal = new NonterminalSymbol(lhs.getName(), lhs.getReturnType(), name);
        if ((minOccurs == 0 || minOccurs == 1) && maxOccurs == Range.INFINITY) {
            Alternative alternative1 = new Alternative();
            Alternative alternative2 = new Alternative();
            Alternative alternative3 = new Alternative();

            alternative1.addSymbol(rhsNonterminal);
            alternative1.addSymbol(sepTerminal);
            alternative1.addSymbol(symbol);
            alternative1.addActions(SemLangFactory.createAddElementToCollectionAndReturnActions(rhsNonterminal, symbol));

            if (minOccurs == 1) {
                alternative2.addSymbol(symbol);
                alternative2.addActions(unique
                        ? SemLangFactory.createOrderedSetAndAddElementAndReturnActions(cmpType.getComponentType(), name, symbol)
                        : SemLangFactory.createListAndAddElementAndReturnActions(cmpType.getComponentType(), name, symbol)
                );
            } else {
                alternative2.addActions(unique
                        ? SemLangFactory.createOrderedSetAndReturnActions(cmpType.getComponentType())
                        : SemLangFactory.createListAndReturnActions(cmpType.getComponentType())
                );
                if (sepTerminal != null) {
                    alternative3.addSymbol(symbol);
                    alternative3.addActions(unique
                            ? SemLangFactory.createOrderedSetAndAddElementAndReturnActions(cmpType.getComponentType(), name, symbol)
                            : SemLangFactory.createListAndAddElementAndReturnActions(cmpType.getComponentType(), name, symbol)
                    );
                }
            }

            production.addAlternative(alternative1);
            production.addAlternative(alternative2);
            if (!alternative3.isEmpty()) {
                production.addAlternative(alternative3);
            }
        } else {
            int symID = 1;
            List<Symbol> symbols = new ArrayList<>(maxOccurs);
            for (int i = 0; i < minOccurs; i++) {
                symbols.add(symbol.withVarName(DEFAULT_VAR_NAME + symID++));
            }

            for (int occurrenceIndex = minOccurs; occurrenceIndex <= maxOccurs; occurrenceIndex++) {
                Alternative alternative = new Alternative();
                for (int symbolIndex = 0; symbolIndex < symbols.size(); symbolIndex++) {
                    if (symbolIndex > 0) {
                        alternative.addSymbol(sepTerminal);
                    }
                    alternative.addSymbol(symbols.get(symbolIndex));
                }
                alternative.addActions(unique
                        ? SemLangFactory.createOrderedSetAndAddElementsAndReturnActions(cmpType.getComponentType(), name, symbols)
                        : SemLangFactory.createListAndAddElementsAndReturnActions(cmpType.getComponentType(), name, symbols)
                );
                production.addAlternative(alternative);

                symbols.add(symbol.withVarName(DEFAULT_VAR_NAME + symID++));
            }
        }

        grammar.addProduction(production);
        grammar.addSequence(symbol.toString(), minOccurs, maxOccurs, separator, unique, null, lhs);

        return new NonterminalSymbol(lhs.getName(), cmpType);
    }

    private NonterminalSymbol createSequenceProductionWithSharedFor(Symbol symbol, int minOccurs, int maxOccurs, String separator, ComponentType cmpType, Shared shared) {
        NonterminalSymbol nonterminal = grammar.getSequenceNonterminalFor(symbol.toString(), minOccurs, maxOccurs, separator, false, null);
        if (nonterminal == null) {
            nonterminal = createSequenceProductionFor(symbol, minOccurs, maxOccurs, separator, cmpType, false);
        }

        this.sharedPartName = Character.toUpperCase(shared.getValue().charAt(0)) + shared.getValue().substring(1);

        NonterminalSymbol lhs = grammar.getSequenceNonterminalFor(nonterminal.toString(), minOccurs, maxOccurs, separator, false, null);
        if (lhs == null) {
            lhs = new NonterminalSymbol(symbol.getName() + "Array" + arrayID++ + DEFAUL_SYMBOL_WITH_SHARED_SUFFIX, new ListTypeWithShared(cmpType.getComponentType()));
        }
        grammar.addNonterminal(lhs);

        TerminalSymbol sepTerminal = getTerminalFor(shared.getSeparator());
        Production production = new Production(lhs);

        NonterminalSymbol rhsNonterminal = new NonterminalSymbol(lhs.getName(), lhs.getReturnType(), DEFAULT_LIST_NAME);
        Alternative alternative1 = new Alternative();
        Alternative alternative2 = new Alternative();

        alternative1.addSymbol(rhsNonterminal);
        if (sepTerminal != null) {
            alternative1.addSymbol(sepTerminal);
        }
        alternative1.addSymbol(nonterminal);
        nonterminal.setVarName(DEFAULT_ELEMENT_NAME);
        alternative1.addActions(SemLangFactory.createAddElementToCollectionAndReturnActions(rhsNonterminal, nonterminal));

        alternative2.addSymbol(nonterminal);
        alternative2.addActions(SemLangFactory.createListWithSharedAndAddElementAndReturnActions(cmpType.getComponentType(), DEFAULT_LIST_NAME, nonterminal));

        production.addAlternative(alternative1);
        production.addAlternative(alternative2);

        grammar.addProduction(production);
        grammar.addSequence(nonterminal.toString(), 1, Range.INFINITY, shared.getSeparator(), false, shared.getValue(), lhs);

        return new NonterminalSymbol(lhs.getName(), cmpType);
    }

    private void processParenthesesOperator() {
        List<Integer> priorities = new ArrayList(grammar.getOperatorPool().keySet());
        Collections.sort(priorities);
        Collections.reverse(priorities);
        priorities.remove(priorities.size() - 1);

        Integer foundPriority = -1;
        for (Integer priority : priorities) {
            Alternative opAlternative = grammar.getOperatorPool().get(priority).get(0);
            Operator opPattern = (Operator) opAlternative.getPattern(Operator.class);

            if (opPattern.getAssociativity() != Associativity.LEFT) {
                continue;
            }

            foundPriority = priority;
            break;
        }

        if (foundPriority != -1) {
            Alternative parAlternative = grammar.getOperatorPool().get(0).get(0);
            grammar.getOperatorPool().remove(0);

            parAlternative.addPattern(new Operator(foundPriority, Associativity.LEFT));
            parAlternative.addPattern(new Parentheses());
            grammar.getOperatorPool().get(foundPriority).add(parAlternative);
        } else {
            int newPriority = priorities.get(0) + 1;
            grammar.getOperatorPool().put(newPriority, grammar.getOperatorPool().get(0));
            grammar.getOperatorPool().remove(0);

            Alternative parAlternative = grammar.getOperatorPool().get(newPriority).get(0);
            parAlternative.addPattern(new Operator(newPriority, Associativity.LEFT));
            parAlternative.addPattern(new Parentheses());
        }
    }

    private List<Pattern> toPatternList(List<? extends Pattern> list) {
        List<Pattern> newList = new ArrayList<Pattern>(list.size());
        newList.addAll(list);

        return newList;
    }

    //    private List<Symbol> toSymbolList(Symbol symbol) {
//        List<Symbol> list = new ArrayList<Symbol>(1);
//        list.add(symbol);
//
//        return list;
//    }
    private TokenDef getDefinedToken(String name) {
        String upperCaseNotation = Utilities.toUpperCaseNotation(name);
        for (TokenDef token : language.getTokens()) {
            if (token.getName().equals(name) || token.getName().equals(upperCaseNotation)) {
                return token;
            }
        }

        if (name.endsWith("s")) {
            return getDefinedToken(name.substring(0, name.length() - 1));
        }

        return null;
    }

    private TerminalSymbol createTerminalFor(String regex) {
        TerminalSymbol terminal = grammar.getTerminal(regex);
        if (terminal == null) {
            terminal = new TerminalSymbol(DEFAULT_SYMBOL_NAME + regexToName(regex), null);
            grammar.addTerminal(terminal, regex);
        }

        return terminal;
    }

    private String regexToName(String regex) {
        boolean keepName = Character.isLetter(regex.charAt(0));
        if (keepName) {
            for (int i = 1; i < regex.length(); i++) {
                if (!Character.isLetterOrDigit(regex.charAt(i))) {
                    keepName = false;
                    break;
                }
            }
        }

        if (keepName) {
            return regex.toUpperCase();
        }

        StringBuilder builder = new StringBuilder(2 * regex.length());
        for (int i = 0; i < regex.length(); i++) {
            char sym = regex.charAt(i);
            if (Character.isLetterOrDigit(sym)) {
                builder.append(sym);
            } else {
                builder.append("_");
                builder.append(Integer.toString((int) sym));
                builder.append("_");
            }
        }
        if (builder.charAt(builder.length() - 1) == '_') {
            builder.setLength(builder.length() - 1);
        }

        return builder.toString();
    }

    public static YajcoModelToBNFGrammarTranslator getInstance() {
        return instance;
    }
}
