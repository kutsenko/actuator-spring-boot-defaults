package com.github.kutsenko.actuatordefaults.contributor;

import com.github.kutsenko.actuatordefaults.DefaultSetting;
import com.github.kutsenko.actuatordefaults.DefaultsContributor;
import com.github.kutsenko.actuatordefaults.DefaultsGroup;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Timeout defaults for the HikariCP JDBC connection pool (the pool Spring Boot uses for
 * PostgreSQL, MySQL, and every other JDBC database).
 *
 * <p>The framework defaults are read from a fresh {@link HikariConfig} rather than hard-coded, so
 * they track the HikariCP version the consumer actually ships. The values in force are read from
 * the running {@link HikariDataSource}; when the application configures no data source the actual
 * values are reported as {@code null}.
 */
public class HikariTimeoutDefaultsContributor implements DefaultsContributor {

    private final ObjectProvider<HikariDataSource> dataSources;

    public HikariTimeoutDefaultsContributor(ObjectProvider<HikariDataSource> dataSources) {
        this.dataSources = Objects.requireNonNull(dataSources, "dataSources");
    }

    @Override
    public DefaultsGroup contribute() {
        var defaults = new HikariConfig();
        var actual = dataSources.stream().findFirst().orElse(null);
        var settings = List.of(
                setting("connection-timeout", "spring.datasource.hikari.connection-timeout",
                        defaults.getConnectionTimeout(), actual, HikariConfig::getConnectionTimeout),
                setting("validation-timeout", "spring.datasource.hikari.validation-timeout",
                        defaults.getValidationTimeout(), actual, HikariConfig::getValidationTimeout),
                setting("idle-timeout", "spring.datasource.hikari.idle-timeout",
                        defaults.getIdleTimeout(), actual, HikariConfig::getIdleTimeout),
                setting("max-lifetime", "spring.datasource.hikari.max-lifetime",
                        defaults.getMaxLifetime(), actual, HikariConfig::getMaxLifetime),
                setting("keepalive-time", "spring.datasource.hikari.keepalive-time",
                        defaults.getKeepaliveTime(), actual, HikariConfig::getKeepaliveTime));
        return new DefaultsGroup("timeouts", "HikariCP (JDBC connection pool)", settings);
    }

    private static DefaultSetting setting(
            String key,
            String property,
            long defaultMillis,
            HikariDataSource actual,
            java.util.function.ToLongFunction<HikariConfig> reader) {
        var actualMillis = actual == null ? null : reader.applyAsLong(actual);
        return DefaultSetting.ofMillis(key, property, defaultMillis, actualMillis);
    }
}
