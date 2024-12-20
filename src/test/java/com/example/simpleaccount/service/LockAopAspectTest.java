package com.example.simpleaccount.service;

import com.example.simpleaccount.dto.UseBalance;
import com.example.simpleaccount.exception.AccountException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.example.simpleaccount.type.ErrorCode.ACCOUNT_NOT_FOUND;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LockAopAspectTest {

    @Mock
    private LockService lockService;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @InjectMocks
    private LockAopAspect lockAopAspect;

    @Test
    void lock_and_unlock_success() throws Throwable {
        // given
        ArgumentCaptor<String> lockArgumentCaptor =
                ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> unlockArgumentCaptor =
                ArgumentCaptor.forClass(String.class);

        UseBalance.Request request = new UseBalance.Request(1L, "1111111111", 1000L);

        // when
        lockAopAspect.aroundMethod(proceedingJoinPoint, request);

        // then
        verify(lockService, times(1))
                .lock(lockArgumentCaptor.capture());
        verify(lockService, times(1))
                .unlock(unlockArgumentCaptor.capture());

        assertEquals("1111111111", lockArgumentCaptor.getValue());
        assertEquals("1111111111", unlockArgumentCaptor.getValue());
    }

    @Test
    void lock_and_unlock_even_if_throw() throws Throwable {
        // given
        ArgumentCaptor<String> lockArgumentCaptor =
                ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> unlockArgumentCaptor =
                ArgumentCaptor.forClass(String.class);

        UseBalance.Request request = new UseBalance.Request(1L, "2222222222", 1000L);

        given(proceedingJoinPoint.proceed())
                .willThrow(new AccountException(ACCOUNT_NOT_FOUND));

        // when
        assertThrows(AccountException.class,
                () -> lockAopAspect.aroundMethod(proceedingJoinPoint, request));

        // then
        verify(lockService, times(1))
                .lock(lockArgumentCaptor.capture());
        verify(lockService, times(1))
                .unlock(unlockArgumentCaptor.capture());

        assertEquals("2222222222", lockArgumentCaptor.getValue());
        assertEquals("2222222222", unlockArgumentCaptor.getValue());
    }

}