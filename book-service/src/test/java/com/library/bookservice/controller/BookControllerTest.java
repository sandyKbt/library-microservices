package com.library.bookservice.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.library.bookservice.model.Book;
import com.library.bookservice.repository.BookRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class BookControllerTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookController bookController;

    @Test
    void addBookDefaultsAvailabilityAndReturnsCreated() {
        Book request = new Book(
                "9780132350884",
                "Clean Code",
                "Robert C. Martin",
                "Programming",
                2008,
                null,
                3,
                null
        );

        when(bookRepository.existsById(request.getIsbn())).thenReturn(false);
        when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ResponseEntity<?> response = bookController.addBook(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        Book savedBook = (Book) response.getBody();
        assertNotNull(savedBook);
        assertEquals(Integer.valueOf(3), savedBook.getAvailableCopies());
        assertTrue(Boolean.TRUE.equals(savedBook.getIsAvailable()));
        verify(bookRepository).save(request);
    }

    @Test
    void addBookRejectsDuplicateIsbn() {
        Book request = new Book(
                "duplicate-isbn",
                "Existing Book",
                "Author",
                "Category",
                2026,
                true,
                1,
                1
        );

        when(bookRepository.existsById(request.getIsbn())).thenReturn(true);

        ResponseEntity<?> response = bookController.addBook(request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Book with this ISBN already exists", response.getBody());
        verify(bookRepository, never()).save(any(Book.class));
    }

    @Test
    void decreaseAvailableCopiesRejectsUnavailableBook() {
        Book unavailableBook = new Book(
                "unavailable-isbn",
                "Unavailable Book",
                "Author",
                "Category",
                2026,
                false,
                1,
                0
        );

        when(bookRepository.findById(unavailableBook.getIsbn()))
                .thenReturn(Optional.of(unavailableBook));

        ResponseEntity<?> response = bookController.decreaseAvailableCopies(unavailableBook.getIsbn());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("No available copies", response.getBody());
        verify(bookRepository, never()).save(any(Book.class));
    }
}
