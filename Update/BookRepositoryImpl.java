// BookRepositoryImpl.java
package com.boondi.cache.book.cache;

import com.boondi.cache.book.Book;
import lombok.extern.log4j.Log4j2;
import oracle.jdbc.OracleTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

/**
 * Repository implementation for fetching Book data by calling stored procedures
 * from the VSTest Oracle package. This approach centralizes SQL logic in the database
 * and uses efficient, stream-based data retrieval with SYS_REFCURSOR.
 */
@Repository
@Log4j2
public class BookRepositoryImpl implements BookRepository {

    private static final int FETCH_SIZE = 5000;
    private final JdbcTemplate jdbcTemplate;

    public static final RowMapper<Book> BOOK_ROW_MAPPER = (rs, rowNum) -> new Book(
            rs.getLong("id"),
            rs.getLong("parent_id"),
            rs.getInt("book_level"),
            rs.getString("code"),
            rs.getString("settlement_account")
    );

    @Autowired
    public BookRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Book> findAll() {
        log.debug("Calling procedure VSTest.GET_ALL_BOOKS");
        return callProcedureAndFetchList("{call VSTest.GET_ALL_BOOKS(?)}", BOOK_ROW_MAPPER);
    }

    @Override
    public List<Book> findByHistoricUsageWithCutoff(int months) {
        log.debug("Calling procedure VSTest.GET_HISTORICAL_BOOKS with months: {}", months);
        return callProcedureAndFetchList("{call VSTest.GET_HISTORICAL_BOOKS(?, ?)}", BOOK_ROW_MAPPER, months);
    }

    @Override
    public Book findByCodeId(String code) {
        log.debug("Calling procedure VSTest.GET_BOOK_BY_CODE with code: {}", code);
        List<Book> results = callProcedureAndFetchList("{call VSTest.GET_BOOK_BY_CODE(?, ?)}", BOOK_ROW_MAPPER, code);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * A generic helper method to execute a stored procedure that returns a SYS_REFCURSOR
     * and map the results to a list of objects.
     *
     * @param procedureCall The SQL string to call the procedure (e.g., "{call MY_PROC(?, ?)}").
     * @param rowMapper     The RowMapper to convert each row to an object.
     * @param params        The IN parameters for the stored procedure.
     * @return A list of mapped objects, or an empty list if an error occurs.
     */
    private <T> List<T> callProcedureAndFetchList(String procedureCall, RowMapper<T> rowMapper, Object... params) {
        try {
            return jdbcTemplate.execute(procedureCall, (CallableStatement cs) -> {
                // Set IN parameters
                for (int i = 0; i < params.length; i++) {
                    cs.setObject(i + 1, params[i]);
                }

                // Register the OUT parameter (the cursor)
                int cursorParamIndex = params.length + 1;
                cs.registerOutParameter(cursorParamIndex, OracleTypes.CURSOR);

                cs.execute();

                List<T> results = new ArrayList<>();
                // Use try-with-resources to ensure the ResultSet is always closed
                try (ResultSet rs = (ResultSet) cs.getObject(cursorParamIndex)) {
                    // CRITICAL: Set the fetch size for efficient streaming
                    rs.setFetchSize(FETCH_SIZE);
                    log.info("Streaming results with fetch size: {}", FETCH_SIZE);

                    int rowNum = 0;
                    while (rs.next()) {
                        results.add(rowMapper.mapRow(rs, rowNum++));
                        if (rowNum % FETCH_SIZE == 0) {
                            log.debug("Fetched {} rows so far...", rowNum);
                        }
                    }
                    log.info("Completed fetching {} total rows.", rowNum);
                }
                return results;
            });
        } catch (DataAccessException e) {
            log.error("Error calling stored procedure: {}", procedureCall, e);
            // Return an empty list to prevent downstream null pointers
            return Collections.emptyList();
        }
    }
}