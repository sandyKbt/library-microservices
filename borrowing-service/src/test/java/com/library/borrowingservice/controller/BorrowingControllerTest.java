package com.library.borrowingservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.library.borrowingservice.controller.BorrowingController.BookInfo;
import com.library.borrowingservice.model.Borrowing;
import com.library.borrowingservice.repository.BorrowingRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class BorrowingControllerTest {

    private static final String ISBN = "9780132350884";
    private static final String BOOK_URL = "http://BOOK-SERVICE/api/books/" + ISBN;

    @Mock
    private BorrowingRepository borrowingRepository;

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private BorrowingController borrowingController;

    @Test
    void borrowBookCreatesActiveRecordAndDefaultsDates() {
        Borrowing request = new Borrowing();
        request.setIsbn(ISBN);
        request.setMemberId("M001");

        BookInfo book = availableBook();
        when(restTemplate.getForObject(BOOK_URL, BookInfo.class)).thenReturn(book);
        when(borrowingRepository.save(any(Borrowing.class))).thenAnswer(invocation -> {
            Borrowing saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        ResponseEntity<?> response = borrowingController.borrowBook(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Borrowing saved = (Borrowing) response.getBody();
        assertNotNull(saved);
        assertEquals("ACTIVE", saved.getStatus());
        assertEquals(Double.valueOf(0.0), saved.getLateFee());
        assertNotNull(saved.getBorrowDate());
        assertEquals(
                LocalDate.parse(saved.getBorrowDate()).plusDays(14),
                LocalDate.parse(saved.getDueDate())
        );
        assertNull(saved.getReturnDate());

        verify(restTemplate).put("http://BOOK-SERVICE/api/books/" + ISBN + "/decrease", null);
        verify(borrowingRepository).save(request);
    }

    @Test
    void borrowBookRejectsUnreachableBookService() {
        Borrowing request = new Borrowing();
        request.setIsbn(ISBN);
        request.setMemberId("M001");

        when(restTemplate.getForObject(BOOK_URL, BookInfo.class))
                .thenThrow(new RestClientException("offline"));

        ResponseEntity<?> response = borrowingController.borrowBook(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Book does not exist or Book Service is not reachable", response.getBody());
        verify(borrowingRepository, never()).save(any(Borrowing.class));
    }

    @Test
    void returnBookCalculatesLateFeeAndMarksRecordReturned() {
        Borrowing activeBorrowing = new Borrowing(
                1L,
                ISBN,
                "M001",
                LocalDate.now().minusDays(17).toString(),
                LocalDate.now().minusDays(3).toString(),
                null,
                "ACTIVE",
                0.0
        );

        when(borrowingRepository.findById(1L)).thenReturn(Optional.of(activeBorrowing));
        when(borrowingRepository.save(any(Borrowing.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = borrowingController.returnBook(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Borrowing saved = (Borrowing) response.getBody();
        assertNotNull(saved);
        assertEquals("RETURNED", saved.getStatus());
        assertEquals(Double.valueOf(3.0), saved.getLateFee());
        assertEquals(LocalDate.now().toString(), saved.getReturnDate());

        verify(restTemplate).put("http://BOOK-SERVICE/api/books/" + ISBN + "/increase", null);
        verify(borrowingRepository).save(activeBorrowing);
    }

    private BookInfo availableBook() {
        BookInfo book = new BookInfo();
        book.setIsbn(ISBN);
        book.setTitle("Clean Code");
        book.setTotalCopies(3);
        book.setAvailableCopies(3);
        book.setIsAvailable(true);
        return book;
    }
}
