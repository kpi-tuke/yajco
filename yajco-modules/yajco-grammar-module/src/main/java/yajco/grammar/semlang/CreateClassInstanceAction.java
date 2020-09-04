package yajco.grammar.semlang;

import java.util.List;

public class CreateClassInstanceAction extends CreateInstanceAction {

    private final String classType;
    private final String factoryMethodName;
    private final List<RValue> parameters;

    public CreateClassInstanceAction(String classType, List<RValue> parameters) {
        Utilities.checkForNullOrEmptyString(classType);

        this.classType = classType;
        this.factoryMethodName = null;
        this.parameters = parameters;
    }

    public CreateClassInstanceAction(String classType, String factoryMethodName, List<RValue> parameters) {
        Utilities.checkForNullOrEmptyString(classType);
        Utilities.checkForNullOrEmptyString(factoryMethodName);

        this.classType = classType;
        this.factoryMethodName = factoryMethodName;
        this.parameters = parameters;
    }

    public String getClassType() {
        return classType;
    }

    public String getFactoryMethodName() {
        return factoryMethodName;
    }

    public List<RValue> getParameters() {
        return parameters;
    }

    @Override
    public ActionType getActionType() {
        return ActionType.CREATE_CLASS_INST;
    }
}
