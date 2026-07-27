package com.github.kutsenko.actuatordefaults;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class DefaultsServiceTest {

    private static DefaultSetting unchanged(String key) {
        return DefaultSetting.ofMillis(key, "p." + key, 1_000L, 1_000L); // actual == default
    }

    private static DefaultSetting changed(String key) {
        return DefaultSetting.ofMillis(key, "p." + key, 1_000L, 2_000L); // actual != default
    }

    private static DefaultsGroup group(String dependency, DefaultSetting... settings) {
        return new DefaultsGroup("timeouts", dependency, List.of(settings));
    }

    @Test
    void aggregatesEveryContributorGroup() {
        var a = group("A", unchanged("x"));
        var b = group("B", unchanged("y"));
        var service = new DefaultsService(List.of(() -> a, () -> b));

        assertThat(service.report().groups()).containsExactlyInAnyOrder(a, b);
    }

    @Test
    void dropsEmptyGroups() {
        var nonEmpty = group("A", unchanged("x"));
        var empty = group("Empty");
        var service = new DefaultsService(List.of(() -> nonEmpty, () -> empty));

        assertThat(service.report().groups()).containsExactly(nonEmpty);
    }

    @Test
    void ordersGroupsByOverriddenCountDescending() {
        var none = group("none", unchanged("a"));
        var one = group("one", changed("b"), unchanged("c"));
        var two = group("two", changed("d"), changed("e"));
        // Deliberately supplied in the "wrong" order to prove the service reorders.
        var service = new DefaultsService(List.of(() -> none, () -> two, () -> one));

        assertThat(service.report().groups())
                .extracting(DefaultsGroup::dependency)
                .containsExactly("two", "one", "none");
    }

    @Test
    void ordersOverriddenSettingsFirstWithinAGroup() {
        var mixed = group("mixed", unchanged("keep"), changed("override"), unchanged("keep2"));
        var service = new DefaultsService(List.of(() -> mixed));

        var settings = service.report().groups().getFirst().settings();

        assertThat(settings).first().satisfies(s -> assertThat(s.overridden()).isTrue());
        assertThat(settings).extracting(DefaultSetting::key).containsExactly("override", "keep", "keep2");
    }

    @Test
    void keepsInsertionOrderAmongGroupsWithEqualOverrideCount() {
        var first = group("first", unchanged("a"));
        var second = group("second", unchanged("b"));
        var service = new DefaultsService(List.of(() -> first, () -> second));

        assertThat(service.report().groups())
                .extracting(DefaultsGroup::dependency)
                .containsExactly("first", "second");
    }

    @Test
    void surfacesOverrideFlagsAtGroupAndReportLevel() {
        var changedGroup = group("changed", changed("a"), unchanged("b"));
        var cleanGroup = group("clean", unchanged("c"));
        var service = new DefaultsService(List.of(() -> changedGroup, () -> cleanGroup));

        var report = service.report();

        assertThat(report.hasOverrides()).isTrue();
        assertThat(report.overriddenCount()).isEqualTo(1);

        var first = report.groups().getFirst();
        assertThat(first.dependency()).isEqualTo("changed");
        assertThat(first.hasOverrides()).isTrue();
        assertThat(first.overriddenCount()).isEqualTo(1);

        var last = report.groups().getLast();
        assertThat(last.hasOverrides()).isFalse();
        assertThat(last.overriddenCount()).isZero();
    }

    @Test
    void reportHasNoOverridesWhenEverythingIsDefault() {
        var report = new DefaultsService(List.of(() -> group("a", unchanged("x")))).report();

        assertThat(report.hasOverrides()).isFalse();
        assertThat(report.overriddenCount()).isZero();
    }

    @Test
    void reportsNoGroupsWhenNoContributors() {
        assertThat(new DefaultsService(List.of()).report().groups()).isEmpty();
    }

    @Test
    void rejectsNullContributors() {
        assertThatThrownBy(() -> new DefaultsService(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("contributors");
    }
}
