// CounterpartyRepositoryImpl.java
package com.boondi.cache.counterparty.cache;

import com.boondi.cache.counterparty.CounterParty;
import lombok.extern.log4j.Log4j2;
import oracle.jdbc.OracleTypes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Repository implementation for fetching CounterParty data by calling stored procedures
 * from the VSTest Oracle package. This approach centralizes SQL logic in the database
 * and uses efficient, stream-based data retrieval with SYS_REFCURSOR.
 */
@Repository
@Log4j2
public class CounterpartyRepositoryImpl implements CounterpartyRepository {

    private static final int FETCH_SIZE = 5000;
    private final JdbcTemplate jdbcTemplate;

    public static final RowMapper<CounterParty> COUNTERPARTY_ROW_MAPPER = (rs, rowNum) -> new CounterParty(
            rs.getLong("sds_id"),
            rs.getString("fiss_id")
    );

    @Autowired
    public CounterpartyRepositoryImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<CounterParty> findAll() {
        log.debug("Calling procedure VSTest.GET_ALL_COUNTERPARTIES");
        return callProcedureAndFetchList("{call VSTest.GET_ALL_COUNTERPARTIES(?)}", COUNTERPARTY_ROW_MAPPER);
    }

    @Override
    public List<CounterParty> findByHistoricUsageWithCutoff(int months) {
        log.debug("Calling procedure VSTest.GET_HISTORICAL_COUNTERPARTIES with months: {}", months);
        return callProcedureAndFetchList("{call VSTest.GET_HISTORICAL_COUNTERPARTIES(?, ?)}", COUNTERPARTY_ROW_MAPPER, months);
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