package com.odc.paymentservice.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateWithdrawalRequest {
    @NotNull(message = "Số tiền không được để trống")
    @Min(value = 10000, message = "Số tiền rút tối thiểu là 10.000 VND")
    private BigDecimal amount;

    @NotBlank(message = "Tên ngân hàng không được để trống")
    private String bankName;

    @NotBlank(message = "Số tài khoản không được để trống")
    private String accountNumber;

    @NotBlank(message = "Tên chủ tài khoản không được để trống")
    private String accountName;

    @NotBlank(message = "Mã bin ngân hàng không được để trống")
    private String bin;
}