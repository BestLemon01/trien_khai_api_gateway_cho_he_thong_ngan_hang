package com.rikkei.banking.transaction.repository;

import com.rikkei.banking.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}
