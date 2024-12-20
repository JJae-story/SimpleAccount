package com.example.simpleaccount.controller;

import com.example.simpleaccount.aop.AccountLock;
import com.example.simpleaccount.dto.QueryTransactionResponse;
import com.example.simpleaccount.dto.TransactionDto;
import com.example.simpleaccount.dto.CancelBalance;
import com.example.simpleaccount.dto.UseBalance;
import com.example.simpleaccount.exception.AccountException;
import com.example.simpleaccount.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 잔액 관련 컨트롤러
 * 1. 잔액 사용
 * 2. 잔액 사용 취소
 * 3. 거래 확인
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    @PostMapping("/transaction/use")
    @AccountLock
    public UseBalance.Response useBalance(
            @RequestBody @Valid UseBalance.Request request
    ) throws InterruptedException {
        TransactionDto transactionDto =
                transactionService.useBalance(request.getUserId(),
                        request.getAccountNumber(), request.getAmount());

        try {
            Thread.sleep(3000L);
            return UseBalance.Response.from(transactionDto);
        } catch (AccountException e) {
            log.error("Failed to use balance.");

            transactionService.saveFailedUseTransaction(
                    request.getAccountNumber(),
                    request.getAmount()
            );

            throw e;
        }
    }

    @PostMapping("/transaction/cancel")
    @AccountLock
    public CancelBalance.Response cancelBalance(
            @RequestBody @Valid CancelBalance.Request request
    ) {
        TransactionDto transactionDto =
                transactionService.cancelBalance(request.getTransactionId(),
                        request.getAccountNumber(), request.getAmount());

        try {
            return CancelBalance.Response.from(transactionDto);
        } catch (AccountException e) {
            log.error("Failed to cancel balance.");

            transactionService.saveFailedCancelTransaction(
                    request.getAccountNumber(),
                    request.getAmount()
            );

            throw e;
        }
    }

    @GetMapping("/transaction/{transactionId}")
    public QueryTransactionResponse queryTransaction(
            @PathVariable String transactionId) {

        return QueryTransactionResponse.from(
                transactionService.queryTransaction(transactionId));
    }
}
