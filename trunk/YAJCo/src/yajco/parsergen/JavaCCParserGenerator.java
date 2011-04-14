package yajco.parsergen;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.apache.velocity.app.VelocityEngine;
import yajco.annotation.config.Option;
import yajco.annotation.config.Skip;
import yajco.generator.GeneratorException;
import yajco.parsergen.javacc.model.Choice;
import yajco.parsergen.javacc.model.Expansion;
import yajco.parsergen.javacc.model.Model;
import yajco.parsergen.javacc.model.NonTerminal;
import yajco.parsergen.javacc.model.Production;
import yajco.parsergen.javacc.model.Sequence;
import yajco.parsergen.javacc.model.Terminal;
import yajco.parsergen.javacc.model.ZeroOrMany;
import yajco.parsergen.javacc.model.ZeroOrOne;
import yajco.model.BindingNotationPart;
import yajco.model.Concept;
import yajco.model.Language;
import yajco.model.LocalVariablePart;
import yajco.model.Notation;
import yajco.model.NotationPart;
import yajco.model.PropertyReferencePart;
import yajco.model.TokenDef;
import yajco.model.TokenPart;
import yajco.model.pattern.impl.Associativity;
import yajco.model.pattern.impl.Factory;
import yajco.model.pattern.impl.Operator;
import yajco.model.pattern.impl.Parentheses;
import yajco.model.pattern.impl.Range;
import yajco.model.pattern.impl.Separator;
import yajco.model.type.ArrayType;
import yajco.model.type.ComponentType;
import yajco.model.type.ListType;
import yajco.model.type.PrimitiveType;
import yajco.model.type.ReferenceType;
import yajco.model.type.SetType;
import yajco.model.type.Type;

public class JavaCCParserGenerator {

	private final static String PARSER_CLASS_NAME = "Parser";
	private final Language language;
	private final Map<String, Production> productions = new HashMap<String, Production>();
	private final Map<Concept, Set<Integer>> operatorConcepts = new HashMap<Concept, Set<Integer>>();
	private final VelocityEngine velocityEngine = new VelocityEngine();
	private final Map<String, String> definedTokens = new HashMap<String, String>();
	private final Set<Concept> processedConcepts = new HashSet<Concept>();
	private static final Conversions stringConversions = new Conversions();

	public JavaCCParserGenerator(Language language) {
		this.language = language;
	}

	public void generate() throws IOException {
		String parserClassName = PARSER_CLASS_NAME;
		String parserPackageName = language.getName();
		String parserJavaCCPackageName = parserPackageName == null ? "javacc" : parserPackageName + ".javacc";
//		String parserQualifiedClassName = parserPackageName == null ? parserClassName : parserPackageName + "." + parserClassName;

		for (TokenDef token : language.getTokens()) {
			definedTokens.put(token.getName(), token.getRegexp());
		}

		Concept concept = language.getConcepts().get(0);
		processMainConcept(concept, 0);

		Model model = new Model(parserJavaCCPackageName, parserClassName != null ? parserClassName.trim() : "",
				language.getSkips().toArray(new Skip[] {}), definedTokens, new Option[]{}, productions.get(getNonterminal(concept, 0)),
				productions.values().toArray(new Production[]{}));
//		model.generate(new PrintWriter(System.out));
		PrintWriter writer = new PrintWriter(System.out);
		model.generate(writer);
	}

	private void processMainConcept(Concept concept, int paramNumber) {
		if (processedConcepts.contains(concept)) {
			return;
		} else {
			processedConcepts.add(concept);
		}

		Expansion expansion;

		if (concept.getPattern(yajco.model.pattern.impl.Enum.class) != null) {
			expansion = processEnumConcept(concept);
		} else if (isAbstractConcept(concept)) {
			expansion = processAbstractConcept(concept, paramNumber);
		} else {
			expansion = processConcept(concept);
		}

		Production production = new Production(concept.getName(), getFullName(concept.getName()), expansion);
		productions.put(concept.getName(), production);
	}

	private Expansion processEnumConcept(Concept concept) {
		List<Expansion> expansions = new ArrayList<Expansion>();
		List<Notation> notations = concept.getConcreteSyntax();
		TokenPart token = null;
		for (Notation notation : notations) {
			token = (TokenPart) notation.getParts().get(0);

			// TODO: Token pattern
//			if (element.getAnnotation(Token.class) != null) {
//				token = element.getAnnotation(Token.class).value();
//			}
			if (!definedTokens.containsKey(token.getToken())) {
				definedTokens.put(token.getToken(), token.getToken());
			}

			expansions.add(new Terminal(
					null,
					"return " + getFullName(concept.getName()) + "." + token.getToken() + ";",
					/*token == null ? element.getSimpleName().toString() :*/ token.getToken(),
					null));
		}

		return new Choice(expansions.toArray(new Expansion[]{}));
	}

	private Expansion processAbstractConcept(Concept concept, int paramNumber) {
		// Find operators - direct subconcepts
		Map<Integer, List<Concept>> priorityMap = findOperatorsInSubconcepts(concept, null);

		// Process priority map for abstract concept
		if (priorityMap != null) {
			//Add to operator concepts with the lowest priority
			operatorConcepts.put(concept, priorityMap.keySet());
			processPriorityMap(concept, priorityMap, paramNumber);
		}

		// Create production for subclasses
		List<Expansion> expansions = new ArrayList<Expansion>();
		generateProductionsForSubconcepts(concept, getDirectSubconcepts(concept), expansions, paramNumber);

		// Parenthesis for operator
		Parentheses parenthesesPattern = (Parentheses) concept.getPattern(Parentheses.class);
		if (parenthesesPattern != null) {
			expansions.add(new Sequence(
					new Terminal(createTerminal(parenthesesPattern.getLeft())),
					new NonTerminal(getNonterminal(concept, paramNumber), "_value"),
					new Terminal(createTerminal(parenthesesPattern.getRight()))));
		}

		// Lookahead
		// TODO: Lookahead pattern
		String lookahead = null;
//		if (typeElement.getAnnotation(Lookahead.class) != null) {
//			lookahead = typeElement.getAnnotation(Lookahead.class).value();
//		}

		return new Choice(
				"  " + getFullName(concept.getName()) + " _value = null;\n",
				"return _value;",
				lookahead,
				expansions.toArray(new Expansion[]{}));
	}

	private Expansion processConcept(Concept concept) {
		List<Expansion> sequences = new ArrayList<Expansion>();
		List<Notation> alternatives = concept.getConcreteSyntax();

		int paramNumber = 0;
		for (Notation alternative : alternatives) {
			paramNumber++;
			List<Expansion> expansions = new ArrayList<Expansion>();

			StringBuffer code = new StringBuffer();
			boolean separator = false;
			for (NotationPart part : alternative.getParts()) {
				if (part instanceof TokenPart) {
					expansions.add(new Terminal(createTerminal(((TokenPart) part).getToken())));
					continue;
				}
				expansions.add(processBindingNotation((BindingNotationPart) part, paramNumber));
				if (separator) {
					code.append(", ");
				}
				code.append(notationPartToName((BindingNotationPart) part) + "_" + paramNumber);
				separator = true;
			}

			String argsCode = code.toString().trim();
			if ("".equals(argsCode)) {
				argsCode = "";
			} else {
				argsCode = ", (Object)" + code.toString();
			}

			String referenceResolverAction;
			if (alternative.getPattern(Factory.class) == null) { // je to konstruktor
				referenceResolverAction = "return tuke.pargen.ReferenceResolver.getInstance().register(new " + getFullName(concept.getName()) + "( " + code.toString() + ")" + argsCode + ");";
			} else { // je to tovarenska metoda
				Factory factoryPattern = (Factory) alternative.getPattern(Factory.class);
				String factoryMethodName = factoryPattern.getName();
				referenceResolverAction = "return tuke.pargen.ReferenceResolver.getInstance().register(" + getFullName(concept.getName()) + "." + factoryMethodName + "( " + code.toString() + ")" + argsCode + ");";
			}

			Sequence sequence = new Sequence(
					null,
					referenceResolverAction,
					expansions.toArray(new Expansion[]{}));
			sequences.add(sequence);
		}

		Choice choice = new Choice(sequences.toArray(new Expansion[]{}));

		//Find subconcepts if there are any, create choice
		List<Expansion> expansionsInheritedConcepts = new ArrayList<Expansion>();
		generateProductionsForSubconcepts(concept, getDirectSubconcepts(concept), expansionsInheritedConcepts, paramNumber);
		if (expansionsInheritedConcepts.size() != 0) {
			expansionsInheritedConcepts.add(choice);

			return new Choice(
					"  " + getFullName(concept.getName()) + " _value = null;\n",
					"return _value;",
					null,
					expansionsInheritedConcepts.toArray(new Expansion[]{}));
		} else {
			return choice;
		}
	}

	private Expansion processBindingNotation/*processParam*/(BindingNotationPart bindingPart, int paramNumber) {
		List<Expansion> expansions = new ArrayList<Expansion>();

		//Parameter
		Type bindingPartType = bindingPartToType(bindingPart);
//		TypeMirror paramElemType = paramElement.asType();
//		String paramElemTypeString = paramElemType.toString();

		if (bindingPartType instanceof ComponentType) {
			expansions.add(processArrayType(bindingPart, paramNumber));
		} else {
			String variableName = notationPartToName(bindingPart) + "_" + paramNumber;
			expansions.add(processSimpleType(bindingPart, variableName, typeToString(bindingPartType), "", paramNumber));
		}

		//Optional annotation
		Expansion expansion = new Sequence(expansions.toArray(new Expansion[]{}));
		// TODO: Optional pattern
//		if (paramElement.getAnnotation(Optional.class) != null) {
//			expansion = new ZeroOrOne(expansion);
//		}

		return expansion;
	}

	private Expansion processArrayType(BindingNotationPart bindingPart, int paramNumber) {
		int from = 0;
		int to = Range.INFINITY;
		String separator = "";
		ComponentType type = (ComponentType) bindingPartToType(bindingPart);
		Type baseComponentType = type.getComponentType();
		String componentType = null;
		boolean isArray = bindingPartToType(bindingPart) instanceof ArrayType;
		boolean isList = bindingPartToType(bindingPart) instanceof ListType;
		boolean isSet = bindingPartToType(bindingPart) instanceof SetType;
		boolean isPrimitive = false;

		if (isArray) {
			isPrimitive = baseComponentType instanceof PrimitiveType;
			if (isPrimitive) {
				componentType = primitiveTypeToBoxedTypeString((PrimitiveType) baseComponentType);
			} else {
				componentType = typeToString(baseComponentType);
			}
		} else {
			if (baseComponentType instanceof PrimitiveType) {
				componentType = primitiveTypeToBoxedTypeString((PrimitiveType) baseComponentType);
			} else {
				componentType = typeToString(baseComponentType);
			}
		}

		if (bindingPart.getPattern(Range.class) != null) {
			Range rangePattern = (Range) bindingPart.getPattern(Range.class);
			from = rangePattern.getMinOccurs();
			to = rangePattern.getMaxOccurs();
		}
		if (bindingPart.getPattern(Separator.class) != null) {
			separator = ((Separator) bindingPart.getPattern(Separator.class)).getValue();
		}

		StringBuilder decl = new StringBuilder();
		String variableName = notationPartToName(bindingPart) + "_" + paramNumber;
		if (isArray) {
			decl.append("  " + typeToString(baseComponentType) + "[] " + variableName + " = null;\n");
		} else if (isList) {
			decl.append("  java.util.List<" + componentType + "> " + variableName + " = null;\n");
		} else {
			decl.append("  java.util.Set<" + componentType + "> " + variableName + " = null;\n");
		}

		if (isArray || isList) {
			decl.append("  java.util.List<" + componentType + "> _list" + variableName + " = new java.util.ArrayList<" + componentType + ">();\n");
		} else {
			decl.append("  java.util.Set<" + componentType + "> _list" + variableName + " = new java.util.HashSet<" + componentType + ">();\n");
		}
		StringBuilder code = new StringBuilder();
		code.append("_list" + variableName + ".add(_item" + variableName + ");");
		StringBuilder ccode = new StringBuilder();
		if (isArray && isPrimitive) {
			ccode.append(variableName + " = new " + typeToString(baseComponentType) + "[_list" + variableName + ".size()]; for (int i = 0; i < _list" + variableName + ".size(); i++) { " + variableName + "[i] = _list" + variableName + ".get(i); }");
		} else if (isArray && !isPrimitive) {
			ccode.append(variableName + " = _list" + variableName + ".toArray(new " + componentType + "[] {});");
		} else {
			ccode.append(variableName + " = _list" + variableName + ";");
		}

		Expansion separatorTerminal = "".equals(separator) ? null : new Terminal(createTerminal(separator));
		Expansion sexpansion = processSimpleType(bindingPart, "_item" + variableName, componentType, code.toString(), paramNumber);
		decl.append(sexpansion.getDecl());
		sexpansion.setDecl(null);

		//Lookahead
		// TODO: Lookahead pattern
		String lookahead = null;
//		if (paramElement.getAnnotation(Lookahead.class) != null) {
//			lookahead = paramElement.getAnnotation(Lookahead.class).value();
//		}

		if (from == 0 && to == Range.INFINITY && !"".equals(separator)) {
			return new ZeroOrOne(
					decl.toString(),
					ccode.toString(),
					new Sequence(sexpansion,
					new ZeroOrMany(null, null, lookahead,
					new Sequence(separatorTerminal, sexpansion))));
		} else {
			List<Expansion> expansions = new ArrayList<Expansion>();
			for (int i = 0; i < from; i++) {
				if (i > 0 && !"".equals(separator)) {
					expansions.add(separatorTerminal);
				}
				expansions.add(sexpansion);
			}
			if (to == Range.INFINITY) {
				if (!"".equals(separator)) {
					expansions.add(
							new ZeroOrMany(null, null, lookahead,
							new Sequence(separatorTerminal, sexpansion)));
				} else {
					expansions.add(new ZeroOrMany(null, null, lookahead, sexpansion));
				}
			} else {
				if (from < to) {
					Expansion expansion;
					if (from > 0 && !"".equals(separator)) {
						expansion = new Sequence(separatorTerminal, sexpansion);
					} else {
						expansion = sexpansion;
					}
					expansion = new ZeroOrOne(expansion);
					for (int i = from + 1; i < to; i++) {
						if (!"".equals(separator)) {
							expansion = new Sequence(separatorTerminal, sexpansion, expansion);
						} else {
							expansion = new Sequence(sexpansion, expansion);
						}
						expansion = new ZeroOrOne(expansion);
					}
					expansions.add(expansion);
				}
				//TODO: chyba ak plati, ze to<from
			}
			return new Sequence(
					decl.toString(),
					ccode.toString(),
					expansions.toArray(new Expansion[]{}));
		}
	}

	private Expansion processSimpleType(BindingNotationPart bindingPart, String variableName, String bindingPartType, String code, int paramNumber) {
		// TODO: Token pattern
		if (stringConversions.containsConversion(bindingPartType)/* || paramElement.getAnnotation(Token.class) != null*/) {
			//Terminal = conversion exists or param has @Token
			return generateTeminal(bindingPart, bindingPartType, variableName, code);
		} else { //Nonterminal
			return generateNonteminal(bindingPart, variableName, bindingPartType, code, paramNumber);
		}
	}

	private Map<Integer, List<Concept>> findOperatorsInSubconcepts(Concept concept, Map<Integer, List<Concept>> priorityMap) {
		Set<Concept> subconcepts = getDirectSubconcepts(concept);
		for (Concept subconcept : subconcepts) {
			// TODO: ??? ??? ???
			if (isAbstractConcept(subconcept)) {
				priorityMap = findOperatorsInSubconcepts(subconcept, priorityMap);
			} else {
				if (isOperatorConcept(subconcept)) {
					Operator operatorPattern = (Operator) subconcept.getPattern(Operator.class);
					int priority = operatorPattern.getPriority();
					if (priorityMap == null) {
						priorityMap = new TreeMap<Integer, List<Concept>>();
					}
					List<Concept> operatorList = priorityMap.get(priority);
					if (operatorList == null) {
						operatorList = new ArrayList<Concept>();
						priorityMap.put(priority, operatorList);
					}
					operatorList.add(subconcept);
				}
			}
		}
		return priorityMap;
	}

	private void generateProductionsForSubconcepts(Concept superConcept, Set<Concept> subconcepts, List<Expansion> expansions, int paramNumber) {
		for (Concept concept : subconcepts) {
			if (isDirectSubconcept(superConcept, concept) && !isOperatorConcept(concept)) {
				// TODO: ??? ??? ???
				if (isAbstractConcept(concept)) {
					// Dany concept je abstraktny, tak ho v hierarchii preskocme a podme na dalsiu uroven
					// v poradi
					// ak dany concept nema ziadnych potomkov, tak spracovanie nie je nutne
					if (getDirectSubconcepts(concept).size() == 0) {
						continue;
					}
					// prejdime na ďalšiu úroveň v poradí
					generateProductionsForSubconcepts(concept, getDirectSubconcepts(concept), expansions, paramNumber);
				} else {
					expansions.add(new NonTerminal(getNonterminal(concept, paramNumber), "_value"));
				}
			}
		}
	}

	private void processPriorityMap(Concept operatorConcept, Map<Integer, List<Concept>> priorityMap, int paramNumber) {
		for (int priority : priorityMap.keySet()) {
			List<Concept> operatorList = priorityMap.get(priority);

//			ExecutableElement constructorElement = getConstructorElement(operatorList.get(0));
			int arity = getArity(operatorConcept, operatorList.get(0).getConcreteSyntax().get(0));
			Associativity associativity = Associativity.AUTO;

			//Test validity
			for (Concept concept : operatorList) {
//				constructorElement = getConstructorElement(concept);
				if (arity != getArity(operatorConcept, concept.getConcreteSyntax().get(0))) {
					throw new GeneratorException("All operators of type '" + getFullName(operatorConcept.getName()) +
							"' with the same priority must have the same arity (difference found in '" + getFullName(concept.getName()) + "')");
				}
				Associativity operatorAssociativity = ((Operator) concept.getPattern(Operator.class)).getAssociativity();
				//
				if (operatorAssociativity == null) {
					operatorAssociativity = Associativity.AUTO;
				}
				//
				if (associativity == Associativity.AUTO) {
					associativity = operatorAssociativity;
				}
				if (operatorAssociativity != Associativity.AUTO) {
					if (associativity != Associativity.AUTO && associativity != operatorAssociativity) {
						throw new GeneratorException("All operators of type '" + getFullName(operatorConcept.getName()) + "' with the same priority must have the same association type (difference found in '" + getFullName(concept.getName()) + "')");
					}
				}
				if (arity == 0) {
					throw new GeneratorException("Nulary operators are not supported '" + getFullName(concept.getName()) + "'");
				}
				boolean prefixed = hasPrefix(operatorConcept, concept.getConcreteSyntax().get(0));
				boolean postfixed = hasPostfix(operatorConcept, concept.getConcreteSyntax().get(0));
				if (prefixed && postfixed) {
					throw new GeneratorException("The operator of type '" + getFullName(concept.getName()) + "' is prefixed and postfixed at the same time. Remove the Operator pattern");
				}
				if (arity == 1) {
					if (!prefixed && !postfixed) {
						throw new GeneratorException("Unary prefix operator of type '" + getFullName(concept.getName()) + "' should be prefixed or postfixed");
					}
					if (associativity == Associativity.LEFT && prefixed) {
						throw new GeneratorException("Unary prefix operator of type '" + getFullName(concept.getName()) + "' should not be left-associative");
					}
					if (associativity == Associativity.RIGHT && postfixed) {
						throw new GeneratorException("Unary postfix operator of type '" + getFullName(concept.getName()) + "' should not be right-associative");
					}

					if (associativity == Associativity.AUTO) {
						if (prefixed) {
							associativity = Associativity.RIGHT;
						}
						if (postfixed) {
							associativity = Associativity.LEFT;
						}
					} /* else {
					if (associativity == Associativity.RIGHT && prefixed) {
					throw new GeneratorException("Nary right-associative operator of type " + subclassElement + " should not be prefixed");
					}
					if (associativity == Associativity.LEFT && postfixed) {
					throw new GeneratorException("N-ary left-associative operator of type " + subclassElement + " should not be postfixed");
					}
					} */
				}

			}

			//Set auto associativity to left if it is not set
			if (associativity == Associativity.AUTO) {
				associativity = Associativity.LEFT;
			}

			//Declarations
			StringBuilder decl = new StringBuilder();
			for (int i = 1; i <= arity; i++) {
				decl.append("  " + getFullName(operatorConcept.getName()) + " _node" + i + " = null;\n");
			}

			String highestPriorityNonterminal = getNonterminal(operatorConcept, paramNumber);
			String nextPriorityNonterminal = getHigherPriorityNonterminal(priority, operatorConcept, priorityMap.keySet());
			String currentPriorityNonterminal = operatorConcept.getName() + priority;
			Expansion expansion = null;
			if (arity == 1) {
				if (associativity == Associativity.LEFT) {
					expansion = new Sequence(decl.toString(), "return _node1;",
							new NonTerminal(nextPriorityNonterminal, "_node1"),
							new ZeroOrMany(generatePostfixOptions(null, null, operatorList, operatorConcept, paramNumber)));
				} else if (associativity == Associativity.RIGHT) {
					expansion = new Choice(decl.toString(), "return _node1;",
							generatePrefixOptions(currentPriorityNonterminal, operatorList, operatorConcept, paramNumber),
							new NonTerminal(nextPriorityNonterminal, "_node1"));
				} else if (associativity == Associativity.NONE) {
					Expansion prefixExpansion = generatePrefixOptions(nextPriorityNonterminal, operatorList, operatorConcept, paramNumber);
					Expansion postfixExpansion = generatePostfixOptions(null, null, operatorList, operatorConcept, paramNumber);
					if (prefixExpansion != null && postfixExpansion != null) {
						expansion = new Choice(decl.toString(), "return _node1;",
								new Sequence(
								new NonTerminal(nextPriorityNonterminal, "_node1"),
								new ZeroOrOne(postfixExpansion)),
								prefixExpansion);
					} else if (prefixExpansion != null) {
						expansion = new Choice(decl.toString(), "return _node1;",
								prefixExpansion,
								new NonTerminal(nextPriorityNonterminal, "_node1"));
					} else {
						expansion = new Sequence(decl.toString(), "return _node1;",
								new NonTerminal(nextPriorityNonterminal, "_node1"),
								new ZeroOrOne(postfixExpansion));
					}
				}
			} else {
				//TODO: podpora len pre infixne binarne operatory
				//TODO: Co ak by bol format A -> '+' A A, resp. A -> A A '-'
				//TODO: dorobit aritu > 2
				//Ak je ohranicene medzi dvoma terminalmi/neterminalny treba dat highestpriority
				//A -> '?' A ':' A
				if (associativity == Associativity.LEFT) {
					expansion = new Sequence(decl.toString(), "return _node1;",
							new NonTerminal(nextPriorityNonterminal, "_node1"),
							new ZeroOrMany(generatePostfixOptions(highestPriorityNonterminal, nextPriorityNonterminal, operatorList, operatorConcept, paramNumber)));
				} else if (associativity == Associativity.RIGHT) {
					expansion = new Sequence(decl.toString(), "return _node1;",
							new NonTerminal(nextPriorityNonterminal, "_node1"),
							new ZeroOrOne(generatePostfixOptions(highestPriorityNonterminal, currentPriorityNonterminal, operatorList, operatorConcept, paramNumber)));
				} else if (associativity == Associativity.NONE) {
					expansion = new Sequence(decl.toString(), "return _node1;",
							new NonTerminal(nextPriorityNonterminal, "_node1"),
							new ZeroOrOne(generatePostfixOptions(highestPriorityNonterminal, nextPriorityNonterminal, operatorList, operatorConcept, paramNumber)));
				}
			}
			Production production = new Production(operatorConcept.getName() + priority, getFullName(operatorConcept.getName()), expansion);
			productions.put(operatorConcept.getName() + priority, production);
		}
	}

	private Expansion generatePrefixOptions(String nonterminal, List<Concept> operatorList, Concept operatorConcept, int paramNumber) {
		List<Expansion> oExpansions = new ArrayList<Expansion>();
		int type = 0;
		StringBuilder code = new StringBuilder();
		code.append("switch(_type) {");
		for (Concept subconcept : operatorList) {
			if (hasPrefix(operatorConcept, subconcept.getConcreteSyntax().get(0))) {
				List<Expansion> sExpansions = new ArrayList<Expansion>();
				type++;
				StringBuilder params = new StringBuilder();
				code.append("case " + type + ": _node1 = tuke.pargen.ReferenceResolver.getInstance().register(new " + getFullName(subconcept.getName()) + "(");
				boolean separator = false;
				int index = 0;
				List<NotationPart> notationParts = subconcept.getConcreteSyntax().get(0).getParts();
				for (int i = 0; i < notationParts.size(); i++) {
					NotationPart notationPart = notationParts.get(i);
					if (notationPart instanceof TokenPart) {
						sExpansions.add(new Terminal(createTerminal(((TokenPart) notationPart).getToken())));
						continue;
					}
					if (separator) {
						params.append(", ");
					}
					separator = true;
					if (isOperatorType(operatorConcept, notationPart)) {
						//TODO: Spracovat ak je to subclass a nie priamo trieda operatora
						//System.out.println(">>>>>>>>>>>>>>>> " + paramElement.asType());
						index++;
						// pridajme pretipovanie, ak je nutne
						if (isSameConcept(operatorConcept, bindingPartToConcept((BindingNotationPart) notationPart))) {
							params.append("_node" + index);
						} else {
							params.append("(" + getFullName(notationPartToName(notationPart)) + ")" + "_node" + index);
						}
					} else {
						sExpansions.add(processBindingNotation((BindingNotationPart) notationPart, paramNumber));
						params.append(notationPartToName(notationPart));
					}
				}

				code.append(params + "), (Object)" + params);
				code.append("); break; ");

				oExpansions.add(new Sequence(null, "_type = " + type + ";", sExpansions.toArray(new Expansion[]{})));
			}
		}

		code.append("};");

		Expansion expansion;

		if (oExpansions.size() == 0) {
			return null;
		} else if (oExpansions.size() > 1) {
			expansion = new Choice(oExpansions.toArray(new Expansion[]{}));
		} else {
			expansion = oExpansions.get(0);
		}

		Expansion nonTerminal = new NonTerminal(nonterminal, "_node1");
		expansion = new Sequence("  int _type = 0;\n", code.toString(), expansion, nonTerminal);

		return expansion;
	}

	private Expansion generatePostfixOptions(String highestPriorityNonterminal, String nonterminal, List<Concept> operatorList, Concept operatorConcept, int paramNumber) {
		List<Expansion> oExpansions = new ArrayList<Expansion>();
		for (Concept subconcept : operatorList) {
			if (hasPostfix(operatorConcept, subconcept.getConcreteSyntax().get(0)) || nonterminal != null) {
				List<Expansion> sExpansions = new ArrayList<Expansion>();
				StringBuilder code = new StringBuilder();
				StringBuilder params = new StringBuilder();
				code.append("_node1 = tuke.pargen.ReferenceResolver.getInstance().register(new " + getFullName(subconcept.getName()) + "(");
				boolean separator = false;
				int index = 0;
				List<NotationPart> notationParts = subconcept.getConcreteSyntax().get(0).getParts();
				List<BindingNotationPart> bindingNotationParts = getBindingNotationParts(subconcept.getConcreteSyntax().get(0));
				for (int i = 0, j = 0; i < notationParts.size(); i++) {
					NotationPart notationPart = notationParts.get(i);
					if (notationPart instanceof TokenPart) {
						sExpansions.add(new Terminal(createTerminal(((TokenPart) notationPart).getToken())));
						continue;
					}
					j++;
					if (separator) {
						params.append(", ");
					}

					separator = true;
					if (isOperatorType(operatorConcept, notationPart)) {
						//TODO: Spracovat ak je to subclass a nie priamo trieda operatora
						/*if (!isSameOperatorType(classElement, paramElement)) {
						//						System.out.println("------>> Postfix - spracuvavam triedu: " + getTypeElementFrom(paramElement.asType()));
						processTypeElement(getTypeElementFrom(paramElement.asType()));
						}*/
						//System.out.println(">>>>>>>>>>>>>>>> " + paramElement.asType());
						index++;

						if (j == bindingNotationParts.size() - 1 && !hasPostfix(operatorConcept, subconcept.getConcreteSyntax().get(0))) {
							sExpansions.add(new NonTerminal(nonterminal, "_node" + index));
						} else {
							sExpansions.add(new NonTerminal(highestPriorityNonterminal, "_node" + index));
						}

						// add type casting if necessary
						if (isSameConcept(operatorConcept, bindingPartToConcept((BindingNotationPart) notationPart))) {
							params.append("_node" + index);
						} else {
							params.append("(" + getFullName(notationPartToName(notationPart)) + ")" + "_node" + index);
						}
					} else {
						sExpansions.add(processBindingNotation((BindingNotationPart) notationPart, paramNumber));
						params.append(notationPartToName(notationPart));
					}
				}

				code.append(params + "), (Object)" + params);
				code.append(");");
				oExpansions.add(new Sequence(null, code.toString(), sExpansions.toArray(new Expansion[]{})));
			}
		}

		if (oExpansions.size() == 0) {
			return null;
		} else if (oExpansions.size() > 1) {
			return new Choice(oExpansions.toArray(new Expansion[]{}));
		} else {
			return oExpansions.get(0);
		}
	}

	private boolean hasPrefix(Concept operatorConcept, Notation notation) {
		NotationPart notationPart = notation.getParts().get(0);
		if (notationPart instanceof TokenPart) {
			return true;
		} else if (!isOperatorType(operatorConcept, notationPart)) {
			return true;
		}
		return false;
	}

	private boolean hasPostfix(Concept operatorConcept, Notation notation) {
		NotationPart notationPart = notation.getParts().get(notation.getParts().size() - 1);
		if (notationPart instanceof TokenPart) {
			return true;
		} else if (!isOperatorType(operatorConcept, notationPart)) {
			return true;
		}
		return false;
	}

	private int getArity(Concept operatorConcept, Notation notation) {
		int count = 0;
		for (NotationPart notationPart : notation.getParts()) {
			if (isOperatorType(operatorConcept, notationPart)) {
				count++;
			}
		}
		return count;
	}

	private boolean isOperatorType(Concept operatorConcept, NotationPart notationPart) {
		if (notationPart instanceof TokenPart) {
			return false;
		}
		return isSubconcept(bindingPartToConcept((BindingNotationPart) notationPart), operatorConcept);
	}

	private String createTerminal(String token) {
		if (!definedTokens.containsKey(token)) {
			definedTokens.put(token, token);
		}
		return token;
	}

	private Terminal generateTeminal(BindingNotationPart bindingPart, String type, String variableName, String code) {
		String token = "";
		// TODO: Token pattern
//		if (paramElement.getAnnotation(Token.class) != null) {
//			token = paramElement.getAnnotation(Token.class).value();
//		}
		if ("".equals(token)) {
			String partName = notationPartToName(bindingPart);
			token = toUpperCaseNotation(partName);
			if (!definedTokens.containsValue(token) && partName.endsWith("s")) {
				token = toUpperCaseNotation(partName.substring(0, partName.length() - 1));
			}
		}

		if (stringConversions.containsConversion(type)) {
			String conversion = stringConversions.getConversion(type);
			String defaultValue = stringConversions.getDefaultValue(type);
			Formatter decl = new Formatter();
			decl.format("  %s %s = %s;\n", type, variableName, defaultValue);
			decl.format("  Token _token%s = null;\n", variableName);

			Formatter codet = new Formatter();
			codet.format("%s = ", variableName);
			codet.format(conversion, "_token" + variableName + ".image");
			codet.format(";");
			codet.format("%s", code);

			return new Terminal(decl.toString(), codet.toString(), token, "_token" + variableName);
		} else {
			throw new GeneratorException("Unsuported parameter '" + notationPartToName(bindingPart) + " : " + type +/*
					"' in element '" + paramElement.getEnclosingElement().getEnclosingElement() + */ "'");
		}
	}

	private String getNonterminal(Concept concept, int paramNumber) {
		processMainConcept(concept, paramNumber);
		if (operatorConcepts.containsKey(concept)) {
			return concept.getName() + operatorConcepts.get(concept).iterator().next().toString();
		}
		return concept.getName();
	}

	private Expansion generateNonteminal(BindingNotationPart bindingPart, String variableName, String type, String code, int paramNumber) {
//		if (concept.getPattern(yajco.model.pattern.impl.Enum.class) != null/* || isKnownClass(element)*/) { // Neterminal
		return new NonTerminal(
				"  " + type + " " + variableName + " = null;\n",
				code,
				getNonterminal(bindingPartToConcept(bindingPart), paramNumber),
				variableName);
//		} else {
//			throw new GeneratorException("1Unsuported parameter '" + paramElement + " : " + paramElement.asType() +
//					"' in element '" + paramElement.getEnclosingElement().getEnclosingElement() + "'");
//		}
	}

	private String getHigherPriorityNonterminal(int current, Concept concept, Set<Integer> priorities) {
		Iterator<Integer> iterator = priorities.iterator();
		while (iterator.hasNext()) {
			if (iterator.next().equals(current)) {
				break;
			}
		}
		if (iterator.hasNext()) {
			return concept.getName() + String.valueOf(iterator.next());
		}
		return concept.getName();
	}

	private boolean isDirectSubconcept(Concept superConcept, Concept concept) {
		Set<Concept> subconcepts = getDirectSubconcepts(superConcept);
		for (Concept subconcept : subconcepts) {
			if (subconcept.getName().equals(concept.getName())) {
				return true;
			}
		}
		return false;
	}

	private String typeToString(Type type) {
		if (type instanceof PrimitiveType) {
			return primitiveTypeToString((PrimitiveType) type);
		} else if (type instanceof ReferenceType) {
			ReferenceType refType = (ReferenceType) type;
			return getFullName(refType.getConcept().getName());
		} else if (type instanceof ArrayType) {
			return typeToString(((ArrayType) type).getComponentType());
		}
		throw new UnsupportedOperationException("Unknown type found");
	}

	private String primitiveTypeToString(PrimitiveType type) {
		switch (type.getPrimitiveTypeConst()) {
			case BOOLEAN:
				return "boolean";
			case INTEGER:
				return "int";
			case REAL:
				return "float";
			case STRING:
				return "java.lang.String";
			default:
				throw new UnsupportedOperationException("Unknown primitive type " + type.toString());
		}
	}

	private String primitiveTypeToBoxedTypeString(PrimitiveType type) {
		switch (type.getPrimitiveTypeConst()) {
			case BOOLEAN:
				return "Boolean";
			case INTEGER:
				return "Integer";
			case REAL:
				return "Float";
			case STRING:
				return "String";
			default:
				throw new UnsupportedOperationException("Unknown primitive type " + type);
		}
	}

	private String toUpperCaseNotation(String camelNotation) {
		StringBuilder sb = new StringBuilder(camelNotation.length() + 10);
		boolean change = true;
		for (int i = 0; i < camelNotation.length(); i++) {
			char c = camelNotation.charAt(i);
			change = !change && Character.isUpperCase(c);
			if (change) {
				sb.append('_');
			}
			sb.append(Character.toUpperCase(c));
			change = Character.isUpperCase(c);
		}
		return sb.toString();
	}

	private Set<Concept> getDirectSubconcepts(Concept parent) {
		Set<Concept> subconcepts = new HashSet<Concept>();
		for (Concept concept : language.getConcepts()) {
			if (concept.getParent() != null && concept.getParent().equals(parent)) {
				subconcepts.add(concept);
			}
		}
		return subconcepts;
	}

	private boolean isOperatorConcept(Concept concept) {
		return concept.getPattern(Operator.class) != null;
	}

	int getArity(Notation notation) {
		int arity = 0;
		for (NotationPart part : notation.getParts()) {
			if (part instanceof PropertyReferencePart || part instanceof LocalVariablePart) {
				arity++;
			}
		}
		return arity;
	}

	private boolean isSameConcept(Concept c1, Concept c2) {
		return c1.getName().equals(c2.getName());
	}

	private boolean isSubconcept(Concept c1, Concept c2) {
		if (c1.getName().equals(c2.getName())) {
			return true; // kazdy typ sa povazuje za podtyp sameho seba
		}
		Set<Concept> c2Subconcepts = getDirectSubconcepts(c2);
		for (Concept subconcept : c2Subconcepts) {
			if (subconcept.getName().equals(c1.getName())) {
				return true;
			}
		}
		for (Concept subconcept : c2Subconcepts) {
			if (isSubconcept(c1, subconcept)) {
				return true;
			}
		}
		return false;
	}

	private boolean isAbstractConcept(Concept concept) {
		return concept.getAbstractSyntax().size() == 0 && concept.getConcreteSyntax().size() == 0;
	}

	private List<BindingNotationPart> getBindingNotationParts(Notation notation) {
		List<BindingNotationPart> parts = new ArrayList<BindingNotationPart>();
		for (NotationPart part : notation.getParts()) {
			if (part instanceof BindingNotationPart) {
				parts.add((BindingNotationPart) part);
			}
		}
		return parts;
	}

	private Concept bindingPartToConcept(BindingNotationPart bindingPart) {
		Type type = bindingPartToType(bindingPart);
		if (type instanceof ComponentType) {
			type = ((ComponentType) type).getComponentType();
		}
		ReferenceType refType = (ReferenceType) type;
//		if (bindingPart instanceof LocalVariablePart) {
//			refType = (ReferenceType) ((LocalVariablePart) bindingPart).getType();
//		} else {
//			refType = (ReferenceType) ((PropertyReferencePart) bindingPart).getProperty().getType();
//		}
		return refType.getConcept();
	}

	private Type bindingPartToType(BindingNotationPart bindingPart) {
		if (bindingPart instanceof PropertyReferencePart) {
			return ((PropertyReferencePart) bindingPart).getProperty().getType();
		} else { // je to LocalVariablePart
			return ((LocalVariablePart) bindingPart).getType();
		}
	}

	private String notationPartToName(NotationPart notationPart) {
		if (notationPart instanceof TokenPart) {
			return ((TokenPart) notationPart).getToken();
		} else if (notationPart instanceof PropertyReferencePart) {
			return ((PropertyReferencePart) notationPart).getProperty().getName();
		} else { // je to LocalVariablePart
			return ((LocalVariablePart) notationPart).getName();
		}
	}

	private String getFullName(String name) {
		if (language.getName() == null) {
			return name;
		}
		return language.getName() + "." + name;
	}
}
