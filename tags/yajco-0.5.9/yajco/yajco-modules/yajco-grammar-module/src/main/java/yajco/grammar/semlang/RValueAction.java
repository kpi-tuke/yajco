package yajco.grammar.semlang;

public abstract class RValueAction extends Action {

	private final RValue rValue;

	public RValueAction(RValue rValue) {
		Utilities.checkForNullPointer(rValue);

		this.rValue = rValue;
	}

	public RValue getRValue() {
		return rValue;
	}

	public abstract ActionType getActionType();
}
