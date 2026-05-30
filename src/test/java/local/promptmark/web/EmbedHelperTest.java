package local.promptmark.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EmbedHelperTest {

    @Test
    void youtube_watch_url_to_embed() {
        assertThat(EmbedHelper.toIframeSrc("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
            .isEqualTo("https://www.youtube.com/embed/dQw4w9WgXcQ");
    }

    @Test
    void youtube_watch_with_extra_params() {
        assertThat(EmbedHelper.toIframeSrc("https://www.youtube.com/watch?v=dQw4w9WgXcQ&feature=share"))
            .isEqualTo("https://www.youtube.com/embed/dQw4w9WgXcQ");
    }

    @Test
    void youtu_be_short_url_to_embed() {
        assertThat(EmbedHelper.toIframeSrc("https://youtu.be/dQw4w9WgXcQ"))
            .isEqualTo("https://www.youtube.com/embed/dQw4w9WgXcQ");
    }

    @Test
    void youtube_already_embed_url_passes_through() {
        assertThat(EmbedHelper.toIframeSrc("https://www.youtube.com/embed/abc123XYZ"))
            .isEqualTo("https://www.youtube.com/embed/abc123XYZ");
    }

    @Test
    void vimeo_url_to_embed() {
        assertThat(EmbedHelper.toIframeSrc("https://vimeo.com/123456789"))
            .isEqualTo("https://player.vimeo.com/video/123456789");
    }

    @Test
    void unknown_host_returns_null() {
        assertThat(EmbedHelper.toIframeSrc("https://evil.example.com/v/abc")).isNull();
    }

    @Test
    void null_or_empty_returns_null() {
        assertThat(EmbedHelper.toIframeSrc(null)).isNull();
        assertThat(EmbedHelper.toIframeSrc("")).isNull();
        assertThat(EmbedHelper.toIframeSrc("   ")).isNull();
    }
}
