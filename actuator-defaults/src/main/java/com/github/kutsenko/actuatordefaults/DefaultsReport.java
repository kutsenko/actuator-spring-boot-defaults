package com.github.kutsenko.actuatordefaults;

import java.util.List;

/**
 * The full report returned by the endpoint: one {@link DefaultsGroup} per dependency for which
 * defaults could be determined. Dependencies absent from the application do not appear.
 *
 * @param groups the groups; never {@code null}, may be empty when no supported dependency is present
 */
public record DefaultsReport(List<DefaultsGroup> groups) {

    public DefaultsReport {
        groups = List.copyOf(groups);
    }
}
