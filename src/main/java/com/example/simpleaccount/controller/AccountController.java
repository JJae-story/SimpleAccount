package com.example.simpleaccount.controller;

import com.example.simpleaccount.domain.Account;
import com.example.simpleaccount.dto.AccountDto;
import com.example.simpleaccount.dto.AccountInfo;
import com.example.simpleaccount.dto.CreateAccount;
import com.example.simpleaccount.dto.DeleteAccount;
import com.example.simpleaccount.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    // 계좌 생성
    @PostMapping("/account")
    public CreateAccount.Response createAccount(
            @RequestBody @Valid CreateAccount.Request request) {

        AccountDto accountDto = accountService.createAccount(
                request.getUserId(),
                request.getInitialBalance()
        );

        return CreateAccount.Response.from(accountDto);
    }

    // 계좌 해지
    @DeleteMapping("/account")
    public DeleteAccount.Response deleteAccount(
            @RequestBody @Valid DeleteAccount.Request request) {

        AccountDto accountDto = accountService.deleteAccount(
                request.getUserId(),
                request.getAccountNumber()
        );

        return DeleteAccount.Response.from(accountDto);
    }

    @GetMapping("/account")
    public List<AccountInfo> getAccountsByUserId(
            @RequestParam("user_id") Long userId) {

        return accountService.getAccountsByUserId(userId)
                .stream().map(accountDto -> AccountInfo.builder()
                        .accountNumber(accountDto.getAccountNumber())
                        .balance(accountDto.getBalance())
                        .build()).collect(Collectors.toList());
    }

    @GetMapping("/account/{id}")
    public Account getAccount(
            @PathVariable Long id) {
        return accountService.getAccount(id);
    }
}