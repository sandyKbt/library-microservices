package com.library.bookservice.repository;

import com.library.bookservice.model.Book;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, String> {

    List<Book> findByAuthorContainingIgnoreCase(String author);

    List<Book> findByAvailableCopiesGreaterThan(Integer copies);
}