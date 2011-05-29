package yajco.grammar.semlang;

import yajco.model.type.PrimitiveType;

public class ConvertStringToPrimitiveTypeAction extends ConvertAction {

	private final PrimitiveType type;

	public ConvertStringToPrimitiveTypeAction(PrimitiveType type, RValue rValue) {
		super(rValue);
		this.type = type;
	}

	public PrimitiveType getType() {
		return type;
	}

	@Override
	public ActionType getActionType() {
		return ActionType.CONVERT_STRING_TO_PRIMITIVE;
	}
}
