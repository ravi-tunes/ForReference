# Book Cache and Lookup Service

This module implements an efficient and flexible strategy-based caching system for Book data, supporting both eager and lazy loading modes.

## ✅ Features

- **Pre-caching Strategy**:
  - Load all books (`history = -1`)
  - Load historical books based on `trade_date` (`history > 0`)
  - Skip caching (`enabled = false`)
- **On-demand DB Fetch**: If a book is not found in cache, it is fetched from DB and cached.
- **High Performance**: Uses FastUtil's Long2ObjectOpenHashMap for memory-efficient caching.
- **Config-Driven**: Controlled via `application.yml`.

## ⚙️ Configuration

```yaml
book-cache:
  pre-caching:
    enabled: true
    history: 30  # or -1 for full load
```

## 🧩 Component Overview

- `BookCacheProperties`: Maps config to POJO.
- `BookLoadStrategy`: Strategy interface for book loading.
- `AllBooksLoader`: Loads all books.
- `HistoricalBooksLoader`: Loads books within date range.
- `NoPreloadBookLoader`: Loads nothing.
- `BookRepository`: Interface with fallback methods.
- `BookRepositoryImpl`: JDBC implementation.
- `BookLookupService`: Central cache and lookup service.

## 🧪 Testing

- Unit tests with 100% coverage using JUnit 5 and Mockito.
- Integration test to verify end-to-end loading logic.

## 📦 Usage

Inject `BookLookupService` anywhere and call:
```java
Book book = bookLookupService.getBookById(123L);
```

## 🧰 Future Extensions

- Support for cache eviction or TTL
- Metrics for cache hit/miss
- Optional use of Caffeine for bounded cache
