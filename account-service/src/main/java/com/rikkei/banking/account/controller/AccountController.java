package com.rikkei.banking.account.controller;

import com.rikkei.banking.account.dto.CreateAccountRequest;
import com.rikkei.banking.account.entity.Account;
import com.rikkei.banking.account.repository.AccountRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/accounts")
public class AccountController {

    private final AccountRepository accountRepository;

    public AccountController(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @PostMapping
    public ResponseEntity<?> createAccount(@Valid @RequestBody CreateAccountRequest req) {
        if (accountRepository.findByAccountNumber(req.getAccountNumber()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Account number already exists"));
        }
        Account acc = new Account(req.getAccountNumber(), req.getOwnerName(), req.getBalance());
        Account saved = accountRepository.save(acc);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
            "status", 201,
            "message", "Account created successfully",
            "data", saved
        ));
    }

    @GetMapping
    public List<Account> getAllAccounts() {
        return accountRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAccountById(@PathVariable Long id) {
        return accountRepository.findById(id)
                .map(acc -> ResponseEntity.ok(Map.of("status", 200, "data", acc)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-number/{accountNumber}")
    public ResponseEntity<?> getAccountByNumber(@PathVariable String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .map(acc -> ResponseEntity.ok(Map.of("status", 200, "data", acc)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/transfer-deduct")
    @Transactional
    public ResponseEntity<?> transferDeduct(@RequestBody Map<String, Object> req) {
        String fromAcc = (String) req.get("fromAccount");
        String toAcc = (String) req.get("toAccount");
        BigDecimal amount = new BigDecimal(req.get("amount").toString());

        Account sender = accountRepository.findByAccountNumber(fromAcc)
                .orElseThrow(() -> new IllegalArgumentException("Sender account not found"));
        Account receiver = accountRepository.findByAccountNumber(toAcc)
                .orElseThrow(() -> new IllegalArgumentException("Receiver account not found"));

        if (sender.getBalance().compareTo(amount) < 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Insufficient balance"));
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));
        accountRepository.save(sender);
        accountRepository.save(receiver);

        return ResponseEntity.ok(Map.of(
            "status", 200,
            "message", "Transfer balance updated successfully",
            "fromAccount", fromAcc,
            "newBalance", sender.getBalance()
        ));
    }
}
