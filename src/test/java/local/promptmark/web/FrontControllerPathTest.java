package local.promptmark.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FrontControllerPathTest {

    @Test
    void parses_module_action() {
        assertThat(FrontController.parseActionKey("/auth/login")).isEqualTo("auth.login");
        assertThat(FrontController.parseActionKey("/asset/list")).isEqualTo("asset.list");
    }

    @Test
    void trailing_slash_is_tolerated() {
        assertThat(FrontController.parseActionKey("/auth/login/")).isEqualTo("auth.login");
    }

    @Test
    void null_or_empty_returns_null() {
        assertThat(FrontController.parseActionKey(null)).isNull();
        assertThat(FrontController.parseActionKey("")).isNull();
        assertThat(FrontController.parseActionKey("/")).isNull();
    }

    @Test
    void single_segment_returns_null() {
        assertThat(FrontController.parseActionKey("/auth")).isNull();
        assertThat(FrontController.parseActionKey("/auth/")).isNull();
    }

    @Test
    void nested_segments_rejected() {
        assertThat(FrontController.parseActionKey("/admin/users/ban")).isNull();
    }
}
