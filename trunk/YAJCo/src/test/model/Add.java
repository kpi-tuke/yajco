package test.model;

import tuke.pargen.annotation.*;
import tuke.pargen.annotation.reference.*;

import yajco.annotation.printer.*;


public class Add extends Expression {
    private Expression expression1;
    private Expression expression2;

    @Operator(priority = 1)
    public Add(Expression expression1, @Before("+")
    Expression expression2) {
        this.expression1 = expression1;
        this.expression2 = expression2;
    }

    public Expression getExpression1() {
        return expression1;
    }

    public void setExpression1(Expression expression1) {
        this.expression1 = expression1;
    }

    public Expression getExpression2() {
        return expression2;
    }

    public void setExpression2(Expression expression2) {
        this.expression2 = expression2;
    }
}
