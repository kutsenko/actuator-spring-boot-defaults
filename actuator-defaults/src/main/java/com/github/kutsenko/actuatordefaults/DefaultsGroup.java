package com.github.kutsenko.actuatordefaults;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
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
@JsonPropertyOrder({"dependency", "category", "hasOverrides", "overriddenCount", "settings"})
public record DefaultsGroup(String category, String dependency, List<DefaultSetting> settings) {

    public DefaultsGroup {
        Objects.requireNonNull(category, "category");
        Objects.requireNonNull(dependency, "dependency");
        settings = List.copyOf(settings);
    }

    /** How many settings in this group have been changed away from their default. */
    @JsonProperty
    public long overriddenCount() {
        return settings.stream().filter(DefaultSetting::overridden).count();
    }

    /** Group-level flag: does this dependency have any changed value? */
    @JsonProperty
    public boolean hasOverrides() {
        return overriddenCount() > 0;
    }
}
