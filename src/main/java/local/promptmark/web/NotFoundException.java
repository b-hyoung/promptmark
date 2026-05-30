package local.promptmark.web;

public class NotFoundException extends AppException {
    public NotFoundException(String msg) { super(404, msg); }
}
