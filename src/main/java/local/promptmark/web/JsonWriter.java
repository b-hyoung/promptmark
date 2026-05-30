package local.promptmark.web;

import java.util.Collection;
import java.util.Map;

/**
 * Minimal dependency-free JSON serializer. Handles the shapes we emit from
 * {@link ViewResult#json(Object)} in Phase 2 (error envelopes: Map / List /
 * String / Number / Boolean / null). A richer Jackson-based writer can replace
 * this in Phase 4 when full chat responses arrive.
 */
final class JsonWriter {

    private JsonWriter() {}

    static String toJson(Object o) {
        StringBuilder sb = new StringBuilder(64);
        write(sb, o);
        return sb.toString();
    }

    private static void write(StringBuilder sb, Object o) {
        if (o == null) { sb.append("null"); return; }
        if (o instanceof Boolean || o instanceof Number) { sb.append(o); return; }
        if (o instanceof CharSequence) { writeString(sb, o.toString()); return; }
        if (o instanceof Map) { writeMap(sb, (Map<?, ?>) o); return; }
        if (o instanceof Collection) { writeArray(sb, (Collection<?>) o); return; }
        if (o.getClass().isArray()) { writeArray(sb, arrayToList(o)); return; }
        // Fallback: toString as JSON string. Keeps us forward-compatible without Jackson.
        writeString(sb, o.toString());
    }

    private static Collection<?> arrayToList(Object array) {
        int n = java.lang.reflect.Array.getLength(array);
        java.util.ArrayList<Object> list = new java.util.ArrayList<>(n);
        for (int i = 0; i < n; i++) list.add(java.lang.reflect.Array.get(array, i));
        return list;
    }

    private static void writeMap(StringBuilder sb, Map<?, ?> m) {
        sb.append('{');
        boolean first = true;
        for (Map.Entry<?, ?> e : m.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            writeString(sb, String.valueOf(e.getKey()));
            sb.append(':');
            write(sb, e.getValue());
        }
        sb.append('}');
    }

    private static void writeArray(StringBuilder sb, Collection<?> c) {
        sb.append('[');
        boolean first = true;
        for (Object item : c) {
            if (!first) sb.append(',');
            first = false;
            write(sb, item);
        }
        sb.append(']');
    }

    private static void writeString(StringBuilder sb, String s) {
        sb.append('"');
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b");  break;
                case '\f': sb.append("\\f");  break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (ch < 0x20) {
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
            }
        }
        sb.append('"');
    }
}
