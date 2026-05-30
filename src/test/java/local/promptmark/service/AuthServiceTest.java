package local.promptmark.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;
import org.mockito.ArgumentCaptor;

import local.promptmark.dao.UserDao;
import local.promptmark.dto.LoginUser;
import local.promptmark.dto.User;
import local.promptmark.web.ConflictException;
import local.promptmark.web.Role;
import local.promptmark.web.UnauthorizedException;
import local.promptmark.web.ValidationException;
import org.mockito.Mockito;

class AuthServiceTest {

    private UserDao userDao;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userDao = Mockito.mock(UserDao.class);
        authService = new AuthService(userDao);
    }

    // ───── signup ─────────────────────────────────────────────────────

    @Test
    void signup_happy_path_hashes_password_and_inserts_USER() {
        when(userDao.existsByEmail("a@b.com")).thenReturn(false);
        when(userDao.existsByNickname("alice")).thenReturn(false);
        User row = new User(1L, "a@b.com", "hash", "alice",
            Role.USER, User.Status.ACTIVE, Instant.now());
        when(userDao.insert(eq("a@b.com"), anyString(), eq("alice"), eq(Role.USER)))
            .thenReturn(row);

        User result = authService.signup("a@b.com", "password1", "password1", "alice", "USER");

        assertThat(result.getId()).isEqualTo(1L);
        ArgumentCaptor<String> hashCap = ArgumentCaptor.forClass(String.class);
        verify(userDao).insert(eq("a@b.com"), hashCap.capture(), eq("alice"), eq(Role.USER));
        // BCrypt hash should verify against original password.
        assertThat(BCrypt.checkpw("password1", hashCap.getValue())).isTrue();
    }

    @Test
    void signup_allows_SELLER_role() {
        when(userDao.existsByEmail(anyString())).thenReturn(false);
        when(userDao.existsByNickname(anyString())).thenReturn(false);
        when(userDao.insert(anyString(), anyString(), anyString(), eq(Role.SELLER)))
            .thenReturn(new User(2L, "s@b.com", "h", "seller",
                Role.SELLER, User.Status.ACTIVE, Instant.now()));

        User u = authService.signup("s@b.com", "password1", "password1", "seller", "SELLER");
        assertThat(u.getRole()).isEqualTo(Role.SELLER);
    }

    @Test
    void signup_rejects_ADMIN_role_via_validation() {
        ValidationException ex = catchValidation(() ->
            authService.signup("a@b.com", "password1", "password1", "alice", "ADMIN"));
        assertThat(ex.fieldErrors()).containsKey("role");
        verify(userDao, never()).insert(anyString(), anyString(), anyString(), any());
    }

    @Test
    void signup_rejects_unknown_role() {
        ValidationException ex = catchValidation(() ->
            authService.signup("a@b.com", "password1", "password1", "alice", "WIZARD"));
        assertThat(ex.fieldErrors()).containsKey("role");
    }

    @Test
    void signup_rejects_short_password() {
        ValidationException ex = catchValidation(() ->
            authService.signup("a@b.com", "short", "short", "alice", "USER"));
        assertThat(ex.fieldErrors()).containsKey("password");
    }

    @Test
    void signup_rejects_too_long_password() {
        String tooLong = repeat("a", 65);
        ValidationException ex = catchValidation(() ->
            authService.signup("a@b.com", tooLong, tooLong, "alice", "USER"));
        assertThat(ex.fieldErrors()).containsKey("password");
    }

    @Test
    void signup_rejects_password_mismatch() {
        ValidationException ex = catchValidation(() ->
            authService.signup("a@b.com", "password1", "password2", "alice", "USER"));
        assertThat(ex.fieldErrors()).containsKey("password_confirm");
    }

    @Test
    void signup_rejects_bad_email() {
        ValidationException ex = catchValidation(() ->
            authService.signup("not-an-email", "password1", "password1", "alice", "USER"));
        assertThat(ex.fieldErrors()).containsKey("email");
    }

    @Test
    void signup_rejects_short_nickname() {
        ValidationException ex = catchValidation(() ->
            authService.signup("a@b.com", "password1", "password1", "x", "USER"));
        assertThat(ex.fieldErrors()).containsKey("nickname");
    }

    @Test
    void signup_throws_conflict_on_duplicate_email() {
        when(userDao.existsByEmail("dup@b.com")).thenReturn(true);

        assertThatExceptionOfType(ConflictException.class).isThrownBy(() ->
            authService.signup("dup@b.com", "password1", "password1", "alice", "USER"));

        verify(userDao, never()).insert(anyString(), anyString(), anyString(), any());
    }

    @Test
    void signup_throws_conflict_on_duplicate_nickname() {
        when(userDao.existsByEmail("a@b.com")).thenReturn(false);
        when(userDao.existsByNickname("alice")).thenReturn(true);

        assertThatExceptionOfType(ConflictException.class).isThrownBy(() ->
            authService.signup("a@b.com", "password1", "password1", "alice", "USER"));

        verify(userDao, never()).insert(anyString(), anyString(), anyString(), any());
    }

    // ───── login ──────────────────────────────────────────────────────

    @Test
    void login_happy_path_returns_login_user() {
        String hash = BCrypt.hashpw("password1", BCrypt.gensalt(4));
        User row = new User(7L, "a@b.com", hash, "alice",
            Role.USER, User.Status.ACTIVE, Instant.now());
        when(userDao.findByEmail("a@b.com")).thenReturn(Optional.of(row));

        LoginUser lu = authService.login("a@b.com", "password1");
        assertThat(lu.getId()).isEqualTo(7L);
        assertThat(lu.getEmail()).isEqualTo("a@b.com");
        assertThat(lu.getNickname()).isEqualTo("alice");
        assertThat(lu.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void login_wrong_password_throws_unauthorized() {
        String hash = BCrypt.hashpw("password1", BCrypt.gensalt(4));
        User row = new User(7L, "a@b.com", hash, "alice",
            Role.USER, User.Status.ACTIVE, Instant.now());
        when(userDao.findByEmail("a@b.com")).thenReturn(Optional.of(row));

        assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(() ->
            authService.login("a@b.com", "wrong-password"));
    }

    @Test
    void login_unknown_email_throws_unauthorized() {
        when(userDao.findByEmail("missing@b.com")).thenReturn(Optional.empty());

        assertThatExceptionOfType(UnauthorizedException.class).isThrownBy(() ->
            authService.login("missing@b.com", "password1"));
    }

    @Test
    void login_banned_account_throws_unauthorized() {
        String hash = BCrypt.hashpw("password1", BCrypt.gensalt(4));
        User row = new User(9L, "b@b.com", hash, "banned",
            Role.USER, User.Status.BANNED, Instant.now());
        when(userDao.findByEmail("b@b.com")).thenReturn(Optional.of(row));

        assertThatExceptionOfType(UnauthorizedException.class)
            .isThrownBy(() -> authService.login("b@b.com", "password1"))
            .withMessageContaining("정지");
    }

    // ───── helpers ────────────────────────────────────────────────────

    private static ValidationException catchValidation(Runnable r) {
        try {
            r.run();
            throw new AssertionError("Expected ValidationException");
        } catch (ValidationException e) {
            return e;
        }
    }

    private static String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder(s.length() * n);
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }
}
