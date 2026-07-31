package com.library.borrowingservice.repository;

import com.library.borrowingservice.model.Borrowing;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BorrowingRepository extends JpaRepository<Borrowing, Long> {

    List<Borrowing> findByMemberId(String memberId);

    List<Borrowing> findByStatus(String status);
}