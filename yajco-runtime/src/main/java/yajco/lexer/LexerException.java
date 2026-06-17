package yajco.lexer;

public class LexerException extends Exception {
    public LexerException(String message) {
        super(message);
    }

    public LexerException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
