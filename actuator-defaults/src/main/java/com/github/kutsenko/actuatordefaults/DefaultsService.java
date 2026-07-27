package com.github.kutsenko.actuatordefaults;

import java.util.List;
import java.util.Objects;

/**
 * Determines the effective defaults report by aggregating every {@link DefaultsContributor}
 * present in the application. This is the actual "business logic" of the library and is kept
 * deliberately separate from the actuator endpoint ({@link BootDefaultsEndpoint}) that exposes it,
 * so it can be unit-tested and reused without any actuator dependency.
 */
public class DefaultsService {

    private final List<DefaultsContributor> contributors;

    public DefaultsService(List<DefaultsContributor> contributors) {
        this.contributors = List.copyOf(Objects.requireNonNull(contributors, "contributors"));
    }

    /**
     * Collects each contributor's group, dropping any that turned out empty (a contributor whose
     * dependency is present but that could determine no settings).
     */
    public DefaultsReport report() {
        var groups = contributors.stream()
                .map(DefaultsContributor::contribute)
                .filter(group -> !group.settings().isEmpty())
                .toList();
        return new DefaultsReport(groups);
    }
}
