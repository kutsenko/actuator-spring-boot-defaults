package com.github.kutsenko.actuatordefaults.contributor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.kutsenko.actuatordefaults.DefaultSetting;
import com.zaxxer.hikari.HikariDataSource;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class HikariTimeoutDefaultsContributorTest {

    @Test
    void reportsHikariOwnDefaultsAndFlagsOverrides() {
        try (var dataSource = new HikariDataSource()) {
            dataSource.setConnectionTimeout(45_000); // differs from Hikari's 30 000 ms default

            var group = new HikariTimeoutDefaultsContributor(provider(dataSource)).contribute();

            assertThat(group.category()).isEqualTo("timeouts");
            assertThat(group.dependency()).contains("HikariCP");

            var connectionTimeout = settingByKey(group.settings(), "connection-timeout");
            assertThat(connectionTimeout.defaultValue()).isEqualTo(30_000L);
            assertThat(connectionTimeout.actualValue()).isEqualTo(45_000L);
            assertThat(connectionTimeout.overridden()).isTrue();

            // A value left at Hikari's default must NOT be flagged as overridden.
            var idleTimeout = settingByKey(group.settings(), "idle-timeout");
            assertThat(idleTimeout.actualValue()).isEqualTo(idleTimeout.defaultValue());
            assertThat(idleTimeout.overridden()).isFalse();
        }
    }

    @Test
    void reportsDefaultsWithNullActualsWhenNoDataSourceConfigured() {
        var group = new HikariTimeoutDefaultsContributor(provider()).contribute();

        assertThat(group.settings()).isNotEmpty().allSatisfy(setting -> {
            assertThat(setting.defaultValue()).isNotNull();
            assertThat(setting.actualValue()).isNull();
            assertThat(setting.overridden()).isFalse();
        });
    }

    private static DefaultSetting settingByKey(java.util.List<DefaultSetting> settings, String key) {
        return settings.stream().filter(s -> s.key().equals(key)).findFirst().orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<HikariDataSource> provider(HikariDataSource... dataSources) {
        ObjectProvider<HikariDataSource> provider = mock(ObjectProvider.class);
        when(provider.stream()).thenReturn(Stream.of(dataSources));
        return provider;
    }
}
