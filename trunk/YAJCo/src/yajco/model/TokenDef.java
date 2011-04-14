package yajco.model;

import yajco.annotation.Before;
import yajco.annotation.Exclude;
import yajco.annotation.Token;

public class TokenDef extends YajcoModelElement {

    private final String name;
    private final String regexp;

    public TokenDef(String name, @Before("=") @Token("STRING_VALUE") String regexp) {
        super(null);
        this.name = name;
        this.regexp = regexp;
    }

    @Exclude
    public TokenDef(String name, String regexp, Object sourceElement) {
        super(sourceElement);
        this.name = name;
        this.regexp = regexp;
    }

    public String getName() {
        return name;
    }

    public String getRegexp() {
        return regexp;
    }
}
