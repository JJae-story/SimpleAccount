package com.example.simpleaccount.domain;

import com.example.simpleaccount.exception.AccountException;
import com.example.simpleaccount.type.AccountStatus;
import com.example.simpleaccount.type.ErrorCode;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

import static com.example.simpleaccount.type.ErrorCode.AMOUNT_EXCEED_BALANCE;
import static com.example.simpleaccount.type.ErrorCode.INVALID_REQUEST;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Account extends BaseEntity {
    @ManyToOne
    private AccountUser accountUser;
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus;
    private Long balance;

    private LocalDateTime registeredAt;
    private LocalDateTime unRegisteredAt;

    public void useBalance(Long amount) {
        if (amount > balance) {
            throw new AccountException(AMOUNT_EXCEED_BALANCE);
        } else {
            balance -= amount;
        }
    }

    public void cancelBalance(Long amount) {
        if (amount < 0) {
            throw new AccountException(INVALID_REQUEST);
        } else {
            balance += amount;
        }
    }
}