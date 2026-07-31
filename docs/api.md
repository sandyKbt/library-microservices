# API Reference

Use the API Gateway for client requests:

```text
http://localhost:8080
```

Requests with JSON bodies require `Content-Type: application/json`.

## Book representation

```json
{
  "isbn": "9780132350884",
  "title": "Clean Code",
  "author": "Robert C. Martin",
  "category": "Programming",
  "publishedYear": 2008,
  "isAvailable": true,
  "totalCopies": 3,
  "availableCopies": 3
}
```

## Book endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| `GET` | `/api/books` | List every book |
| `GET` | `/api/books/available` | List books with at least one available copy |
| `GET` | `/api/books/author/{author}` | Search authors by case-insensitive substring |
| `GET` | `/api/books/{isbn}` | Get one book |
| `POST` | `/api/books` | Add a book |
| `PUT` | `/api/books/{isbn}` | Replace editable book details and copy counts |
| `DELETE` | `/api/books/{isbn}` | Delete a book |
| `PUT` | `/api/books/{isbn}/decrease` | Decrease availability; used by Borrowing Service |
| `PUT` | `/api/books/{isbn}/increase` | Increase availability; used by Borrowing Service |

### Create a book

`availableCopies` may be omitted. It then defaults to `totalCopies`.

```http
POST /api/books
Content-Type: application/json
```

```json
{
  "isbn": "9780132350884",
  "title": "Clean Code",
  "author": "Robert C. Martin",
  "category": "Programming",
  "publishedYear": 2008,
  "totalCopies": 3
}
```

The service rejects missing ISBNs, duplicate ISBNs, negative copy counts, and an available count greater than the total count.

## Borrowing representation

```json
{
  "id": 1,
  "isbn": "9780132350884",
  "memberId": "M001",
  "borrowDate": "2026-07-31",
  "dueDate": "2026-08-14",
  "returnDate": null,
  "status": "ACTIVE",
  "lateFee": 0.0
}
```

## Borrowing endpoints

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/borrow` | Borrow an available book |
| `PUT` | `/api/borrow/return/{borrowId}` | Return a borrowing and calculate its late fee |
| `GET` | `/api/borrow/member/{memberId}` | List one member's borrowing history |
| `GET` | `/api/borrow/active` | List borrowings with `ACTIVE` status |
| `GET` | `/api/borrow/overdue` | Mark and list active records whose due date has passed |

### Borrow a book

`borrowDate` and `dueDate` are optional. Their format is `yyyy-MM-dd`.

```http
POST /api/borrow
Content-Type: application/json
```

```json
{
  "isbn": "9780132350884",
  "memberId": "M001"
}
```

The service verifies the book through `BOOK-SERVICE`, decreases availability, and then stores the borrowing as `ACTIVE`.

### Return a book

```http
PUT /api/borrow/return/1
```

The service rejects an unknown borrowing ID or a record already marked `RETURNED`.

## Direct development URLs

The Gateway is the intended public entry point, but the business services can also be called directly during development:

- Book Service: `http://localhost:8081`
- Borrowing Service: `http://localhost:8082`
- Eureka dashboard: `http://localhost:8761`
