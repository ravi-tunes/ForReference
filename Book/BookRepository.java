import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookRepository {

    private final DataSource dataSource;

    public BookRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Fetches all books from the 'atlas_owner.book' table.
     * @return List of Book objects
     * @throws SQLException on database access error
     */
    public List<Book> fetchAllBooks() throws SQLException {
        String sql = "SELECT id, parent_id, book_level, code, settlement_account FROM atlas_owner.book";

        List<Book> books = new ArrayList<>(200_000); // initial capacity to minimize resizing

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                long id = rs.getLong("id");
                long parentId = rs.getLong("parent_id");
                int bookLevel = rs.getInt("book_level");
                String code = rs.getString("code");
                String settlementAccount = rs.getString("settlement_account");

                books.add(new Book(id, parentId, bookLevel, code, settlementAccount));
            }
        }

        return books;
    }
}