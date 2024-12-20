package com.example.simpleaccount.service;

import com.example.simpleaccount.domain.Account;
import com.example.simpleaccount.domain.AccountUser;
import com.example.simpleaccount.domain.Transaction;
import com.example.simpleaccount.dto.TransactionDto;
import com.example.simpleaccount.exception.AccountException;
import com.example.simpleaccount.repository.AccountRepository;
import com.example.simpleaccount.repository.AccountUserRepository;
import com.example.simpleaccount.repository.TransactionRepository;
import com.example.simpleaccount.type.AccountStatus;
import com.example.simpleaccount.type.ErrorCode;
import com.example.simpleaccount.type.TransactionResultType;
import com.example.simpleaccount.type.TransactionType;
import jakarta.transaction.Transactional;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static com.example.simpleaccount.type.ErrorCode.*;
import static com.example.simpleaccount.type.TransactionResultType.F;
import static com.example.simpleaccount.type.TransactionResultType.S;
import static com.example.simpleaccount.type.TransactionType.CANCEL;
import static com.example.simpleaccount.type.TransactionType.USE;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final AccountUserRepository accountUserRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public TransactionDto useBalance(Long userId, String accountNumber,
                                     Long amount) {
        AccountUser accountUser = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(ErrorCode.USER_NOT_FOUND));

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

        validateUseBalance(accountUser, account, amount);

        account.useBalance(amount);

        Transaction transaction = saveAndGetTransaction(S, USE, account, amount);

        return TransactionDto.fromEntity(transaction);
    }

    private void validateUseBalance(AccountUser accountUser, Account account, Long amount) {
        if (!Objects.equals(accountUser.getId(), account.getAccountUser().getId())) {
            throw new AccountException(USER_ACCOUNT_UN_MATCH);
        }
        if (account.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new AccountException(ACCOUNT_ALREADY_CLOSED);
        }
        if (account.getBalance() < amount) {
            throw new AccountException(AMOUNT_EXCEED_BALANCE);
        }
    }

    @Transactional
    public void saveFailedUseTransaction(String accountNumber,Long amount) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

        saveAndGetTransaction(F, USE, account, amount);
    }

    private Transaction saveAndGetTransaction(TransactionResultType transactionResultType,
                                              TransactionType transactionType,
                                              Account account, Long amount) {

        return transactionRepository.save(
                Transaction.builder()
                        .transactionType(transactionType)
                        .transactionResult(transactionResultType)
                        .account(account)
                        .amount(amount)
                        .balanceSnapshot(account.getBalance())
                        .transactionId(UUID.randomUUID().toString().replace("-", ""))
                        .transactedAt(LocalDateTime.now())
                        .build()
        );
    }

    @Transactional
    public TransactionDto cancelBalance(String transactionId,
                                        String accountNumber,Long amount) {

        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new AccountException(TRANSACTION_NOT_FOUND));

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

        validateCancelBalance(transaction, account, amount);

        account.cancelBalance(amount);

        return TransactionDto.fromEntity(
                saveAndGetTransaction(S, CANCEL, account, amount)
        );
    }

    private void validateCancelBalance(Transaction transaction, Account account, Long amount) {
        if (!Objects.equals(transaction.getAccount().getId(), account.getId())) {
            throw new AccountException(TRANSACTION_ACCOUNT_UN_MATCH);
        }
        if (!Objects.equals(transaction.getAmount(), amount)) {
            throw new AccountException(CANCEL_MUST_FULLY);
        }
        if (transaction.getTransactedAt().isBefore(LocalDateTime.now().minusYears(1))) {
            throw new AccountException(TOO_OLD_ORDER_TO_CANCEL);
        }
    }

    @Transactional
    public void saveFailedCancelTransaction(String accountNumber, Long amount) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

        saveAndGetTransaction(F, CANCEL, account, amount);
    }

    public TransactionDto queryTransaction(String transactionId) {
        Transaction transaction = transactionRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new AccountException(TRANSACTION_NOT_FOUND));

        return TransactionDto.fromEntity(transaction);
    }
}
