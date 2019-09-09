package yajco.xtext.commons.model;

public enum RuleType{
    MAIN(0),
    DECLARATOR(1),
    OPERATOR(2),
    RETURNER_AGGREGATOR(3),
    RETURNER(4),
    PARENTHESES(5),
    PROCESSED(6);

    private int value;

    RuleType(int value) {
        this.value = value;
    }
    public int getValue() {
        return value;
    }
}
