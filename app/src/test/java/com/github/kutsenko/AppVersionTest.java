package com.github.kutsenko;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class AppVersionTest {

    @Test
    void parsesWellFormedVersion() {
        var version = AppVersion.of("4.0.7");
        assertThat(version.major()).isEqualTo(4);
        assertThat(version.minor()).isEqualTo(0);
        assertThat(version.patch()).isEqualTo(7);
        assertThat(version).hasToString("4.0.7");
    }

    @Test
    void rejectsNullValue() {
        assertThatThrownBy(() -> AppVersion.of(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("value");
    }

    @Test
    void rejectsWrongComponentCount() {
        assertThatThrownBy(() -> AppVersion.of("4.0"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MAJOR.MINOR.PATCH");
    }

    @Test
    void rejectsNegativeComponents() {
        assertThatThrownBy(() -> new AppVersion(1, -1, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("non-negative");
    }
}
