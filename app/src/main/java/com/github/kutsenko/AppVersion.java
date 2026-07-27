package com.github.kutsenko;

import java.util.Objects;

/**
 * Semantic application version ({@code MAJOR.MINOR.PATCH}). A seed value type that
 * demonstrates the project conventions: a record for the value object, {@code of()}
 * for wrapping an existing value, {@code Objects.requireNonNull} at the boundary, and
 * fail-fast validation (an unparseable version throws loudly rather than defaulting).
 */
public record AppVersion(int major, int minor, int patch) {

    public AppVersion {
        if (major < 0 || minor < 0 || patch < 0)
            throw new IllegalArgumentException("version components must be non-negative");
    }

    /** Parses {@code "MAJOR.MINOR.PATCH"}; throws {@link IllegalArgumentException} on any other shape. */
    public static AppVersion of(String value) {
        Objects.requireNonNull(value, "value");
        var parts = value.split("\\.");
        if (parts.length != 3)
            throw new IllegalArgumentException("expected MAJOR.MINOR.PATCH, got: " + value);
        return new AppVersion(
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]));
    }

    @Override
    public String toString() {
        return major + "." + minor + "." + patch;
    }
}
