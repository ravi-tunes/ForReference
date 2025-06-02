# Spring Wiring Guide for Book Caching Logic

## 📁 Component Configuration

### 1. `BookCacheProperties.java`
- Uses `@ConfigurationProperties(prefix = "book-cache.pre-caching")`
- Automatically populated from YAML
- Provides `enabled` and `history` settings

### 2. Strategy Beans
- Conditional loading via:
  - `@ConditionalOnProperty(name = "book-cache.pre-caching.enabled", havingValue = "true")`
  - `@ConditionalOnExpression(...)`
- Ensures only one strategy (`BookLoadStrategy`) is active

### 3. `BookRepositoryImpl`
- Implements DB access using `JdbcTemplate`
- Marked with `@Repository`
- Spring auto-wires `JdbcTemplate`

### 4. `BookLookupService`
- Marked with `@Service`
- Injects:
  - `BookRepository`
  - `BookLoadStrategy`
- Manages cache and DB fallback logic

---

## 🧪 Testing Setup

Ensure the following in your `src/test/resources/application-test.yml`:

```yaml
book-cache:
  pre-caching:
    enabled: true
    history: -1  # or any test-specific override

spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password:
  h2:
    console:
      enabled: true
  jpa:
    hibernate:
      ddl-auto: none
    show-sql: true
```

---

## 🧪 Integration Testing Notes

- Use `@SpringBootTest`
- Activate profile via:
  ```java
  @ActiveProfiles("test")
  ```
- Or pass via CLI: `--spring.profiles.active=test`

---

## 🧠 Reminder

Spring will auto-discover and wire everything based on the annotations and configuration properties you’ve defined.
