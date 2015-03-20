package yajco.model;

import yajco.annotation.Before;
import yajco.annotation.Exclude;
import yajco.annotation.Token;

public class TokenDef extends YajcoModelElement {

    private String name;
    private String regexp;

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
    
    //needed for XML binding
    @Exclude
    private TokenDef() {
        super(null);
    }

    public String getName() {
        return name;
    }

    public String getRegexp() {
        return regexp;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof TokenDef)) {
            return false;
        }
        TokenDef that = (TokenDef) obj;
        if (this.name == null && that.name == null) {
            return true;
        }
        return this.name.equalsIgnoreCase(that.name);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 67 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }
    
}
