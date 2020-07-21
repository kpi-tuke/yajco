package yajco.model.pattern.impl;

import yajco.annotation.After;
import yajco.annotation.Before;
import yajco.annotation.Exclude;
import yajco.annotation.StringToken;
import yajco.model.pattern.PropertyPattern;

public class QuotedString extends PropertyPattern {
    private String delimiter = "\"";

    @Before({"Quoted string", "("})
    @After(")")
    public QuotedString(@StringToken String stringValue) {
        super(null);
        this.delimiter = stringValue;
    }

    @Exclude
    public QuotedString(String delimiter, Object sourceElement) {
        super(sourceElement);
        this.delimiter = delimiter;
    }

    @Exclude
    public QuotedString() {
        super(null);
    }

    public String getDelimiter() {
        return delimiter;
    }
}
