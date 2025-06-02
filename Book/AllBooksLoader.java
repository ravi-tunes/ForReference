package com.yourapp.books.strategy;

import com.yourapp.books.model.Book;
import com.yourapp.books.repository.BookRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "book-cache.pre-caching.enabled", havingValue = "true")
@ConditionalOnExpression("${book-cache.pre-caching.history} == -1")
public class AllBooksLoader implements BookLoadStrategy {

    private static final Logger logger = LoggerFactory.getLogger(AllBooksLoader.class);
    private final BookRepository bookRepository;

    @Autowired
    public AllBooksLoader(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public List<Book> loadBooks() {
        logger.debug("Loading all books");
        return bookRepository.findAll();
    }
}