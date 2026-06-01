package local.promptmark.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SafeNextTest {

    private static final String FALLBACK = "/";

    @Test
    void allows_app_paths() {
        assertThat(SafeNext.resolveValue("/app/plugin/list", FALLBACK))
            .isEqualTo("/app/plugin/list");
        assertThat(SafeNext.resolveValue("/app/auth/login?msg=ok", FALLBACK))
            .isEqualTo("/app/auth/login?msg=ok");
    }

    @Test
    void rejects_protocol_relative() {
        assertThat(SafeNext.resolveValue("//evil.com/login", FALLBACK)).isEqualTo(FALLBACK);
    }

    @Test
    void rejects_javascript_scheme() {
        assertThat(SafeNext.resolveValue("javascript:alert(1)", FALLBACK)).isEqualTo(FALLBACK);
        assertThat(SafeNext.resolveValue("JaVaScRiPt:alert(1)", FALLBACK)).isEqualTo(FALLBACK);
    }

    @Test
    void rejects_absolute_urls() {
        assertThat(SafeNext.resolveValue("http://evil.com", FALLBACK)).isEqualTo(FALLBACK);
        assertThat(SafeNext.resolveValue("https://evil.com/app/plugin/list", FALLBACK))
            .isEqualTo(FALLBACK);
    }

    @Test
    void rejects_paths_outside_app() {
        assertThat(SafeNext.resolveValue("/", FALLBACK)).isEqualTo(FALLBACK);
        assertThat(SafeNext.resolveValue("/admin", FALLBACK)).isEqualTo(FALLBACK);
        assertThat(SafeNext.resolveValue("app/plugin/list", FALLBACK)).isEqualTo(FALLBACK);
    }

    @Test
    void rejects_null_and_empty() {
        assertThat(SafeNext.resolveValue(null, FALLBACK)).isEqualTo(FALLBACK);
        assertThat(SafeNext.resolveValue("", FALLBACK)).isEqualTo(FALLBACK);
        assertThat(SafeNext.resolveValue("   ", FALLBACK)).isEqualTo(FALLBACK);
    }
}
