import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.sql.DataSource;
import java.sql.*;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BookRepositoryTest {

    private DataSource dataSource;
    private Connection connection;
    private PreparedStatement statement;
    private ResultSet resultSet;
    private BookRepository bookRepository;

    @BeforeEach
    void setup() throws Exception {
        dataSource = mock(DataSource.class);
        connection = mock(Connection.class);
        statement = mock(PreparedStatement.class);
        resultSet = mock(ResultSet.class);

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(any())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);

        bookRepository = new BookRepository(dataSource);
    }

    @Test
    void testFetchAllBooks_ReturnsBooks() throws Exception {
        when(resultSet.next()).thenReturn(true, false); // one row
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getLong("parent_id")).thenReturn(10L);
        when(resultSet.getInt("book_level")).thenReturn(2);
        when(resultSet.getString("code")).thenReturn("B001");
        when(resultSet.getString("settlement_account")).thenReturn("ACC001");

        List<Book> books = bookRepository.fetchAllBooks();

        assertEquals(1, books.size());
        Book book = books.get(0);
        assertEquals(1L, book.getId());
        assertEquals(10L, book.getParentId());
        assertEquals(2, book.getBookLevel());
        assertEquals("B001", book.getCode());
        assertEquals("ACC001", book.getSettlementAccount());

        verify(resultSet, times(1)).close();
        verify(statement, times(1)).close();
        verify(connection, times(1)).close();
    }
}