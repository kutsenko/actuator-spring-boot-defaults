package com.github.kutsenko.actuatordefaults.contributor;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.kutsenko.actuatordefaults.DefaultSetting;
import com.zaxxer.hikari.HikariDataSource;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class HikariTimeoutDefaultsContributorTest {

    @Test
    void reportsHikariOwnDefaultsAndFlagsOverrides() {
        try (var dataSource = new HikariDataSource()) {
            dataSource.setConnectionTimeout(45_000); // differs from Hikari's 30 000 ms default

            var group = new HikariTimeoutDefaultsContributor(provider(dataSource)).contribute();

            assertThat(group.category(), is("timeouts"));
            assertThat(group.dependency(), containsString("HikariCP"));

            var connectionTimeout = settingByKey(group.settings(), "connection-timeout");
            assertThat(connectionTimeout.defaultValue(), is(30_000L));
            assertThat(connectionTimeout.actualValue(), is(45_000L));
            assertThat(connectionTimeout.overridden(), is(true));

            // A value left at Hikari's default must NOT be flagged as overridden.
            var idleTimeout = settingByKey(group.settings(), "idle-timeout");
            assertThat(idleTimeout.actualValue(), is(idleTimeout.defaultValue()));
            assertThat(idleTimeout.overridden(), is(false));
        }
    }

    @Test
    void reportsDefaultsWithNullActualsWhenNoDataSourceConfigured() {
        var group = new HikariTimeoutDefaultsContributor(provider()).contribute();

        assertThat(group.settings(), is(not(empty())));
        for (var setting : group.settings()) {
            assertThat(setting.defaultValue(), is(notNullValue()));
            assertThat(setting.actualValue(), is(nullValue()));
            assertThat(setting.overridden(), is(false));
        }
    }

    private static DefaultSetting settingByKey(List<DefaultSetting> settings, String key) {
        return settings.stream().filter(s -> s.key().equals(key)).findFirst().orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<HikariDataSource> provider(HikariDataSource... dataSources) {
        ObjectProvider<HikariDataSource> provider = mock(ObjectProvider.class);
        when(provider.stream()).thenReturn(Stream.of(dataSources));
        return provider;
    }
}
