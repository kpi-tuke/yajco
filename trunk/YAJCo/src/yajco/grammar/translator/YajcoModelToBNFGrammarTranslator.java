package yajco.grammar.translator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import yajco.generator.util.Utilities;
import yajco.model.Concept;
import yajco.model.Language;
import yajco.model.LocalVariablePart;
import yajco.model.Notation;
import yajco.model.NotationPart;
import yajco.model.PropertyReferencePart;
import yajco.model.TokenDef;
import yajco.model.TokenPart;
import yajco.grammar.bnf.Alternative;
import yajco.grammar.bnf.Grammar;
import yajco.grammar.NonterminalSymbol;
import yajco.grammar.bnf.Production;
import yajco.grammar.Symbol;
import yajco.grammar.TerminalSymbol;
import yajco.grammar.semlang.SemLangFactory;
import yajco.model.pattern.Pattern;
import yajco.model.pattern.impl.Associativity;
import yajco.model.pattern.impl.Factory;
import yajco.model.pattern.impl.Operator;
import yajco.model.pattern.impl.Parentheses;
import yajco.model.pattern.impl.Range;
import yajco.model.pattern.impl.Separator;
import yajco.model.pattern.impl.Token;
import yajco.model.type.ComponentType;
import yajco.model.type.ListType;
import yajco.model.type.PrimitiveType;
import yajco.model.type.ReferenceType;
import yajco.model.type.Type;

public class YajcoModelToBNFGrammarTranslator {

	public static final String DEFAULT_SYMBOL_NAME = "SYMBOL";
	public static final String DEFAULT_VAR_NAME = "val";
	public static final String DEFAULT_LIST_NAME = "list";
	public static final String DEFAULT_ELEMENT_NAME = "elem";
	private static final YajcoModelToBNFGrammarTranslator instance = new YajcoModelToBNFGrammarTranslator();
	private Language language;
	private Grammar grammar;
	private int arrayID;

	private YajcoModelToBNFGrammarTranslator() {
		language = null;
		grammar = null;
		arrayID = 1;
	}

	public Grammar translate(Language language) {
		if (language == null) {
			throw new NullPointerException("Parameter 'language' cann't be null!");
		}

		this.language = language;
		arrayID = 1;

		Concept mainConcept = language.getConcepts().get(0);
		NonterminalSymbol startSymbol = new NonterminalSymbol(mainConcept.getConceptName(), new ReferenceType(Utilities.getTopLevelParent(mainConcept), null), toPatternList(mainConcept.getPatterns()));

		grammar = new Grammar(startSymbol);
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
                            //System.out.println("Translating concept: "+c.getConceptName());
                            grammar.addProduction(translateConcept(c));
			}
		}

		if (grammar.getOperatorPool().containsKey(Integer.valueOf(0))) {
			processParenthesesOperator();
		}

		return grammar;
	}

	private Production translateConcept(Concept concept) {
		if (concept.getConcreteSyntax().size() == 0) {
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

			parAlternative.addSymbol(createTerminalFor(parPattern.getLeft()));
			parAlternative.addSymbol(nonterminal);
			parAlternative.addSymbol(createTerminalFor(parPattern.getRight()));
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
				NonterminalSymbol ddcNonterminal = new NonterminalSymbol(ddc.getConceptName(), new ReferenceType(ddc, null));
				ddcNonterminal.setVarName(DEFAULT_VAR_NAME);
				Alternative ddcAlternative = new Alternative();
				ddcAlternative.addSymbol(ddcNonterminal);
				ddcAlternative.addActions(SemLangFactory.createReturnSymbolValueActions(ddcNonterminal));
				alternatives.add(ddcAlternative);
			}
		}

		return new Production(conceptNonterminal, alternatives, toPatternList(concept.getPatterns()));
	}

	private Production translateNonAbstractConcept(Concept concept) {
		NonterminalSymbol conceptNonterminal = grammar.getNonterminal(concept.getConceptName());
		List<Alternative> alternatives = new ArrayList<Alternative>();

		for (Notation notation : concept.getConcreteSyntax()) {
			alternatives.add(translateNotation(notation, concept));
		}
		for (Concept ddc : Utilities.getDirectDescendantConcepts(concept, language)) {
			NonterminalSymbol ddcNonterminal = new NonterminalSymbol(ddc.getConceptName(), new ReferenceType(ddc, null));
			ddcNonterminal.setVarName(DEFAULT_VAR_NAME);
			Alternative ddcAlternative = new Alternative();
			ddcAlternative.addSymbol(ddcNonterminal);
			ddcAlternative.addActions(SemLangFactory.createReturnSymbolValueActions(ddcNonterminal));
			alternatives.add(ddcAlternative);
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
			Symbol symbol = null;
			if (part instanceof TokenPart) {
				symbol = translateTokenNotationPart((TokenPart) part);
			} else if (part instanceof PropertyReferencePart) {
				symbol = translatePropertyRefNotationPart((PropertyReferencePart) part);
				parameters.add(symbol);
			} else if (part instanceof LocalVariablePart) {
				symbol = translateLocalVarPart((LocalVariablePart) part);
				parameters.add(symbol);
			} else {
				throw new IllegalArgumentException("Unknown notation part: '" + part.getClass().getCanonicalName() + "'!");
			}
			alternative.addSymbol(symbol);
		}

		Operator opPattern = (Operator) concept.getPattern(Operator.class);
		Factory factoryPattern = (Factory) notation.getPattern(Factory.class);
		if (factoryPattern != null) {
//			if (opPattern == null) {
			alternative.addActions(SemLangFactory.createRefResolverFactoryClassInstRegisterAndReturnActions(Utilities.getFullConceptClassName(language, concept), factoryPattern.getName(), parameters));
//			} else {
//				alternative.addActions(SemLangFactory.createFactoryClassInstanceAndReturnActions(Utilities.getFullConceptClassName(language, concept), factoryPattern.getName(), parameters));
//			}
		} else {
//			if (opPattern == null) {
			alternative.addActions(SemLangFactory.createRefResolverNewClassInstRegisterAndReturnActions(Utilities.getFullConceptClassName(language, concept), parameters));
//			} else {
//				alternative.addActions(SemLangFactory.createNewClassInstanceAndReturnActions(Utilities.getFullConceptClassName(language, concept), parameters));
//			}
		}

		return alternative;
	}

	private TerminalSymbol translateTokenNotationPart(TokenPart part) {
		TokenDef token = getDefinedToken(part.getToken());
		TerminalSymbol terminal = null;
		if (token != null) {
			terminal = new TerminalSymbol(token.getName(), null);
			grammar.addTerminal(terminal, token.getRegexp());
		} else {
			terminal = createTerminalFor(part.getToken());
		}

		return terminal;
	}

	private Symbol translatePropertyRefNotationPart(PropertyReferencePart part) {
		Type type = part.getProperty().getType();
		Symbol symbol = null;

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
			TerminalSymbol terminal = new TerminalSymbol(token.getName(), primType);
			if (!grammar.getTerminals().containsKey(token.getName())) {
				grammar.addTerminal((TerminalSymbol) terminal, token.getRegexp());
			}

			symbol = terminal;
		}

		symbol.setVarName(part.getProperty().getName());
		return symbol;
	}

	private Symbol translateLocalVarPart(LocalVariablePart part) {
		if (!(part.getType() instanceof PrimitiveType)) {
			throw new IllegalArgumentException("Type " + part.getType() + " is not primitive!");
		}

		Token tokenPattern = (Token) part.getPattern(Token.class);
		TokenDef token = getDefinedToken(tokenPattern != null ? tokenPattern.getName() : part.getName());
		TerminalSymbol terminal = new TerminalSymbol(token.getName(), part.getType(), part.getName(), toPatternList(part.getPatterns()));
		if (!grammar.getTerminals().containsKey(token.getName())) {
			grammar.addTerminal(terminal, token.getRegexp());
		}

		return terminal;
	}

	private NonterminalSymbol translateComponentTypePropertyRef(PropertyReferencePart part) {
		ComponentType cmpType = (ComponentType) part.getProperty().getType();
		Type innerType = cmpType.getComponentType();
		Symbol symbol = null;
		String name, separator;
		int min, max;

		if (innerType instanceof ReferenceType) {
			ReferenceType refType = (ReferenceType) innerType;
			symbol = new NonterminalSymbol(refType.getConcept().getConceptName(), refType, DEFAULT_ELEMENT_NAME);
		} else {
			TokenDef token = getDefinedToken(part.getProperty().getName());
			symbol = new TerminalSymbol(token.getName(), innerType, DEFAULT_ELEMENT_NAME);
			if (!grammar.getTerminals().containsKey(token.getName())) {
				grammar.addTerminal((TerminalSymbol) symbol, token.getRegexp());
			}
		}

		Separator sepPattern = (Separator) part.getPattern(Separator.class);
		Range rangePattern = (Range) part.getPattern(Range.class);
		separator = sepPattern != null ? sepPattern.getValue() : "";
		min = rangePattern != null ? rangePattern.getMinOccurs() : 0;
		max = rangePattern != null ? rangePattern.getMaxOccurs() : Range.INFINITY;

		NonterminalSymbol nonterminal = grammar.getSequenceNonterminalFor(symbol.toString(), min, max, separator);
		if (nonterminal != null) {
			return new NonterminalSymbol(nonterminal.getName(), cmpType, nonterminal.getVarName());
		} else {
			return createSequenceProductionFor(symbol, min, max, separator, cmpType);
		}
	}

	private NonterminalSymbol createSequenceProductionFor(Symbol symbol, int minOccurs, int maxOccurs, String separator, ComponentType cmpType) {
		NonterminalSymbol lhs = new NonterminalSymbol(symbol.getName() + "Array" + arrayID++, new ListType(cmpType.getComponentType()));
		grammar.addNonterminal(lhs);
		TerminalSymbol sepTerminal = (separator != null && !separator.equals("")) ? createTerminalFor(separator) : null;
		Production production = new Production(lhs);

		NonterminalSymbol rhsNonterminal = new NonterminalSymbol(lhs.getName(), lhs.getReturnType(), DEFAULT_LIST_NAME);
		if ((minOccurs == 0 || minOccurs == 1) && maxOccurs == Range.INFINITY) {
			Alternative alternative1 = new Alternative();
			Alternative alternative2 = new Alternative();
			Alternative alternative3 = new Alternative();

			alternative1.addSymbol(rhsNonterminal);
			if (sepTerminal != null) {
				alternative1.addSymbol(sepTerminal);
			}
			alternative1.addSymbol(symbol);
			alternative1.addActions(SemLangFactory.createAddElementToCollectionAndReturnActions(rhsNonterminal, symbol));

			if (minOccurs == 1) {
				alternative2.addSymbol(symbol);
				alternative2.addActions(SemLangFactory.createListAndAddElementAndReturnActions(cmpType.getComponentType(), DEFAULT_LIST_NAME, symbol));
			} else {
				alternative2.addActions(SemLangFactory.createListAndReturnActions(cmpType.getComponentType()));
				if (sepTerminal != null) {
					alternative3.addSymbol(symbol);
					alternative3.addActions(SemLangFactory.createListAndAddElementAndReturnActions(cmpType.getComponentType(), DEFAULT_LIST_NAME, symbol));
				}
			}

			production.addAlternative(alternative1);
			production.addAlternative(alternative2);
			if (!alternative3.isEmpty()) {
				production.addAlternative(alternative3);
			}
		} else {
			int symID = 1;
			List<Symbol> symbols = new ArrayList<Symbol>(maxOccurs);
			for (int i = 0; i < minOccurs; i++) {
				symbols.add(symbol instanceof NonterminalSymbol ? new NonterminalSymbol(symbol.getName(), symbol.getReturnType(), DEFAULT_VAR_NAME + symID++) : new TerminalSymbol(symbol.getName(), symbol.getReturnType(), DEFAULT_VAR_NAME + symID++));
			}

			for (int i = minOccurs; i <= maxOccurs; i++) {
				Alternative alternative = new Alternative();
				alternative.addSymbols(symbols);
				alternative.addActions(SemLangFactory.createListAndAddElementsAndReturnActions(cmpType.getComponentType(), DEFAULT_LIST_NAME, symbols));
				production.addAlternative(alternative);

				symbols.add(symbol instanceof NonterminalSymbol ? new NonterminalSymbol(symbol.getName(), symbol.getReturnType(), DEFAULT_VAR_NAME + symID++) : new TerminalSymbol(symbol.getName(), symbol.getReturnType(), DEFAULT_VAR_NAME + symID++));
			}
		}

		grammar.addProduction(production);
		grammar.addSequence(symbol.toString(), minOccurs, maxOccurs, separator, lhs);

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
			Alternative parAlternative = grammar.getOperatorPool().get(Integer.valueOf(0)).get(0);
			grammar.getOperatorPool().remove(Integer.valueOf(0));

			parAlternative.addPattern(new Operator(foundPriority.intValue(), Associativity.LEFT));
			parAlternative.addPattern(new Parentheses());
			grammar.getOperatorPool().get(foundPriority).add(parAlternative);
		} else {
			Integer newPriority = Integer.valueOf(priorities.get(0).intValue() + 1);
			grammar.getOperatorPool().put(newPriority, grammar.getOperatorPool().get(Integer.valueOf(0)));
			grammar.getOperatorPool().remove(Integer.valueOf(0));

			Alternative parAlternative = grammar.getOperatorPool().get(newPriority).get(0);
			parAlternative.addPattern(new Operator(newPriority.intValue(), Associativity.LEFT));
			parAlternative.addPattern(new Parentheses());
		}
	}

	private List<Pattern> toPatternList(List<? extends Pattern> list) {
		List<Pattern> newList = new ArrayList<Pattern>(list.size());
		for (Pattern p : list) {
			newList.add(p);
		}

		return newList;
	}

//	private List<Symbol> toSymbolList(Symbol symbol) {
//		List<Symbol> list = new ArrayList<Symbol>(1);
//		list.add(symbol);
//
//		return list;
//	}
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
