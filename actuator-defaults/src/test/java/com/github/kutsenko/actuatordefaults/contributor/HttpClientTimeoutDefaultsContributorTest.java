package com.github.kutsenko.actuatordefaults.contributor;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.kutsenko.actuatordefaults.DefaultSetting;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class HttpClientTimeoutDefaultsContributorTest {

    @Test
    void parsesConfiguredDurationsAndReportsNoFrameworkDefault() {
        var environment = new MockEnvironment()
                .withProperty("spring.http.client.connect-timeout", "5s")
                .withProperty("spring.http.client.read-timeout", "500ms");

        var group = new HttpClientTimeoutDefaultsContributor(environment).contribute();

        var connect = settingByKey(group.settings(), "connect-timeout");
        assertThat(connect.defaultValue()).isNull();      // Spring Boot ships no numeric default
        assertThat(connect.actualValue()).isEqualTo(5_000L);
        assertThat(connect.overridden()).isTrue();
        assertThat(connect.note()).isNotBlank();

        var read = settingByKey(group.settings(), "read-timeout");
        assertThat(read.actualValue()).isEqualTo(500L);
    }

    @Test
    void reportsNullActualsWhenNothingConfigured() {
        var group = new HttpClientTimeoutDefaultsContributor(new MockEnvironment()).contribute();

        assertThat(group.settings()).isNotEmpty().allSatisfy(setting -> {
            assertThat(setting.defaultValue()).isNull();
            assertThat(setting.actualValue()).isNull();
            assertThat(setting.overridden()).isFalse();
        });
    }

    private static DefaultSetting settingByKey(List<DefaultSetting> settings, String key) {
        return settings.stream().filter(s -> s.key().equals(key)).findFirst().orElseThrow();
    }
}
