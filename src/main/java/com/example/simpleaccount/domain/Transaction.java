package com.example.simpleaccount.domain;

import com.example.simpleaccount.type.AccountStatus;
import com.example.simpleaccount.type.TransactionResultType;
import com.example.simpleaccount.type.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Transaction extends BaseEntity {
    @Enumerated(EnumType.STRING)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    private TransactionResultType transactionResult;

    @ManyToOne
    private Account account;
    private Long amount;
    private Long balanceSnapshot;

    private String transactionId;
    private LocalDateTime transactedAt;
}
