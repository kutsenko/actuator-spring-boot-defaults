package com.github.kutsenko.actuatordefaults;

import java.util.List;
import java.util.Objects;

/**
 * A named group of {@link DefaultSetting}s belonging to one dependency and category, e.g. the
 * timeout settings of the HikariCP connection pool.
 *
 * @param category   the kind of setting, e.g. {@code "timeouts"}
 * @param dependency human-readable name of the dependency the settings belong to, e.g.
 *                   {@code "HikariCP (JDBC connection pool)"}
 * @param settings   the settings; never {@code null}, may be empty (an empty group is dropped by
 *                   {@link DefaultsService})
 */
public record DefaultsGroup(String category, String dependency, List<DefaultSetting> settings) {

    public DefaultsGroup {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(dependency, "dependency");
        settings = List.copyOf(settings);
    }
}
