package com.example.simpleaccount.controller;

import com.example.simpleaccount.dto.AccountDto;
import com.example.simpleaccount.dto.TransactionDto;
import com.example.simpleaccount.dto.CancelBalance;
import com.example.simpleaccount.dto.UseBalance;
import com.example.simpleaccount.service.TransactionService;
import com.example.simpleaccount.type.TransactionType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
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

import static com.example.simpleaccount.type.TransactionResultType.S;
import static com.example.simpleaccount.type.TransactionType.USE;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {
    @MockBean
    private TransactionService transactionService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("잔액 사용 성공")
    void use_balance_success() throws Exception {
        // given
        given(transactionService.useBalance(anyLong(), anyString(), anyLong()))
                .willReturn(TransactionDto.builder()
                        .accountNumber("1111111111")
                        .transactedAt(LocalDateTime.now())
                        .amount(11111L)
                        .transactionId("transactionId")
                        .transactionResult(S)
                        .build());
        // when
        // then
        mockMvc.perform(MockMvcRequestBuilders.post("/transaction/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new UseBalance.Request(1L, "2222222222", 22222L)
                        ))
                ).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1111111111"))
                .andExpect(jsonPath("$.transactionResult").value("S"))
                .andExpect(jsonPath("$.transactionId").value("transactionId"))
                .andExpect(jsonPath("$.amount").value("11111"));

    }

    @Test
    @DisplayName("잔액 사용 취소 성공")
    void cancel_balance_success() throws Exception {
        // given
        given(transactionService.cancelBalance(anyString(), anyString(), anyLong()))
                .willReturn(TransactionDto.builder()
                        .accountNumber("1111111111")
                        .transactedAt(LocalDateTime.now())
                        .amount(11111L)
                        .transactionId("transactionIdForCancel")
                        .transactionResult(S)
                        .build());
        // when
        // then
        mockMvc.perform(MockMvcRequestBuilders.post("/transaction/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CancelBalance.Request("transactionId", "2222222222", 22222L)
                        ))
                ).andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1111111111"))
                .andExpect(jsonPath("$.transactionResult").value("S"))
                .andExpect(jsonPath("$.transactionId").value("transactionIdForCancel"))
                .andExpect(jsonPath("$.amount").value("11111"));

    }

    @Test
    @DisplayName("거래 확인 성공")
    void get_query_transaction() throws Exception {
        // given
        given(transactionService.queryTransaction(anyString()))
                .willReturn(TransactionDto.builder()
                        .accountNumber("1111111111")
                        .transactionType(USE)
                        .transactedAt(LocalDateTime.now())
                        .amount(11111L)
                        .transactionId("transactionIdForCancel")
                        .transactionResult(S)
                        .build());

        // when
        // then
        mockMvc.perform(MockMvcRequestBuilders.get("/transaction/11111"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountNumber").value("1111111111"))
                .andExpect(jsonPath("$.transactionType").value("USE"))
                .andExpect(jsonPath("$.transactionId").value("transactionIdForCancel"))
                .andExpect(jsonPath("$.amount").value("11111"));
    }
}