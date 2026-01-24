package com.odc.paymentservice.service;

import com.odc.common.constant.Status;
import com.odc.common.exception.BusinessException;
import com.odc.common.util.BankInfoUtil;
import com.odc.paymentservice.entity.WithdrawalRequest;
import com.odc.paymentservice.repository.WithdrawalRequestRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import vn.payos.PayOS;
import vn.payos.model.v1.payouts.Payout;
import vn.payos.model.v1.payouts.batch.PayoutBatchItem;
import vn.payos.model.v1.payouts.batch.PayoutBatchRequest;
import vn.payos.service.blocking.v1.payouts.batch.BatchService;
import vn.payos.service.blocking.v1.payouts.batch.BatchServiceImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class FeeDistributionServiceImpl implements FeeDistributionService {

    private static final int BATCH_SIZE = 20;

    private final WithdrawalRequestRepository withdrawalRepository;
    private final PayOS payOS;

    @Override
    public void processFeeDistribution() {

        log.info("[FeeDistribution][START] Job triggered");

        int totalProcessed = 0;
        int batchCount = 0;

        while (true) {
            List<WithdrawalRequest> withdrawals = fetchBatch();

            if (withdrawals.isEmpty()) {
                log.info("[FeeDistribution][END] No more withdrawals. totalProcessed={}, batches={}",
                        totalProcessed, batchCount);
                return;
            }

            batchCount++;
            log.info("[FeeDistribution][BATCH-{}] Processing {} withdrawals",
                    batchCount, withdrawals.size());

            try {
                processSingleBatch(withdrawals);
                totalProcessed += withdrawals.size();
            } catch (Exception ex) {
                log.error("[FeeDistribution][BATCH-{}][FAILED] Stop job", batchCount, ex);
                return;
            }

            // Nếu batch BATCH_SIZE -> không còn batch tiếp
            if (withdrawals.size() < BATCH_SIZE) {
                log.info("[FeeDistribution][END] Last batch processed. totalProcessed={}",
                        totalProcessed);
                return;
            }
        }
    }

    private void processSingleBatch(List<WithdrawalRequest> withdrawals) {

        markProcessing(withdrawals);

        Payout payout;
        try {
            payout = sendBatchToPayOS(withdrawals);
        } catch (Exception ex) {
            rollbackProcessing(withdrawals);
            throw ex;
        }

        markSubmitted(withdrawals, payout);

        log.info("[FeeDistribution][BATCH-DONE] payoutId={}, size={}",
                payout.getId(), withdrawals.size());
    }

    private List<WithdrawalRequest> fetchBatch() {
        Pageable pageable = PageRequest.of(0, BATCH_SIZE);
        LocalDate today = LocalDate.now();

        List<WithdrawalRequest> list =
                withdrawalRepository.findBatchToProcess(today, pageable);

        log.info("[FeeDistribution][FETCH] Found {} withdrawals", list.size());
        return list;
    }

    @Transactional
    protected void markProcessing(List<WithdrawalRequest> withdrawals) {
        withdrawals.forEach(w ->
                w.setStatus(Status.PROCESSING.toString()));
        withdrawalRepository.saveAll(withdrawals);

        log.info("[FeeDistribution][LOCK] Marked {} withdrawals as PROCESSING",
                withdrawals.size());
    }

    protected Payout sendBatchToPayOS(List<WithdrawalRequest> withdrawals) {

        BatchService batchService = new BatchServiceImpl(payOS);
        PayoutBatchRequest request = buildPayoutBatchRequest(withdrawals);

        log.info("[FeeDistribution][PAYOS] Sending batch reference={}",
                request.getReferenceId());

        return batchService.create(
                request,
                generateIdempotencyKey()
        );
    }

    @Transactional
    protected void markSubmitted(List<WithdrawalRequest> withdrawals,
                                 Payout payout) {

        withdrawals.forEach(w -> {
            w.setStatus(Status.SUBMITTED.toString());
            w.setExternalRef(payout.getId());
        });

        withdrawalRepository.saveAll(withdrawals);

        log.info("[FeeDistribution][SUBMITTED] payoutId={}, state={}",
                payout.getId(), payout.getApprovalState());
    }

    @Transactional
    protected void rollbackProcessing(List<WithdrawalRequest> withdrawals) {

        withdrawals.forEach(w ->
                w.setStatus(Status.APPROVED.toString()));

        withdrawalRepository.saveAll(withdrawals);

        log.error("[FeeDistribution][ROLLBACK] Reverted to APPROVED");
    }

    private PayoutBatchRequest buildPayoutBatchRequest(
            List<WithdrawalRequest> withdrawals) {

        PayoutBatchRequest.PayoutBatchRequestBuilder builder =
                PayoutBatchRequest.builder()
                        .referenceId("withdrawal-batch-" + System.currentTimeMillis());

        for (WithdrawalRequest w : withdrawals) {

            Map<String, String> bankInfo = w.getBankInfo();
            if (bankInfo == null) {
                throw new BusinessException("Withdrawal " + w.getId() + " missing bankInfo");
            }

            String bin = BankInfoUtil.getBin(bankInfo.get("bankName"));
            if (bin == null) {
                throw new BusinessException("Unsupported bank: " + bankInfo.get("bankName"));
            }

            builder.payout(
                    PayoutBatchItem.builder()
                            .referenceId(w.getId().toString())
                            .amount(w.getAmount().longValueExact())
                            .description("Withdrawal payout")
                            .toBin(bin)
                            .toAccountNumber(bankInfo.get("accountNumber"))
                            .build()
            );
        }

        return builder.build();
    }

    private String generateIdempotencyKey() {
        return "payout-" + System.currentTimeMillis();
    }
}