package com.example.simpleaccount.controller;

import com.example.simpleaccount.dto.AccountDto;
import com.example.simpleaccount.dto.CreateAccount;
import com.example.simpleaccount.dto.DeleteAccount;
import com.example.simpleaccount.exception.AccountException;
import com.example.simpleaccount.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static com.example.simpleaccount.type.ErrorCode.ACCOUNT_NOT_FOUND;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @MockBean
    private AccountService accountService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void create_account_success() throws Exception {
        // given
        given(accountService.createAccount(anyLong(), anyLong()))
                .willReturn(AccountDto.builder()
                        .userId(1L)
                        .accountNumber("1234567890")
                        .registeredAt(LocalDateTime.now())
                        .unRegisteredAt(LocalDateTime.now())
                        .build());
        // when
        // then
        mockMvc.perform(MockMvcRequestBuilders.post("/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateAccount.Request(1L, 100L)
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.accountNumber").value(1234567890))
                .andDo(print());
    }

    @Test
    void delete_account_success() throws Exception {
        // given
        given(accountService.deleteAccount(anyLong(), anyString()))
                .willReturn(AccountDto.builder()
                        .userId(1L)
                        .accountNumber("1234567890")
                        .registeredAt(LocalDateTime.now())
                        .unRegisteredAt(LocalDateTime.now())
                        .build());
        // when
        // then
        mockMvc.perform(MockMvcRequestBuilders.delete("/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new DeleteAccount.Request(1L, "1111111111")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.accountNumber").value(1234567890))
                .andDo(print());
    }

    @Test
    void get_accounts_by_userId_success() throws Exception {
        // given
        List<AccountDto> accountDtos =
                Arrays.asList(AccountDto.builder()
                                .userId(1L)
                                .accountNumber("1111111111")
                                .balance(100L)
                                .build(),
                        AccountDto.builder()
                                .userId(1L)
                                .accountNumber("2222222222")
                                .balance(200L)
                                .build()
                );

        given(accountService.getAccountsByUserId(anyLong()))
                .willReturn(accountDtos);

        // when
        // then
        mockMvc.perform(MockMvcRequestBuilders.get("/account?user_id=1"))
                .andDo(print())
                .andExpect(jsonPath("$[0].accountNumber").value("1111111111"))
                .andExpect(jsonPath("$[0].balance").value("100"))
                .andExpect(jsonPath("$[1].accountNumber").value("2222222222"))
                .andExpect(jsonPath("$[1].balance").value("200"));
    }

    @Test
    void get_account_fail() throws Exception {
        // given
        given(accountService.getAccount(anyLong()))
                .willThrow(new AccountException(ACCOUNT_NOT_FOUND));
        // when
        // then
        mockMvc.perform(MockMvcRequestBuilders.get("/account/111"))
                .andDo(print())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_NOT_FOUND"))
                .andExpect(jsonPath("$.errorMessage").value("계좌가 없습니다."))
                .andExpect(status().isOk());
    }
}