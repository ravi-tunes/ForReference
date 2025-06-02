package com.yourapp.books;

import com.yourapp.books.model.Book;
import com.yourapp.books.repository.BookRepository;
import com.yourapp.books.service.BookLookupService;
import com.yourapp.config.BookCacheProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@Import(TestConfig.class)
@TestPropertySource(properties = {
    "book-cache.pre-caching.enabled=true",
    "book-cache.pre-caching.history=-1"
})
class BookIntegrationTest {

    @Autowired
    private BookLookupService lookupService;

    @MockBean
    private BookRepository bookRepository;

    @Test
    void shouldPreCacheAllBooksOnStartup() {
        // Setup
        Book book1 = new Book(1L, "Book1", LocalDate.now());
        Book book2 = new Book(2L, "Book2", LocalDate.now().minusDays(10));
        
        when(bookRepository.findAll()).thenReturn(Arrays.asList(book1, book2));

        // Verify pre-caching
        assertEquals(2, lookupService.getCacheSize());
        
        // Test cache hit
        Book result1 = lookupService.getBookById(1L);
        assertEquals(book1, result1);
        
        // Test cache miss with DB fallback
        Book book3 = new Book(3L, "Book3", LocalDate.now());
        when(bookRepository.findById(3L)).thenReturn(book3);
        Book result3 = lookupService.getBookById(3L);
        assertEquals(book3, result3);
        
        // Verify cache size increased
        assertEquals(3, lookupService.getCacheSize());
        
        // Test not found
        Book result4 = lookupService.getBookById(99L);
        assertNull(result4);
    }
}

// Test Configuration
class TestConfig {
    @Bean
    public BookCacheProperties bookCacheProperties() {
        BookCacheProperties properties = new BookCacheProperties();
        properties.setEnabled(true);
        properties.setHistory(-1);
        return properties;
    }
}