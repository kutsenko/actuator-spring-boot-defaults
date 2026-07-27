package com.github.kutsenko.actuatordefaults.contributor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.github.kutsenko.actuatordefaults.DefaultSetting;
import com.mongodb.MongoClientSettings;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class MongoTimeoutDefaultsContributorTest {

    @Test
    void reportsDriverDefaultsAndFlagsOverrides() {
        var actual = MongoClientSettings.builder()
                .applyToSocketSettings(b -> b.connectTimeout(2_500, TimeUnit.MILLISECONDS))
                .build();

        var group = new MongoTimeoutDefaultsContributor(provider(actual)).contribute();

        assertThat(group.dependency()).contains("MongoDB");

        var connectTimeout = settingByKey(group.settings(), "connect-timeout");
        assertThat(connectTimeout.defaultValue()).isEqualTo(10_000L); // driver default
        assertThat(connectTimeout.actualValue()).isEqualTo(2_500L);
        assertThat(connectTimeout.overridden()).isTrue();

        var serverSelection = settingByKey(group.settings(), "server-selection-timeout");
        assertThat(serverSelection.actualValue()).isEqualTo(serverSelection.defaultValue());
        assertThat(serverSelection.overridden()).isFalse();
    }

    @Test
    void reportsDefaultsWithNullActualsWhenNoSettingsBean() {
        var group = new MongoTimeoutDefaultsContributor(provider()).contribute();

        assertThat(group.settings()).isNotEmpty().allSatisfy(setting -> {
            assertThat(setting.defaultValue()).isNotNull();
            assertThat(setting.actualValue()).isNull();
        });
    }

    private static DefaultSetting settingByKey(List<DefaultSetting> settings, String key) {
        return settings.stream().filter(s -> s.key().equals(key)).findFirst().orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private static ObjectProvider<MongoClientSettings> provider(MongoClientSettings settings) {
        ObjectProvider<MongoClientSettings> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(settings);
        return provider;
    }

    private static ObjectProvider<MongoClientSettings> provider() {
        return provider((MongoClientSettings) null);
    }
}
