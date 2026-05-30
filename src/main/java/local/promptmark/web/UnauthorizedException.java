package local.promptmark.web;

public class UnauthorizedException extends AppException {
    public UnauthorizedException(String msg) { super(401, msg); }
}
