package yajco.model.type;

import tuke.pargen.annotation.Before;

public class SetType extends ComponentType {

	@Before({"set", "of"})
	public SetType(Type componentType) {
		super(componentType);
	}
}
