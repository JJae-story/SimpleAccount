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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.example.simpleaccount.type.AccountStatus.ACTIVE;
import static com.example.simpleaccount.type.TransactionResultType.F;
import static com.example.simpleaccount.type.TransactionResultType.S;
import static com.example.simpleaccount.type.TransactionType.CANCEL;
import static com.example.simpleaccount.type.TransactionType.USE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {
    public static final long USE_AMOUNT = 200L;
    public static final long CANCEL_AMOUNT = 200L;
    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountUserRepository accountUserRepository;

    @Mock
    private AccountService accountService;

    @Mock
    private TransactionDto transactionDto;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    @DisplayName("잔액 사용 성공")
    void use_balance_success() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .name("one").build();

        accountUser.setId(1L);

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));

        String accountNumber = accountService.uniqueAccountNumber();

        Account account = Account.builder()
                .accountUser(accountUser)
                .accountStatus(ACTIVE)
                .accountNumber(accountNumber)
                .balance(10000L)
                .build();

        given(accountRepository.findByAccountNumber(accountNumber))
                .willReturn(Optional.of(account));

        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(USE)
                        .transactionResult(S)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .balanceSnapshot(9000L)
                        .build());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        // when
        TransactionDto transactionDto = transactionService.useBalance(1L, accountNumber, USE_AMOUNT);

        // then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(USE_AMOUNT, captor.getValue().getAmount());
        assertEquals(9800L, captor.getValue().getBalanceSnapshot());
        assertEquals(S, transactionDto.getTransactionResult());
        assertEquals(USE, transactionDto.getTransactionType());
        assertEquals(9000L, transactionDto.getBalanceSnapshot());
        assertEquals(1000L, transactionDto.getAmount());
    }

    @Test
    @DisplayName("잔액 사용 실패 - 해당 유저 없음")
    void use_balance_user_not_found() {
        // given
        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.empty());

        String accountNumber = accountService.uniqueAccountNumber();

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, accountNumber, USE_AMOUNT));

        // then
        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 실패 - 해당 계좌 없음")
    void use_balance_account_not_found() {
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
                () -> transactionService.useBalance(1L, accountNumber, USE_AMOUNT));

        // then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 실패 - 사용자 아이디와 소유주가 다른 경우")
    void use_balance_fail_user_un_matched() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .name("one").build();

        accountUser.setId(1L);

        AccountUser otherUser = AccountUser.builder()
                .name("two").build();

        otherUser.setId(2L);

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
                () -> transactionService.useBalance(1L, accountNumber, USE_AMOUNT));

        // then
        assertEquals(ErrorCode.USER_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 실패 - 이미 해지된 계좌인 경우")
    void use_balance_fail_account_already_closed() {
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
                () -> transactionService.useBalance(1L, accountNumber, USE_AMOUNT));

        // then
        assertEquals(ErrorCode.ACCOUNT_ALREADY_CLOSED, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 실패 - 거래 금액이 잔액보다 큰 경우")
    void use_balance_fail() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .name("one").build();

        accountUser.setId(1L);

        given(accountUserRepository.findById(anyLong()))
                .willReturn(Optional.of(accountUser));

        String accountNumber = accountService.uniqueAccountNumber();

        Account account = Account.builder()
                .accountUser(accountUser)
                .accountStatus(ACTIVE)
                .accountNumber(accountNumber)
                .balance(100L)
                .build();

        given(accountRepository.findByAccountNumber(accountNumber))
                .willReturn(Optional.of(account));

        // when
        // then

        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.useBalance(1L, accountNumber, USE_AMOUNT));

        verify(transactionRepository, times(0)).save(any());
        assertEquals(ErrorCode.AMOUNT_EXCEED_BALANCE, exception.getErrorCode());
    }

    @Test
    @DisplayName("실패 트랜잭션 저장 성공")
    void save_failed_use_transaction() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .name("one").build();

        accountUser.setId(1L);

        String accountNumber = accountService.uniqueAccountNumber();

        Account account = Account.builder()
                .accountUser(accountUser)
                .accountStatus(ACTIVE)
                .accountNumber(accountNumber)
                .balance(10000L)
                .build();

        given(accountRepository.findByAccountNumber(accountNumber))
                .willReturn(Optional.of(account));

        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(USE)
                        .transactionResult(S)
                        .transactionId("transactionId")
                        .transactedAt(LocalDateTime.now())
                        .amount(1000L)
                        .balanceSnapshot(9000L)
                        .build());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        // when
        transactionService.saveFailedUseTransaction(accountNumber, USE_AMOUNT);

        // then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(USE_AMOUNT, captor.getValue().getAmount());
        assertEquals(10000L, captor.getValue().getBalanceSnapshot());
        assertEquals(F, captor.getValue().getTransactionResult());
    }

    @Test
    @DisplayName("잔액 사용 취소 성공")
    void cancel_balance_success() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .name("one").build();

        accountUser.setId(1L);

        String accountNumber = accountService.uniqueAccountNumber();

        Account account = Account.builder()
                .accountUser(accountUser)
                .accountStatus(ACTIVE)
                .accountNumber(accountNumber)
                .balance(10000L)
                .build();

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResult(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        given(accountRepository.findByAccountNumber(accountNumber))
                .willReturn(Optional.of(account));

        given(transactionRepository.save(any()))
                .willReturn(Transaction.builder()
                        .account(account)
                        .transactionType(CANCEL)
                        .transactionResult(S)
                        .transactionId("transactionIdForCancel")
                        .transactedAt(LocalDateTime.now())
                        .amount(CANCEL_AMOUNT)
                        .balanceSnapshot(10000L)
                        .build());

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);

        // when
        TransactionDto transactionDto = transactionService.cancelBalance("transactionIdForCancel", accountNumber, CANCEL_AMOUNT);

        // then
        verify(transactionRepository, times(1)).save(captor.capture());
        assertEquals(CANCEL_AMOUNT, captor.getValue().getAmount());
        assertEquals(10000L + CANCEL_AMOUNT, captor.getValue().getBalanceSnapshot());
        assertEquals(S, transactionDto.getTransactionResult());
        assertEquals(CANCEL, transactionDto.getTransactionType());
        assertEquals(10000L, transactionDto.getBalanceSnapshot());
        assertEquals(CANCEL_AMOUNT, transactionDto.getAmount());
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 해당 계좌 없음")
    void cancel_balance_account_not_found() {
        // given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(Transaction.builder().build()));

        String accountNumber = accountService.uniqueAccountNumber();

        given(accountRepository.findByAccountNumber(accountNumber))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionIdForCancel", accountNumber, CANCEL_AMOUNT));

        // then
        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 해당 거래 없음")
    void cancel_balance_transaction_not_found() {
        // given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionIdForCancel", "accountNumber", CANCEL_AMOUNT));

        // then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 거래와 계좌가 매칭되지 않음")
    void cancel_balance_transaction_account_un_matched() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .name("one").build();
        accountUser.setId(1L);

        String accountNumber = accountService.uniqueAccountNumber();

        Account account = Account.builder()
                .accountUser(accountUser)
                .accountStatus(ACTIVE)
                .accountNumber(accountNumber)
                .balance(10000L)
                .build();

        account.setId(1L);

        Account otherAccount = Account.builder()
                .accountUser(accountUser)
                .accountStatus(ACTIVE)
                .accountNumber(accountNumber)
                .balance(10000L)
                .build();

        otherAccount.setId(2L);

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResult(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));


        given(accountRepository.findByAccountNumber(accountNumber))
                .willReturn(Optional.of(otherAccount));

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionIdForCancel", accountNumber, CANCEL_AMOUNT));

        // then
        assertEquals(ErrorCode.TRANSACTION_ACCOUNT_UN_MATCH, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 거래 금액과 취소 금액이 다름")
    void cancel_balance_cancel_must_fully() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .name("one").build();
        accountUser.setId(1L);

        String accountNumber = accountService.uniqueAccountNumber();

        Account account = Account.builder()
                .accountUser(accountUser)
                .accountStatus(ACTIVE)
                .accountNumber(accountNumber)
                .balance(10000L)
                .build();

        account.setId(1L);

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResult(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT + 1000L)
                .balanceSnapshot(9000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));


        given(accountRepository.findByAccountNumber(accountNumber))
                .willReturn(Optional.of(account));

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionIdForCancel", accountNumber, CANCEL_AMOUNT));

        // then
        assertEquals(ErrorCode.CANCEL_MUST_FULLY, exception.getErrorCode());
    }

    @Test
    @DisplayName("잔액 사용 취소 실패 - 1년이 지난 거래는 취소 불가능")
    void cancel_balance_too_old_order_to_cancel() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .name("one").build();
        accountUser.setId(1L);

        String accountNumber = accountService.uniqueAccountNumber();

        Account account = Account.builder()
                .accountUser(accountUser)
                .accountStatus(ACTIVE)
                .accountNumber(accountNumber)
                .balance(10000L)
                .build();

        account.setId(1L);

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResult(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now().minusYears(1))
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));


        given(accountRepository.findByAccountNumber(accountNumber))
                .willReturn(Optional.of(account));

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.cancelBalance("transactionIdForCancel", accountNumber, CANCEL_AMOUNT));

        // then
        assertEquals(ErrorCode.TOO_OLD_ORDER_TO_CANCEL, exception.getErrorCode());
    }

    @Test
    @DisplayName("거래 확인 성공")
    void query_transaction_success() {
        // given
        AccountUser accountUser = AccountUser.builder()
                .name("one").build();

        accountUser.setId(1L);

        String accountNumber = accountService.uniqueAccountNumber();

        Account account = Account.builder()
                .accountUser(accountUser)
                .accountStatus(ACTIVE)
                .accountNumber(accountNumber)
                .balance(10000L)
                .build();

        account.setId(1L);

        Transaction transaction = Transaction.builder()
                .account(account)
                .transactionType(USE)
                .transactionResult(S)
                .transactionId("transactionId")
                .transactedAt(LocalDateTime.now())
                .amount(CANCEL_AMOUNT)
                .balanceSnapshot(9000L)
                .build();

        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.of(transaction));

        // when
        TransactionDto transactionDto = transactionService.queryTransaction("trxId");

        // then
        assertEquals(USE, transactionDto.getTransactionType());
        assertEquals(S, transactionDto.getTransactionResult());
        assertEquals(CANCEL_AMOUNT, transactionDto.getAmount());
        assertEquals("transactionId", transactionDto.getTransactionId());
    }

    @Test
    @DisplayName("거래 확인 실패 - 해당 거래 없음")
    void query_transaction_not_found() {
        // given
        given(transactionRepository.findByTransactionId(anyString()))
                .willReturn(Optional.empty());

        // when
        AccountException exception = assertThrows(AccountException.class,
                () -> transactionService.queryTransaction("transactionId"));

        // then
        assertEquals(ErrorCode.TRANSACTION_NOT_FOUND, exception.getErrorCode());
    }
}