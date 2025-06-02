package com.yourapp.books.repository.impl;

import com.yourapp.books.model.Book;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookRepositoryImplTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ResultSet resultSet;

    @InjectMocks
    private BookRepositoryImpl repository;

    @Test
    void bookRowMapper_shouldMapRowCorrectly() throws SQLException {
        // Arrange
        when(resultSet.getLong("id")).thenReturn(1L);
        when(resultSet.getLong("parent_id")).thenReturn(10L);
        when(resultSet.getInt("book_level")).thenReturn(2);
        when(resultSet.getString("code")).thenReturn("ABC");
        when(resultSet.getString("settlement_account")).thenReturn("ACC123");
        when(resultSet.getDate("trade_date")).thenReturn(Date.valueOf("2023-01-15"));

        // Act
        RowMapper<Book> mapper = BookRepositoryImpl.BOOK_ROW_MAPPER;
        Book book = mapper.mapRow(resultSet, 1);

        // Assert
        assertEquals(1L, book.getId());
        assertEquals(10L, book.getParentId());
        assertEquals(2, book.getBookLevel());
        assertEquals("ABC", book.getCode());
        assertEquals("ACC123", book.getSettlementAccount());
        assertEquals(LocalDate.of(2023, 1, 15), book.getTradeDate());
    }

    @Test
    void bookRowMapper_shouldHandleNullTradeDate() throws SQLException {
        // Arrange
        when(resultSet.getLong("id")).thenReturn(2L);
        when(resultSet.getDate("trade_date")).thenReturn(null);

        // Act
        Book book = BookRepositoryImpl.BOOK_ROW_MAPPER.mapRow(resultSet, 1);

        // Assert
        assertNull(book.getTradeDate());
    }

    @Test
    void findAll_shouldExecuteQuery() {
        // Arrange
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
            .thenReturn(Collections.singletonList(new Book()));

        // Act
        List<Book> result = repository.findAll();

        // Assert
        assertEquals(1, result.size());
        verify(jdbcTemplate).query(
            eq("SELECT id, parent_id, book_level, code, settlement_account, trade_date FROM atlas_owner.book"),
            any(RowMapper.class)
        );
    }

    @Test
    void findByTradeDateFromCutoff_shouldExecuteQueryWithParameter() {
        // Arrange
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), any()))
            .thenReturn(Collections.singletonList(new Book()));

        // Act
        List<Book> result = repository.findByTradeDateFromCutoff(30);

        // Assert
        assertEquals(1, result.size());
        verify(jdbcTemplate).query(
            eq("SELECT id, parent_id, book_level, code, settlement_account, trade_date FROM atlas_owner.book WHERE trade_date >= ?"),
            any(RowMapper.class),
            any(LocalDate.class)
        );
    }

    @Test
    void findById_shouldReturnBook() {
        // Arrange
        Book expected = new Book();
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(1L)))
            .thenReturn(Collections.singletonList(expected));

        // Act
        Book result = repository.findById(1L);

        // Assert
        assertEquals(expected, result);
    }

    @Test
    void findById_shouldReturnNullWhenNotFound() {
        // Arrange
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(2L)))
            .thenReturn(Collections.emptyList());

        // Act
        Book result = repository.findById(2L);

        // Assert
        assertNull(result);
    }
}