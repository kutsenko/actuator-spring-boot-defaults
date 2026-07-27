package com.github.kutsenko.actuatordefaults;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.List;

/**
 * The full report returned by the endpoint: one {@link DefaultsGroup} per dependency for which
 * defaults could be determined. Dependencies absent from the application do not appear.
 *
 * <p>{@link #hasOverrides()} / {@link #overriddenCount()} let a caller see at the top of the
 * response whether — and how much — the application diverges from the Boot defaults, without
 * scanning every setting.
 *
 * @param groups the groups; never {@code null}, may be empty when no supported dependency is present
 */
@JsonPropertyOrder({"hasOverrides", "overriddenCount", "groups"})
public record DefaultsReport(List<DefaultsGroup> groups) {

    public DefaultsReport {
        groups = List.copyOf(groups);
    }

    /** Total number of changed values across all groups. */
    @JsonProperty
    public long overriddenCount() {
        return groups.stream().mapToLong(DefaultsGroup::overriddenCount).sum();
    }

    /** Report-level flag: does the application override any Boot default at all? */
    @JsonProperty
    public boolean hasOverrides() {
        return overriddenCount() > 0;
    }
}
