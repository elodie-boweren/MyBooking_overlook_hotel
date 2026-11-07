package com.MyBooking.common.security;

import com.MyBooking.common.config.CorsConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.jwt.JwtDecoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FailFastSecretStartupTest {

    private final SecurityConfig securityConfig = new SecurityConfig();

    @Test
    @DisplayName("jwtDecoder bean should throw IllegalStateException if secret is null or empty")
    void shouldFailWhenSecretIsNullOrEmpty() {
        assertThatThrownBy(() -> securityConfig.jwtDecoder(null))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("JWT_SECRET must be configured and be at least 256 bits (32 bytes)");

        assertThatThrownBy(() -> securityConfig.jwtDecoder(""))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("JWT_SECRET must be configured and be at least 256 bits (32 bytes)");

        assertThatThrownBy(() -> securityConfig.jwtDecoder("   "))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("JWT_SECRET must be configured and be at least 256 bits (32 bytes)");
    }

    @Test
    @DisplayName("jwtDecoder bean should throw IllegalStateException if secret is shorter than 32 bytes (256 bits)")
    void shouldFailWhenSecretIsTooShort() {
        // 11 bytes legacy weak secret
        String weakSecret = "mySecretKey";
        assertThatThrownBy(() -> securityConfig.jwtDecoder(weakSecret))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("JWT_SECRET must be configured and be at least 256 bits (32 bytes)");

        // 31 bytes (1 byte short of 256 bits)
        String almostSecret = "1234567890123456789012345678901";
        assertThat(almostSecret.getBytes().length).isEqualTo(31);
        assertThatThrownBy(() -> securityConfig.jwtDecoder(almostSecret))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("JWT_SECRET must be configured and be at least 256 bits (32 bytes)");
    }

    @Test
    @DisplayName("jwtDecoder bean should succeed when secret is exactly 32 bytes or longer")
    void shouldSucceedWhenSecretIsAtLeast32Bytes() {
        // Exactly 32 bytes (256 bits)
        String valid32ByteSecret = "12345678901234567890123456789012";
        assertThat(valid32ByteSecret.getBytes().length).isEqualTo(32);
        JwtDecoder decoder = securityConfig.jwtDecoder(valid32ByteSecret);
        assertThat(decoder).isNotNull();

        // 64 bytes (512 bits)
        String valid64ByteSecret = "1234567890123456789012345678901212345678901234567890123456789012";
        JwtDecoder decoder64 = securityConfig.jwtDecoder(valid64ByteSecret);
        assertThat(decoder64).isNotNull();
    }

    @Test
    @DisplayName("ApplicationContext should fail to start when jwt.secret is weak or missing")
    void applicationContextShouldFailFastOnWeakSecret() {
        new ApplicationContextRunner()
            .withUserConfiguration(SecurityConfig.class, CorsConfig.class)
            .withPropertyValues("jwt.secret=tooShortSecret")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasRootCauseInstanceOf(IllegalStateException.class)
                    .hasRootCauseMessage("JWT_SECRET must be configured and be at least 256 bits (32 bytes)");
            });
    }

    @Test
    @DisplayName("ApplicationContext should start successfully when jwt.secret is valid (>= 256 bits)")
    void applicationContextShouldStartWithValidSecret() {
        new ApplicationContextRunner()
            .withUserConfiguration(SecurityConfig.class, CorsConfig.class)
            .withPropertyValues("jwt.secret=aVerySecureKeyThatIsAtLeast32BytesLongForHmac256!")
            .run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(JwtDecoder.class);
                assertThat(context).hasSingleBean(JwtAuthenticationConverter.class);
            });
    }
}

