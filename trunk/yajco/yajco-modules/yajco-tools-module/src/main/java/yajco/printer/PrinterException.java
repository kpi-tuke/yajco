package yajco.printer;

public class PrinterException extends RuntimeException {
    public PrinterException(String message) {
        super(message);
    }

    public PrinterException(String message, Throwable cause) {
        super(message, cause);
    }
}
