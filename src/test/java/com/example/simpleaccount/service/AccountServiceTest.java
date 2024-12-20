package com.example.simpleaccount.service;


import com.example.simpleaccount.domain.Account;
import com.example.simpleaccount.domain.AccountUser;
import com.example.simpleaccount.dto.AccountDto;
import com.example.simpleaccount.exception.AccountException;
import com.example.simpleaccount.repository.AccountRepository;
import com.example.simpleaccount.repository.AccountUserRepository;
import com.example.simpleaccount.type.AccountStatus;
import com.example.simpleaccount.type.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @InjectMocks
    private AccountService accountService;

    @Test
    @DisplayName("랜덤 계좌 생성 성공")
    void create_account_success() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .name("one").build();
        accountUser.setId(1L);
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));

        String accountNumber = accountService.uniqueAccountNumber();

        given(accountRepository.save(any()))
                .willReturn(Account.builder()
                        .accountUser(accountUser)
                        .accountNumber(accountNumber)
                        .build());

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        // when
        AccountDto accountDto = accountService.createAccount(1L, 10000L);

        // then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(1L, accountDto.getUserId());
        assertNotEquals(accountNumber, captor.getValue().getAccountNumber());
    }

    @Test
    @DisplayName("계좌 생성 실패 - 해당 유저 없음")
    void create_account_user_not_found() {
        // given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 1000L));

        // then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 생성 실패 - 사용자 최대 계좌 생성 10개")
    void max_account_per_user_10() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .name("one").build();

        accountUser.setId(1L);

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));

        given(accountRepository.countByAccountUser(any()))
                .willReturn(10);

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.createAccount(1L, 1000L));

        // then
        assertEquals(ErrorCode.MAX_ACCOUNT_PER_USER_10, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 해지 성공")
    void delete_account_success() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .name("one").build();

        accountUser.setId(1L);

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));

        String accountNumber = accountService.uniqueAccountNumber();

        given(accountRepository.findByAccountNumber(accountNumber))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(accountUser)
                        .accountNumber(accountNumber)
                        .balance(0L)
                        .build()));

        ArgumentCaptor<Account> captor = ArgumentCaptor.forClass(Account.class);

        // when
        AccountDto accountDto = accountService.deleteAccount(1L, accountNumber);

        // then
        verify(accountRepository, times(1)).save(captor.capture());
        assertEquals(1L, accountDto.getUserId());
        assertEquals(accountNumber, captor.getValue().getAccountNumber());
        assertEquals(AccountStatus.CLOSED, captor.getValue().getAccountStatus());
    }

    @Test
    @DisplayName("계좌 해지 실패 - 해당 유저 없음 ")
    void delete_account_user_not_found() {
        // given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, "1111111111"));

        // then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 해지 실패 - 해당 계좌 없음")
    void delete_account_account_not_found() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .name("one").build();

        accountUser.setId(1L);

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));

        String accountNumber = accountService.uniqueAccountNumber();

        given(accountRepository.findByAccountNumber(accountNumber))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, accountNumber));

        // then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 해지 실패 - 사용자 아이디와 소유주가 다른 경우")
    void delete_account_fail_user_un_matched() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .name("one").build();

        accountUser.setId(1L);

        AccountUser otherUser = AccountUser.builder()
                .name("two").build();

        accountUser.setId(2L);

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));

        String accountNumber = accountService.uniqueAccountNumber();

        given(accountRepository.findByAccountNumber(accountNumber))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(otherUser)
                        .accountNumber(accountNumber)
                        .balance(0L)
                        .build()));

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, accountNumber));

        // then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 해지 실패 - 이미 해지된 계좌인 경우")
    void delete_account_fail_account_already_closed() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .name("one").build();

        accountUser.setId(1L);

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));

        String accountNumber = accountService.uniqueAccountNumber();

        given(accountRepository.findByAccountNumber(accountNumber))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(accountUser)
                        .accountNumber(accountNumber)
                        .accountStatus(AccountStatus.CLOSED)
                        .balance(0L)
                        .build()));

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, accountNumber));

        // then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_CLOSED, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 해지 실패 - 잔액이 남아 있는 경우")
    void delete_account_fail_balance_not_empty() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .name("one").build();

        accountUser.setId(1L);

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));

        String accountNumber = accountService.uniqueAccountNumber();

        given(accountRepository.findByAccountNumber(accountNumber))
                .willReturn(Optional.of(Account.builder()
                        .accountUser(accountUser)
                        .accountNumber(accountNumber)
                        .balance(100L)
                        .build()));

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.deleteAccount(1L, accountNumber));

        // then
        assertEquals(ErrorCode.ACCOUNT_BALANCE_NOT_EMPTY, exception.getErrorCode());
    }

    @Test
    @DisplayName("계좌 확인 성공")
    void get_account_by_userId_success() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .name("one").build();

        accountUser.setId(1L);

        List<Account> accounts = Arrays.asList(
                Account.builder()
                        .accountUser(accountUser)
                        .accountNumber("1111111111")
                        .balance(100L)
                        .build(),
                Account.builder()
                        .accountUser(accountUser)
                        .accountNumber("2222222222")
                        .balance(200L)
                        .build(),
                Account.builder()
                        .accountUser(accountUser)
                        .accountNumber("3333333333")
                        .balance(300L)
                        .build()
        );

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));

        given(accountRepository.findByAccountUser(any()))
                .willReturn(accounts);

        // when
        List<AccountDto> accountDtos =
                accountService.getAccountsByUserId(1L);

        // then
        assertEquals(3, accountDtos.size());
        assertEquals("1111111111", accountDtos.get(0).getAccountNumber());
        assertEquals(100L, accountDtos.get(0).getBalance());
        assertEquals("2222222222", accountDtos.get(1).getAccountNumber());
        assertEquals(200L, accountDtos.get(1).getBalance());
        assertEquals("3333333333", accountDtos.get(2).getAccountNumber());
        assertEquals(300L, accountDtos.get(2).getBalance());
    }

    @Test
    @DisplayName("계좌 확인 실패 - 사용자 없는 경우")
    void get_account_fail() {
        // given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> accountService.getAccountsByUserId(1L));

        // then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }
}