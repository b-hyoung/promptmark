package local.promptmark.web;

import java.util.Collections;
import java.util.Map;

public class ValidationException extends AppException {

    private final Map<String, String> fieldErrors;

    public ValidationException(String msg, Map<String, String> fieldErrors) {
        super(400, msg);
        this.fieldErrors = (fieldErrors == null)
            ? Collections.emptyMap()
            : Collections.unmodifiableMap(fieldErrors);
    }

    public Map<String, String> fieldErrors() { return fieldErrors; }
}
