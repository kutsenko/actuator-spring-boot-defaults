package com.github.kutsenko.actuatordefaults;

/**
 * SPI for a single dependency's defaults. Each implementation knows one dependency (a connection
 * pool, a driver, an HTTP client, …), determines that dependency's framework defaults and the
 * values actually in force, and returns them as a {@link DefaultsGroup}.
 *
 * <p>Implementations are registered as beans behind a {@code @ConditionalOnClass} guard, so a
 * contributor exists only when its dependency is on the classpath. {@link DefaultsService}
 * aggregates whichever contributors are present — that is how "dependency absent → no defaults"
 * is realised without any contributor knowing about the others.
 */
@FunctionalInterface
public interface DefaultsContributor {

    /** Determines this dependency's defaults and actual values. Called per endpoint invocation. */
    DefaultsGroup contribute();
}
