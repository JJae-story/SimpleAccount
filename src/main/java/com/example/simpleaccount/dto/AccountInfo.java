package com.example.simpleaccount.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountInfo {
    private String accountNumber;
    private Long balance;
}
