package com.yourapp.books.repository;

import com.yourapp.books.model.Book;
import java.util.List;

public interface BookRepository {

    List<Book> findAll();
    
    /**
     * Fetch books where trade_date is >= cutoff date (current date minus days)
     * @param days Number of days
     */
    List<Book> findByTradeDateFromCutoff(int days);

    Book findById(long id);
}