package util;

/**
 * Exception carrying a message that is safe and understandable to show
 * to the user (business-rule violations, friendly database errors).
 */
public class HostelException extends Exception {

    private static final long serialVersionUID = 1L;

    public HostelException(String message) {
        super(message);
    }
}
