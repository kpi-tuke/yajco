package yajco.grammar.semlang;

public class UnquoteStringAction extends ConvertAction {

    private final String delimiter;

    public UnquoteStringAction(String delimiter, RValue rValue) {
        super(rValue);
        this.delimiter = delimiter;
    }

    public String getDelimiter() {
        return delimiter;
    }

    @Override
    public ActionType getActionType() {
        return ActionType.UNQUOTE_STRING;
    }
}
