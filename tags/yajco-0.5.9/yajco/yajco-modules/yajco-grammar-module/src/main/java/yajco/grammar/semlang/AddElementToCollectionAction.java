package yajco.grammar.semlang;

public class AddElementToCollectionAction extends ValueAction {

	public AddElementToCollectionAction(LValue lValue, RValue rValue) {
		super(lValue, rValue);
	}

	@Override
	public ActionType getActionType() {
		return ActionType.ADD_ELEMENT_TO_COLLECTION;
	}
}
