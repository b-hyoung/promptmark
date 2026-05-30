package local.promptmark.dto;

import java.io.Serializable;

import local.promptmark.web.Role;

/**
 * Immutable session-friendly snapshot of a logged-in user. Never contains the
 * password hash. Stored under session attribute {@code LOGIN_USER}.
 */
public final class LoginUser implements Serializable {

    private static final long serialVersionUID = 1L;

    private final long id;
    private final String email;
    private final String nickname;
    private final Role role;

    public LoginUser(long id, String email, String nickname, Role role) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.role = role;
    }

    public long getId() { return id; }
    public String getEmail() { return email; }
    public String getNickname() { return nickname; }
    public Role getRole() { return role; }

    /** EL-friendly string view of the role for {@code ${LOGIN_USER.role == 'ADMIN'}}. */
    public String getRoleName() { return role == null ? null : role.name(); }
}
