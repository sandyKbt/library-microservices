package com.library.borrowingservice.controller;

import com.library.borrowingservice.model.Borrowing;
import com.library.borrowingservice.repository.BorrowingRepository;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/borrow")
public class BorrowingController {

    private final BorrowingRepository borrowingRepository;
    private final RestTemplate restTemplate;

    private static final String BOOK_SERVICE_URL = "http://BOOK-SERVICE/api/books";

    public BorrowingController(BorrowingRepository borrowingRepository, RestTemplate restTemplate) {
        this.borrowingRepository = borrowingRepository;
        this.restTemplate = restTemplate;
    }

    @PostMapping
    public ResponseEntity<?> borrowBook(@RequestBody Borrowing borrowing) {
        if (isBlank(borrowing.getIsbn())) {
            return ResponseEntity.badRequest().body("ISBN is required");
        }

        if (isBlank(borrowing.getMemberId())) {
            return ResponseEntity.badRequest().body("memberId is required");
        }

        if (isBlank(borrowing.getBorrowDate())) {
            borrowing.setBorrowDate(LocalDate.now().toString());
        }

        if (isBlank(borrowing.getDueDate())) {
            borrowing.setDueDate(LocalDate.now().plusDays(14).toString());
        }

        if (!isValidDate(borrowing.getBorrowDate()) || !isValidDate(borrowing.getDueDate())) {
            return ResponseEntity.badRequest().body("Dates must use this format: yyyy-MM-dd");
        }

        BookInfo book;

        try {
            book = restTemplate.getForObject(
                    BOOK_SERVICE_URL + "/" + borrowing.getIsbn(),
                    BookInfo.class
            );
        } catch (RestClientException ex) {
            return ResponseEntity.badRequest().body("Book does not exist or Book Service is not reachable");
        }

        if (book == null) {
            return ResponseEntity.badRequest().body("Book does not exist");
        }

        if (book.getAvailableCopies() == null || book.getAvailableCopies() <= 0) {
            return ResponseEntity.badRequest().body("No available copies for this book");
        }

        try {
            restTemplate.put(BOOK_SERVICE_URL + "/" + borrowing.getIsbn() + "/decrease", null);
        } catch (RestClientException ex) {
            return ResponseEntity.badRequest().body("Could not decrease available copies");
        }

        borrowing.setId(null);
        borrowing.setReturnDate(null);
        borrowing.setStatus("ACTIVE");
        borrowing.setLateFee(0.0);

        Borrowing savedBorrowing = borrowingRepository.save(borrowing);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedBorrowing);
    }

    @PutMapping("/return/{borrowId}")
    public ResponseEntity<?> returnBook(@PathVariable Long borrowId) {
        Optional<Borrowing> optionalBorrowing = borrowingRepository.findById(borrowId);

        if (optionalBorrowing.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Borrowing record not found");
        }

        Borrowing borrowing = optionalBorrowing.get();

        if ("RETURNED".equalsIgnoreCase(borrowing.getStatus())) {
            return ResponseEntity.badRequest().body("This book is already returned");
        }

        if (!isValidDate(borrowing.getDueDate())) {
            return ResponseEntity.badRequest().body("Invalid dueDate in borrowing record");
        }

        LocalDate today = LocalDate.now();
        LocalDate dueDate = LocalDate.parse(borrowing.getDueDate());

        long lateDays = ChronoUnit.DAYS.between(dueDate, today);

        if (lateDays > 0) {
            borrowing.setLateFee(lateDays * 1.0);
        } else {
            borrowing.setLateFee(0.0);
        }

        try {
            restTemplate.put(BOOK_SERVICE_URL + "/" + borrowing.getIsbn() + "/increase", null);
        } catch (RestClientException ex) {
            return ResponseEntity.badRequest().body("Could not increase available copies");
        }

        borrowing.setReturnDate(today.toString());
        borrowing.setStatus("RETURNED");

        Borrowing savedBorrowing = borrowingRepository.save(borrowing);
        return ResponseEntity.ok(savedBorrowing);
    }

    @GetMapping("/member/{memberId}")
    public List<Borrowing> getBorrowingsByMember(@PathVariable String memberId) {
        return borrowingRepository.findByMemberId(memberId);
    }

    @GetMapping("/active")
    public List<Borrowing> getActiveBorrowings() {
        return borrowingRepository.findByStatus("ACTIVE");
    }

    @GetMapping("/overdue")
    public List<Borrowing> getOverdueBorrowings() {
        List<Borrowing> activeBorrowings = borrowingRepository.findByStatus("ACTIVE");
        LocalDate today = LocalDate.now();

        for (Borrowing borrowing : activeBorrowings) {
            if (isValidDate(borrowing.getDueDate())) {
                LocalDate dueDate = LocalDate.parse(borrowing.getDueDate());

                if (dueDate.isBefore(today)) {
                    borrowing.setStatus("OVERDUE");
                    borrowingRepository.save(borrowing);
                }
            }
        }

        return borrowingRepository.findByStatus("OVERDUE");
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isValidDate(String value) {
        try {
            LocalDate.parse(value);
            return true;
        } catch (DateTimeParseException ex) {
            return false;
        }
    }

    public static class BookInfo {

        private String isbn;
        private String title;
        private String author;
        private String category;
        private Integer publishedYear;
        private Boolean isAvailable;
        private Integer totalCopies;
        private Integer availableCopies;

        public BookInfo() {
        }

        public String getIsbn() {
            return isbn;
        }

        public String getTitle() {
            return title;
        }

        public String getAuthor() {
            return author;
        }

        public String getCategory() {
            return category;
        }

        public Integer getPublishedYear() {
            return publishedYear;
        }

        public Boolean getIsAvailable() {
            return isAvailable;
        }

        public Integer getTotalCopies() {
            return totalCopies;
        }

        public Integer getAvailableCopies() {
            return availableCopies;
        }

        public void setIsbn(String isbn) {
            this.isbn = isbn;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public void setPublishedYear(Integer publishedYear) {
            this.publishedYear = publishedYear;
        }

        public void setIsAvailable(Boolean available) {
            isAvailable = available;
        }

        public void setTotalCopies(Integer totalCopies) {
            this.totalCopies = totalCopies;
        }

        public void setAvailableCopies(Integer availableCopies) {
            this.availableCopies = availableCopies;
        }
    }
}