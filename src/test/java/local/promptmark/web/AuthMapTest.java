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
        assertThat(AuthMap.required("future.unmapped")).isEqualTo(Role.USER);
        assertThat(AuthMap.required("anything.unknown")).isEqualTo(Role.USER);
        assertThat(AuthMap.isAnonymous("anything.unknown")).isFalse();
    }

    @Test
    void mypage_requires_user() {
        assertThat(AuthMap.required("mypage.index")).isEqualTo(Role.USER);
    }

    @Test
    void admin_endpoints_require_admin() {
        assertThat(AuthMap.required("admin.reports")).isEqualTo(Role.ADMIN);
        assertThat(AuthMap.required("admin.report.resolve")).isEqualTo(Role.ADMIN);
        assertThat(AuthMap.required("admin.user.ban")).isEqualTo(Role.ADMIN);
    }

    @Test
    void hierarchy_drives_satisfiedBy_for_required_role() {
        Role req = AuthMap.required("auth.logout"); // USER
        assertThat(req.satisfiedBy(Role.USER)).isTrue();
        assertThat(req.satisfiedBy(Role.SELLER)).isTrue();
        assertThat(req.satisfiedBy(Role.ADMIN)).isTrue();
        assertThat(req.satisfiedBy(Role.ANONYMOUS)).isFalse();
    }

    @Test
    void chat_endpoints_are_anonymous() {
        assertThat(AuthMap.required("chat.page")).isEqualTo(Role.ANONYMOUS);
        assertThat(AuthMap.required("chat.recommend")).isEqualTo(Role.ANONYMOUS);
        assertThat(AuthMap.isAnonymous("chat.page")).isTrue();
        assertThat(AuthMap.isAnonymous("chat.recommend")).isTrue();
    }
}
