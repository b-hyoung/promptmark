package local.promptmark.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;

class AppExceptionTest {

    @Test
    void notFoundCarries404() {
        AppException e = new NotFoundException("plugin not found");
        assertThat(e.code()).isEqualTo(404);
        assertThat(e.userMessage()).isEqualTo("plugin not found");
    }

    @Test
    void forbiddenCarries403() {
        assertThat(new ForbiddenException("nope").code()).isEqualTo(403);
    }

    @Test
    void unauthorizedCarries401() {
        assertThat(new UnauthorizedException("login").code()).isEqualTo(401);
    }

    @Test
    void conflictCarries409() {
        assertThat(new ConflictException("duplicate email").code()).isEqualTo(409);
    }

    @Test
    void validationCarriesFieldErrors() {
        Map<String, String> errors = Map.of("email", "invalid", "password", "too short");
        ValidationException e = new ValidationException("validation failed", errors);
        assertThat(e.code()).isEqualTo(400);
        assertThat(e.fieldErrors()).containsEntry("email", "invalid")
                                   .containsEntry("password", "too short");
    }
}
