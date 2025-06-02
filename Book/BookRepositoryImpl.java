package com.yourapp.books.repository.impl;

import com.yourapp.books.model.Book;
import com.yourapp.books.repository.BookRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

@Repository
public class BookRepositoryImpl implements BookRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public BookRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    private static final RowMapper<Book> BOOK_ROW_MAPPER = (rs, rowNum) -> {
        Book book = new Book();
        book.setId(rs.getLong("id"));
        book.setParentId(rs.getLong("parent_id"));
        book.setBookLevel(rs.getInt("book_level"));
        book.setCode(rs.getString("code"));
        book.setSettlementAccount(rs.getString("settlement_account"));
        
        // Handle null trade_date
        java.sql.Date tradeDate = rs.getDate("trade_date");
        if (tradeDate != null) {
            book.setTradeDate(tradeDate.toLocalDate());
        }
        return book;
    };

    @Override
    public List<Book> findAll() {
        String sql = "SELECT id, parent_id, book_level, code, settlement_account, trade_date FROM atlas_owner.book";
        return jdbcTemplate.query(sql, BOOK_ROW_MAPPER);
    }

    @Override
    public List<Book> findByTradeDateFromCutoff(int days) {
        LocalDate cutoffDate = LocalDate.now().minusDays(days);
        String sql = "SELECT id, parent_id, book_level, code, settlement_account, trade_date " +
                     "FROM atlas_owner.book WHERE trade_date >= ?";
        return jdbcTemplate.query(sql, BOOK_ROW_MAPPER, cutoffDate);
    }

    @Override
    public Book findById(long id) {
        String sql = "SELECT id, parent_id, book_level, code, settlement_account, trade_date " +
                     "FROM atlas_owner.book WHERE id = ?";
        List<Book> results = jdbcTemplate.query(sql, BOOK_ROW_MAPPER, id);
        return results.isEmpty() ? null : results.get(0);
    }
}