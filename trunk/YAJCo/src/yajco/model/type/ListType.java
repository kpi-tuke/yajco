package yajco.model.type;

import tuke.pargen.annotation.Before;

public class ListType extends ComponentType {

	@Before({"list", "of"})
	public ListType(Type componentType) {
		super(componentType);
	}
}
