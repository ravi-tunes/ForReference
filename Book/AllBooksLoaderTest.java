package com.yourapp.books.strategy;

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
class AllBooksLoaderTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private AllBooksLoader loader;

    @Test
    void loadBooks_shouldReturnAllBooks() {
        // Arrange
        List<Book> expectedBooks = Arrays.asList(new Book(), new Book());
        when(bookRepository.findAll()).thenReturn(expectedBooks);

        // Act
        List<Book> result = loader.loadBooks();

        // Assert
        assertEquals(2, result.size());
        verify(bookRepository, times(1)).findAll();
    }
}