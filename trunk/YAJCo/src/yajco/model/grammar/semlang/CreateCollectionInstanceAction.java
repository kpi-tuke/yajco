package yajco.model.grammar.semlang;

import yajco.model.type.ComponentType;
import yajco.model.type.Type;

public class CreateCollectionInstanceAction extends CreateInstanceAction {

	private final ComponentType componentType;

	public CreateCollectionInstanceAction(ComponentType componentType) {
		this.componentType = componentType;
	}

	public Type getInnerType() {
		return componentType.getComponentType();
	}

	public ComponentType getComponentType() {
		return componentType;
	}

	@Override
	public ActionType getActionType() {
		return ActionType.CREATE_COLLECTION_INST;
	}
}
