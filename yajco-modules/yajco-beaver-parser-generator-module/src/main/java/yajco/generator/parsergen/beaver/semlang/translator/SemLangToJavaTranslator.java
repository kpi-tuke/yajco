package yajco.generator.parsergen.beaver.semlang.translator;

import yajco.ReferenceResolver;
import yajco.grammar.semlang.*;
import yajco.grammar.type.HashMapType;
import yajco.grammar.type.ObjectType;
import yajco.grammar.type.UnorderedParamType;
import yajco.model.Language;
import yajco.model.type.*;

import java.io.PrintStream;
import java.util.List;

public class SemLangToJavaTranslator {

	private final static String REFERENCE_RESOLVER_CLASS_NAME = ReferenceResolver.class.getCanonicalName();
	private final static SemLangToJavaTranslator instance = new SemLangToJavaTranslator();
	private Language language;

	private SemLangToJavaTranslator() {
	}

	public void translateActions(List<Action> actions, Language language, PrintStream writer) {
		if (actions == null || actions.isEmpty()) {
			return;
		}
		if (writer == null) {
			throw new NullPointerException("Parameter 'writer' cann't be null!");
		}

		this.language = language;
//		String parserPackageName = this.language.getName() != null ? this.language.getName() + "." + BeaverParserGenerator.DEFAULT_PACKAGE_NAME : BeaverParserGenerator.DEFAULT_PACKAGE_NAME;
		for (Action action : actions) {
			translateAction(action, writer);
		}

	}

	private void translateAction(Action action, PrintStream writer) {
		if (action == null) {
			return;
		}
		if (writer == null) {
			throw new NullPointerException("Parameter 'writer' cann't be null!");
		}

		switch (action.getActionType()) {
			case ASSIGN:
				translateAssignAction((AssignAction) action, writer);
				break;
			case DEFINE_VAR:
				translateDefineVariableAction((DefineVariableAction) action, writer);
				break;
			case RETURN:
				translateReturnAction((ReturnAction) action, writer);
				break;
			case CONVERT_STRING_TO_PRIMITIVE:
				translateConvertStringToPrimitiveTypeAction((ConvertStringToPrimitiveTypeAction) action, writer);
				break;
			case CONVERT_COLLECTION_TO_ARRAY:
				translateConvertCollectionToArrayAction((ConvertCollectionToArrayAction) action, writer);
				break;
			case CONVERT_LIST_TO_COLLECTION:
				translateConvertListToCollectionAction((ConvertListToCollectionAction) action, writer);
				break;
			case CREATE_COLLECTION_INST:
				translateCreateCollectionInstanceAction((CreateCollectionInstanceAction) action, writer);
				break;
			case ADD_ELEMENT_TO_COLLECTION:
				translateAddElementToCollectionAction((AddElementToCollectionAction) action, writer);
				break;
			case CREATE_CLASS_INST:
				translateCreateClassInstanceAction((CreateClassInstanceAction) action, writer);
				break;
			case CREATE_ENUM_INST:
				translateCreateEnumInstanceAction((CreateEnumInstanceAction) action, writer);
				break;
			case REF_RESOLVER_REGISTER:
				translateReferenceResolverRegisterAction((ReferenceResolverRegisterAction) action, writer);
				break;
			case CREATE_OPTIONAL_CLASS_INST:
				translateCreateOptionalClassInstanceAction((CreateOptionalClassInstanceAction) action, writer);
				break;
			case CONVERT_LIST_WITH_SHARED_TO_COLLECTION:
				translateConvertListWithSharedToCollectionAction((ConvertListWithSharedToCollectionAction) action, writer);
				break;
			case UNQUOTE_STRING:
				translateUnquoteStringAction((UnquoteStringAction) action, writer);
				break;
			case CREATE_UNORDERED_PARAM_CLASS_INST:
				translateCreateUnorderedParamClassInstanceAction((CreateUnorderedParamClassInstanceAction) action, writer);
				break;
			case CONVERT_UNORDERED_PARAMS_TO_OBJECT:
				translateConvertUnorderedParamsToObjectAction((ConvertUnorderedParamsToObjectAction) action, writer);
				break;
			default:
				throw new IllegalArgumentException("Unknown SemLang action detected: '" + action.getClass().getCanonicalName() + "'!");
		}
	}

	private void translateAssignAction(AssignAction action, PrintStream writer) {
		translateLValue(action.getLValue(), writer);
		writer.print(" = ");
		translateRValue(action.getRValue(), writer);
		writer.print("; ");
	}

	private void translateDefineVariableAction(DefineVariableAction action, PrintStream writer) {
		writer.print(typeToString(action.getVarType()));
		writer.print(" ");
		writer.print(action.getVarName());
		writer.print(" = null; ");
	}

	private void translateReturnAction(ReturnAction action, PrintStream writer) {
		//DOMINIK TEST
		//writer.print("return (Symbol)");
		//writer.print("return (Symbol) new "+ parserPackageName +".SymbolWrapper(");
		writer.print("return (Symbol) new SymbolWrapper(");
		translateRValue(action.getRValue(), writer);
		//writer.print("; ");
		writer.print("); "); // mnou dodane
	}

	private void translateConvertStringToPrimitiveTypeAction(ConvertStringToPrimitiveTypeAction action, PrintStream writer) {
		if (action.getType().getPrimitiveTypeConst() == PrimitiveTypeConst.STRING) {
			return;
		}

		writer.print(primitiveTypeToString(action.getType()));
		writer.print(".valueOf(");
		translateRValue(action.getRValue(), writer);
		writer.print(")");
	}

	private void translateConvertCollectionToArrayAction(ConvertCollectionToArrayAction action, PrintStream writer) {
		if (action.getCollectionType() instanceof ArrayType) {
			return;
		}

		translateRValue(action.getRValue(), writer);
		writer.print(".toArray(new ");
		writer.print(typeToString(action.getInnerType()));
		writer.print("[]{})");
	}

	private void translateConvertListToCollectionAction(ConvertListToCollectionAction action, PrintStream writer) {
		if (action.getResultCollectionType() instanceof ArrayType) {
			translateRValue(action.getRValue(), writer);
			writer.print(".toArray(new ");
			writer.print(typeToString(action.getResultCollectionInnerType()));
			writer.print("[]{})");
		} else if (action.getResultCollectionType() instanceof ListType) {
			writer.print("new java.util.ArrayList<");
			writer.print(typeToString(action.getResultCollectionInnerType()));
			writer.print(">(");
			translateRValue(action.getRValue(), writer);
			writer.print(")");
		} else if (action.getResultCollectionType() instanceof SetType) {
			writer.print("new java.util.HashSet<");
			writer.print(typeToString(action.getResultCollectionInnerType()));
			writer.print(">(");
			translateRValue(action.getRValue(), writer);
			writer.print(")");
		} else if (action.getResultCollectionType() instanceof OrderedSetType) {
			writer.print("new java.util.LinkedHashSet<");
			writer.print(typeToString(action.getResultCollectionInnerType()));
			writer.print(">(");
			translateRValue(action.getRValue(), writer);
			writer.print(")");
		} else if (action.getResultCollectionType() instanceof HashMapType) {
			writer.print("new java.util.HashMap<String, ");
			writer.print(typeToString(action.getResultCollectionInnerType()));
			writer.print(">(");
			translateRValue(action.getRValue(), writer);
			writer.print(")");
		} else if (action.getResultCollectionType() instanceof OptionalType) {
			writer.print("java.util.Optional.empty()");
		} else {
			throw new IllegalArgumentException("Unknown component type detected: '" + action.getResultCollectionType().getClass().getCanonicalName() + "'!");
		}
	}

	private void translateConvertListWithSharedToCollectionAction(ConvertListWithSharedToCollectionAction action, PrintStream writer) {
		if (action.getResultCollectionType() instanceof ArrayType) {
			translateSharedSymbol(action, writer);
			writer.print(".toArray(new ");
			writer.print(typeToString(action.getResultCollectionInnerType()));
			writer.print("[]{})");
		} else if (action.getResultCollectionType() instanceof ListType || action.getResultCollectionType() instanceof ListTypeWithShared) {
			writer.print("new java.util.ArrayList<");
			writer.print(typeToString(action.getResultCollectionInnerType()));
			writer.print(">(");
			translateSharedSymbol(action, writer);
			writer.print(")");
		} else if (action.getResultCollectionType() instanceof SetType) {
			writer.print("new java.util.HashSet<");
			writer.print(typeToString(action.getResultCollectionInnerType()));
			writer.print(">(");
			translateSharedSymbol(action, writer);
			writer.print(")");
		} else if (action.getResultCollectionType() instanceof OptionalType) {
			writer.print("java.util.Optional.empty()");
		} else {
			throw new IllegalArgumentException("Unknown component type detected: '" + action.getResultCollectionType().getClass().getCanonicalName() + "'!");
		}
	}

	private void translateSharedSymbol(ConvertListWithSharedToCollectionAction action, PrintStream writer) {
		translateLValue(action.getRValue(), writer);
		writer.print(".getWrappedObject()");
		writer.print(".getUpdatedList(\""  + action.getSharedPartName() +  "\")");
	}

	private void translateConvertUnorderedParamsToObjectAction(ConvertUnorderedParamsToObjectAction action, PrintStream writer) {
		RValue rValue = action.getRValue();
		String varName = rValue.getSymbol().getVarName();
        writer.print("(" + typeToString(action.getResultInnerType()) + ") ");
        writer.print("params.getWrappedObject()");
        writer.print(".get(\"" + varName + "\")");
	}

	private void translateCreateCollectionInstanceAction(CreateCollectionInstanceAction action, PrintStream writer) {
		if (action.getComponentType() instanceof ArrayType) {
			writer.print("new ");
			writer.print(typeToString(action.getInnerType()));
			writer.print("[0]");
		} else if (action.getComponentType() instanceof ListType) {
			//writer.print("new java.util.ArrayList<");
			writer.print("new SymbolListImpl<");
			writer.print(typeToString(action.getInnerType()));
			writer.print(">()");
		} else if (action.getComponentType() instanceof ListTypeWithShared) {
			writer.print("new SymbolListImplWithShared<");
			writer.print(typeToString(action.getInnerType()));
			writer.print(">()");
		} else if (action.getComponentType() instanceof SetType) {
			writer.print("new java.util.HashSet<");
			writer.print(typeToString(action.getInnerType()));
			writer.print(">()");
		} else if (action.getComponentType() instanceof OrderedSetType) {
			writer.print("new SymbolLinkedHashSetImpl<");
			writer.print(typeToString(action.getInnerType()));
			writer.print(">()");
		} else if (action.getComponentType() instanceof OptionalType) {
			writer.print("java.util.Optional.empty()");
		} else if (action.getComponentType() instanceof HashMapType) {
			writer.print("new SymbolHashMapImpl<String, ");
			writer.print(typeToString(action.getInnerType()));
			writer.print(">()");
		} else {
			throw new IllegalArgumentException("Unknown component type detected: '" + action.getComponentType().getClass().getCanonicalName() + "'!");
		}
	}

	private void translateAddElementToCollectionAction(AddElementToCollectionAction action, PrintStream writer) {
		translateLValue(action.getLValue(), writer);
		//TODO Neviem ci je to dobre takto
		//DOMINIK UPRAVA
		if (action.getLValue().getSymbol() != null) {
			writer.print(".getWrappedObject()");
		}

		if ((action.getLValue().getSymbol() != null && action.getLValue().getSymbol().getReturnType() instanceof ListTypeWithShared)
				|| (action.getRValue().getSymbol() != null && action.getRValue().getSymbol().getReturnType() instanceof ListType)) {
			writer.print(".addAll(");
			translateRValue(action.getRValue(), writer);
			writer.print("); ");
		} else if (action.getComponentType() != null && action.getComponentType() instanceof HashMapType) {
			writer.print(".put(");
			translateRValue(action.getRValue(), writer);
			writer.print(".getVarName(), ");
			translateRValue(action.getRValue(), writer);
			writer.print(".getValue()");
			writer.print("); ");
		} else {
			writer.print(".add(");
			translateRValue(action.getRValue(), writer);
			writer.print("); ");
		}
	}

	private void translateCreateClassInstanceAction(CreateClassInstanceAction action, PrintStream writer) {
		if (action.getFactoryMethodName() == null || action.getFactoryMethodName().equals("")) {
			writer.print("new ");
			writer.print(action.getClassType());
			writer.print("(");
		} else {
			writer.print(action.getClassType());
			writer.print(".");
			writer.print(action.getFactoryMethodName());
			writer.print("(");
		}

		for (int i = 0; i < action.getParameters().size(); i++) {
			translateRValue(action.getParameters().get(i), writer);
			if (i != (action.getParameters().size() - 1)) {
				writer.print(", ");
			}
		}
		writer.print(")");
	}

	private void translateCreateOptionalClassInstanceAction(CreateOptionalClassInstanceAction action, PrintStream writer) {
		if (action.getParameter() == null) {
			writer.print("java.util.Optional.empty()");
		} else {
			writer.print("java.util.Optional.of(");
			translateRValue(action.getParameter(), writer);
			writer.print(")");
		}
	}

	private void translateUnquoteStringAction(UnquoteStringAction action, PrintStream writer) {
		writer.print("QuotedStringUtils.unquote(" + action.getRValue().getSymbol().getVarName() + ")");
	}

	private void translateCreateUnorderedParamClassInstanceAction(CreateUnorderedParamClassInstanceAction action, PrintStream writer) {
		writer.print("new SymbolUnorderedParam(");
		translateRValue(action.getParameters().get(0), writer);
		writer.print(", \"" + action.getVarName() + "\")");
	}

	private void translateCreateEnumInstanceAction(CreateEnumInstanceAction action, PrintStream writer) {
		writer.print(action.getEnumType());
		writer.print(".");
		writer.print(action.getEnumConstant());
	}

	private void translateReferenceResolverRegisterAction(ReferenceResolverRegisterAction action, PrintStream writer) {
		writer.print(REFERENCE_RESOLVER_CLASS_NAME);
		writer.print(".getInstance().register(");
		translateCreateClassInstanceAction(action, writer);
		String factoryMethodName = action.getFactoryMethodName();
		if (factoryMethodName != null && !factoryMethodName.isEmpty()) {
			writer.print(", \""+factoryMethodName+"\"");
		}
		if (action.getParameters().size() > 0) {
			writer.print(", (Object)");
			for (int i = 0; i < action.getParameters().size(); i++) {
				translateRValue(action.getParameters().get(i), writer);
				if (i != (action.getParameters().size() - 1)) {
					writer.print(", ");
				}
			}
		}
		writer.print(")");
	}

	private void translateLValue(LValue lValue, PrintStream writer) {
		if (lValue.getSymbol() != null) {
			//DOMINIK TEST
			writer.print(lValue.getSymbol().getVarName()); //tento riadok tu len bol
			//writer.print(lValue.getSymbol().getVarName() + ".getWrappedObject()"); //toto je moje dopisane
		} else {
			writer.print(lValue.getVarName());
		}
	}

	private void translateRValue(RValue rValue, PrintStream writer) {
		if (rValue.getSymbol() != null || rValue.getVarName() != null) {
			translateLValue(rValue, writer);
			//DOMINIK TEST
			if (rValue.getSymbol() != null
					&& !(rValue.getSymbol().getReturnType() instanceof PrimitiveType)) {
				writer.print(".getWrappedObject()");
			}
			//POTIAL
		} else {
			translateAction(rValue.getAction(), writer);
		}
	}

	private String typeToString(Type type) {
		if (type instanceof PrimitiveType) {
			return primitiveTypeToString((PrimitiveType) type);
		} else if (type instanceof ComponentType) {
			return componentTypeToString((ComponentType) type);
		} else if (type instanceof ReferenceType) {
			return referenceTypeToString((ReferenceType) type);
		} else if (type instanceof ObjectType) {
			return "Object";
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
		} else if (componentType instanceof ListType || componentType instanceof ListTypeWithShared) {
			return "java.util.List<" + typeToString(componentType.getComponentType()) + ">";
		} else if (componentType instanceof SetType || componentType instanceof OrderedSetType) {
			return "java.util.Set<" + typeToString(componentType.getComponentType()) + ">";
		} else if (componentType instanceof OptionalType) {
			return "java.util.Optional<" + typeToString(componentType.getComponentType()) + ">";
		} else if (componentType instanceof HashMapType) {
			return "java.util.Map<String, " + typeToString(componentType.getComponentType()) + ">";
		} else if (componentType instanceof UnorderedParamType) {
			return "SymbolUnorderedParam";
		} else {
			throw new IllegalArgumentException("Unknown component type detected: '" + componentType.getClass().getCanonicalName() + "'!");
		}
	}

	private String referenceTypeToString(ReferenceType referenceType) {
		return yajco.model.utilities.Utilities.getFullConceptClassName(language, referenceType.getConcept());
	}

	public static SemLangToJavaTranslator getInstance() {
		return instance;
	}
}
