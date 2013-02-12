package yajco.visitor;

public class VisitorException extends RuntimeException {
    public VisitorException(String message) {
        super(message);
    }

    public VisitorException(String message, Throwable cause) {
        super(message, cause);
    }
}