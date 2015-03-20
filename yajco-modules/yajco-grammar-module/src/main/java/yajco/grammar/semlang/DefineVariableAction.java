package yajco.grammar.semlang;

import yajco.model.type.Type;

public class DefineVariableAction extends Action {

	private final Type varType;
	private final String varName;

	public DefineVariableAction(Type varType, String varName) {
		Utilities.checkForNullPointer(varType);
		Utilities.checkForNullOrEmptyString(varName);

		this.varType = varType;
		this.varName = varName;
	}

	public Type getVarType() {
		return varType;
	}

	public String getVarName() {
		return varName;
	}

	@Override
	public ActionType getActionType() {
		return ActionType.DEFINE_VAR;
	}
}
