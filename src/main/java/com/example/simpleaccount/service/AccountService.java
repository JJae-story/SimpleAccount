package com.example.simpleaccount.service;

import com.example.simpleaccount.domain.Account;
import com.example.simpleaccount.domain.AccountUser;
import com.example.simpleaccount.dto.AccountDto;
import com.example.simpleaccount.exception.AccountException;
import com.example.simpleaccount.repository.AccountRepository;
import com.example.simpleaccount.repository.AccountUserRepository;
import com.example.simpleaccount.type.ErrorCode;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

import static com.example.simpleaccount.type.AccountStatus.ACTIVE;
import static com.example.simpleaccount.type.AccountStatus.CLOSED;
import static com.example.simpleaccount.type.ErrorCode.*;

@Service
@RequiredArgsConstructor
public class AccountService {
    private final AccountRepository accountRepository;
    private final AccountUserRepository accountUserRepository;

    /**
     * 사용자가 있는지 조회
     * 계좌 번호 랜덤 생성 (10자리)
     * 계좌 저장
     */
    @Transactional
    public AccountDto createAccount(Long userId, Long initialBalance) {
        AccountUser accountUser = getAccountUser(userId);

        // 계좌 10개인지 확인
        validateCreateAccount(accountUser);

        String accountNumber = uniqueAccountNumber();

        Account account = accountRepository.save(
                Account.builder()
                        .accountUser(accountUser)
                        .accountStatus(ACTIVE)
                        .accountNumber(accountNumber)
                        .balance(initialBalance)
                        .registeredAt(LocalDateTime.now())
                        .build()
        );

        return AccountDto.fromEntity(account);
    }

    private void validateCreateAccount(AccountUser accountUser) throws AccountException {
        if (accountRepository.countByAccountUser(accountUser) >= 10) {
            throw new AccountException(MAX_ACCOUNT_PER_USER_10);
        }
    }

    // 랜덤 계좌번호 생성 (10자리)
    private String randomAccountNumber() {
        Random random = new Random();
        StringBuilder accountNumber = new StringBuilder();

        for (int i = 1; i <= 10; i++) {
            accountNumber.append(random.nextInt(10));
        }

        return accountNumber.toString();
    }

    // 중복 체크 및 재시도
    public String uniqueAccountNumber() {
        String accountNumber = randomAccountNumber();
        // 중복되면 다시 생성
        while (accountRepository.existsByAccountNumber(accountNumber)) {
            accountNumber = randomAccountNumber();
        }

        return accountNumber;
    }


    @Transactional
    public Account getAccount(Long id) {
        if (id < 0) {
            throw new RuntimeException("Minus");
        }
        return accountRepository.findById(id).get();
    }

    @Transactional
    public AccountDto deleteAccount(Long userId, String accountNumber) {
        AccountUser accountUser = getAccountUser(userId);

        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountException(ACCOUNT_NOT_FOUND));

        validateDeleteAccount(accountUser, account);

        account.setAccountStatus(CLOSED);
        account.setUnRegisteredAt(LocalDateTime.now());

        // 테스트용
        accountRepository.save(account);

        return AccountDto.fromEntity(account);
    }

    private AccountUser getAccountUser(Long userId) {
        AccountUser accountUser = accountUserRepository.findById(userId)
                .orElseThrow(() -> new AccountException(USER_NOT_FOUND));
        return accountUser;
    }

    private void validateDeleteAccount(AccountUser accountUser, Account account) throws AccountException {
        if (!Objects.equals(accountUser.getId(), account.getAccountUser().getId())) {
            throw new AccountException(USER_ACCOUNT_UN_MATCH);
        }
        if (account.getAccountStatus() == CLOSED) {
            throw new AccountException(ACCOUNT_ALREADY_CLOSED);
        }
        if (account.getBalance() > 0) {
            throw new AccountException(ACCOUNT_BALANCE_NOT_EMPTY);
        }
    }

    @Transactional
    public List<AccountDto> getAccountsByUserId(Long userId) {
        AccountUser accountUser = getAccountUser(userId);

        List<Account> accounts =
                accountRepository.findByAccountUser(accountUser);

        return accounts.stream()
                .map(AccountDto::fromEntity)
                .collect(Collectors.toList());
    }
}
