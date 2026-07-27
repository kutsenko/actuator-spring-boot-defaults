package com.github.kutsenko.actuatordefaults;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class DefaultsServiceTest {

    private static final DefaultsGroup NON_EMPTY = new DefaultsGroup(
            "timeouts", "Some dependency", List.of(DefaultSetting.ofMillis("k", "p", 1L, 1L)));
    private static final DefaultsGroup EMPTY = new DefaultsGroup("timeouts", "Empty dependency", List.of());

    @Test
    void aggregatesEveryContributorGroup() {
        var service = new DefaultsService(List.of(() -> NON_EMPTY, () -> NON_EMPTY));

        var report = service.report();

        assertThat(report.groups()).hasSize(2).allSatisfy(g -> assertThat(g).isEqualTo(NON_EMPTY));
    }

    @Test
    void dropsEmptyGroups() {
        var service = new DefaultsService(List.of(() -> NON_EMPTY, () -> EMPTY));

        var report = service.report();

        assertThat(report.groups()).containsExactly(NON_EMPTY);
    }

    @Test
    void reportsNoGroupsWhenNoContributors() {
        var service = new DefaultsService(List.of());

        assertThat(service.report().groups()).isEmpty();
    }

    @Test
    void rejectsNullContributors() {
        assertThatThrownBy(() -> new DefaultsService(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("contributors");
    }
}
