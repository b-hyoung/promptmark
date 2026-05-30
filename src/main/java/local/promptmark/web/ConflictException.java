package local.promptmark.web;

public class ConflictException extends AppException {
    public ConflictException(String msg) { super(409, msg); }
}
