// KerberosAuthException.java
public class KerberosAuthException extends Exception {
    public KerberosAuthException(String message) {
        super(message);
    }

    public KerberosAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}