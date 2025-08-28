import java.util.List;
import java.util.Optional;

/**
 * Finds a trading book using a multi-step, rule-based process.
 * This class is a direct implementation of the provided detailed workflow diagram.
 *
 * @version 17
 */
public final class TradingBookFinder {

    // --- Public Records and Enums (Data Contracts) ---

    public enum TradeType { EQUITY, OPTION, FUTURE, OTHER }
    public enum ErrorCode { BOOK_NOT_FOUND, MULTIPLE_BOOKS_FOUND, MULTIPLE_SUBFOLDER_BOOKS_FOUND, MULTIPLE_SYMBOL_BOOKS_FOUND }

    public record ListedTrade(TradeType tradeType, String account, String bookCode, String subBookField) {}
    public record ListedExecution(String symbol, String sourceSystem) {}
    public record TradingBook(String id, String name, String parentId) {}
    public record TradingBookException(ErrorCode code, String message) {}

    // A helper record to pass processing flags internally.
    private record ProcessingFlags(boolean processSubBook, boolean lookupSymbol) {}

    // --- Main Workflow Method ---

    /**
     * Main entry point to get the trading book for a given trade.
     */
    public TradingBook getTradingBook(
        final ListedTrade trade,
        final ListedExecution execution,
        final List<TradingBookException> exceptions
    ) {
        // 1. Pick path + preprocessing flags
        if (trade.tradeType() == TradeType.OTHER) {
            System.err.println("Unsupported trade type: " + trade.tradeType()); // Use a proper logger
            return null;
        }

        final ProcessingFlags flags = switch (trade.tradeType()) {
            case EQUITY -> new ProcessingFlags(true, true);
            case OPTION, FUTURE -> new ProcessingFlags(isSourceAllowedForSubBook(execution.sourceSystem()), false);
            default -> new ProcessingFlags(false, false); // Should not happen due to the check above
        };

        // 2. Find the initial level-5 book
        final Optional<TradingBook> initialBookOpt = findInitialLevel5Book(trade, exceptions);
        if (initialBookOpt.isEmpty()) {
            return null; // Exception was added within the helper method
        }
        final TradingBook baseBook = initialBookOpt.get();

        // 3. Subfolder processing decision
        if (!flags.processSubBook()) {
            return baseBook;
        }

        // 3a. Sub-book processing via iterative hierarchy
        if (trade.subBookField() != null && !trade.subBookField().isBlank()) {
            return refineBySubBookHierarchy(baseBook, trade.subBookField(), exceptions);
        }

        // 4. Symbol mapping path
        if (flags.lookupSymbol()) {
            return refineBySymbolMapping(baseBook, execution, exceptions);
        }
        
        return baseBook;
    }

    // --- Private Helper Methods ---

    /**
     * Finds the initial "Level 5" book. Tries by account first, then falls back to book code.
     * Corresponds to the "FindL5" and "L5Res" nodes.
     */
    private Optional<TradingBook> findInitialLevel5Book(final ListedTrade trade, final List<TradingBookException> exceptions) {
        // TODO: Implement your database/service logic here.
        // 1. Attempt to find a unique, level-5 book by trade.account().
        // Optional<TradingBook> book = findByAccount(trade.account());
        // if (book.isPresent()) return book;

        // 2. If not found by account, attempt to find by trade.bookCode().
        // List<TradingBook> books = findByBookCode(trade.bookCode());
        //
        // switch (books.size()) {
        //     case 0 -> {
        //         exceptions.add(new TradingBookException(ErrorCode.BOOK_NOT_FOUND, "No Level 5 book found."));
        //         return Optional.empty();
        //     }
        //     case 1 -> {
        //         return Optional.of(books.get(0));
        //     }
        //     default -> {
        //         exceptions.add(new TradingBookException(ErrorCode.MULTIPLE_BOOKS_FOUND, "Multiple Level 5 books found."));
        //         return Optional.empty();
        //     }
        // }
        
        // Placeholder implementation for demonstration
        if ("ACC_123".equals(trade.account())) {
            return Optional.of(new TradingBook("BASE_BOOK_ID", "Base L5 Book", null));
        } else {
            exceptions.add(new TradingBookException(ErrorCode.BOOK_NOT_FOUND, "No Level 5 book found for account " + trade.account()));
            return Optional.empty();
        }
    }

    /**
     * Iteratively drills down the book hierarchy based on a dot-separated path.
     * Corresponds to the "ProcessNextPart" loop in the diagram.
     */
    private TradingBook refineBySubBookHierarchy(
        TradingBook currentBook,
        final String subBookPath,
        final List<TradingBookException> exceptions
    ) {
        final String[] pathParts = subBookPath.split("\\.");

        for (final String part : pathParts) {
            // TODO: Implement logic to find direct children of `currentBook` with a matching code/name.
            // List<TradingBook> children = findChildrenByCode(currentBook.id(), part);
            List<TradingBook> children = List.of(); // Placeholder

            switch (children.size()) {
                case 0 -> {
                    // No child found, stop processing and return the last valid book.
                    return currentBook;
                }
                case 1 -> {
                    // Unique child found, update currentBook and continue to the next part.
                    currentBook = children.get(0);
                }
                default -> {
                    // Multiple children found, add exception and return the last valid book.
                    String message = String.format("Multiple subfolder books found for '%s' under book '%s'", part, currentBook.id());
                    exceptions.add(new TradingBookException(ErrorCode.MULTIPLE_SUBFOLDER_BOOKS_FOUND, message));
                    return currentBook;
                }
            }
        }
        // If the loop completes, we have successfully traversed the entire path.
        return currentBook;
    }

    /**
     * Finds a subfolder book based on the execution symbol, with a fallback to "EVERYTHING".
     * Corresponds to the "Symbol mapping path" section of the diagram.
     */
    private TradingBook refineBySymbolMapping(
        final TradingBook originalBook,
        final ListedExecution execution,
        final List<TradingBookException> exceptions
    ) {
        // Try to find a book with the specific symbol first.
        Optional<TradingBook> foundBook = findSubfolderBySymbol(originalBook, execution.symbol(), exceptions);
        if (foundBook.isPresent()) {
            return foundBook.get();
        }

        // If not found, try the "EVERYTHING" fallback symbol.
        // The check for `!foundBook.isPresent()` is implicitly handled by the previous return.
        // TODO: You may want to add a business rule check here, e.g., `if (isEverythingFallbackAllowed())`
        Optional<TradingBook> fallbackBook = findSubfolderBySymbol(originalBook, "EVERYTHING", exceptions);
        
        // Return the fallback book if found, otherwise return the original book.
        return fallbackBook.orElse(originalBook);
    }
    
    /**
     * A helper for refineBySymbolMapping to perform the actual lookup for a given symbol.
     */
    private Optional<TradingBook> findSubfolderBySymbol(
        final TradingBook parentBook, 
        final String symbol, 
        final List<TradingBookException> exceptions
    ) {
        // TODO: Implement logic to find subfolder books of `parentBook` mapped to the given `symbol`.
        // List<TradingBook> books = findSubfoldersBySymbolMapping(parentBook.id(), symbol);
        List<TradingBook> books = List.of(); // Placeholder

        return switch (books.size()) {
            case 0 -> Optional.empty(); // No match, caller will handle fallback or return original.
            case 1 -> Optional.of(books.get(0)); // Unique match found.
            default -> {
                String message = String.format("Multiple books found for symbol '%s' under book '%s'", symbol, parentBook.id());
                exceptions.add(new TradingBookException(ErrorCode.MULTIPLE_SYMBOL_BOOKS_FOUND, message));
                yield Optional.empty(); // Treat multiple as "not found" so the caller returns the original book.
            }
        };
    }

    /**
     * Business logic to check if a source system can process sub-books.
     */
    private boolean isSourceAllowedForSubBook(final String sourceSystem) {
        // TODO: Implement your business rule, e.g., check against a config list of allowed systems.
        // return ALLOWED_SYSTEMS_LIST.contains(sourceSystem);
        return true; // Placeholder
    }
}