package com.yourapp.books.strategy;

import com.yourapp.books.model.Book;
import com.yourapp.books.repository.BookRepository;
import com.yourapp.config.BookCacheProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "book-cache.pre-caching.enabled", havingValue = "true")
@ConditionalOnExpression("${book-cache.pre-caching.history} >= 0")  // Fixed condition
public class HistoricalBooksLoader implements BookLoadStrategy {

    private static final Logger logger = LoggerFactory.getLogger(HistoricalBooksLoader.class);
    private final BookRepository bookRepository;
    private final BookCacheProperties properties;

    @Autowired
    public HistoricalBooksLoader(BookRepository bookRepository, BookCacheProperties properties) {
        this.bookRepository = bookRepository;
        this.properties = properties;
    }

    @Override
    public List<Book> loadBooks() {
        logger.debug("Loading historical books for last {} days", properties.getHistory());
        return bookRepository.findByTradeDateFromCutoff(properties.getHistory());
    }
}