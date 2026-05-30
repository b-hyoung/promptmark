package local.promptmark.dto;

import java.time.Instant;

import local.promptmark.web.Role;

/**
 * Mutable-ish data holder for a {@code users} row. Constructor sets all fields;
 * accessors are JavaBean-style so EL can read them.
 *
 * <p>Carries {@code passwordHash} — never expose to a JSP or place on the session.
 * Use {@link LoginUser} for anything user-facing.
 */
public final class User {

    public enum Status { ACTIVE, BANNED }

    private final long id;
    private final String email;
    private final String passwordHash;
    private final String nickname;
    private final Role role;
    private final Status status;
    private final Instant createdAt;

    public User(long id, String email, String passwordHash, String nickname,
                Role role, Status status, Instant createdAt) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
        this.role = role;
        this.status = status;
        this.createdAt = createdAt;
    }

    public long getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getNickname() { return nickname; }
    public Role getRole() { return role; }
    public Status getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public static Status statusFromDb(String dbVal) {
        if (dbVal == null) return Status.ACTIVE;
        try {
            return Status.valueOf(dbVal.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return Status.ACTIVE;
        }
    }
}
