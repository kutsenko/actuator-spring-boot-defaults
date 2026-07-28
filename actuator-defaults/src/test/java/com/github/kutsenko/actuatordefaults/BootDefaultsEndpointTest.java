package com.github.kutsenko.actuatordefaults;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

        assertThat(endpoint.bootDefaults(), is(sameInstance(report)));
    }

    @Test
    void rejectsNullService() {
        var exception = assertThrows(NullPointerException.class, () -> new BootDefaultsEndpoint(null));
        assertThat(exception.getMessage(), is("service"));
    }
}
