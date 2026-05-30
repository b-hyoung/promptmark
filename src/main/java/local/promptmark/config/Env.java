package local.promptmark.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public final class Env {

    private final Map<String, String> values;

    private Env(Map<String, String> values) {
        this.values = values;
    }

    /** Load .env at given path (silently ignored if missing) + System.getenv() (lower priority). */
    public static Env fromFile(Path envFile) {
        Map<String, String> map = new HashMap<>(System.getenv());
        if (envFile != null && Files.isRegularFile(envFile)) {
            try {
                for (String line : Files.readAllLines(envFile)) {
                    String trimmed = line.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                    int eq = trimmed.indexOf('=');
                    if (eq < 0) continue;
                    String key = trimmed.substring(0, eq).trim();
                    String val = trimmed.substring(eq + 1).trim();
                    map.put(key, val);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read " + envFile, e);
            }
        }
        return new Env(map);
    }

    /** Default loader: looks for ./.env in current working directory. */
    public static Env load() {
        return fromFile(Paths.get(".env"));
    }

    public String get(String key) {
        return values.get(key);
    }

    public String getOrDefault(String key, String def) {
        String v = values.get(key);
        return (v == null) ? def : v;
    }

    public String getRequired(String key) {
        String v = values.get(key);
        if (v == null || v.isEmpty()) {
            throw new IllegalStateException("Missing required env var: " + key);
        }
        return v;
    }

    public int getInt(String key, int def) {
        String v = values.get(key);
        if (v == null || v.isEmpty()) return def;
        return Integer.parseInt(v);
    }
}
