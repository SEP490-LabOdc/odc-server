package com.odc.paymentservice.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreatePaymentResponse {
    private String bin;           // Mã ngân hàng (nếu muốn custom QR)
    private String accountNumber; // Số tài khoản (nếu muốn custom QR)
    private Long orderCode;       // Mã đơn hàng
    private Integer amount;
    private String description;
    private String checkoutUrl;   // Link thanh toán (FE redirect user sang đây)
    private String qrCode;        // Mã QR dạng chuỗi (để FE tự generate ảnh nếu muốn)
    private String status;
}