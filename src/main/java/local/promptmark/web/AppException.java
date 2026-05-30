package local.promptmark.web;

public class AppException extends RuntimeException {

    private final int code;
    private final String userMessage;

    public AppException(int code, String userMessage) {
        super(userMessage);
        this.code = code;
        this.userMessage = userMessage;
    }

    public AppException(int code, String userMessage, Throwable cause) {
        super(userMessage, cause);
        this.code = code;
        this.userMessage = userMessage;
    }

    public int code() { return code; }
    public String userMessage() { return userMessage; }
}
