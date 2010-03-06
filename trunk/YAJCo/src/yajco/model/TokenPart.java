package yajco.model;

public class TokenPart implements NotationPart {
    private final String token;

    public TokenPart(String stringValue) {
        this.token = stringValue;
    }

    public String getToken() {
        return token;
    }
}
