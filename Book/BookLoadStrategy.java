package com.yourapp.books.strategy;

import com.yourapp.books.model.Book;
import java.util.List;

public interface BookLoadStrategy {
    List<Book> loadBooks();
}
