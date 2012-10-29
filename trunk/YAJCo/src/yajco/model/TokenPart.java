package yajco.model;

import yajco.annotation.Exclude;

public class TokenPart extends YajcoModelElement implements NotationPart {

    private final String token;

    public TokenPart(String stringValue) {
        super(null);
        this.token = stringValue;
    }

    @Exclude
    public TokenPart(String token, Object sourceElement) {
        super(sourceElement);
        this.token = token;
    }

    public String getToken() {
        return token;
    }

    @Override
    public String toString() {
        return "Token: "+token;
    }
    
    
}
