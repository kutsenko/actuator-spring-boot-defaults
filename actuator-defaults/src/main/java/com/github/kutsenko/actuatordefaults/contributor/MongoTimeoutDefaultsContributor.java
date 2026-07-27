package com.github.kutsenko.actuatordefaults.contributor;

import com.github.kutsenko.actuatordefaults.DefaultSetting;
import com.github.kutsenko.actuatordefaults.DefaultsContributor;
import com.github.kutsenko.actuatordefaults.DefaultsGroup;
import com.mongodb.MongoClientSettings;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.ToLongFunction;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Timeout defaults for the MongoDB driver.
 *
 * <p>The framework defaults are read from a freshly built {@link MongoClientSettings} (the driver's
 * own defaults), and the values in force from the application's configured
 * {@code MongoClientSettings} bean — the object Spring Boot builds from {@code spring.data.mongodb.*}
 * and any {@code MongoClientSettingsBuilderCustomizer}. When no such bean exists the actual values
 * are reported as {@code null}.
 */
public class MongoTimeoutDefaultsContributor implements DefaultsContributor {

    private final ObjectProvider<MongoClientSettings> settings;

    public MongoTimeoutDefaultsContributor(ObjectProvider<MongoClientSettings> settings) {
        this.settings = Objects.requireNonNull(settings, "settings");
    }

    @Override
    public DefaultsGroup contribute() {
        var defaults = MongoClientSettings.builder().build();
        var actual = this.settings.getIfAvailable();
        var reported = List.of(
                setting("connect-timeout", "socketSettings.connectTimeout", defaults, actual,
                        s -> s.getSocketSettings().getConnectTimeout(TimeUnit.MILLISECONDS)),
                setting("read-timeout", "socketSettings.readTimeout", defaults, actual,
                        s -> s.getSocketSettings().getReadTimeout(TimeUnit.MILLISECONDS)),
                setting("server-selection-timeout", "clusterSettings.serverSelectionTimeout", defaults, actual,
                        s -> s.getClusterSettings().getServerSelectionTimeout(TimeUnit.MILLISECONDS)),
                setting("max-wait-time", "connectionPoolSettings.maxWaitTime", defaults, actual,
                        s -> s.getConnectionPoolSettings().getMaxWaitTime(TimeUnit.MILLISECONDS)));
        return new DefaultsGroup("timeouts", "MongoDB driver (MongoClientSettings)", reported);
    }

    private static DefaultSetting setting(
            String key,
            String property,
            MongoClientSettings defaults,
            MongoClientSettings actual,
            ToLongFunction<MongoClientSettings> reader) {
        var actualMillis = actual == null ? null : reader.applyAsLong(actual);
        return DefaultSetting.ofMillis(key, property, reader.applyAsLong(defaults), actualMillis);
    }
}
