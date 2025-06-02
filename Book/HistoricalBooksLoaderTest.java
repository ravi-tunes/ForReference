package com.yourapp.books.strategy;

import com.yourapp.config.BookCacheProperties;
import com.yourapp.books.model.Book;
import com.yourapp.books.repository.BookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HistoricalBooksLoaderTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookCacheProperties properties;

    @InjectMocks
    private HistoricalBooksLoader loader;

    @Test
    void loadBooks_shouldReturnHistoricalBooks() {
        // Arrange
        when(properties.getHistory()).thenReturn(30);
        List<Book> expectedBooks = Arrays.asList(new Book(), new Book());
        when(bookRepository.findByTradeDateFromCutoff(30)).thenReturn(expectedBooks);

        // Act
        List<Book> result = loader.loadBooks();

        // Assert
        assertEquals(2, result.size());
        verify(bookRepository).findByTradeDateFromCutoff(30);
    }
}