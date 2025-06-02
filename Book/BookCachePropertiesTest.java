package com.yourapp.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class BookCachePropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(TestConfiguration.class);

    @Test
    void shouldHaveDefaultValues() {
        contextRunner.run(context -> {
            BookCacheProperties properties = context.getBean(BookCacheProperties.class);
            assertThat(properties.isEnabled()).isFalse();
            assertThat(properties.getHistory()).isEqualTo(30);
        });
    }

    @Test
    void shouldBindConfigurationValues() {
        contextRunner
            .withPropertyValues(
                "book-cache.pre-caching.enabled=true",
                "book-cache.pre-caching.history=90"
            )
            .run(context -> {
                BookCacheProperties properties = context.getBean(BookCacheProperties.class);
                assertThat(properties.isEnabled()).isTrue();
                assertThat(properties.getHistory()).isEqualTo(90);
            });
    }

    @EnableConfigurationProperties(BookCacheProperties.class)
    static class TestConfiguration {}
}