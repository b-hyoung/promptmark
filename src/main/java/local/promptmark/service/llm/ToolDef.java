package local.promptmark.service.llm;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Tool schema descriptor passed to the LLM (OpenAI / Claude tool-calling).
 * {@link #parametersSchema()} is a JSON Schema object describing the arguments.
 */
public final class ToolDef {

    private final String name;
    private final String description;
    private final JsonNode parametersSchema;

    public ToolDef(String name, String description, JsonNode parametersSchema) {
        this.name = name;
        this.description = description;
        this.parametersSchema = parametersSchema;
    }

    public String name() { return name; }
    public String description() { return description; }
    public JsonNode parametersSchema() { return parametersSchema; }
}
