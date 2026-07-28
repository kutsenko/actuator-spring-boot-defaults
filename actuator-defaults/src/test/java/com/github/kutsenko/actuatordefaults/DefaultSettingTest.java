package com.github.kutsenko.actuatordefaults;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class DefaultSettingTest {

    @Test
    void marksSettingOverriddenWhenActualDiffersFromDefault() {
        var setting = DefaultSetting.ofMillis("connection-timeout", "some.property", 30000L, 45000L);

        assertThat(setting.overridden(), is(true));
        assertThat(setting.defaultValue(), is(30_000L));
        assertThat(setting.actualValue(), is(45_000L));
        assertThat(setting.unit(), is("ms"));
    }

    @Test
    void notOverriddenWhenActualEqualsDefault() {
        var setting = DefaultSetting.ofMillis("connection-timeout", "some.property", 30000L, 30000L);

        assertThat(setting.overridden(), is(false));
    }

    @Test
    void notOverriddenWhenActualIsUnset() {
        var setting = DefaultSetting.ofMillis("connection-timeout", "some.property", 30000L, null);

        assertThat(setting.overridden(), is(false));
        assertThat(setting.actualValue(), is(nullValue()));
    }

    @Test
    void overriddenWhenFrameworkHasNoDefaultButValueIsConfigured() {
        var setting = DefaultSetting.ofMillis("read-timeout", "spring.http.client.read-timeout", null, 5000L);

        assertThat(setting.overridden(), is(true));
        assertThat(setting.defaultValue(), is(nullValue()));
    }

    @Test
    void attachesNote() {
        var setting = DefaultSetting.ofMillis("read-timeout", "prop", null, null, "explains the default");

        assertThat(setting.note(), is("explains the default"));
        assertThat(setting.overridden(), is(false));
    }

    @Test
    void rejectsNullMandatoryFields() {
        var missingKey = assertThrows(NullPointerException.class,
                () -> DefaultSetting.ofMillis(null, "prop", 1L, 1L));
        assertThat(missingKey.getMessage(), is("key"));

        var missingProperty = assertThrows(NullPointerException.class,
                () -> DefaultSetting.ofMillis("key", null, 1L, 1L));
        assertThat(missingProperty.getMessage(), is("property"));
    }
}
