package local.promptmark.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UploadUtilTest {

    @Test
    void sanitizeFilename_strips_path_segments() {
        assertThat(UploadUtil.sanitizeFilename("../../etc/passwd")).isEqualTo("passwd");
        assertThat(UploadUtil.sanitizeFilename("C:\\Users\\me\\evil.md")).isEqualTo("evil.md");
    }

    @Test
    void sanitizeFilename_replaces_special_chars_with_underscore() {
        assertThat(UploadUtil.sanitizeFilename("my file (1).md"))
            .isEqualTo("my_file__1_.md");
    }

    @Test
    void sanitizeFilename_keeps_safe_chars() {
        assertThat(UploadUtil.sanitizeFilename("AaZz0-9._.md")).isEqualTo("AaZz0-9._.md");
    }

    @Test
    void sanitizeFilename_null_returns_default() {
        assertThat(UploadUtil.sanitizeFilename(null)).isEqualTo("file");
    }

    @Test
    void sanitizeFilename_only_dots_returns_default() {
        assertThat(UploadUtil.sanitizeFilename("...")).isEqualTo("file");
    }

    @Test
    void sanitizeFilename_clamps_to_80_chars() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 200; i++) sb.append('a');
        String out = UploadUtil.sanitizeFilename(sb.toString());
        assertThat(out).hasSize(80);
    }
}
