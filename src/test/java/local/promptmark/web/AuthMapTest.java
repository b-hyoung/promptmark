package local.promptmark.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuthMapTest {

    @Test
    void anonymous_actions_listed_in_spec_resolve_to_anonymous() {
        assertThat(AuthMap.required("auth.signup")).isEqualTo(Role.ANONYMOUS);
        assertThat(AuthMap.required("auth.login")).isEqualTo(Role.ANONYMOUS);
        assertThat(AuthMap.required("asset.list")).isEqualTo(Role.ANONYMOUS);
        assertThat(AuthMap.required("asset.detail")).isEqualTo(Role.ANONYMOUS);

        assertThat(AuthMap.isAnonymous("auth.login")).isTrue();
        assertThat(AuthMap.isAnonymous("asset.list")).isTrue();
    }

    @Test
    void logout_requires_user() {
        assertThat(AuthMap.required("auth.logout")).isEqualTo(Role.USER);
        assertThat(AuthMap.isAnonymous("auth.logout")).isFalse();
    }

    @Test
    void unmapped_actions_default_to_user() {
        assertThat(AuthMap.required("mypage.index")).isEqualTo(Role.USER);
        assertThat(AuthMap.required("admin.reports")).isEqualTo(Role.USER);
        assertThat(AuthMap.isAnonymous("anything.unknown")).isFalse();
    }

    @Test
    void hierarchy_drives_satisfiedBy_for_required_role() {
        Role req = AuthMap.required("auth.logout"); // USER
        assertThat(req.satisfiedBy(Role.USER)).isTrue();
        assertThat(req.satisfiedBy(Role.SELLER)).isTrue();
        assertThat(req.satisfiedBy(Role.ADMIN)).isTrue();
        assertThat(req.satisfiedBy(Role.ANONYMOUS)).isFalse();
    }
}
