package com.github.kutsenko.actuatordefaults.contributor;

import com.github.kutsenko.actuatordefaults.DefaultSetting;
import com.github.kutsenko.actuatordefaults.DefaultsContributor;
import com.github.kutsenko.actuatordefaults.DefaultsGroup;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.core.env.Environment;

/**
 * Timeout defaults for Spring's blocking HTTP clients ({@code RestClient} / {@code RestTemplate}),
 * driven by {@code spring.http.client.connect-timeout} and {@code spring.http.client.read-timeout}.
 *
 * <p>Unlike the pool/driver contributors, Spring Boot ships <em>no</em> numeric default here: when
 * these properties are unset the request factory falls back to the underlying HTTP client's own
 * default (typically no timeout). The default value is therefore reported as {@code null} with an
 * explanatory note, while the actual value reflects whatever the application configured.
 */
public class HttpClientTimeoutDefaultsContributor implements DefaultsContributor {

    private static final String UNSET_NOTE =
            "Spring Boot leaves this unset; the underlying HTTP client's own default applies (usually no timeout).";

    private final Environment environment;

    public HttpClientTimeoutDefaultsContributor(Environment environment) {
        this.environment = Objects.requireNonNull(environment, "environment");
    }

    @Override
    public DefaultsGroup contribute() {
        var settings = List.of(
                setting("connect-timeout", "spring.http.client.connect-timeout"),
                setting("read-timeout", "spring.http.client.read-timeout"));
        return new DefaultsGroup("timeouts", "Spring HTTP client (RestClient / RestTemplate)", settings);
    }

    private DefaultSetting setting(String key, String property) {
        var actualMillis = millis(environment.getProperty(property));
        return DefaultSetting.ofMillis(key, property, null, actualMillis, UNSET_NOTE);
    }

    /** Parses a Boot-style duration ({@code "5s"}, {@code "5000ms"}, {@code "PT5S"}, {@code "5000"}). */
    private static Long millis(String value) {
        if (value == null || value.isBlank())
            return null;
        Duration duration = DurationStyle.detectAndParse(value.trim());
        return duration.toMillis();
    }
}
