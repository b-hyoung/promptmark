package local.promptmark.service.llm;

/**
 * Unchecked failure raised by LLM / embedding clients on HTTP errors, timeouts,
 * or malformed responses. The agent catches this and falls back to rule mode.
 */
public class LlmException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public LlmException(String message) {
        super(message);
    }

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }
}
