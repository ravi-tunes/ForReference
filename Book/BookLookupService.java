import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class BookLookupService {

    private static final Logger logger = LogManager.getLogger(BookLookupService.class);

    private final BookRepository bookRepository;

    private final Long2ObjectOpenHashMap<Book> idToBook = new Long2ObjectOpenHashMap<>();
    private final Object2LongOpenHashMap codeToId = new Object2LongOpenHashMap();
    private final Object2LongOpenHashMap accountToId = new Object2LongOpenHashMap();
    private final Long2LongOpenHashMap parentIdToId = new Long2LongOpenHashMap();

    private volatile boolean initialized = false;

    public BookLookupService(BookRepository bookRepository) {
        this.bookRepository = Objects.requireNonNull(bookRepository, "BookRepository cannot be null");
        this.codeToId.defaultReturnValue(-1L);
        this.accountToId.defaultReturnValue(-1L);
        this.parentIdToId.defaultReturnValue(-1L);
    }

    /**
     * Loads all books and populates the lookup maps.
     * This method is safe to call multiple times, but it is synchronized to prevent concurrent reloads.
     */
    public synchronized void initialize() {
        if (initialized) {
            logger.info("BookLookupService is already initialized.");
            return;
        }

        try {
            logger.info("Initializing BookLookupService...");
            List<Book> books = bookRepository.fetchAllBooks();

            if (books == null || books.isEmpty()) {
                logger.warn("No books found in the repository.");
                return;
            }

            for (Book book : books) {
                if (book == null) continue;

                long id = book.getId();
                long parentId = book.getParentId();

                idToBook.put(id, book);

                if (book.getCode() != null && !book.getCode().isEmpty()) {
                    codeToId.put(book.getCode(), id);
                }

                if (book.getSettlementAccount() != null && !book.getSettlementAccount().isEmpty()) {
                    accountToId.put(book.getSettlementAccount(), id);
                }

                parentIdToId.put(parentId, id);
            }

            initialized = true;
            logger.info("BookLookupService initialized successfully with {} books.", books.size());

        } catch (SQLException e) {
            logger.error("Failed to initialize BookLookupService due to database error", e);
        } catch (Exception ex) {
            logger.error("Unexpected error during BookLookupService initialization", ex);
        }
    }

    public Book getBookById(long id) {
        return initialized ? idToBook.get(id) : null;
    }

    public Book getBookByCode(String code) {
        if (!initialized || code == null) return null;
        long id = codeToId.getLong(code);
        return id != -1L ? idToBook.get(id) : null;
    }

    public Book getBookBySettlementAccount(String account) {
        if (!initialized || account == null) return null;
        long id = accountToId.getLong(account);
        return id != -1L ? idToBook.get(id) : null;
    }

    public Book getBookByParentId(long parentId) {
        if (!initialized) return null;
        long id = parentIdToId.get(parentId);
        return id != -1L ? idToBook.get(id) : null;
    }

    public boolean isInitialized() {
        return initialized;
    }
}