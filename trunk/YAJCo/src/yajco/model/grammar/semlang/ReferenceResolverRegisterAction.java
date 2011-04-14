package yajco.model.grammar.semlang;

import java.util.List;

public class ReferenceResolverRegisterAction extends CreateClassInstanceAction {

	public ReferenceResolverRegisterAction(String classType, List<RValue> parameters) {
		super(classType, parameters);
	}

	public ReferenceResolverRegisterAction(String classType, String factoryMethodName, List<RValue> parameters) {
		super(classType, factoryMethodName, parameters);
	}

	@Override
	public ActionType getActionType() {
		return ActionType.REF_RESOLVER_REGISTER;
	}
}
