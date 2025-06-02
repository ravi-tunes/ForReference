package com.yourapp.books.service;

import com.yourapp.books.model.Book;
import com.yourapp.books.repository.BookRepository;
import com.yourapp.books.strategy.BookLoadStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class BookLookupService {

    private static final Logger logger = LoggerFactory.getLogger(BookLookupService.class);
    private final ConcurrentMap<Long, Book> idToBook = new ConcurrentHashMap<>();
    private final BookRepository bookRepository;

    @Autowired
    public BookLookupService(BookLoadStrategy strategy, BookRepository bookRepository) {
        this.bookRepository = bookRepository;
        try {
            List<Book> books = strategy.loadBooks();
            books.forEach(book -> idToBook.put(book.getId(), book));
            logger.info("Pre-cached {} books", books.size());
        } catch (Exception e) {
            logger.error("Failed to pre-cache books. Falling back to lazy-loading", e);
        }
    }

    public Book getBookById(long id) {
        return idToBook.computeIfAbsent(id, key -> {
            Book book = bookRepository.findById(id);
            if (book == null) {
                logger.debug("Book {} not found in DB", id);
            } else {
                logger.debug("Loaded book {} from DB", id);
            }
            return book; // Caches null for missing books
        });
    }

    public int getCacheSize() {
        return idToBook.size();
    }
}