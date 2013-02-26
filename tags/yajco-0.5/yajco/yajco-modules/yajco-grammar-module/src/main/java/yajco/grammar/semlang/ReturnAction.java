package yajco.grammar.semlang;

public class ReturnAction extends RValueAction {

	public ReturnAction(RValue rValue) {
		super(rValue);
	}

	@Override
	public ActionType getActionType() {
		return ActionType.RETURN;
	}
}
