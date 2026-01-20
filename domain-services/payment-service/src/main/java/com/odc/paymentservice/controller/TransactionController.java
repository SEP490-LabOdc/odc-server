package com.odc.paymentservice.controller;

import com.odc.common.dto.ApiResponse;
import com.odc.common.dto.PaginatedResult;
import com.odc.paymentservice.dto.response.TransactionResponse;
import com.odc.paymentservice.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/project/{projectId}")
    public ResponseEntity<ApiResponse<PaginatedResult<TransactionResponse>>> getTransactionsByProjectId(
            @PathVariable UUID projectId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        return ResponseEntity.ok(
                transactionService.getTransactionsByProjectId(projectId, page, size)
        );
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionDetail(
            @PathVariable UUID transactionId) {

        return ResponseEntity.ok(
                transactionService.getTransactionDetail(transactionId)
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PaginatedResult<TransactionResponse>>> getAllTransactions(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        return ResponseEntity.ok(
                transactionService.getAllTransactions(page, size)
        );
    }

    @GetMapping("/my-transactions")
    public ResponseEntity<ApiResponse<PaginatedResult<TransactionResponse>>> getMyTransactions(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        return ResponseEntity.ok(
                transactionService.getMyTransactions(page, size)
        );
    }
}
