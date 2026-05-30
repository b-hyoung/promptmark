package local.promptmark.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class EnvTest {

    @Test
    void parsesKeyValueLinesAndIgnoresCommentsAndBlanks(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve(".env");
        Files.write(f, ("# comment\n"
                      + "DB_URL=jdbc:postgresql://x\n"
                      + "\n"
                      + "ADMIN_EMAIL=a@b.com\n"
                      + "EMPTY=\n").getBytes());

        Env env = Env.fromFile(f);

        assertThat(env.get("DB_URL")).isEqualTo("jdbc:postgresql://x");
        assertThat(env.get("ADMIN_EMAIL")).isEqualTo("a@b.com");
        assertThat(env.get("EMPTY")).isEmpty();
    }

    @Test
    void fallsBackToSystemEnvWhenFileMissing(@TempDir Path tmp) {
        Path missing = tmp.resolve(".env");
        Env env = Env.fromFile(missing);                       // file does not exist
        assertThat(env.get("PATH")).isNotNull();               // system PATH should exist
    }

    @Test
    void getRequiredThrowsWhenMissing(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve(".env");
        Files.write(f, "FOO=bar\n".getBytes());
        Env env = Env.fromFile(f);
        assertThatThrownBy(() -> env.getRequired("MISSING_KEY"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("MISSING_KEY");
    }

    @Test
    void getOrDefaultReturnsDefaultWhenMissing(@TempDir Path tmp) throws Exception {
        Path f = tmp.resolve(".env");
        Files.write(f, "FOO=bar\n".getBytes());
        Env env = Env.fromFile(f);
        assertThat(env.getOrDefault("FOO", "x")).isEqualTo("bar");
        assertThat(env.getOrDefault("MISSING", "default")).isEqualTo("default");
    }
}
