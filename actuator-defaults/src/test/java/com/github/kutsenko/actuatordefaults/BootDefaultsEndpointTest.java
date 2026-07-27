package com.github.kutsenko.actuatordefaults;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;

class BootDefaultsEndpointTest {

    @Test
    void delegatesToService() {
        var service = mock(DefaultsService.class);
        var report = new DefaultsReport(List.of());
        when(service.report()).thenReturn(report);

        var endpoint = new BootDefaultsEndpoint(service);

        assertThat(endpoint.bootDefaults()).isSameAs(report);
    }

    @Test
    void rejectsNullService() {
        assertThatThrownBy(() -> new BootDefaultsEndpoint(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("service");
    }
}
