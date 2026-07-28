package com.github.kutsenko.actuatordefaults;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    private static List<String> dependencies(DefaultsService service) {
        return service.report().groups().stream().map(DefaultsGroup::dependency).toList();
    }

    @Test
    void aggregatesEveryContributorGroup() {
        var a = group("A", unchanged("x"));
        var b = group("B", unchanged("y"));
        var service = new DefaultsService(List.of(() -> a, () -> b));

        assertThat(service.report().groups(), containsInAnyOrder(a, b));
    }

    @Test
    void dropsEmptyGroups() {
        var nonEmpty = group("A", unchanged("x"));
        var empty = group("Empty");
        var service = new DefaultsService(List.of(() -> nonEmpty, () -> empty));

        assertThat(service.report().groups(), contains(nonEmpty));
    }

    @Test
    void ordersGroupsByOverriddenCountDescending() {
        var none = group("none", unchanged("a"));
        var one = group("one", changed("b"), unchanged("c"));
        var two = group("two", changed("d"), changed("e"));
        // Deliberately supplied in the "wrong" order to prove the service reorders.
        var service = new DefaultsService(List.of(() -> none, () -> two, () -> one));

        assertThat(dependencies(service), contains("two", "one", "none"));
    }

    @Test
    void ordersOverriddenSettingsFirstWithinAGroup() {
        var mixed = group("mixed", unchanged("keep"), changed("override"), unchanged("keep2"));
        var service = new DefaultsService(List.of(() -> mixed));

        var settings = service.report().groups().getFirst().settings();

        assertThat(settings.getFirst().overridden(), is(true));
        assertThat(settings.stream().map(DefaultSetting::key).toList(), contains("override", "keep", "keep2"));
    }

    @Test
    void keepsInsertionOrderAmongGroupsWithEqualOverrideCount() {
        var first = group("first", unchanged("a"));
        var second = group("second", unchanged("b"));
        var service = new DefaultsService(List.of(() -> first, () -> second));

        assertThat(dependencies(service), contains("first", "second"));
    }

    @Test
    void surfacesOverrideFlagsAtGroupAndReportLevel() {
        var changedGroup = group("changed", changed("a"), unchanged("b"));
        var cleanGroup = group("clean", unchanged("c"));
        var service = new DefaultsService(List.of(() -> changedGroup, () -> cleanGroup));

        var report = service.report();

        assertThat(report.hasOverrides(), is(true));
        assertThat(report.overriddenCount(), is(1L));

        var first = report.groups().getFirst();
        assertThat(first.dependency(), is("changed"));
        assertThat(first.hasOverrides(), is(true));
        assertThat(first.overriddenCount(), is(1L));

        var last = report.groups().getLast();
        assertThat(last.hasOverrides(), is(false));
        assertThat(last.overriddenCount(), is(0L));
    }

    @Test
    void reportHasNoOverridesWhenEverythingIsDefault() {
        var report = new DefaultsService(List.of(() -> group("a", unchanged("x")))).report();

        assertThat(report.hasOverrides(), is(false));
        assertThat(report.overriddenCount(), is(0L));
    }

    @Test
    void reportsNoGroupsWhenNoContributors() {
        assertThat(new DefaultsService(List.of()).report().groups(), is(empty()));
    }

    @Test
    void rejectsNullContributors() {
        var exception = assertThrows(NullPointerException.class, () -> new DefaultsService(null));
        assertThat(exception.getMessage(), is("contributors"));
    }
}
