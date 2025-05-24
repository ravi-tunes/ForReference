import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BookLookupServiceTest {

    private BookRepository bookRepository;
    private BookLookupService service;

    @BeforeEach
    void setUp() {
        bookRepository = mock(BookRepository.class);
        service = new BookLookupService(bookRepository);
    }

    @Test
    void testInitializeAndGetters() throws SQLException {
        Book book = new Book(1L, 10L, 2, "CODE1", "ACC1");
        List<Book> books = Collections.singletonList(book);

        when(bookRepository.fetchAllBooks()).thenReturn(books);
        service.initialize();

        assertTrue(service.isInitialized());
        assertEquals(book, service.getBookById(1L));
        assertEquals(book, service.getBookByCode("CODE1"));
        assertEquals(book, service.getBookBySettlementAccount("ACC1"));
        assertEquals(book, service.getBookByParentId(10L));
    }

    @Test
    void testNullAndEmptyHandling() throws SQLException {
        Book book = new Book(1L, 10L, 2, "", null);
        when(bookRepository.fetchAllBooks()).thenReturn(Collections.singletonList(book));
        service.initialize();

        assertNull(service.getBookByCode(null));
        assertNull(service.getBookBySettlementAccount(null));
        assertNull(service.getBookByCode("non-existent"));
        assertNull(service.getBookBySettlementAccount("non-existent"));
        assertNotNull(service.getBookById(1L));
        assertNotNull(service.getBookByParentId(10L));
    }

    @Test
    void testEmptyDatabase() throws SQLException {
        when(bookRepository.fetchAllBooks()).thenReturn(Collections.emptyList());
        service.initialize();
        assertFalse(service.isInitialized());
    }

    @Test
    void testDoubleInitializationIsSafe() throws SQLException {
        when(bookRepository.fetchAllBooks()).thenReturn(Collections.singletonList(new Book(1L, 10L, 2, "CODE", "ACC")));
        service.initialize();
        service.initialize(); // Should do nothing
        assertTrue(service.isInitialized());
    }

    @Test
    void testSQLExceptionIsHandledGracefully() throws SQLException {
        when(bookRepository.fetchAllBooks()).thenThrow(new SQLException("DB error"));
        service.initialize();
        assertFalse(service.isInitialized());
    }
}
