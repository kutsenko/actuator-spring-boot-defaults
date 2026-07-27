package com.github.kutsenko.actuatordefaults;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import java.util.Objects;

/**
 * A single configurable value, reported as both its framework/library default and the value
 * effectively in force in the running application.
 *
 * @param key          short identifier, e.g. {@code "connection-timeout"}
 * @param property     the configuration property or driver setting it maps to, e.g.
 *                     {@code "spring.datasource.hikari.connection-timeout"}
 * @param unit         the unit of {@code defaultValue}/{@code actualValue}, e.g. {@code "ms"}
 * @param defaultValue the framework/library default; {@code null} when the framework leaves it
 *                     unset (the underlying component's own default then applies)
 * @param actualValue  the value in force; {@code null} when nothing is configured (no such bean)
 * @param overridden   whether {@code actualValue} is set and differs from {@code defaultValue} —
 *                     the flag that tells the user, without comparing, that this value was changed
 * @param note         optional human-readable clarification; {@code null} when none applies
 */
@JsonPropertyOrder({"key", "property", "overridden", "defaultValue", "actualValue", "unit", "note"})
public record DefaultSetting(
        String key,
        String property,
        String unit,
        Long defaultValue,
        Long actualValue,
        boolean overridden,
        String note) {

    public DefaultSetting {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(property, "property");
        Objects.requireNonNull(unit, "unit");
    }

    /**
     * Builds a millisecond-valued setting, deriving {@code overridden} = "actual is set and
     * differs from the default".
     */
    public static DefaultSetting ofMillis(String key, String property, Long defaultMillis, Long actualMillis) {
        return ofMillis(key, property, defaultMillis, actualMillis, null);
    }

    /** As {@link #ofMillis(String, String, Long, Long)} but attaches an explanatory {@code note}. */
    public static DefaultSetting ofMillis(
            String key, String property, Long defaultMillis, Long actualMillis, String note) {
        var overridden = actualMillis != null && !actualMillis.equals(defaultMillis);
        return new DefaultSetting(key, property, "ms", defaultMillis, actualMillis, overridden, note);
    }
}
