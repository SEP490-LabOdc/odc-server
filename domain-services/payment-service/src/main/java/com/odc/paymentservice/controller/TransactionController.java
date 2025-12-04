package com.odc.paymentservice.controller;

import com.odc.common.dto.ApiResponse;
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
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getTransactionsByProjectId(
            @PathVariable UUID projectId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        ApiResponse<Page<TransactionResponse>> response =
                transactionService.getTransactionsByProjectId(projectId, pageable);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<TransactionResponse>> getTransactionDetail(
            @PathVariable UUID transactionId) {

        ApiResponse<TransactionResponse> response =
                transactionService.getTransactionDetail(transactionId);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getAllTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("ASC")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        ApiResponse<Page<TransactionResponse>> response =
                transactionService.getAllTransactions(pageable);

        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}