public class Book {
    private final long id;
    private final long parentId;
    private final int bookLevel;
    private final String code;
    private final String settlementAccount;

    public Book(long id, long parentId, int bookLevel, String code, String settlementAccount) {
        this.id = id;
        this.parentId = parentId;
        this.bookLevel = bookLevel;
        this.code = code;
        this.settlementAccount = settlementAccount;
    }

    // Getters
    public long getId() { return id; }
    public long getParentId() { return parentId; }
    public int getBookLevel() { return bookLevel; }
    public String getCode() { return code; }
    public String getSettlementAccount() { return settlementAccount; }
}