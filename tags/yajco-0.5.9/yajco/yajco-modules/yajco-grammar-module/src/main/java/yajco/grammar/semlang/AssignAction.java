package yajco.grammar.semlang;

public class AssignAction extends ValueAction {

	public AssignAction(LValue lValue, RValue rValue) {
		super(lValue, rValue);
	}

	@Override
	public ActionType getActionType() {
		return ActionType.ASSIGN;
	}
}
