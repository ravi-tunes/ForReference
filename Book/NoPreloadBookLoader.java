package com.yourapp.books.strategy;

import com.yourapp.books.model.Book;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@ConditionalOnProperty(name = "book-cache.pre-caching.enabled", havingValue = "false", matchIfMissing = true)
public class NoPreloadBookLoader implements BookLoadStrategy {

    private static final Logger logger = LoggerFactory.getLogger(NoPreloadBookLoader.class);

    @Override
    public List<Book> loadBooks() {
        logger.debug("Skipping book pre-caching");
        return Collections.emptyList();
    }
}