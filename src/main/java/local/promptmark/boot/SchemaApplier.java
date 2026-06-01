package local.promptmark.boot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SchemaApplier {

    private static final Logger log = LoggerFactory.getLogger(SchemaApplier.class);

    private SchemaApplier() {}

    public static void applyFromClasspath(DataSource ds, String classpathResource) {
        String sql = readResource(classpathResource);
        List<String> statements = splitStatements(sql);

        try (Connection c = ds.getConnection(); Statement s = c.createStatement()) {
            for (String stmt : statements) {
                if (stmt.isEmpty()) continue;
                log.debug("Executing: {}", stmt.substring(0, Math.min(80, stmt.length())));
                s.execute(stmt);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Schema apply failed: " + e.getMessage(), e);
        }
        log.info("Schema applied: {} statements", statements.size());
    }

    private static String readResource(String path) {
        try (InputStream in = SchemaApplier.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) throw new IllegalStateException("Resource not found: " + path);
            try (BufferedReader r = new BufferedReader(
                    new InputStreamReader(in, StandardCharsets.UTF_8))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = r.readLine()) != null) sb.append(line).append('\n');
                return sb.toString();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to read " + path, e);
        }
    }

    // Splits on ';' that ends a statement. Strips full-line `--` comments only.
    // Aware of PostgreSQL dollar-quoted string blocks ($$ ... $$, $tag$ ... $tag$):
    // a ';' inside a dollar-quoted block does not end a statement.
    private static List<String> splitStatements(String sql) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String rawLine : sql.split("\n")) {
            String line = rawLine.trim();
            if (line.startsWith("--") || line.isEmpty()) continue;
            cur.append(rawLine).append('\n');

            String accum = cur.toString();
            if (!endsStatement(accum)) continue;

            String stmt = accum.trim();
            if (stmt.endsWith(";")) stmt = stmt.substring(0, stmt.length() - 1);
            out.add(stmt.trim());
            cur.setLength(0);
        }
        if (cur.length() > 0) {
            String last = cur.toString().trim();
            if (!last.isEmpty()) out.add(last);
        }
        return out;
    }

    /** True when the accumulated text ends with a top-level ';' (i.e. not inside a $$ block). */
    private static boolean endsStatement(String s) {
        String trimmed = s.trim();
        if (!trimmed.endsWith(";")) return false;
        // Scan to confirm we are not inside an open dollar-quoted block.
        boolean inDollar = false;
        String dollarTag = null;
        int i = 0;
        while (i < trimmed.length()) {
            char c = trimmed.charAt(i);
            if (c == '$') {
                // Read optional tag word until next '$'
                int end = trimmed.indexOf('$', i + 1);
                if (end < 0) break;
                String tag = trimmed.substring(i + 1, end);
                if (tag.matches("[A-Za-z_][A-Za-z0-9_]*|")) {
                    if (!inDollar) { inDollar = true; dollarTag = tag; }
                    else if (tag.equals(dollarTag)) { inDollar = false; dollarTag = null; }
                    i = end + 1;
                    continue;
                }
            }
            i++;
        }
        return !inDollar;
    }
}
