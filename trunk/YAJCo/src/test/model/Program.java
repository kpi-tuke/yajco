package test.model;

import tuke.pargen.annotation.*;
import tuke.pargen.annotation.reference.*;

import yajco.annotation.printer.*;

import java.util.List;


public class Program {
    private Expression expression;
    private List<Constant> constants;

    public Program(@Before("print")
    Expression expression) {
        this.expression = expression;
    }

    public Program(@Before("print")
    @NewLine
    @Indent
    Expression expression,
        @Before("where")
    @Separator(",")
    @Range(minOccurs = 1)
    @NewLine
    List<Constant> constants) {
        this.expression = expression;
        this.constants = constants;
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    public List<Constant> getConstants() {
        return constants;
    }

    public void setConstants(List<Constant> constants) {
        this.constants = constants;
    }
}
