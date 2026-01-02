package com.odc.paymentservice.service;

import com.odc.common.constant.PaymentConstant;
import com.odc.common.constant.Role;
import com.odc.common.constant.Status;
import com.odc.common.dto.ApiResponse;
import com.odc.common.exception.BusinessException;
import com.odc.common.exception.ResourceNotFoundException;
import com.odc.paymentservice.dto.request.CreateDisbursementRequest;
import com.odc.paymentservice.dto.request.MilestoneDisbursement;
import com.odc.paymentservice.dto.request.MilestoneDisbursementRequest;
import com.odc.paymentservice.dto.response.DisbursementCalculationResponse;
import com.odc.paymentservice.dto.response.DisbursementResponse;
import com.odc.paymentservice.dto.response.LeaderDisbursementInfo;
import com.odc.paymentservice.entity.Disbursement;
import com.odc.paymentservice.entity.SystemConfig;
import com.odc.paymentservice.entity.Transaction;
import com.odc.paymentservice.entity.Wallet;
import com.odc.paymentservice.repository.DisbursementRepository;
import com.odc.paymentservice.repository.SystemConfigRepository;
import com.odc.paymentservice.repository.TransactionRepository;
import com.odc.paymentservice.repository.WalletRepository;
import com.odc.projectservice.v1.GetLeaderInfoRequest;
import com.odc.projectservice.v1.GetLeaderInfoResponse;
import com.odc.projectservice.v1.LeaderInfo;
import com.odc.projectservice.v1.ProjectServiceGrpc;
import io.grpc.ManagedChannel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DisbursementServiceImpl implements DisbursementService {
    private final SystemConfigRepository systemConfigRepository;
    private final ManagedChannel projectServiceChannel;
    private final DisbursementRepository disbursementRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    @Override
    public DisbursementCalculationResponse calculatePreview(UUID milestoneId, BigDecimal totalAmount) {
        SystemConfig systemConfig = systemConfigRepository
                .findByName("fee-distribution")
                .orElseThrow(() -> new BusinessException("Không tìm thấy cấu hình fee-distribution"));

        Map<String, Object> properties = systemConfig.getProperties();
        BigDecimal talentShareRate = new BigDecimal(properties.get("talentShareRate").toString());
        BigDecimal mentorShareRate = new BigDecimal(properties.get("mentorShareRate").toString());
        BigDecimal systemFeeRate = new BigDecimal(properties.get("systemFeeRate").toString());

        BigDecimal systemFee = totalAmount.multiply(systemFeeRate);
        BigDecimal mentorShare = totalAmount.multiply(mentorShareRate);
        BigDecimal talentShare = totalAmount.multiply(talentShareRate);

        ProjectServiceGrpc.ProjectServiceBlockingStub projectServiceBlockingStub = ProjectServiceGrpc.newBlockingStub(projectServiceChannel);
        GetLeaderInfoResponse leaderInfoResponse = projectServiceBlockingStub.getLeaderInfo(GetLeaderInfoRequest
                .newBuilder()
                .setMilestoneId(milestoneId.toString())
                .build());

        LeaderInfo mentor = null;
        LeaderInfo talent = null;

        for (LeaderInfo leader : leaderInfoResponse.getLeadersList()) {
            if (Role.MENTOR.toString().equals(leader.getRoleInProject())) {
                mentor = leader;
            } else if (Role.TALENT.toString().equals(leader.getRoleInProject())) {
                talent = leader;
            }
        }

        // Nếu không tìm thấy mentor hoặc talent thì throw exception
        if (mentor == null) {
            throw new BusinessException("Không tìm thấy leader MENTOR cho milestone");
        }
        if (talent == null) {
            throw new BusinessException("Không tìm thấy leader TALENT cho milestone");
        }

        // Tạo đối tượng LeaderDisbursementInfo cho MENTOR
        LeaderDisbursementInfo mentorObj = LeaderDisbursementInfo.builder()
                .userId(mentor.getUserId())
                .fullName(mentor.getFullName())
                .email(mentor.getEmail())
                .avatarUrl(mentor.getAvatarUrl())
                .isLeader(true)
                .roleInProject(Role.MENTOR.toString())
                .amount(mentorShare)
                .build();

        // Tạo đối tượng LeaderDisbursementInfo cho TALENT
        LeaderDisbursementInfo talentObj = LeaderDisbursementInfo.builder()
                .userId(talent.getUserId())
                .fullName(talent.getFullName())
                .email(talent.getEmail())
                .avatarUrl(talent.getAvatarUrl())
                .isLeader(true)
                .roleInProject(Role.TALENT.toString())
                .amount(talentShare)
                .build();

        // Trả về DisbursementCalculationResponse
        return DisbursementCalculationResponse.builder()
                .milestoneId(milestoneId.toString())
                .totalAmount(totalAmount)
                .systemFee(systemFee)
                .mentorLeader(mentorObj)
                .talentLeader(talentObj)
                .status(Status.PENDING.toString())
                .build();
    }

    @Override
    public ApiResponse<Map<String, String>> calculateDisbursement(CreateDisbursementRequest request) {
        SystemConfig systemConfig = systemConfigRepository
                .findByName("fee-distribution")
                .orElseThrow(() -> new BusinessException("Không tìm thấy cấu hình fee-distribution"));

        Map<String, Object> properties = systemConfig.getProperties();
        BigDecimal talentShareRate = new BigDecimal(properties.get("talentShareRate").toString());
        BigDecimal mentorShareRate = new BigDecimal(properties.get("mentorShareRate").toString());
        BigDecimal systemFeeRate = new BigDecimal(properties.get("systemFeeRate").toString());

        BigDecimal systemFee = request.getTotalAmount().multiply(systemFeeRate);
        BigDecimal mentorShare = request.getTotalAmount().multiply(mentorShareRate);
        BigDecimal talentShare = request.getTotalAmount().multiply(talentShareRate);

        Disbursement disbursement = Disbursement
                .builder()
                .projectId(request.getProjectId())
                .milestoneId(request.getMilestoneId())
                .totalAmount(request.getTotalAmount())
                .systemFee(systemFee)
                .mentorLeaderId(request.getMentorLeaderId())
                .mentorAmount(mentorShare)
                .talentLeaderId(request.getTalentLeaderId())
                .talentAmount(talentShare)
                .status(Status.PENDING.toString())
                .build();

        disbursementRepository.save(disbursement);

        return ApiResponse.success(Map.of("disbursementId", disbursement.getId().toString()));
    }

    @Override
    public DisbursementCalculationResponse executeDisbursement(UUID disbursementId) {
        Disbursement disbursement = disbursementRepository.findById(disbursementId)
                .orElseThrow(() -> new BusinessException("Không tìm thấy kết quả để thực thi phân bổ tiền với id: " + disbursementId));

        if (!Status.PENDING.toString().equals(disbursement.getStatus())) {
            throw new BusinessException("Tiền đã được phân bổ về ví các team leader");
        }

        ProjectServiceGrpc.ProjectServiceBlockingStub projectServiceBlockingStub = ProjectServiceGrpc.newBlockingStub(projectServiceChannel);
        GetLeaderInfoResponse leaderInfoResponse = projectServiceBlockingStub.getLeaderInfo(GetLeaderInfoRequest
                .newBuilder()
                .setMilestoneId(disbursement.getMilestoneId().toString())
                .build());

        Wallet systemWallet = walletRepository.findByOwnerType("MILESTONE").orElseThrow();
        // Minus talent amount + mentor amount + system fee from milestone wallet
        BigDecimal totalOut = disbursement.getMentorAmount().add(disbursement.getTalentAmount()).add(disbursement.getSystemFee());
        if (systemWallet.getBalance().compareTo(totalOut) < 0) {
            throw new BusinessException("Số dư ví hệ thống không đủ để thực hiện giải ngân");
        }
        systemWallet.setBalance(systemWallet.getBalance().subtract(totalOut));

        // Add system fee to system wallet
        Wallet systemFeeWallet = getOrCreateSystemWallet();
        systemFeeWallet.setBalance(systemFeeWallet.getBalance().add(disbursement.getSystemFee()));

        createTransaction(systemFeeWallet, disbursement.getSystemFee(), "CREDIT", "DISBURSEMENT_IN",
                "Nhận phí hệ thống từ giải ngân Milestone", disbursement.getId(), "DISBURSEMENT",
                disbursement.getProjectId(), disbursement.getMilestoneId());

        // Add mentor amount to team mentor wallet
        Wallet teamMentorWallet = getOrCreateTeamWallet(disbursement.getMilestoneId(), "TEAM_MENTOR");
        teamMentorWallet.setBalance(teamMentorWallet.getBalance().add(disbursement.getMentorAmount()));

        createTransaction(teamMentorWallet, disbursement.getMentorAmount(), "CREDIT", "DISBURSEMENT_IN",
                "Nhận tiền giải ngân Milestone", disbursement.getId(), "DISBURSEMENT",
                disbursement.getProjectId(), disbursement.getMilestoneId());

        // Add talent amount to team talent wallet
        Wallet teamTalentWallet = getOrCreateTeamWallet(disbursement.getMilestoneId(), "TEAM_TALENT");
        teamTalentWallet.setBalance(teamTalentWallet.getBalance().add(disbursement.getTalentAmount()));

        createTransaction(teamTalentWallet, disbursement.getTalentAmount(), "CREDIT", "DISBURSEMENT_IN",
                "Nhận tiền giải ngân Milestone", disbursement.getId(), "DISBURSEMENT",
                disbursement.getProjectId(), disbursement.getMilestoneId());

        disbursement.setStatus(Status.COMPLETED.toString());
        disbursementRepository.save(disbursement);
        walletRepository.save(systemWallet);
        walletRepository.save(teamMentorWallet);
        walletRepository.save(teamTalentWallet);

        LeaderInfo mentor = null;
        LeaderInfo talent = null;

        for (LeaderInfo leader : leaderInfoResponse.getLeadersList()) {
            if (Role.MENTOR.toString().equals(leader.getRoleInProject())) {
                mentor = leader;
            } else if (Role.TALENT.toString().equals(leader.getRoleInProject())) {
                talent = leader;
            }
        }

        // Nếu không tìm thấy mentor hoặc talent thì throw exception
        if (mentor == null) {
            throw new BusinessException("Không tìm thấy leader MENTOR cho milestone");
        }
        if (talent == null) {
            throw new BusinessException("Không tìm thấy leader TALENT cho milestone");
        }

        // Tạo đối tượng LeaderDisbursementInfo cho MENTOR
        LeaderDisbursementInfo mentorObj = LeaderDisbursementInfo.builder()
                .userId(mentor.getUserId())
                .fullName(mentor.getFullName())
                .email(mentor.getEmail())
                .avatarUrl(mentor.getAvatarUrl())
                .isLeader(true)
                .roleInProject(Role.MENTOR.toString())
                .amount(disbursement.getMentorAmount())
                .build();

        // Tạo đối tượng LeaderDisbursementInfo cho TALENT
        LeaderDisbursementInfo talentObj = LeaderDisbursementInfo.builder()
                .userId(talent.getUserId())
                .fullName(talent.getFullName())
                .email(talent.getEmail())
                .avatarUrl(talent.getAvatarUrl())
                .isLeader(true)
                .roleInProject(Role.TALENT.toString())
                .amount(disbursement.getTalentAmount())
                .build();

        return DisbursementCalculationResponse.builder()
                .milestoneId(disbursement.getMilestoneId().toString())
                .totalAmount(disbursement.getTotalAmount())
                .systemFee(disbursement.getSystemFee())
                .mentorLeader(mentorObj)
                .talentLeader(talentObj)
                .status(disbursement.getStatus())
                .build();
    }

    @Override
    @Transactional
    public ApiResponse<Void> processMilestoneDisbursement(
            UUID milestoneId,
            MilestoneDisbursementRequest request
    ) {
        // 1. Lấy Disbursement
        Disbursement disbursement = disbursementRepository.findByMilestoneId(milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy Disbursement cho milestoneId: " + milestoneId));

        if (!Status.COMPLETED.toString().equals(disbursement.getStatus())) {
            throw new BusinessException(
                    "Milestone chưa hoàn tất thanh toán: " + milestoneId);
        }

        // 2. Thu thập userIds
        Set<UUID> userIds = request.getDisbursements()
                .stream()
                .map(MilestoneDisbursement::getUserId)
                .collect(Collectors.toSet());

        boolean isMentorFlow = userIds.contains(disbursement.getMentorLeaderId());
        boolean isTalentFlow = userIds.contains(disbursement.getTalentLeaderId());

        if (isMentorFlow && isTalentFlow) {
            throw new BusinessException("Không thể chia tiền cho cả Mentor và Talent trong cùng 1 request");
        }

        if (!isMentorFlow && !isTalentFlow) {
            throw new BusinessException("Request không thuộc Mentor Team hoặc Talent Team");
        }

        if (isMentorFlow) {

        }
        // 3. Xác định leader & số tiền khả dụng
        UUID leaderId = isMentorFlow
                ? disbursement.getMentorLeaderId()
                : disbursement.getTalentLeaderId();

        BigDecimal totalAvailable = isMentorFlow
                ? disbursement.getMentorAmount()
                : disbursement.getTalentAmount();

        // 4. Tổng tiền request
        BigDecimal totalRequested = request.getDisbursements()
                .stream()
                .map(MilestoneDisbursement::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalRequested.compareTo(totalAvailable) > 0) {
            throw new BusinessException("Số tiền leader nhận được từ milestone không đủ để phân bổ cho các thành viên khác.");
        }

        // 5. Lấy ví leader
        Wallet leaderWallet = walletRepository.findByOwnerId(leaderId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Không tìm thấy ví của leader: " + leaderId));

        // 6. Thực hiện disbursement
        for (MilestoneDisbursement item : request.getDisbursements()) {
            UUID memberId = item.getUserId();
            BigDecimal amount = item.getAmount();

            if (memberId.equals(leaderId)) {
                throw new BusinessException("Leader không thể tự phân bổ tiền cho chính mình");
            }

            // ---- DEBIT leader ----
            leaderWallet.setBalance(leaderWallet.getBalance().subtract(amount));
            walletRepository.save(leaderWallet);

            Transaction leaderTx = Transaction.builder()
                    .wallet(leaderWallet)
                    .amount(amount)
                    .type(PaymentConstant.TRANSACTION_TYPE_DISBURSEMENT)
                    .direction(PaymentConstant.DEBIT)
                    .status(Status.COMPLETED.toString())
                    .milestoneId(milestoneId)
                    .refId(disbursement.getId())
                    .refType(PaymentConstant.REF_TYPE_DISBURSEMENT)
                    .relatedUserId(memberId)
                    .balanceAfter(leaderWallet.getBalance())
                    .build();

            transactionRepository.save(leaderTx);

            // ---- CREDIT member ----
            Wallet memberWallet = walletRepository.findByOwnerId(memberId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Không tìm thấy ví member: " + memberId));

            memberWallet.setBalance(memberWallet.getBalance().add(amount));
            walletRepository.save(memberWallet);

            Transaction memberTx = Transaction.builder()
                    .wallet(memberWallet)
                    .amount(amount)
                    .type(PaymentConstant.TRANSACTION_TYPE_DISBURSEMENT)
                    .direction(PaymentConstant.CREDIT)
                    .status(Status.COMPLETED.toString())
                    .milestoneId(milestoneId)
                    .refId(disbursement.getId())
                    .refType(PaymentConstant.REF_TYPE_DISBURSEMENT)
                    .relatedUserId(leaderId)
                    .balanceAfter(memberWallet.getBalance())
                    .build();

            transactionRepository.save(memberTx);

            log.info("Leader {} disbursed {} to member {}", leaderId, amount, memberId);
        }

        return ApiResponse.success("Phân bổ tiền thành công", null);
    }

    @Override
    public ApiResponse<DisbursementResponse> getByMilestoneId(UUID milestoneId) {

        Disbursement disbursement = disbursementRepository
                .findByMilestoneId(milestoneId)
                .orElseThrow(() ->
                        new BusinessException("Chưa có thông tin giải ngân cho milestone: " + milestoneId)
                );

        DisbursementResponse response = DisbursementResponse.builder()
                .disbursementId(disbursement.getId())
                .milestoneId(disbursement.getMilestoneId())
                .projectId(disbursement.getProjectId())
                .totalAmount(disbursement.getTotalAmount())
                .systemFee(disbursement.getSystemFee())
                .mentorAmount(disbursement.getMentorAmount())
                .mentorLeaderId(disbursement.getMentorLeaderId())
                .talentAmount(disbursement.getTalentAmount())
                .talentLeaderId(disbursement.getTalentLeaderId())
                .status(disbursement.getStatus())
                .updatedAt(disbursement.getUpdatedAt())
                .build();

        return ApiResponse.success(response);
    }

    private Wallet getOrCreateTeamWallet(UUID ownerId, String ownerType) {
        return walletRepository.findByOwnerIdAndOwnerType(ownerId, ownerType)
                .orElseGet(() -> {
                    Wallet newWallet = new Wallet();
                    newWallet.setOwnerId(ownerId);
                    newWallet.setOwnerType(ownerType);
                    newWallet.setBalance(BigDecimal.ZERO);
                    newWallet.setHeldBalance(BigDecimal.ZERO);
                    newWallet.setCurrency("VND");
                    newWallet.setStatus(Status.ACTIVE.toString());
                    newWallet.setBankInfos(new ArrayList<>());
                    return walletRepository.save(newWallet);
                });
    }

    private Wallet getOrCreateWallet(UUID userId) {
        return walletRepository.findByOwnerId(userId)
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setOwnerId(userId);
                    w.setOwnerType(Role.COMPANY.toString()); // Mặc định là COMPANY nếu là người thanh toán
                    w.setBalance(BigDecimal.ZERO);
                    w.setHeldBalance(BigDecimal.ZERO);
                    w.setCurrency("VND");
                    w.setStatus(Status.ACTIVE.toString());
                    return walletRepository.save(w);
                });
    }

    private Wallet getOrCreateSystemWallet() {
        return walletRepository.findByOwnerType("SYSTEM")
                .orElseGet(() -> {
                    Wallet w = new Wallet();
                    w.setOwnerType("SYSTEM");
                    w.setOwnerId(UUID.randomUUID()); // ID ảo
                    w.setBalance(BigDecimal.ZERO);
                    w.setHeldBalance(BigDecimal.ZERO);
                    w.setCurrency("VND");
                    w.setStatus(Status.ACTIVE.toString());
                    return walletRepository.save(w);
                });
    }

    private void createTransaction(Wallet wallet, BigDecimal amount, String type, String direction,
                                   String desc, UUID refId, String refType,
                                   UUID projectId, UUID milestoneId) {
        Transaction tx = Transaction.builder()
                .wallet(wallet)
                .amount(amount)
                .type(type)
                .direction(direction)
                .description(desc)
                .refId(refId).refType(refType)
                .projectId(projectId).milestoneId(milestoneId)
                .status(Status.SUCCESS.toString())
                .balanceAfter(wallet.getBalance())
                .build();
        transactionRepository.save(tx);
    }
}
