package com.github.kutsenko.actuatordefaults;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class DefaultSettingTest {

    @Test
    void marksSettingOverriddenWhenActualDiffersFromDefault() {
        var setting = DefaultSetting.ofMillis("connection-timeout", "some.property", 30000L, 45000L);

        assertThat(setting.overridden()).isTrue();
        assertThat(setting.defaultValue()).isEqualTo(30000L);
        assertThat(setting.actualValue()).isEqualTo(45000L);
        assertThat(setting.unit()).isEqualTo("ms");
    }

    @Test
    void notOverriddenWhenActualEqualsDefault() {
        var setting = DefaultSetting.ofMillis("connection-timeout", "some.property", 30000L, 30000L);

        assertThat(setting.overridden()).isFalse();
    }

    @Test
    void notOverriddenWhenActualIsUnset() {
        var setting = DefaultSetting.ofMillis("connection-timeout", "some.property", 30000L, null);

        assertThat(setting.overridden()).isFalse();
        assertThat(setting.actualValue()).isNull();
    }

    @Test
    void overriddenWhenFrameworkHasNoDefaultButValueIsConfigured() {
        var setting = DefaultSetting.ofMillis("read-timeout", "spring.http.client.read-timeout", null, 5000L);

        assertThat(setting.overridden()).isTrue();
        assertThat(setting.defaultValue()).isNull();
    }

    @Test
    void attachesNote() {
        var setting = DefaultSetting.ofMillis("read-timeout", "prop", null, null, "explains the default");

        assertThat(setting.note()).isEqualTo("explains the default");
        assertThat(setting.overridden()).isFalse();
    }

    @Test
    void rejectsNullMandatoryFields() {
        assertThatThrownBy(() -> DefaultSetting.ofMillis(null, "prop", 1L, 1L))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("key");
        assertThatThrownBy(() -> DefaultSetting.ofMillis("key", null, 1L, 1L))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("property");
    }
}
