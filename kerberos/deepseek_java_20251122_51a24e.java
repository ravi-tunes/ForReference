// SolaceConnectionException.java
public class SolaceConnectionException extends Exception {
    public SolaceConnectionException(String message) {
        super(message);
    }

    public SolaceConnectionException(String message, Throwable cause) {
        super(message, cause);
    }
}