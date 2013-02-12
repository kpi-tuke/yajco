package yajco.grammar.semlang;

import yajco.model.type.ComponentType;
import yajco.model.type.Type;

public class ConvertCollectionToArrayAction extends ConvertAction {

	private final ComponentType collectionType;

	public ConvertCollectionToArrayAction(ComponentType collectionType, RValue rValue) {
		super(rValue);
		this.collectionType = collectionType;
	}

	public Type getInnerType() {
		return collectionType.getComponentType();
	}

	public ComponentType getCollectionType() {
		return collectionType;
	}

	@Override
	public ActionType getActionType() {
		return ActionType.CONVERT_COLLECTION_TO_ARRAY;
	}
}
