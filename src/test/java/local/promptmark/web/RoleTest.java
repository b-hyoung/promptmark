package local.promptmark.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RoleTest {

    @Test
    void hierarchy_admin_satisfies_everything() {
        assertThat(Role.USER.satisfiedBy(Role.ADMIN)).isTrue();
        assertThat(Role.SELLER.satisfiedBy(Role.ADMIN)).isTrue();
        assertThat(Role.ADMIN.satisfiedBy(Role.ADMIN)).isTrue();
        assertThat(Role.ANONYMOUS.satisfiedBy(Role.ADMIN)).isTrue();
    }

    @Test
    void hierarchy_seller_does_not_satisfy_admin() {
        assertThat(Role.ADMIN.satisfiedBy(Role.SELLER)).isFalse();
        assertThat(Role.SELLER.satisfiedBy(Role.SELLER)).isTrue();
        assertThat(Role.USER.satisfiedBy(Role.SELLER)).isTrue();
    }

    @Test
    void hierarchy_anonymous_only_satisfies_anonymous_requirement() {
        assertThat(Role.ANONYMOUS.satisfiedBy(Role.ANONYMOUS)).isTrue();
        assertThat(Role.USER.satisfiedBy(Role.ANONYMOUS)).isFalse();
        assertThat(Role.SELLER.satisfiedBy(Role.ANONYMOUS)).isFalse();
        assertThat(Role.ADMIN.satisfiedBy(Role.ANONYMOUS)).isFalse();
    }

    @Test
    void satisfiedBy_treats_null_as_anonymous() {
        assertThat(Role.ANONYMOUS.satisfiedBy(null)).isTrue();
        assertThat(Role.USER.satisfiedBy(null)).isFalse();
    }

    @Test
    void fromDb_maps_known_values() {
        assertThat(Role.fromDb("ADMIN")).isEqualTo(Role.ADMIN);
        assertThat(Role.fromDb("seller")).isEqualTo(Role.SELLER);
        assertThat(Role.fromDb(" user ")).isEqualTo(Role.USER);
    }

    @Test
    void fromDb_falls_back_to_user_on_garbage() {
        assertThat(Role.fromDb(null)).isEqualTo(Role.USER);
        assertThat(Role.fromDb("")).isEqualTo(Role.USER);
        assertThat(Role.fromDb("Wizard")).isEqualTo(Role.USER);
    }
}
