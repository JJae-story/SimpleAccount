package com.example.simpleaccount.dto;

import com.example.simpleaccount.domain.Transaction;
import com.example.simpleaccount.type.TransactionResultType;
import com.example.simpleaccount.type.TransactionType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TransactionDto {
    private String accountNumber;

    private TransactionType transactionType;

    private TransactionResultType transactionResult;

    private Long amount;
    private Long balanceSnapshot;

    private String transactionId;
    private LocalDateTime transactedAt;

    public static TransactionDto fromEntity(Transaction transaction) {
        return TransactionDto.builder()
                .accountNumber(transaction.getAccount().getAccountNumber())
                .transactionType(transaction.getTransactionType())
                .transactionResult(transaction.getTransactionResult())
                .amount(transaction.getAmount())
                .balanceSnapshot(transaction.getBalanceSnapshot())
                .transactionId(transaction.getTransactionId())
                .transactedAt(transaction.getTransactedAt())
                .build();
    }
}
