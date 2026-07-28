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

        assertThat(group.dependency(), containsString("MongoDB"));

        var connectTimeout = settingByKey(group.settings(), "connect-timeout");
        assertThat(connectTimeout.defaultValue(), is(10_000L)); // driver default
        assertThat(connectTimeout.actualValue(), is(2_500L));
        assertThat(connectTimeout.overridden(), is(true));

        var serverSelection = settingByKey(group.settings(), "server-selection-timeout");
        assertThat(serverSelection.actualValue(), is(serverSelection.defaultValue()));
        assertThat(serverSelection.overridden(), is(false));
    }

    @Test
    void reportsDefaultsWithNullActualsWhenNoSettingsBean() {
        var group = new MongoTimeoutDefaultsContributor(provider()).contribute();

        assertThat(group.settings(), is(not(empty())));
        for (var setting : group.settings()) {
            assertThat(setting.defaultValue(), is(notNullValue()));
            assertThat(setting.actualValue(), is(nullValue()));
        }
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
