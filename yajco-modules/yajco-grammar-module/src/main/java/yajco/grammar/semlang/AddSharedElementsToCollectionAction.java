package yajco.grammar.semlang;

import yajco.grammar.Symbol;

import java.util.List;

public class AddSharedElementsToCollectionAction extends Action {
    private final LValue collection;
    private final String classType;
    private final String factoryMethodName;
    private final Symbol repeatedValues;
    private final String repeatedParameterName;
    private final List<Symbol> constructorParameters;

    public AddSharedElementsToCollectionAction(
            LValue collection,
            String classType,
            String factoryMethodName,
            Symbol repeatedValues,
            String repeatedParameterName,
            List<Symbol> constructorParameters) {
        this.collection = collection;
        this.classType = classType;
        this.factoryMethodName = factoryMethodName;
        this.repeatedValues = repeatedValues;
        this.repeatedParameterName = repeatedParameterName;
        this.constructorParameters = constructorParameters;
    }

    public LValue getCollection() {
        return collection;
    }

    public String getClassType() {
        return classType;
    }

    public String getFactoryMethodName() {
        return factoryMethodName;
    }

    public Symbol getRepeatedValues() {
        return repeatedValues;
    }

    public String getRepeatedParameterName() {
        return repeatedParameterName;
    }

    public List<Symbol> getConstructorParameters() {
        return constructorParameters;
    }

    @Override
    public ActionType getActionType() {
        return ActionType.ADD_SHARED_ELEMENTS_TO_COLLECTION;
    }
}
