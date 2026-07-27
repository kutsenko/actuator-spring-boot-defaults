package com.github.kutsenko.actuatordefaults;

import java.util.Objects;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

/**
 * Actuator endpoint exposing the Spring Boot defaults and the values actually in force, as JSON
 * (actuator serialises the returned {@link DefaultsReport} via Jackson).
 *
 * <p>Reachable at {@code /actuator/bootdefaults} once the id is exposed, e.g.
 * {@code management.endpoints.web.exposure.include=bootdefaults}.
 *
 * <p>This class is intentionally a thin adapter: it holds no logic beyond delegating to
 * {@link DefaultsService}, keeping the actuator surface and the defaults-determination logic
 * separate.
 */
@Endpoint(id = "bootdefaults")
public class BootDefaultsEndpoint {

    private final DefaultsService service;

    public BootDefaultsEndpoint(DefaultsService service) {
        this.service = Objects.requireNonNull(service, "service");
    }

    @ReadOperation
    public DefaultsReport bootDefaults() {
        return service.report();
    }
}
