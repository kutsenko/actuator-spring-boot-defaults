package com.github.kutsenko.actuatordefaults;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Determines the effective defaults report by aggregating every {@link DefaultsContributor}
 * present in the application. This is the actual "business logic" of the library and is kept
 * deliberately separate from the actuator endpoint ({@link BootDefaultsEndpoint}) that exposes it,
 * so it can be unit-tested and reused without any actuator dependency.
 *
 * <p>The report is ordered so that <b>overridden values surface first</b> — the settings a
 * consumer has actually changed away from the Boot default are what matters most. Groups are
 * ordered by how many overridden settings they carry (most first), and within each group the
 * overridden settings come before the untouched ones. Ordering is otherwise stable, preserving
 * contributor/catalogue order among ties.
 */
public class DefaultsService {

    private static final Comparator<DefaultSetting> OVERRIDDEN_SETTINGS_FIRST =
            Comparator.comparing(setting -> setting.overridden() ? 0 : 1);

    private static final Comparator<DefaultsGroup> MOST_OVERRIDES_FIRST =
            Comparator.comparingLong(DefaultsService::overriddenCount).reversed();

    private final List<DefaultsContributor> contributors;

    public DefaultsService(List<DefaultsContributor> contributors) {
        this.contributors = List.copyOf(Objects.requireNonNull(contributors, "contributors"));
    }

    /**
     * Collects each contributor's group, dropping any that turned out empty (a contributor whose
     * dependency is present but that could determine no settings), then orders both groups and
     * their settings with overridden values first.
     */
    public DefaultsReport report() {
        var groups = contributors.stream()
                .map(DefaultsContributor::contribute)
                .filter(group -> !group.settings().isEmpty())
                .map(DefaultsService::withOverriddenSettingsFirst)
                .sorted(MOST_OVERRIDES_FIRST)
                .toList();
        return new DefaultsReport(groups);
    }

    private static DefaultsGroup withOverriddenSettingsFirst(DefaultsGroup group) {
        var ordered = group.settings().stream().sorted(OVERRIDDEN_SETTINGS_FIRST).toList();
        return new DefaultsGroup(group.category(), group.dependency(), ordered);
    }

    private static long overriddenCount(DefaultsGroup group) {
        return group.settings().stream().filter(DefaultSetting::overridden).count();
    }
}
