package local.promptmark.service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

import org.mindrot.jbcrypt.BCrypt;

import local.promptmark.dao.UserDao;
import local.promptmark.dto.LoginUser;
import local.promptmark.dto.User;
import local.promptmark.web.ConflictException;
import local.promptmark.web.Role;
import local.promptmark.web.UnauthorizedException;
import local.promptmark.web.ValidationException;

/**
 * Authentication workflows: signup (with validation and BCrypt) + login.
 * Role on signup is constrained to {USER, SELLER}; ADMIN is seeded by
 * {@code AdminSeeder} during boot only.
 */
public class AuthService {

    private static final Pattern EMAIL_REGEX = Pattern.compile(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final int PASSWORD_MIN = 8;
    private static final int PASSWORD_MAX = 64;
    private static final int NICKNAME_MIN = 2;
    private static final int NICKNAME_MAX = 20;

    private final UserDao userDao;

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * Validate inputs, hash the password, and insert. On duplicate email or
     * nickname throws {@link ConflictException}; on bad input
     * {@link ValidationException} with a per-field error map.
     */
    public User signup(String email,
                       String password,
                       String passwordConfirm,
                       String nickname,
                       String roleStr) {
        Map<String, String> errors = new LinkedHashMap<>();

        String e = trim(email);
        if (e.isEmpty()) {
            errors.put("email", "이메일을 입력하세요");
        } else if (e.length() > 120 || !EMAIL_REGEX.matcher(e).matches()) {
            errors.put("email", "올바른 이메일 형식이 아닙니다");
        }

        String p = (password == null) ? "" : password;
        if (p.length() < PASSWORD_MIN || p.length() > PASSWORD_MAX) {
            errors.put("password",
                "비밀번호는 " + PASSWORD_MIN + "~" + PASSWORD_MAX + "자여야 합니다");
        }

        String pc = (passwordConfirm == null) ? "" : passwordConfirm;
        if (!p.equals(pc)) {
            errors.put("password_confirm", "비밀번호가 일치하지 않습니다");
        }

        String n = trim(nickname);
        if (n.length() < NICKNAME_MIN || n.length() > NICKNAME_MAX) {
            errors.put("nickname",
                "닉네임은 " + NICKNAME_MIN + "~" + NICKNAME_MAX + "자여야 합니다");
        }

        Role role = parseSignupRole(roleStr, errors);

        if (!errors.isEmpty()) {
            throw new ValidationException("입력값을 확인해주세요", errors);
        }

        if (userDao.existsByEmail(e)) {
            throw new ConflictException("이미 사용 중인 이메일입니다");
        }
        if (userDao.existsByNickname(n)) {
            throw new ConflictException("이미 사용 중인 닉네임입니다");
        }

        String hash = BCrypt.hashpw(p, BCrypt.gensalt(12));
        return userDao.insert(e, hash, n, role);
    }

    /**
     * Verify credentials. Returns a session-safe {@link LoginUser}. On any
     * failure path throws {@link UnauthorizedException} with a generic message
     * so attackers can't enumerate accounts.
     */
    public LoginUser login(String email, String password) {
        String e = trim(email);
        String p = (password == null) ? "" : password;

        Optional<User> opt = userDao.findByEmail(e);
        if (!opt.isPresent()) {
            throw new UnauthorizedException("이메일/비밀번호 불일치");
        }
        User u = opt.get();

        // Check password BEFORE status to avoid leaking that the account exists
        // via differential responses. (Both paths take BCrypt time.)
        boolean pwOk;
        try {
            pwOk = BCrypt.checkpw(p, u.getPasswordHash());
        } catch (IllegalArgumentException ex) {
            // Corrupt hash on the row — treat as login failure.
            pwOk = false;
        }
        if (!pwOk) {
            throw new UnauthorizedException("이메일/비밀번호 불일치");
        }

        if (u.getStatus() == User.Status.BANNED) {
            throw new UnauthorizedException("정지된 계정입니다");
        }

        return new LoginUser(u.getId(), u.getEmail(), u.getNickname(), u.getRole());
    }

    private static Role parseSignupRole(String roleStr, Map<String, String> errors) {
        String r = (roleStr == null) ? "" : roleStr.trim().toUpperCase();
        if ("USER".equals(r)) return Role.USER;
        if ("SELLER".equals(r)) return Role.SELLER;
        errors.put("role", "역할은 USER 또는 SELLER 만 선택할 수 있습니다");
        return Role.USER; // placeholder; we'll throw below
    }

    private static String trim(String s) {
        return (s == null) ? "" : s.trim();
    }
}
