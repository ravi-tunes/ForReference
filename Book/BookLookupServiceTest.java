package com.yourapp.books.service;

import com.yourapp.books.model.Book;
import com.yourapp.books.repository.BookRepository;
import com.yourapp.books.strategy.BookLoadStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookLookupServiceTest {

    @Mock
    private BookLoadStrategy strategy;

    @Mock
    private BookRepository bookRepository;

    private BookLookupService service;

    @BeforeEach
    void setUp() {
        service = new BookLookupService(strategy, bookRepository);
    }

    @Test
    void constructor_shouldPreCacheBooks() {
        // Arrange
        Book book1 = new Book(); book1.setId(1L);
        Book book2 = new Book(); book2.setId(2L);
        when(strategy.loadBooks()).thenReturn(Arrays.asList(book1, book2));

        // Act
        BookLookupService newService = new BookLookupService(strategy, bookRepository);

        // Assert
        assertEquals(2, newService.getCacheSize());
    }

    @Test
    void getBookById_shouldReturnCachedBook() {
        // Arrange
        Book cachedBook = new Book(); cachedBook.setId(1L);
        when(strategy.loadBooks()).thenReturn(Collections.singletonList(cachedBook));
        service = new BookLookupService(strategy, bookRepository);

        // Act
        Book result = service.getBookById(1L);

        // Assert
        assertEquals(cachedBook, result);
        verify(bookRepository, never()).findById(anyLong());
    }

    @Test
    void getBookById_shouldLoadFromDbWhenMissing() {
        // Arrange
        Book dbBook = new Book(); dbBook.setId(2L);
        when(bookRepository.findById(2L)).thenReturn(dbBook);

        // Act
        Book result = service.getBookById(2L);

        // Assert
        assertEquals(dbBook, result);
        assertEquals(1, service.getCacheSize());
    }

    @Test
    void getBookById_shouldCacheNullWhenNotFound() {
        // Arrange
        when(bookRepository.findById(3L)).thenReturn(null);

        // Act
        Book result = service.getBookById(3L);
        Book secondCall = service.getBookById(3L);

        // Assert
        assertNull(result);
        assertNull(secondCall);
        verify(bookRepository, times(1)).findById(3L);
    }
}