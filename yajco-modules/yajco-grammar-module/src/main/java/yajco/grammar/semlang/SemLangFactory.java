package yajco.grammar.semlang;

import yajco.grammar.NonterminalSymbol;
import yajco.grammar.Symbol;
import yajco.grammar.TerminalSymbol;
import yajco.model.type.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SemLangFactory {

	public static List<Action> createReturnSymbolValueActions(Symbol symbol) {
		return createReturnValueActions(new RValue(symbol));
	}

	public static List<Action> createNewClassInstanceActions(String classType, List<Symbol> symbols) {
		return createClassInstanceActions(classType, null, symbolsToRValues(symbols));
	}

	public static List<Action> createNewClassInstanceAndReturnActions(String classType, List<Symbol> symbols) {
		return createClassInstanceAndReturnActions(classType, null, symbolsToRValues(symbols));
	}

	public static List<Action> createNewOptionalClassInstanceAndReturnActions(List<Symbol> symbols) {
		return createOptionalClassInstanceAndReturnActions(symbolsToRValues(symbols));
	}

	public static List<Action> createNewUnorderedParamClassInstanceAndReturnActions(List<Symbol> symbols) {
		return createUnorderedParamClassInstanceAndReturnActions(symbolsToRValues(symbols));
	}

	public static List<Action> createFactoryClassInstanceActions(String classType, String factoryMethodName, List<Symbol> symbols) {
		return createClassInstanceActions(classType, factoryMethodName, symbolsToRValues(symbols));
	}

	public static List<Action> createFactoryClassInstanceAndReturnActions(String classType, String factoryMethodName, List<Symbol> symbols) {
		return createClassInstanceAndReturnActions(classType, factoryMethodName, symbolsToRValues(symbols));
	}

	public static List<Action> createRefResolverNewClassInstRegisterActions(String classType, List<Symbol> symbols) {
		return createReferenceResolverRegisterActions(classType, null, symbolsToRValues(symbols));
	}

	public static List<Action> createRefResolverNewClassInstRegisterAndReturnActions(String classType, List<Symbol> symbols) {
		return createReferenceResolverRegisterAndReturnActions(classType, null, symbolsToRValues(symbols));
	}

	public static List<Action> createRefResolverFactoryClassInstRegisterActions(String classType, String factoryMethodName, List<Symbol> symbols) {
		return createReferenceResolverRegisterActions(classType, factoryMethodName, symbolsToRValues(symbols));
	}

	public static List<Action> createRefResolverFactoryClassInstRegisterAndReturnActions(String classType, String factoryMethodName, List<Symbol> symbols) {
		return createReferenceResolverRegisterAndReturnActions(classType, factoryMethodName, symbolsToRValues(symbols));
	}

	private static List<Action> createEnumInstanceActions(String enumType, String enumConstant) {
		List<Action> actions = new ArrayList<Action>(1);
		actions.add(new CreateEnumInstanceAction(enumType, enumConstant));

		return actions;
	}

	public static List<Action> createEnumInstanceAndReturnActions(String enumType, String enumConstant) {
		List<Action> actions = new ArrayList<Action>(1);
		actions.add(new ReturnAction(new RValue(createEnumInstanceActions(enumType, enumConstant).get(0))));

		return actions;
	}

	public static List<Action> createAddElementToCollectionActions(Symbol collection, Symbol element) {
		return createAddElementsToCollectionActions(new LValue(collection), Collections.singletonList(new RValue(element)));
	}

	public static List<Action> createAddElementToCollectionAndReturnActions(Symbol collection, Symbol element) {
		return createAddElementsToCollectionAndReturnActions(new LValue(collection), simpleSymbolsToRValues(Collections.singletonList(element)));
	}

	public static List<Action> createAddElementsToCollectionActions(Symbol collection, List<Symbol> elements) {
		return createAddElementsToCollectionActions(new LValue(collection), simpleSymbolsToRValues(elements));
	}

	public static List<Action> createAddElementsToCollectionAndReturnActions(Symbol collection, List<Symbol> elements) {
		return createAddElementsToCollectionAndReturnActions(new LValue(collection), simpleSymbolsToRValues(elements));
	}

	public static List<Action> createListAndReturnActions(Type innerType) {
		return createCollectionAndReturnActions(new ListType(innerType));
	}

	public static List<Action> createListAndAddElementActions(Type varType, String varName, Symbol symbol) {
		return createCollectionAndAddElementsActions(varName, new ListType(varType), Collections.singletonList(new RValue(symbol)));
	}

	public static List<Action> createListAndAddElementAndReturnActions(Type varType, String varName, Symbol symbol) {
		return createCollectionAndAddElementsAndReturnActions(varName, new ListType(varType), Collections.singletonList(new RValue(symbol)));
	}

	public static List<Action> createListAndAddElementsActions(Type varType, String varName, List<Symbol> symbols) {
		return createCollectionAndAddElementsActions(varName, new ListType(varType), simpleSymbolsToRValues(symbols));
	}

	public static List<Action> createListAndAddElementsAndReturnActions(Type varType, String varName, List<Symbol> symbol) {
		return createCollectionAndAddElementsAndReturnActions(varName, new ListType(varType), simpleSymbolsToRValues(symbol));
	}

	public static List<Action> createHashMapAndPutElementsAndReturnActions(Type varType, String varName, List<Symbol> symbol) {
		return createCollectionAndAddElementsAndReturnActions(varName, new HashMapType(varType), simpleSymbolsToRValues(symbol));
	}

	private static List<Action> createReturnValueActions(RValue value) {
		List<Action> actions = new ArrayList<Action>(1);
		actions.add(new ReturnAction(value));

		return actions;
	}

	private static List<Action> createClassInstanceActions(String classType, String factoryMethodName, List<RValue> parameters) {
		List<Action> actions = new ArrayList<Action>(1);
		if (factoryMethodName == null || factoryMethodName.equals("")) {
			actions.add(new CreateClassInstanceAction(classType, parameters));
		} else {
			actions.add(new CreateClassInstanceAction(classType, factoryMethodName, parameters));
		}
		return actions;
	}

	private static List<Action> createOptionalClassInstanceActions(RValue parameter) {
		List<Action> actions = new ArrayList<Action>(1);
		actions.add(new CreateOptionalClassInstanceAction(parameter));
		return actions;
	}

	private static List<Action> createUnorderedParamClassInstanceActions(List<RValue> parameters) {
		List<Action> actions = new ArrayList<Action>(1);
		actions.add(new CreateUnorderedParamClassInstanceAction(parameters));
		return actions;
	}

	private static List<Action> createClassInstanceAndReturnActions(String classType, String factoryMethodName, List<RValue> parameters) {
		List<Action> actions = new ArrayList<Action>(1);
		actions.add(new ReturnAction(new RValue(createClassInstanceActions(classType, factoryMethodName, parameters).get(0))));
		return actions;
	}

	private static List<Action> createOptionalClassInstanceAndReturnActions(List<RValue> parameters) {
		List<Action> actions = new ArrayList<Action>(1);
		if (parameters.size() > 0) {
			actions.add(new ReturnAction(new RValue(createOptionalClassInstanceActions(parameters.get(0)).get(0))));
		} else {
			actions.add(new ReturnAction(new RValue(createOptionalClassInstanceActions(null).get(0))));
		}

		return actions;
	}

	private static List<Action> createUnorderedParamClassInstanceAndReturnActions(List<RValue> parameters) {
		List<Action> actions = new ArrayList<Action>(1);
		actions.add(new ReturnAction(new RValue(createUnorderedParamClassInstanceActions(parameters).get(0))));
		return actions;
	}

	private static List<Action> createReferenceResolverRegisterActions(String classType, String factoryMethodName, List<RValue> parameters) {
		List<Action> actions = new ArrayList<Action>(1);
		if (factoryMethodName == null || factoryMethodName.equals("")) {
			actions.add(new ReferenceResolverRegisterAction(classType, parameters));
		} else {
			actions.add(new ReferenceResolverRegisterAction(classType, factoryMethodName, parameters));
		}
		return actions;
	}

	private static List<Action> createReferenceResolverRegisterAndReturnActions(String classType, String factoryMethodName, List<RValue> parameters) {
		List<Action> actions = new ArrayList<Action>(1);
		actions.add(new ReturnAction(new RValue(createReferenceResolverRegisterActions(classType, factoryMethodName, parameters).get(0))));
		return actions;
	}

	private static List<Action> createAddElementsToCollectionActions(LValue lValue, List<RValue> rValues) {
		List<Action> actions = new ArrayList<Action>(rValues.size());
		for (RValue rValue : rValues) {
			actions.add(new AddElementToCollectionAction(lValue, rValue));
		}

		return actions;
	}

	private static List<Action> createPutElementsToCollectionActions(ComponentType collectionType, LValue lValue, List<RValue> rValues) {
		List<Action> actions = new ArrayList<Action>(rValues.size());
		for (RValue rValue : rValues) {
			actions.add(new AddElementToCollectionAction(collectionType, lValue, rValue));
		}

		return actions;
	}

	private static List<Action> createAddElementsToCollectionAndReturnActions(LValue lValue, List<RValue> rValues) {
		List<Action> actions = new ArrayList<Action>(1 + rValues.size());
		actions.addAll(createAddElementsToCollectionActions(lValue, rValues));
		if (lValue.getSymbol() != null) {
			actions.add(new ReturnAction(new RValue(lValue.getSymbol())));
		} else {
			actions.add(new ReturnAction(new RValue(lValue.getVarName())));
		}

		return actions;
	}

	private static List<Action> createCollectionAndReturnActions(ComponentType collectionType) {
		List<Action> actions = new ArrayList<Action>(1);
		actions.add(new ReturnAction(new RValue(new CreateCollectionInstanceAction(collectionType))));

		return actions;
	}

	private static List<Action> createCollectionAndAddElementsActions(String varName, ComponentType collectionType, List<RValue> rValues) {
		List<Action> actions = new ArrayList<Action>(2 + rValues.size());
		actions.add(new DefineVariableAction(collectionType, varName));
		actions.add(new AssignAction(new LValue(varName), new RValue(new CreateCollectionInstanceAction(collectionType))));
		if (collectionType instanceof HashMapType) {
			actions.addAll(createPutElementsToCollectionActions(collectionType, new LValue(varName), rValues));
		} else {
			actions.addAll(createAddElementsToCollectionActions(new LValue(varName), rValues));
		}

		return actions;
	}

	private static List<Action> createCollectionAndAddElementsAndReturnActions(String varName, ComponentType collectionType, List<RValue> rValues) {
		List<Action> actions = new ArrayList<Action>(3 + rValues.size());
		actions.addAll(createCollectionAndAddElementsActions(varName, collectionType, rValues));
		actions.add(new ReturnAction(new RValue(varName)));

		return actions;
	}

	private static List<RValue> symbolsToRValues(List<Symbol> symbols) {
		List<RValue> rValues = new ArrayList<RValue>(symbols.size());
		for (Symbol symbol : symbols) {
			if (symbol instanceof TerminalSymbol) {
				TerminalSymbol terminal = (TerminalSymbol) symbol;
				PrimitiveType primType = (PrimitiveType) terminal.getReturnType();
				if (primType.getPrimitiveTypeConst() == PrimitiveTypeConst.STRING) {
					rValues.add(new RValue(symbol));
				} else {
					rValues.add(new RValue(new ConvertStringToPrimitiveTypeAction(primType, new RValue(symbol))));
				}
			} else {
				NonterminalSymbol nonterminal = (NonterminalSymbol) symbol;
				Type type = nonterminal.getReturnType();
				if (type instanceof OptionalType) {
					rValues.add(new RValue(symbol));
				} else if (type instanceof UnorderedParamType) {
					rValues.add(new RValue(new ConvertUnorderedParamsToObjectAction(((ComponentType) type), new RValue(symbol))));
				} else if (type instanceof ComponentType /*&& !(type instanceof ListType)*/) {
					rValues.add(new RValue(new ConvertListToCollectionAction(((ComponentType) type), new RValue(symbol))));
				} else {
					rValues.add(new RValue(symbol));
				}
			}
		}
		return rValues;
	}

	private static List<RValue> simpleSymbolsToRValues(List<Symbol> symbols) {
		List<RValue> rValues = new ArrayList<RValue>(symbols.size());
		for (Symbol symbol : symbols) {
			rValues.add(new RValue(symbol));
		}

		return rValues;
	}
}
