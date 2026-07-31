package com.library.bookservice.controller;

import com.library.bookservice.model.Book;
import com.library.bookservice.repository.BookRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookRepository bookRepository;

    public BookController(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @GetMapping
    public List<Book> getAllBooks() {
        return bookRepository.findAll();
    }

    @GetMapping("/available")
    public List<Book> getAvailableBooks() {
        return bookRepository.findByAvailableCopiesGreaterThan(0);
    }

    @GetMapping("/author/{author}")
    public List<Book> getBooksByAuthor(@PathVariable String author) {
        return bookRepository.findByAuthorContainingIgnoreCase(author);
    }

    @GetMapping("/{isbn}")
    public ResponseEntity<?> getBookByIsbn(@PathVariable String isbn) {
        Optional<Book> book = bookRepository.findById(isbn);

        if (book.isPresent()) {
            return ResponseEntity.ok(book.get());
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Book not found");
    }

    @PostMapping
    public ResponseEntity<?> addBook(@RequestBody Book book) {
        if (book.getIsbn() == null || book.getIsbn().isBlank()) {
            return ResponseEntity.badRequest().body("ISBN is required");
        }

        if (bookRepository.existsById(book.getIsbn())) {
            return ResponseEntity.badRequest().body("Book with this ISBN already exists");
        }

        if (book.getTotalCopies() == null || book.getTotalCopies() < 0) {
            return ResponseEntity.badRequest().body("totalCopies must be zero or more");
        }

        if (book.getAvailableCopies() == null) {
            book.setAvailableCopies(book.getTotalCopies());
        }

        if (book.getAvailableCopies() < 0) {
            return ResponseEntity.badRequest().body("availableCopies cannot be negative");
        }

        if (book.getAvailableCopies() > book.getTotalCopies()) {
            return ResponseEntity.badRequest().body("availableCopies cannot be greater than totalCopies");
        }

        book.setIsAvailable(book.getAvailableCopies() > 0);

        Book savedBook = bookRepository.save(book);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedBook);
    }

    @PutMapping("/{isbn}")
    public ResponseEntity<?> updateBook(@PathVariable String isbn, @RequestBody Book updatedBook) {
        Optional<Book> optionalBook = bookRepository.findById(isbn);

        if (optionalBook.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Book not found");
        }

        Book book = optionalBook.get();

        book.setTitle(updatedBook.getTitle());
        book.setAuthor(updatedBook.getAuthor());
        book.setCategory(updatedBook.getCategory());
        book.setPublishedYear(updatedBook.getPublishedYear());
        book.setTotalCopies(updatedBook.getTotalCopies());
        book.setAvailableCopies(updatedBook.getAvailableCopies());

        if (book.getTotalCopies() == null || book.getTotalCopies() < 0) {
            return ResponseEntity.badRequest().body("totalCopies must be zero or more");
        }

        if (book.getAvailableCopies() == null || book.getAvailableCopies() < 0) {
            return ResponseEntity.badRequest().body("availableCopies must be zero or more");
        }

        if (book.getAvailableCopies() > book.getTotalCopies()) {
            return ResponseEntity.badRequest().body("availableCopies cannot be greater than totalCopies");
        }

        book.setIsAvailable(book.getAvailableCopies() > 0);

        Book savedBook = bookRepository.save(book);
        return ResponseEntity.ok(savedBook);
    }

    @DeleteMapping("/{isbn}")
    public ResponseEntity<?> deleteBook(@PathVariable String isbn) {
        if (!bookRepository.existsById(isbn)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Book not found");
        }

        bookRepository.deleteById(isbn);
        return ResponseEntity.ok("Book deleted successfully");
    }

    @PutMapping("/{isbn}/decrease")
    public ResponseEntity<?> decreaseAvailableCopies(@PathVariable String isbn) {
        Optional<Book> optionalBook = bookRepository.findById(isbn);

        if (optionalBook.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Book not found");
        }

        Book book = optionalBook.get();

        if (book.getAvailableCopies() == null || book.getAvailableCopies() <= 0) {
            return ResponseEntity.badRequest().body("No available copies");
        }

        book.setAvailableCopies(book.getAvailableCopies() - 1);
        book.setIsAvailable(book.getAvailableCopies() > 0);

        Book savedBook = bookRepository.save(book);
        return ResponseEntity.ok(savedBook);
    }

    @PutMapping("/{isbn}/increase")
    public ResponseEntity<?> increaseAvailableCopies(@PathVariable String isbn) {
        Optional<Book> optionalBook = bookRepository.findById(isbn);

        if (optionalBook.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Book not found");
        }

        Book book = optionalBook.get();

        if (book.getAvailableCopies() == null) {
            book.setAvailableCopies(0);
        }

        if (book.getTotalCopies() == null) {
            return ResponseEntity.badRequest().body("totalCopies is missing");
        }

        if (book.getAvailableCopies() < book.getTotalCopies()) {
            book.setAvailableCopies(book.getAvailableCopies() + 1);
        }

        book.setIsAvailable(book.getAvailableCopies() > 0);

        Book savedBook = bookRepository.save(book);
        return ResponseEntity.ok(savedBook);
    }
}