package com.rikkei.banking.transaction.controller;

import com.rikkei.banking.transaction.entity.Transaction;
import com.rikkei.banking.transaction.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/transactions")
public class TransactionController {

    private final TransactionRepository transactionRepository;
    private final RestTemplate restTemplate;

    @Value("${account.service.url}")
    private String accountServiceUrl;

    public TransactionController(TransactionRepository transactionRepository, RestTemplate restTemplate) {
        this.transactionRepository = transactionRepository;
        this.restTemplate = restTemplate;
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transferMoney(@RequestBody Map<String, Object> req) {
        String fromAcc = (String) req.get("fromAccount");
        String toAcc = (String) req.get("toAccount");
        BigDecimal amount = new BigDecimal(req.get("amount").toString());

        // Inter-service call via Docker DNS: http://account-service:8080/accounts/transfer-deduct
        String url = accountServiceUrl + "/accounts/transfer-deduct";
        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(url, req, Map.class);
            if (resp.getStatusCode().is2xxSuccessful()) {
                String txCode = "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
                Transaction tx = new Transaction(txCode, fromAcc, toAcc, amount, "SUCCESS");
                transactionRepository.save(tx);
                return ResponseEntity.ok(Map.of(
                    "status", 200,
                    "message", "Transfer successful",
                    "transactionCode", txCode,
                    "amount", amount,
                    "statusDetail", "SUCCESS"
                ));
            } else {
                return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
