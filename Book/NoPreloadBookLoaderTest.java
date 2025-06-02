package com.yourapp.books.strategy;

import com.yourapp.books.model.Book;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class NoPreloadBookLoaderTest {

    @Test
    void loadBooks_shouldReturnEmptyList() {
        // Arrange
        NoPreloadBookLoader loader = new NoPreloadBookLoader();

        // Act
        List<Book> result = loader.loadBooks();

        // Assert
        assertTrue(result.isEmpty());
    }
}