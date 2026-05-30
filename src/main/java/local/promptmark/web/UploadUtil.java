package local.promptmark.web;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import com.oreilly.servlet.MultipartRequest;

/**
 * Thin wrapper around cos.jar's {@link MultipartRequest} for the asset upload
 * flow. Parsing writes to {@code uploads/tmp/}; promoting to the permanent
 * store builds a date-bucketed path under {@code uploads/assets/yyyy/MM/dd/}
 * and returns the relative key.
 */
public final class UploadUtil {

    /** Max upload size (bytes). 10MB matches the spec. */
    public static final int MAX_UPLOAD_BYTES = 10 * 1024 * 1024;

    private static final String TMP_DIR_DEFAULT = "uploads/tmp";
    private static final String STORE_ROOT_DEFAULT = "uploads/assets";

    private UploadUtil() {}

    /**
     * Parse a multipart request. Files land in {@code uploads/tmp/}; the
     * directory is created on demand.
     */
    public static MultipartRequest parse(HttpServletRequest req) throws IOException {
        Path tmp = Paths.get(TMP_DIR_DEFAULT);
        Files.createDirectories(tmp);
        return new MultipartRequest(req, tmp.toAbsolutePath().toString(),
            MAX_UPLOAD_BYTES, StandardCharsets.UTF_8.name());
    }

    /**
     * Move the uploaded file under {@code fieldName} into the permanent store
     * and return its relative key (e.g. {@code uploads/assets/2026/05/30/<uuid>-<name>}).
     * Returns null if no file was supplied for that field.
     */
    public static String moveToPermanentStore(MultipartRequest mr, String fieldName)
            throws IOException {
        if (mr == null || fieldName == null) return null;
        File f = mr.getFile(fieldName);
        if (f == null || !f.exists()) return null;

        String original = mr.getFilesystemName(fieldName);
        if (original == null) original = f.getName();
        String safeName = sanitizeFilename(original);

        DateTimeFormatter yy = DateTimeFormatter.ofPattern("yyyy");
        DateTimeFormatter mm = DateTimeFormatter.ofPattern("MM");
        DateTimeFormatter dd = DateTimeFormatter.ofPattern("dd");
        LocalDate today = LocalDate.now();
        Path destDir = Paths.get(STORE_ROOT_DEFAULT,
            today.format(yy), today.format(mm), today.format(dd));
        Files.createDirectories(destDir);

        String key = UUID.randomUUID().toString() + "-" + safeName;
        Path destFile = destDir.resolve(key);
        Files.move(f.toPath(), destFile, StandardCopyOption.REPLACE_EXISTING);
        // Return forward-slash key so it stays portable across OSes.
        return STORE_ROOT_DEFAULT + "/" +
            today.format(yy) + "/" + today.format(mm) + "/" + today.format(dd) + "/" + key;
    }

    public static String getParam(MultipartRequest mr, String name) {
        return mr == null ? null : mr.getParameter(name);
    }

    public static String getParam(MultipartRequest mr, String name, String dflt) {
        String v = getParam(mr, name);
        return (v == null || v.isEmpty()) ? dflt : v;
    }

    /**
     * Strip path components and dangerous characters from a user-supplied
     * filename. Collapses repeated separators, drops anything that isn't
     * alphanumeric / dot / underscore / hyphen, and clamps to 80 chars.
     * Always returns a non-empty string.
     */
    public static String sanitizeFilename(String raw) {
        if (raw == null) return "file";
        // Drop directory traversal segments.
        String n = raw.replace('\\', '/');
        int slash = n.lastIndexOf('/');
        if (slash >= 0) n = n.substring(slash + 1);
        if (n.isEmpty()) return "file";

        StringBuilder sb = new StringBuilder(n.length());
        for (int i = 0; i < n.length(); i++) {
            char ch = n.charAt(i);
            if ((ch >= 'a' && ch <= 'z') ||
                (ch >= 'A' && ch <= 'Z') ||
                (ch >= '0' && ch <= '9') ||
                ch == '.' || ch == '_' || ch == '-') {
                sb.append(ch);
            } else {
                sb.append('_');
            }
        }
        String out = sb.toString();
        // Avoid leading dots ("..", ".bashrc") and reserved Windows names.
        while (out.startsWith(".")) out = out.substring(1);
        if (out.isEmpty()) out = "file";
        if (out.length() > 80) out = out.substring(0, 80);
        return out;
    }
}
