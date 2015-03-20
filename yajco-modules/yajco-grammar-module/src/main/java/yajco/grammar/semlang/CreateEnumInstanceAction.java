package yajco.grammar.semlang;

public class CreateEnumInstanceAction extends CreateInstanceAction {

	private final String enumType;
	private final String enumConstant;

	public CreateEnumInstanceAction(String enumType, String enumConstant) {
		Utilities.checkForNullOrEmptyString(enumType);
		Utilities.checkForNullOrEmptyString(enumConstant);

		this.enumType = enumType;
		this.enumConstant = enumConstant;
	}

	public String getEnumType() {
		return enumType;
	}

	public String getEnumConstant() {
		return enumConstant;
	}

	@Override
	public ActionType getActionType() {
		return ActionType.CREATE_ENUM_INST;
	}
}
