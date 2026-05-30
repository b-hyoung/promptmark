package local.promptmark.web;

public class ForbiddenException extends AppException {
    public ForbiddenException(String msg) { super(403, msg); }
}
