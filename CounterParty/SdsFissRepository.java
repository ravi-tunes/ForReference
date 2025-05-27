import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository for fetching SDS and FISS mappings from the Oracle database.
 */
public class SdsFissRepository {

    private final DataSource dataSource;

    public SdsFissRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public List<SdsFissMapping> fetchAllMappings() throws SQLException {
        String sql = "SELECT sds_id, fiss_id FROM your_schema.sds_fiss_table";
        List<SdsFissMapping> mappings = new ArrayList<>(5_000_000);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                long sdsId = rs.getLong("sds_id");
                String fissId = rs.getString("fiss_id");
                mappings.add(new SdsFissMapping(sdsId, fissId));
            }
        }

        return mappings;
    }
}
